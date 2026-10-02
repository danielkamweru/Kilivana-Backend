package com.kilivana.backend.common.enums;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Matches an incoming value against an enum, ignoring case and word separators.
 *
 * <p>Clients write {@code "on-delivery"} and {@code "pick-up"}; the stored values are
 * {@code ON_DELIVERY} and {@code PICKUP}. Comparing the raw strings would reject both and push
 * each client into its own mapping, which is how a column ends up holding two spellings of one
 * state — the driver vehicle_type column already held both "Truck" and "TRUCK".
 *
 * <p>Separators are stripped rather than translated, so one rule covers hyphens, underscores and
 * spaces without each enum having to know which client used which.
 */
final class EnumNameMatcher {

    private EnumNameMatcher() {
    }

    /** Returns the constant matching {@code value}, or throws naming what was accepted. */
    static <E extends Enum<E>> E match(Class<E> type, String value, String expected) {
        if (value == null) {
            return null;
        }
        String normalized = normalize(value);
        for (E constant : type.getEnumConstants()) {
            if (normalize(constant.name()).equals(normalized)) {
                return constant;
            }
        }
        throw new IllegalArgumentException(
                "Unknown " + expected + " '" + value + "'. Expected one of " + accepted(type) + ".");
    }

    private static String accepted(Class<?> type) {
        return Arrays.stream(type.getEnumConstants())
                .map(constant -> ((Enum<?>) constant).name())
                .collect(Collectors.joining(", "));
    }

    private static String normalize(String value) {
        StringBuilder cleaned = new StringBuilder(value.length());
        for (char character : value.trim().toLowerCase(Locale.ROOT).toCharArray()) {
            if (Character.isLetterOrDigit(character)) {
                cleaned.append(character);
            }
        }
        return cleaned.toString();
    }
}