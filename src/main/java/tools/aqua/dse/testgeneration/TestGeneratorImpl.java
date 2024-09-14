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
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

//TODO: einige logs auf debug
@Slf4j
public class TestGeneratorImpl implements TestGenerator {

    public static final String TESTS_DIRECTORY = "generated-tests/";
    private static final int METHOD_NAME_VARIABLE_LIMIT = 3;
    private static final String PACKAGE_DECLARATION = "com.example";

    private final STGroup templates;
    private final Random random = new Random();

    public TestGeneratorImpl() {
        this.templates = new TemplateLoader().getTemplates();
    }

    @Override
    public void generateTestsBasedOnValuations(final @NotNull List<Valuation> valuations) {
        log.info("Generating tests for valuations-------");

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

        log.info("Generated Code:%n{}", generatedCode);

        CompilationUnit cu;
        try {
            cu = StaticJavaParser.parse(generatedCode);
        } catch (ParseProblemException e) {
            log.error("Error parsing generated code: {}", e.getMessage());
            return;
        }

        final DefaultPrinterConfiguration printerConfiguration = new DefaultPrinterConfiguration();
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter(printerConfiguration);

        try {
            final Path testsDir = Paths.get(TESTS_DIRECTORY);
            if (!Files.exists(testsDir)) {
                Files.createDirectory(testsDir);
            }
            Files.write(
                    Paths.get(TESTS_DIRECTORY + className + ".java"), prettyPrinter
                            .print(cu)
                            .getBytes(StandardCharsets.UTF_8));
            log.info("Successfully saved file {}", className);
        } catch (IOException e) {
            log.error("Could not print file {}", className, e);
        }

        log.info("DONE-----------------------------");
    }


    private @NotNull String generateClassName(@NotNull final List<Valuation> valuations) {
        return "GeneratedTest_" + Math.abs(valuations.hashCode()); //TODO: better name
    }

    @NotNull
    private String generateTestMethod(@NotNull final Valuation valuation) {
        final String methodName = generateMethodName(valuation);
        log.info("MethodName: {}", methodName);

        ST testMethodTemplate = templates.getInstanceOf("testMethod");
        testMethodTemplate.add("methodName", methodName);
        BlockStmt body = new BlockStmt();
        generateMethodBody(valuation, body);

        StringBuilder bodyBuilder = new StringBuilder();
        NodeList<Statement> statements = body.getStatements();
        for (int i = 0; i < statements.size(); i++) {
            final Statement statement = statements.get(i);
            bodyBuilder.append(statement.toString());
            if (i < statements.size() - 1) {
                bodyBuilder.append("\n");
            }
        }
        testMethodTemplate.add("body", bodyBuilder.toString());

        return testMethodTemplate.render();
    }

    private @NotNull String generateMethodName(@NotNull final Valuation valuation) { //TODO: better approach?
        final StringBuilder methodNameBuilder = new StringBuilder("test");
        final Collection<ValuationEntry<?>> valuationEntries = valuation.entries();
        valuationEntries
                .stream()
                .limit(METHOD_NAME_VARIABLE_LIMIT)
                .forEach(valuationEntry -> methodNameBuilder
                        .append(valuationEntry
                                        .getVariable()
                                        .getName())
                        .append("eq")
                        .append(valuationEntry.getValue()));
        if (valuationEntries.size() > METHOD_NAME_VARIABLE_LIMIT) {
            methodNameBuilder
                    .append("_")
                    .append(random.nextInt());
        }
        return methodNameBuilder.toString();
    }

    private void generateMethodBody(
            @NotNull final Valuation valuation,
            @NotNull final BlockStmt body
    ) {
        log.info("Creating method body for valuation {}", valuation);
        final Collection<ValuationEntry<?>> valuationEntries = valuation.entries();

        final String originalClassCall = "Example4.main(new String[]{});";
        if (valuationEntries.isEmpty()) {
            body.addStatement(StaticJavaParser.parseStatement(originalClassCall));
            return;
        }

        TryStmt tryStmt = new TryStmt();
        VariableDeclarationExpr resource = new VariableDeclarationExpr(
                new VariableDeclarator(StaticJavaParser.parseClassOrInterfaceType("MockedStatic<Verifier>"),
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
            @NotNull final Collection<ValuationEntry<?>> valuationEntries,
            @NotNull final BlockStmt tryBlock
    ) {
        for (final ValuationEntry<?> entry : valuationEntries) {
            final String methodCall = getVerifierMockingForValueType(entry.getValue());
            tryBlock.addStatement(StaticJavaParser.parseStatement(methodCall));
        }
    }

    private @NotNull String getVerifierMockingForValueType(@NotNull final Object value) {
        String verifierCall;
        String valuePlaceholderString;

        if (value instanceof Boolean) {
            verifierCall = "Verifier.nondetBoolean()";
            valuePlaceholderString = "%b";
        } else if (value instanceof Byte) {
            verifierCall = "Verifier.nondetByte()";
            valuePlaceholderString = "(byte)%d";
        } else if (value instanceof Character) {
            verifierCall = "Verifier.nondetChar()";
            valuePlaceholderString = "'%c'";
        } else if (value instanceof Short) {
            verifierCall = "Verifier.nondetShort()";
            valuePlaceholderString = "(short)%d";
        } else if (value instanceof Integer) {
            verifierCall = "Verifier.nondetInt()";
            valuePlaceholderString = "%d";
        } else if (value instanceof Long) {
            verifierCall = "Verifier.nondetLong()";
            valuePlaceholderString = "%dL";
        } else if (value instanceof Float) {
            verifierCall = "Verifier.nondetFloat()";
            valuePlaceholderString = "%f";
        } else if (value instanceof Double) {
            verifierCall = "Verifier.nondetDouble()";
            valuePlaceholderString = "%f";
        } else if (value instanceof String) {
            verifierCall = "Verifier.nondetString()";
            valuePlaceholderString = "\"%s\"";
        } else {
            throw new IllegalArgumentException("Unsupported value type: " + value.getClass());
        }

        final String callWithValuePlaceholder = String.format(
                "mockedVerifier.when(() -> %s).thenReturn(%s);", verifierCall, valuePlaceholderString);
        return String.format(callWithValuePlaceholder, value);
    }
}
