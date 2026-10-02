package com.kilivana.backend.admin.dto;

import java.util.Locale;

/**
 * Converts between a vehicle's payload capacity as an administrator writes it ("5T", "200kg")
 * and the kilogram figure the logistics code does arithmetic on.
 *
 * <p>The kilogram value is the stored one because it is the only one that can be compared and
 * summed. The text form is what the admin panel displays, so it is still accepted on input and
 * handed back on output rather than making every client invent its own rendering.
 */
public final class DriverCapacityFormatter {

    private static final long KILOGRAMS_PER_TONNE = 1000L;

    private DriverCapacityFormatter() {
    }

    /**
     * Parses "5T", "5 t", "2 tonnes", "200kg" or a bare "2000" into kilograms.
     *
     * @return the kilogram figure, or null when the text is blank
     * @throws IllegalArgumentException when the text is a capacity in an unreadable form
     */
    public static Integer toKilograms(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String value = text.trim().toLowerCase(Locale.ROOT).replace(" ", "");
        Long multiplier = 1L;
        String numeric = value;
        // Longest unit first: "kg" has to be tested before "k", and "tonnes" before "t",
        // otherwise the digits of the unit end up inside the number.
        for (Unit unit : Unit.values()) {
            if (value.endsWith(unit.suffix)) {
                multiplier = unit.multiplier;
                numeric = value.substring(0, value.length() - unit.suffix.length());
                break;
            }
        }
        try {
            return Math.toIntExact(Long.parseLong(numeric) * multiplier);
        } catch (NumberFormatException | ArithmeticException ex) {
            throw new IllegalArgumentException(
                    "Vehicle capacity '" + text + "' is not a number of kilograms, tonnes or tons");
        }
    }

    /**
     * Renders kilograms the way an administrator writes them: a whole number of tonnes loses
     * its thousands, anything else keeps the kilogram unit.
     *
     * @return the label, or null when there is no capacity
     */
    public static String format(Integer kilograms) {
        if (kilograms == null) {
            return null;
        }
        if (kilograms > 0 && kilograms % KILOGRAMS_PER_TONNE == 0) {
            return (kilograms / KILOGRAMS_PER_TONNE) + "T";
        }
        return kilograms + "kg";
    }

    private enum Unit {
        KILOGRAMS("kg", 1L),
        KILOGRAM("kilo", 1L),
        TONNES("tonnes", KILOGRAMS_PER_TONNE),
        TONNE("tonne", KILOGRAMS_PER_TONNE),
        TONS("tons", KILOGRAMS_PER_TONNE),
        TON("ton", KILOGRAMS_PER_TONNE),
        TONNE_SHORT("t", KILOGRAMS_PER_TONNE);

        private final String suffix;
        private final long multiplier;

        Unit(String suffix, long multiplier) {
            this.suffix = suffix;
            this.multiplier = multiplier;
        }
    }
}