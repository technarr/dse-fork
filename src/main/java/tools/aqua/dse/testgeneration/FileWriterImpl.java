package tools.aqua.dse.testgeneration;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.DefaultPrettyPrinter;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

@Slf4j
public class FileWriterImpl implements FileWriter {
    // Use a relative path for the tests directory.
    private static final String RELATIVE_TESTS_DIRECTORY = "coverage-report/src/test/java";
    private static final String JAVA_FILE_ENDING = ".java";

    private final Logger logger = Logger.getLogger("jdart");

    @Override
    public void saveTestClass(
            @NotNull final String className,
            @NotNull final CompilationUnit compilationUnit
    ) {
        requireNonNull(className);
        requireNonNull(compilationUnit);

        final DefaultPrinterConfiguration printerConfiguration = new DefaultPrinterConfiguration();
        final DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter(printerConfiguration);

        // Use the "project.root" system property if available; otherwise fall back to user.dir.
        String baseDir = System.getProperty("project.root", System.getProperty("user.dir"));
        logger.info("Base directory for saving test class: " + baseDir);

        // Build an absolute path by combining the base directory with the relative tests directory.
        final Path testsDir = Paths.get(baseDir, RELATIVE_TESTS_DIRECTORY);
        // Convert package dots to directory separators and append the .java extension.
        final Path filePath = testsDir.resolve(className.replace('.', '/') + JAVA_FILE_ENDING);

        try {
            // Create all missing parent directories.
            Files.createDirectories(filePath.getParent());
            System.out.printf("TRYING TO WRITE FILE: %s%n", filePath.toAbsolutePath());
            Files.write(
                    filePath,
                    prettyPrinter.print(compilationUnit).getBytes(StandardCharsets.UTF_8)
            );
            logger.info("Successfully saved file: " + filePath.toAbsolutePath().toString());
            System.out.printf("Successfully saved file: %s%n", filePath.toAbsolutePath());
        } catch (IOException e) {
            logger.severe("Could not print file: " + filePath.toAbsolutePath());
            e.printStackTrace();
        }
    }
}
