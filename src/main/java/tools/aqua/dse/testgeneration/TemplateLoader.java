package tools.aqua.dse.testgeneration;

import org.jetbrains.annotations.NotNull;
import org.stringtemplate.v4.STGroup;

public interface TemplateLoader {
    STGroup getTemplates();

    STGroup getTemplates(@NotNull final String templateFilePath);
}
