package tools.aqua.dse.testgeneration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum VerifierValueType {
    BOOLEAN(Boolean.class, "Verifier::nondetBoolean", "%b"),
    BYTE(Byte.class, "Verifier::nondetByte", "(byte)%d"),
    CHARACTER(Character.class, "Verifier::nondetChar", "'%c'"),
    SHORT(Short.class, "Verifier::nondetShort", "(short)%d"),
    INTEGER(Integer.class, "Verifier::nondetInt", "%d"),
    LONG(Long.class, "Verifier::nondetLong", "%dL"),
    FLOAT(Float.class, "Verifier::nondetFloat", "%fF"),
    DOUBLE(Double.class, "Verifier::nondetDouble", "%fD"),
    STRING(String.class, "Verifier::nondetString", "\"%s\"");

    private final Class<?> type;
    private final String nondetMethodCall;
    private final String valueFormat;

    private static final String ERROR_MESSAGE_UNSUPPORTED_VALUE = "Unsupported value type: %s";
    private static final Map<Class<?>, VerifierValueType> LOOKUP =
            Arrays
                    .stream(values())
                    .collect(Collectors.toMap(v -> v.type, Function.identity()));

    public static VerifierValueType from(@NotNull final Class<?> clazz) {
        return Optional
                .ofNullable(LOOKUP.get(clazz))
                .orElseThrow(() ->
                                     new IllegalArgumentException(
                                             String.format(ERROR_MESSAGE_UNSUPPORTED_VALUE, clazz)));
    }
}