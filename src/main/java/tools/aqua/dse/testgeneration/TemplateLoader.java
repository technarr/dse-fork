package tools.aqua.dse.testgeneration;

import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.net.URL;

public class TemplateLoader {

    public static final String TEMPLATE_FILE_PATH = "/templates/TestClassTemplate.stg";
    public static final String TEMPLATE_FILE_ENCODING = "UTF-8";
    public static final char DELIMITER_START_CHAR = '<';
    public static final char DELIMITER_STOP_CHAR = '>';

    public STGroup getTemplates() {
        final URL resource = this
                .getClass()
                .getResource(TEMPLATE_FILE_PATH);
        if (resource == null) {
            throw new IllegalArgumentException("Template file not found: " + TEMPLATE_FILE_PATH);
        }
        return new STGroupFile(resource, TEMPLATE_FILE_ENCODING, DELIMITER_START_CHAR, DELIMITER_STOP_CHAR);
    }
}
