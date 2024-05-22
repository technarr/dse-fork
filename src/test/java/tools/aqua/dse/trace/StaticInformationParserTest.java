package tools.aqua.dse.trace;

import gov.nasa.jpf.constraints.smtlibUtility.parser.SMTLIBParserException;
import org.testng.Assert;
import org.testng.annotations.Test;
import tools.aqua.dse.objects.Objects;

import java.io.IOException;

public class StaticInformationParserTest {

    @Test
    public void testStaticInformationParser() throws IOException, SMTLIBParserException {

        String info =
                "class A { A() }" +
                "class C {}" +
                "class B extends A, C { B(), B(II) }";

        Objects o = new Objects(info);
        Assert.assertEquals(o.getClazzes().size(), 4);

    }
}
