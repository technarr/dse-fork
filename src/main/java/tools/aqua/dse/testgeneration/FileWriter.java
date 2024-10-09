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

import static java.util.Objects.requireNonNull;

@Slf4j
public class FileWriter {
    private static final String TESTS_DIRECTORY = "generated-tests/";
    private static final String JAVA_FILE_ENDING = ".java";


    public void saveTestClass(
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
}
