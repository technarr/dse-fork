package tools.aqua.dse.testgeneration;

import org.jetbrains.annotations.NotNull;
import org.stringtemplate.v4.STGroup;

public interface TemplateLoader {
    @NotNull
    STGroup getTemplates();

    @NotNull
    STGroup getTemplates(@NotNull final String templateFilePath);
}
