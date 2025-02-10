package tools.aqua.dse.testgeneration;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

import java.util.List;

@Slf4j
public class TemplateRendererImpl implements TemplateRenderer {

    private static final String TEMPLATE_CLASS_IDENTIFIER = "testClass";
    private static final String TEMPLATE_METHOD_IDENTIFIER = "testMethod";
    private static final String TEMPLATE_PLACEHOLDER_CLASS_NAME = "className";
    private static final String TEMPLATE_PLACEHOLDER_METHODS = "methods";
    private static final String TEMPLATE_PLACEHOLDER_METHOD_NAME = "methodName";
    private static final String TEMPLATE_PLACEHOLDER_METHOD_BODY = "body";

    private final STGroup templates;

    public TemplateRendererImpl() {
        this.templates = new TemplateLoaderImpl().getTemplates();
    }

    @Override
    public @NotNull String renderTestClass(
            @NotNull final String className,
            @NotNull final List<String> methods
    ) {
        final ST testClassTemplate = templates.getInstanceOf(TEMPLATE_CLASS_IDENTIFIER);
        testClassTemplate.add(TEMPLATE_PLACEHOLDER_CLASS_NAME, className);
        testClassTemplate.add(TEMPLATE_PLACEHOLDER_METHODS, methods);
        return testClassTemplate.render();
    }

    @Override
    public @NotNull String renderTestMethod(
            @NotNull final String methodName,
            @NotNull final String bodyString
    ) {
        final ST testMethodTemplate = templates.getInstanceOf(TEMPLATE_METHOD_IDENTIFIER);
        testMethodTemplate.add(TEMPLATE_PLACEHOLDER_METHOD_NAME, methodName);
        testMethodTemplate.add(TEMPLATE_PLACEHOLDER_METHOD_BODY, bodyString);
        return testMethodTemplate.render();
    }
}
