package tools.aqua.dse.testgeneration;

import org.jetbrains.annotations.NotNull;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

import java.util.List;

public class TemplateManager {

    private static final String PACKAGE_DECLARATION = "com.example";
    private static final String TEMPLATE_CLASS_IDENTIFIER = "testClass";
    private static final String TEMPLATE_METHOD_IDENTIFIER = "testMethod";
    private static final String TEMPLATE_PLACEHOLDER_PACKAGE_NAME = "packageName";
    private static final String TEMPLATE_PLACEHOLDER_CLASS_NAME = "className";
    private static final String TEMPLATE_PLACEHOLDER_METHODS = "methods";
    private static final String TEMPLATE_PLACEHOLDER_METHOD_NAME = "methodName";
    private static final String TEMPLATE_PLACEHOLDER_METHOD_BODY = "body";

    private final STGroup templates;

    public TemplateManager() {
        this.templates = new TemplateLoaderImpl().getTemplates();
    }

    @NotNull
    public String renderTestClass(
            @NotNull final String className,
            @NotNull final List<String> methods
    ) {
        final ST testClassTemplate = templates.getInstanceOf(TEMPLATE_CLASS_IDENTIFIER);
        testClassTemplate.add(TEMPLATE_PLACEHOLDER_PACKAGE_NAME, PACKAGE_DECLARATION);
        testClassTemplate.add(TEMPLATE_PLACEHOLDER_CLASS_NAME, className);
        testClassTemplate.add(TEMPLATE_PLACEHOLDER_METHODS, methods);
        return testClassTemplate.render();
    }

    @NotNull
    public String renderTestMethod(
            final String methodName,
            final String bodyString
    ) {
        final ST testMethodTemplate = templates.getInstanceOf(TEMPLATE_METHOD_IDENTIFIER);
        testMethodTemplate.add(TEMPLATE_PLACEHOLDER_METHOD_NAME, methodName);
        testMethodTemplate.add(TEMPLATE_PLACEHOLDER_METHOD_BODY, bodyString);
        return testMethodTemplate.render();
    }
}
