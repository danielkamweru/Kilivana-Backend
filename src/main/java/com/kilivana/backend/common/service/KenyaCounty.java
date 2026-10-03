package com.kilivana.backend.common.service;

import java.util.List;
import java.util.Locale;

/**
 * The 47 Kenyan counties, in the official order the office of the registrar uses.
 *
 * <p>The admin panel offers the county as a dropdown on the farm, farmer and driver forms
 * and cannot hardcode the list itself without drifting from the backend, so it reads this
 * from {@code GET /api/v1/regions}.
 *
 * <p>Stored values are compared case-insensitively and ignore the typographic apostrophe,
 * because "Murang'a" and "Murang’a" are the same county and only one of them can be typed
 * consistently by hand.
 */
public final class KenyaCounty {

    private static final List<String> COUNTIES = List.of(
            "Mombasa", "Kwale", "Kilifi", "Tana River", "Lamu", "Taita-Taveta",
            "Garissa", "Wajir", "Mandera", "Marsabit", "Isiolo", "Meru",
            "Tharaka-Nithi", "Embu", "Kitui", "Machakos", "Makueni",
            "Nyandarua", "Nyeri", "Kirinyaga", "Murang'a", "Kiambu",
            "Turkana", "West Pokot", "Samburu", "Trans Nzoia", "Uasin Gishu",
            "Elgeyo-Marakwet", "Nandi", "Baringo", "Laikipia", "Nakuru", "Narok",
            "Kajiado", "Kericho", "Bomet",
            "Kakamega", "Vihiga", "Bungoma", "Busia",
            "Siaya", "Kisumu", "Homa Bay", "Migori", "Kisii", "Nyamira",
            "Nairobi City");

    private KenyaCounty() {
    }

    public static List<String> all() {
        return COUNTIES;
    }

    /**
     * The stored spelling when the input names a county, otherwise null.
     *
     * <p>"Nairobi" is accepted for "Nairobi City" because that is how people write it and
     * the older rows in the database say exactly that.
     */
    public static String normalise(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String candidate = normaliseKey(raw);
        for (String county : COUNTIES) {
            if (normaliseKey(county).equals(candidate)) {
                return county;
            }
        }
        if (normaliseKey("Nairobi City").equals(candidate)) {
            return "Nairobi City";
        }
        if (normaliseKey("Nairobi").equals(candidate)) {
            return "Nairobi City";
        }
        return null;
    }

    public static boolean isValid(String raw) {
        return normalise(raw) != null;
    }

/**
     * Compared without apostrophes at all, so "Murang'a", "Murang’a" and the "Muranga"
     * people actually type all reach the same stored value.
     */
    private static String normaliseKey(String value) {
        return value.trim().toLowerCase(Locale.ROOT)
                .replace("’", "")
                .replace("'", "")
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }
}