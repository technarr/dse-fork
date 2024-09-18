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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class TestGeneratorImpl implements TestGenerator {

    public static final String TESTS_DIRECTORY = "generated-tests/";
    private static final String PACKAGE_DECLARATION = "com.example";
    public static final String TEST_METHOD_NAME_PREFIX = "test_valuationNr_";

    private static final Map<Class<?>, String> verifierMethodMap = new HashMap<>();
    static {
        verifierMethodMap.put(Boolean.class, "Verifier::nondetBoolean");
        verifierMethodMap.put(Byte.class, "Verifier::nondetByte");
        verifierMethodMap.put(Character.class, "Verifier::nondetChar");
        verifierMethodMap.put(Short.class, "Verifier::nondetShort");
        verifierMethodMap.put(Integer.class, "Verifier::nondetInt");
        verifierMethodMap.put(Long.class, "Verifier::nondetLong");
        verifierMethodMap.put(Float.class, "Verifier::nondetFloat");
        verifierMethodMap.put(Double.class, "Verifier::nondetDouble");
        verifierMethodMap.put(String.class, "Verifier::nondetString");
    }

    private final STGroup templates;
    private int valuationNo = 0;

    public TestGeneratorImpl() {
        this.templates = new TemplateLoaderImpl().getTemplates();
    }

    @Override
    public void generateTestsBasedOnValuations(final @NotNull List<Valuation> valuations) {
        log.info("Generating test class based on valuations -------");

        final String className = generateClassName(valuations);
        List<String> methods = valuations
                .stream()
                .map(this::generateTestMethod)
                .collect(Collectors.toList());

        ST testClassTemplate = templates.getInstanceOf("testClass");
        testClassTemplate.add("packageName", PACKAGE_DECLARATION);
        testClassTemplate.add("className", className);
        testClassTemplate.add("methods", methods);

        String generatedCode = testClassTemplate.render();

        log.debug(String.format("Generated Code:%n%s", generatedCode));

        CompilationUnit cu;
        try {
            cu = StaticJavaParser.parse(generatedCode);
        } catch (ParseProblemException e) {
            log.error(String.format("Error parsing generated code: %s", generatedCode), e);
            return;
        }

        final DefaultPrinterConfiguration printerConfiguration = new DefaultPrinterConfiguration();
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter(printerConfiguration);

        try {
            final Path testsDir = Paths.get(TESTS_DIRECTORY);
            if (!Files.exists(testsDir)) {
                Files.createDirectory(testsDir);
            } else if (!Files.isDirectory(testsDir)) {
                throw new IOException(testsDir + " exists but is not a directory.");
            }

            Files.write(
                    Paths.get(TESTS_DIRECTORY + className + ".java"),
                    prettyPrinter
                            .print(cu)
                            .getBytes(StandardCharsets.UTF_8)
            );
            log.info("Successfully saved file {}", className);
        } catch (IOException e) {
            log.error("Could not print file {}", className, e);
        }

        log.info("DONE -----------------------------");
    }


    private @NotNull String generateClassName(@NotNull final List<Valuation> valuations) {
        return "GeneratedTest_" + Math.abs(valuations.hashCode()); //TODO: better name
    }

    @NotNull
    private String generateTestMethod(@NotNull final Valuation valuation) {
        final String methodName = generateMethodName();
        log.debug("MethodName: {}", methodName);

        ST testMethodTemplate = templates.getInstanceOf("testMethod");
        testMethodTemplate.add("methodName", methodName);
        BlockStmt body = new BlockStmt();
        generateMethodBody(valuation, body);

        String bodyString = body
                .getStatements()
                .stream()
                .map(Statement::toString)
                .collect(Collectors.joining("\n"));
        testMethodTemplate.add("body", bodyString);

        return testMethodTemplate.render();
    }

    private @NotNull String generateMethodName() { //TODO: better approach?
        return TEST_METHOD_NAME_PREFIX + this.valuationNo++;
    }

    private void generateMethodBody(
            @NotNull final Valuation valuation,
            @NotNull final BlockStmt body
    ) {
        log.debug("Creating method body for valuation {}", valuation);
        final List<ValuationEntry<?>> valuationEntries = new ArrayList<>(valuation.entries());

        final String originalClassCall = "Example4.main(new String[]{});";
        if (valuationEntries.isEmpty()) {
            body.addStatement(StaticJavaParser.parseStatement(originalClassCall));
            return;
        }

        TryStmt tryStmt = new TryStmt();
        VariableDeclarationExpr resource = new VariableDeclarationExpr(
                new VariableDeclarator(
                        StaticJavaParser.parseClassOrInterfaceType("MockedStatic<Verifier>"),
                        "mockedVerifier",
                        StaticJavaParser.parseExpression("Mockito.mockStatic(Verifier.class)")
                ));
        tryStmt.setResources(NodeList.nodeList(resource));
        BlockStmt tryBlock = new BlockStmt();
        addVerifierCalls(valuationEntries, tryBlock);
        tryBlock.addStatement(StaticJavaParser.parseStatement(originalClassCall));
        tryStmt.setTryBlock(tryBlock);
        body.addStatement(tryStmt);
    }


    private void addVerifierCalls(
            @NotNull final List<ValuationEntry<?>> valuationEntries,
            @NotNull final BlockStmt tryBlock
    ) {
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
        // Provided variables are non-ordered, however, their order is important for the mocked verifier call returns
        values.sort(Comparator.comparingInt(v -> {
            String name = valuationEntries
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


    private @NotNull String getVerifierMockingForValueType(
            @NotNull final Class<?> clazz,
            @NotNull final List<Object> values
    ) {
        String verifierCall = verifierMethodMap.get(clazz);
        if (verifierCall == null) {
            throw new IllegalArgumentException("Unsupported value type: " + clazz);
        }

        String valuePlaceholderString = generateValuePlaceholderString(clazz, values);

        return String.format(
                "mockedVerifier.when(%s).thenReturn(%s);",
                verifierCall,
                String.format(
                        String.join(", ", Collections.nCopies(values.size(), valuePlaceholderString)),
                        values.toArray()
                )
        );
    }

    @NotNull
    private String generateValuePlaceholderString(
            @NotNull final Class<?> clazz,
            @NotNull final List<Object> values
    ) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Number of values must be greater than zero.");
        }

        if (clazz == Boolean.class) {
            return "%b";
        } else if (clazz == Byte.class) {
            return "(byte)%d";
        } else if (clazz == Short.class) {
            return "(short)%d";
        } else if (clazz == Integer.class) {
            return "%d";
        } else if (clazz == Character.class) {
            return "'%c'";
        } else if (clazz == Long.class) {
            return "%dL";
        } else if (clazz == Float.class || clazz == Double.class) {
            return "%f";
        } else if (clazz == String.class) {
            return "\"%s\"";
        }


        throw new IllegalArgumentException("Unsupported value type: " + clazz);
    }
}
