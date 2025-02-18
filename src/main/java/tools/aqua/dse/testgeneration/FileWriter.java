package tools.aqua.dse.testgeneration;

import com.github.javaparser.ast.CompilationUnit;
import org.jetbrains.annotations.NotNull;

public interface FileWriter {
    void saveTestClass(
            @NotNull final String className,
            @NotNull final CompilationUnit compilationUnit
    );
}
