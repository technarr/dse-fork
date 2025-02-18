package tools.aqua.dse.testgeneration;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;
import gov.nasa.jpf.constraints.api.ValuationEntry;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Slf4j
public class VerifierMockGeneratorImpl implements VerifierMockGenerator {

    public static final String MOCKITO_MOCK_STATIC_CALL = "Mockito.mockStatic(Verifier.class)";
    public static final String MOCKED_STATIC_TYPE = "MockedStatic<Verifier>";
    public static final String VAR_NAME_MOCKED_VERIFIER = "mockedVerifier";
    public static final String MOCKED_VERIFIER_WHEN_THEN_RETURN_TEMPLATE = "mockedVerifier.when(%s).thenReturn(%s);";

    public static final String DOUBLE_NAN = "Double.NaN";
    public static final String FLOAT_NAN = "Float.NaN";
    public static final String DOUBLE_POSITIVE_INFINITY = "Double.POSITIVE_INFINITY";
    public static final String DOUBLE_NEGATIVE_INFINITY = "Double.NEGATIVE_INFINITY";
    public static final String FLOAT_POSITIVE_INFINITY = "Float.POSITIVE_INFINITY";
    public static final String FLOAT_NEGATIVE_INFINITY = "Float.NEGATIVE_INFINITY";
    public static final String STRING_PLACEHOLDER = "%s";

    @Override
    public @NotNull TryStmt createTryBlock(
            @NotNull final List<ValuationEntry<?>> valuationEntries,
            @NotNull final String originalClassCall
    ) {
        final TryStmt tryStmt = new TryStmt();
        tryStmt.setResources(NodeList.nodeList(
                new VariableDeclarationExpr(new VariableDeclarator(
                        StaticJavaParser.parseClassOrInterfaceType(MOCKED_STATIC_TYPE),
                        VAR_NAME_MOCKED_VERIFIER,
                        StaticJavaParser.parseExpression(MOCKITO_MOCK_STATIC_CALL)
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

        final Map<Class<?>, List<ValuationEntry<?>>> entriesByClass = valuationEntries
                .stream()
                .collect(Collectors.groupingBy(entry -> entry
                        .getValue()
                        .getClass()));

        entriesByClass.forEach((clazz, entries) -> {
            final List<Object> orderedValues = getOrderedValuesForClass(valuationEntries, clazz);
            final String methodCall = getVerifierMockingForValueType(clazz, orderedValues);
            tryBlock.addStatement(createMockedVerifierStatement(methodCall));
        });
    }

    @Nullable
    private Statement createMockedVerifierStatement(@NotNull final String whenThenCall) {
        //TODO: ST4 etc. nutzen zum Bilden der Calls
        return StaticJavaParser.parseStatement(whenThenCall);
    }


    @NotNull
    private List<Object> getOrderedValuesForClass(
            @NotNull final List<ValuationEntry<?>> valuationEntries,
            @NotNull final Class<?> clazz
    ) {
        return valuationEntries
                .stream()
                .filter(entry -> entry
                        .getValue()
                        .getClass()
                        .equals(clazz))
                .sorted(Comparator.comparingInt(this::extractOrder))
                .map(ValuationEntry::getValue)
                .collect(Collectors.toList());
    }

    private int extractOrder(@NotNull final ValuationEntry<?> entry) {
        final String name = entry
                .getVariable()
                .getName();
        try {
            return Integer.parseInt(name.substring(name.lastIndexOf('_') + 1));
        } catch (final NumberFormatException e) {
            log.warn("Unexpected variable name format: {}", name);
            return Integer.MAX_VALUE;
        }
    }

    //TODO: mit STG arbeiten, nicht mit Strings
    @NotNull
    private String getVerifierMockingForValueType(
            @NotNull final Class<?> clazz,
            @NotNull final List<Object> values
    ) {
        requireNonNull(clazz);
        requireNonNull(values);

        final VerifierValueType valueType = VerifierValueType.from(clazz);
        final List<String> placeholderList = getPlaceholderList(values, valueType.getValueFormat());
        final Object[] valueArray = convertToJavaExpressions(values);
        return String.format(
                MOCKED_VERIFIER_WHEN_THEN_RETURN_TEMPLATE,
                valueType.getNondetMethodCall(),
                String.format(String.join(", ", placeholderList), valueArray)
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
                        return STRING_PLACEHOLDER;
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
    private Object @NotNull [] convertToJavaExpressions(final @NotNull List<Object> values) {
        return values
                .stream()
                .map(this::normalizeFloatingPointValue)
                .toArray();
    }

    @NotNull
    private Object normalizeFloatingPointValue(@NotNull final Object value) {
        if (value instanceof Double) {
            Double dValue = (Double) value;
            if (dValue.isNaN()) {
                return DOUBLE_NAN;
            } else if (dValue.isInfinite()) {
                return dValue > 0 ? DOUBLE_POSITIVE_INFINITY : DOUBLE_NEGATIVE_INFINITY;
            }
        } else if (value instanceof Float) {
            Float fValue = (Float) value;
            if (fValue.isNaN()) {
                return FLOAT_NAN;
            } else if (fValue.isInfinite()) {
                return fValue > 0 ? FLOAT_POSITIVE_INFINITY : FLOAT_NEGATIVE_INFINITY;
            }
        }
        return value;
    }
}
