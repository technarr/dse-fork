package tools.aqua.dse.testgeneration;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.printer.DefaultPrettyPrinter;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.ValuationEntry;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import tools.aqua.dse.Config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Slf4j
public class TestGeneratorImpl implements TestGenerator {

    private static final String TESTS_DIRECTORY = "generated-tests/";
    private static final String PACKAGE_DECLARATION = "com.example";
    private static final String TEST_METHOD_NAME_PREFIX = "test_valuationNr_";
    private static final String TEST_CLASS_NAME_SUFFIX = "Test";
    private static final String ORIGINAL_CLASS_NAME_FALLBACK = "Example";

    private static final String TEMPLATE_CLASS_IDENTIFIER = "testClass";
    private static final String TEMPLATE_METHOD_IDENTIFIER = "testMethod";
    private static final String TEMPLATE_PLACEHOLDER_PACKAGE_NAME = "packageName";
    private static final String TEMPLATE_PLACEHOLDER_CLASS_NAME = "className";
    private static final String TEMPLATE_PLACEHOLDER_METHODS = "methods";
    private static final String TEMPLATE_PLACEHOLDER_METHOD_NAME = "methodName";
    private static final String TEMPLATE_PLACEHOLDER_METHOD_BODY = "body";

    private static final String STATIC_MAIN_METHOD_CALL = ".main(new String[]{});";
    private static final String JAVA_FILE_ENDING = ".java";
    private static final String ERROR_MESSAGE_UNSUPPORTED_VALUE = "Unsupported value type: %s";

    private static final Map<Class<?>, List<String>> classRelatedStrings = new HashMap<>();

    static {
        classRelatedStrings.put(Boolean.class, Arrays.asList("Verifier::nondetBoolean", "%b"));
        classRelatedStrings.put(Byte.class, Arrays.asList("Verifier::nondetByte", "(byte)%d"));
        classRelatedStrings.put(Character.class, Arrays.asList("Verifier::nondetChar", "'%c'"));
        classRelatedStrings.put(Short.class, Arrays.asList("Verifier::nondetShort", "(short)%d"));
        classRelatedStrings.put(Integer.class, Arrays.asList("Verifier::nondetInt", "%d"));
        classRelatedStrings.put(Long.class, Arrays.asList("Verifier::nondetLong", "%dL"));
        classRelatedStrings.put(Float.class, Arrays.asList("Verifier::nondetFloat", "%f"));
        classRelatedStrings.put(Double.class, Arrays.asList("Verifier::nondetDouble", "%f"));
        classRelatedStrings.put(String.class, Arrays.asList("Verifier::nondetString", "\"%s\""));
    }

    private final STGroup templates;
    private final String originalClassName;
    private long valuationNo = 0;

    public TestGeneratorImpl(
            @NotNull final
            Config config
    ) {
        requireNonNull(config);

        this.templates = new TemplateLoaderImpl().getTemplates();
        this.originalClassName = extractOriginalClassName(config.getExecutorArgs());
    }

    @NotNull
    private String extractOriginalClassName(@NotNull final String executorArgs) {
        requireNonNull(executorArgs);

        final String[] argsArray = executorArgs.split(" ");
        if (argsArray.length == 0) {
            log.debug(
                    "No classname contained in the executorArgs. Using fallback classname '{}'",
                    ORIGINAL_CLASS_NAME_FALLBACK
            );
            return ORIGINAL_CLASS_NAME_FALLBACK;
        }
        return argsArray[argsArray.length - 1];
    }

    @Override
    public void generateTestsBasedOnValuations(@NotNull final List<Valuation> valuations) {
        log.info(
                "Generating test class for '{}{}' based on valuations -------", this.originalClassName,
                JAVA_FILE_ENDING
        );

        requireNonNull(valuations);

        final String className = this.originalClassName + TEST_CLASS_NAME_SUFFIX;
        final List<String> methods = valuations
                .stream()
                .map(this::generateTestMethod)
                .collect(Collectors.toList());

        final ST testClassTemplate = templates.getInstanceOf(TEMPLATE_CLASS_IDENTIFIER);
        testClassTemplate.add(TEMPLATE_PLACEHOLDER_PACKAGE_NAME, PACKAGE_DECLARATION);
        testClassTemplate.add(TEMPLATE_PLACEHOLDER_CLASS_NAME, className);
        testClassTemplate.add(TEMPLATE_PLACEHOLDER_METHODS, methods);

        final String generatedCode = testClassTemplate.render();

        log.debug(String.format("Generated Code:%n%s", generatedCode));

        CompilationUnit compilationUnit;
        try {
            compilationUnit = StaticJavaParser.parse(generatedCode);
        } catch (ParseProblemException e) {
            log.error(String.format("Error parsing generated code: %s", generatedCode), e);
            return;
        }

        saveTestClass(className, compilationUnit);

        log.info("DONE -----------------------------");
    }

    private void saveTestClass(
            @NotNull final String className,
            @NotNull final CompilationUnit compilationUnit
    ) {
        requireNonNull(className);
        requireNonNull(compilationUnit);

        final DefaultPrinterConfiguration printerConfiguration = new DefaultPrinterConfiguration();
        final DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter(printerConfiguration);
        final String fullFilePath = TESTS_DIRECTORY + className + JAVA_FILE_ENDING;
        try {
            final Path testsDir = Paths.get(TESTS_DIRECTORY);
            if (!Files.exists(testsDir)) {
                Files.createDirectory(testsDir);
            } else if (!Files.isDirectory(testsDir)) {
                throw new IOException(testsDir + " exists but is not a directory.");
            }

            Files.write(
                    Paths.get(fullFilePath),
                    prettyPrinter
                            .print(compilationUnit)
                            .getBytes(StandardCharsets.UTF_8)
            );
            log.info("Successfully saved file {}", fullFilePath);
        } catch (IOException e) {
            log.error("Could not print file {}", fullFilePath, e);
        }
    }

    @NotNull
    private String generateTestMethod(@NotNull final Valuation valuation) {
        requireNonNull(valuation);

        final String methodName = TEST_METHOD_NAME_PREFIX + this.valuationNo++;

        log.debug("Generating method '{}'", methodName);

        final ST testMethodTemplate = templates.getInstanceOf(TEMPLATE_METHOD_IDENTIFIER);
        testMethodTemplate.add(TEMPLATE_PLACEHOLDER_METHOD_NAME, methodName);
        final BlockStmt body = new BlockStmt();
        generateMethodBody(valuation, body);

        final String bodyString = body
                .getStatements()
                .stream()
                .map(Statement::toString)
                .collect(Collectors.joining("\n"));
        testMethodTemplate.add(TEMPLATE_PLACEHOLDER_METHOD_BODY, bodyString);

        return testMethodTemplate.render();
    }

    private void generateMethodBody(
            @NotNull final Valuation valuation,
            @NotNull final BlockStmt body
    ) {
        requireNonNull(valuation);
        requireNonNull(body);

        log.debug("Creating method body for valuation '{}'", valuation);
        final List<ValuationEntry<?>> valuationEntries = new ArrayList<>(valuation.entries());

        final String originalClassCall = this.originalClassName + STATIC_MAIN_METHOD_CALL;
        if (valuationEntries.isEmpty()) {
            body.addStatement(StaticJavaParser.parseStatement(originalClassCall));
            return;
        }

        final TryStmt tryStmt = new TryStmt();
        final VariableDeclarationExpr resource = new VariableDeclarationExpr(
                new VariableDeclarator(
                        StaticJavaParser.parseClassOrInterfaceType("MockedStatic<Verifier>"),
                        "mockedVerifier",
                        StaticJavaParser.parseExpression("Mockito.mockStatic(Verifier.class)")
                ));
        tryStmt.setResources(NodeList.nodeList(resource));
        final BlockStmt tryBlock = new BlockStmt();
        addVerifierCalls(valuationEntries, tryBlock);
        tryBlock.addStatement(StaticJavaParser.parseStatement(originalClassCall));
        tryStmt.setTryBlock(tryBlock);
        body.addStatement(tryStmt);
    }


    private void addVerifierCalls(
            @NotNull final List<ValuationEntry<?>> valuationEntries,
            @NotNull final BlockStmt tryBlock
    ) {
        requireNonNull(valuationEntries);
        requireNonNull(tryBlock);

        final Map<Class<?>, List<Object>> valueClassToValuesMap = new HashMap<>();
        valuationEntries.forEach(entry -> valueClassToValuesMap
                .computeIfAbsent(entry
                                         .getValue()
                                         .getClass(), k -> new ArrayList<>())
                .add(entry.getValue()));

        valueClassToValuesMap.forEach((clazz, values) -> {
            sortValuesByVariableName(valuationEntries, values);

            final String methodCall = getVerifierMockingForValueType(clazz, values);
            tryBlock.addStatement(StaticJavaParser.parseStatement(methodCall));
        });
    }

    private void sortValuesByVariableName(
            @NotNull final List<ValuationEntry<?>> valuationEntries,
            @NotNull final List<Object> values
    ) {
        requireNonNull(valuationEntries);
        requireNonNull(values);

        // Provided variables are non-ordered, however, their order is important for the mocked verifier call returns
        values.sort(Comparator.comparingInt(v -> {
            final String name = valuationEntries
                    .stream()
                    .filter(entry -> entry
                            .getValue()
                            .equals(v))
                    .findFirst()
                    .map(entry -> entry
                            .getVariable()
                            .getName())
                    .orElse("");
            return Integer.parseInt(name.substring(name.lastIndexOf('_') + 1));
        }));
    }

    @NotNull
    private String getVerifierMockingForValueType(
            @NotNull final Class<?> clazz,
            @NotNull final List<Object> values
    ) {
        requireNonNull(clazz);
        requireNonNull(values);


        final List<String> stringsOfClassList = classRelatedStrings.computeIfAbsent(clazz, key -> {
            throw new IllegalArgumentException(String.format(ERROR_MESSAGE_UNSUPPORTED_VALUE, clazz));
        });

        final String verifierCall = stringsOfClassList
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format(ERROR_MESSAGE_UNSUPPORTED_VALUE, clazz)));

        String valuePlaceholderString;
        if (stringsOfClassList.size() > 1) {
            valuePlaceholderString = stringsOfClassList.get(1);
        } else {
            throw new IllegalArgumentException(String.format(ERROR_MESSAGE_UNSUPPORTED_VALUE, clazz));
        }

        return String.format(
                "mockedVerifier.when(%s).thenReturn(%s);",
                verifierCall,
                String.format(
                        String.join(", ", Collections.nCopies(values.size(), valuePlaceholderString)),
                        values.toArray()
                )
        );
    }
}
