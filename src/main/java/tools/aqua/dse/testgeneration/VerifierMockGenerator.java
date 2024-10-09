package tools.aqua.dse.testgeneration;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import gov.nasa.jpf.constraints.api.ValuationEntry;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Slf4j
public class VerifierMockGenerator {
    private static final String ERROR_MESSAGE_UNSUPPORTED_VALUE = "Unsupported value type: %s";

    private static final Map<Class<?>, List<String>> classRelatedStrings = new HashMap<>();

    static {
        classRelatedStrings.put(Boolean.class, Arrays.asList("Verifier::nondetBoolean", "%b"));
        classRelatedStrings.put(Byte.class, Arrays.asList("Verifier::nondetByte", "(byte)%d"));
        classRelatedStrings.put(Character.class, Arrays.asList("Verifier::nondetChar", "'%c'"));
        classRelatedStrings.put(Short.class, Arrays.asList("Verifier::nondetShort", "(short)%d"));
        classRelatedStrings.put(Integer.class, Arrays.asList("Verifier::nondetInt", "%d"));
        classRelatedStrings.put(Long.class, Arrays.asList("Verifier::nondetLong", "%dL"));
        classRelatedStrings.put(Float.class, Arrays.asList("Verifier::nondetFloat", "%fF"));
        classRelatedStrings.put(Double.class, Arrays.asList("Verifier::nondetDouble", "%fD"));
        classRelatedStrings.put(String.class, Arrays.asList("Verifier::nondetString", "\"%s\""));
    }

    @NotNull
    public TryStmt createTryBlock(
            final List<ValuationEntry<?>> valuationEntries,
            @NotNull final String originalClassCall
    ) {
        final TryStmt tryStmt = new TryStmt();
        tryStmt.setResources(NodeList.nodeList(
                new VariableDeclarationExpr(new VariableDeclarator(
                        StaticJavaParser.parseClassOrInterfaceType("MockedStatic<Verifier>"),
                        "mockedVerifier",
                        StaticJavaParser.parseExpression("Mockito.mockStatic(Verifier.class)")
                ))));

        final BlockStmt tryBlock = new BlockStmt();
        tryStmt.setTryBlock(tryBlock);
        this.addVerifierCalls(valuationEntries, tryBlock);
        tryBlock.addStatement(StaticJavaParser.parseStatement(originalClassCall));

        return tryStmt;
    }

    private void addVerifierCalls(
            @NotNull final List<ValuationEntry<?>> valuationEntries,
            @NotNull final BlockStmt tryBlock
    ) {
        requireNonNull(valuationEntries);
        requireNonNull(tryBlock);

        final Map<Class<?>, List<Object>> valueClassToValuesMap = new HashMap<>();
        valuationEntries.forEach(entry -> valueClassToValuesMap
                .computeIfAbsent(entry
                                         .getValue()
                                         .getClass(), k -> new ArrayList<>())
                .add(entry.getValue()));

        valueClassToValuesMap.forEach((clazz, values) -> {
            sortValuesByVariableName(valuationEntries, values);
            final String methodCall = getVerifierMockingForValueType(clazz, values);
            tryBlock.addStatement(StaticJavaParser.parseStatement(methodCall));
        });
    }

    private void sortValuesByVariableName(
            @NotNull final List<ValuationEntry<?>> valuationEntries,
            @NotNull final List<Object> values
    ) {
        requireNonNull(valuationEntries);
        requireNonNull(values);

        // Provided variables are non-ordered, however, their order is important for the mocked verifier call returns
        values.sort(Comparator.comparingInt(v -> {
            final String name = valuationEntries
                    .stream()
                    .filter(entry -> entry
                            .getValue()
                            .equals(v))
                    .findFirst()
                    .map(entry -> entry
                            .getVariable()
                            .getName())
                    .orElse("");
            return Integer.parseInt(name.substring(name.lastIndexOf('_') + 1));
        }));
    }

    @NotNull
    private String getVerifierMockingForValueType(
            @NotNull final Class<?> clazz,
            @NotNull final List<Object> values
    ) {
        requireNonNull(clazz);
        requireNonNull(values);

        final List<String> stringsOfClassList = classRelatedStrings.computeIfAbsent(clazz, key -> {
            throw new IllegalArgumentException(String.format(ERROR_MESSAGE_UNSUPPORTED_VALUE, clazz));
        });

        final String verifierCall = stringsOfClassList.get(0);
        final String valuePlaceholderString = stringsOfClassList.get(1);
        final List<String> placeholderList = getPlaceholderList(values, valuePlaceholderString);
        final Object[] valueArray = replaceInvalidValues(values);
        return String.format(
                "mockedVerifier.when(%s).thenReturn(%s);",
                verifierCall,
                String.format(
                        String.join(", ", placeholderList),
                        valueArray
                )
        );
    }

    @NotNull
    private List<String> getPlaceholderList(
            @NotNull final List<Object> values,
            @NotNull final String classDependentPlaceholderString
    ) {
        return values
                .stream()
                .map(value -> {
                    if (isDoubleOrFloatEdgeCase(value)) {
                        return "%s";
                    }
                    return classDependentPlaceholderString;
                })
                .collect(Collectors.toList());
    }

    private boolean isDoubleOrFloatEdgeCase(@NotNull final Object value) {
        return (value instanceof Double && (((Double) value).isInfinite() || ((Double) value).isNaN())) ||
                (value instanceof Float && (((Float) value).isInfinite() || ((Float) value).isNaN()));
    }

    @NotNull
    private Object @NotNull [] replaceInvalidValues(final @NotNull List<Object> values) {
        return values
                .stream()
                .map(this::replaceIfEdgeCase)
                .toArray();
    }

    @NotNull
    private Object replaceIfEdgeCase(@NotNull final Object value) {
        if (value instanceof Double) {
            Double dValue = (Double) value;
            if (dValue.isNaN()) {
                return "Double.NaN";
            } else if (dValue.isInfinite()) {
                return dValue > 0 ? "Double.POSITIVE_INFINITY" : "Double.NEGATIVE_INFINITY";
            }
        } else if (value instanceof Float) {
            Float fValue = (Float) value;
            if (fValue.isNaN()) {
                return "Float.NaN";
            } else if (fValue.isInfinite()) {
                return fValue > 0 ? "Float.POSITIVE_INFINITY" : "Float.NEGATIVE_INFINITY";
            }
        }
        return value;
    }
}
