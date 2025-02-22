package tools.aqua.dse.testgeneration;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.net.URL;

@Slf4j
public class TemplateLoaderImpl implements TemplateLoader {

    public static final String DEFAULT_TEMPLATE_FILE_PATH = "/templates/TestClassTemplate.stg";
    public static final String TEMPLATE_FILE_ENCODING = "UTF-8";
    public static final char DELIMITER_START_CHAR = '<';
    public static final char DELIMITER_STOP_CHAR = '>';

    @NotNull
    public STGroup getTemplates() {
        return getTemplates(DEFAULT_TEMPLATE_FILE_PATH);
    }

    @NotNull
    public STGroup getTemplates(@NotNull final String templateFilePath) {
        final URL resource = this
                .getClass()
                .getResource(templateFilePath);
        if (resource == null) {
            throw new IllegalArgumentException("Template file not found: " + templateFilePath);
        }
        return new STGroupFile(resource, TEMPLATE_FILE_ENCODING, DELIMITER_START_CHAR, DELIMITER_STOP_CHAR);
    }
}
