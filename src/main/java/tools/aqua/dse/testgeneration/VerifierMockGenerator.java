package tools.aqua.dse.testgeneration;

import com.github.javaparser.ast.stmt.TryStmt;
import gov.nasa.jpf.constraints.api.ValuationEntry;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface VerifierMockGenerator {
    @NotNull TryStmt createTryBlock(
            @NotNull final List<ValuationEntry<?>> valuationEntries,
            @NotNull final String originalClassCall
    );
}
