package tools.aqua.dse.testgeneration;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.ValuationEntry;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import tools.aqua.dse.Config;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Slf4j
public class TestGeneratorImpl implements TestGenerator {

    private static final String TEST_METHOD_NAME_PREFIX = "test_valuationNr_";
    private static final String TEST_CLASS_NAME_SUFFIX = "Test";
    private static final String ORIGINAL_CLASS_NAME_FALLBACK = "Example";

    private static final String STATIC_MAIN_METHOD_CALL = ".main(new String[]{});";
    private static final String JAVA_FILE_ENDING = ".java";

    private long valuationNo = 0;
    private final String originalClassName;
    private final FileWriter fileWriter;
    private final TemplateManager templateManager;
    private final VerifierMockGenerator verifierMockGenerator;

    public TestGeneratorImpl(
            @NotNull final Config config
    ) {
        requireNonNull(config);

        this.originalClassName = extractOriginalClassName(config.getExecutorArgs());
        this.templateManager = new TemplateManager();
        this.fileWriter = new FileWriter();
        this.verifierMockGenerator = new VerifierMockGenerator();
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
        log.info("Generating test class for '{}{}' based on valuations -------", this.originalClassName,
                 JAVA_FILE_ENDING
        ); //TODO: zurück auf debug

        requireNonNull(valuations);

        final String className = this.originalClassName + TEST_CLASS_NAME_SUFFIX;
        final List<String> methods = valuations
                .stream()
                .map(this::generateTestMethod)
                .collect(Collectors.toList());

        final String generatedCode = this.templateManager.renderTestClass(className, methods);
        log.debug(String.format("Generated Code:%n%s", generatedCode));

        CompilationUnit compilationUnit;
        try {
            compilationUnit = StaticJavaParser.parse(generatedCode);
        } catch (ParseProblemException e) {
            log.error(String.format("Error parsing generated code: %s", generatedCode), e);
            return;
        }
        this.fileWriter.saveTestClass(className, compilationUnit);
        log.info("DONE -----------------------------");
    }

    @NotNull
    private String generateTestMethod(@NotNull final Valuation valuation) {
        requireNonNull(valuation);

        final String methodName = TEST_METHOD_NAME_PREFIX + this.valuationNo++;
        log.debug("Generating method '{}'", methodName);

        final BlockStmt body = new BlockStmt();
        generateMethodBody(valuation, body);
        final String bodyString = body
                .getStatements()
                .stream()
                .map(Statement::toString)
                .collect(Collectors.joining("\n"));

        return this.templateManager.renderTestMethod(methodName, bodyString);
    }

    private void generateMethodBody(
            @NotNull final Valuation valuation,
            @NotNull final BlockStmt body
    ) {
        requireNonNull(valuation);
        requireNonNull(body);

        log.info("Creating method body for valuation '{}'", valuation);
        final List<ValuationEntry<?>> valuationEntries = new ArrayList<>(valuation.entries());

        final String originalClassCall = this.originalClassName + STATIC_MAIN_METHOD_CALL;
        if (valuationEntries.isEmpty()) {
            body.addStatement(StaticJavaParser.parseStatement(originalClassCall));
            return;
        }
        body.addStatement(this.verifierMockGenerator.createTryBlock(valuationEntries, originalClassCall));
    }
}
