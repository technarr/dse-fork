package tools.aqua.dse.testgeneration;

import gov.nasa.jpf.constraints.api.Valuation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface TestGenerator {

    void generateTestsBasedOnValuations(@NotNull final List<Valuation> valuations);
}
