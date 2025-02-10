package tools.aqua.dse.testgeneration;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface TemplateRenderer {
    @NotNull String renderTestClass(
            @NotNull final String className,
            @NotNull final List<String> methods
    );

    @NotNull String renderTestMethod(
            @NotNull final String methodName,
            @NotNull final String bodyString
    );
}
