package com.kilivana.backend.common.service;

import java.util.List;
import java.util.Locale;

/**
 * Identity documents a Kenyan driver can present.
 *
 * <p>The admin panel offers these on the driver form and validates the field, but its
 * list was the Ghanaian one - Ghana Card, Passport, Voter ID - so a driver created here
 * was recorded against a document that does not exist in Kenya. The national ID card and
 * the driving licence are the two that matter here: a commercial driver shows the
 * licence, and the national ID is what a rider is asked for.
 */
public final class KenyanIdType {

    private static final List<String> TYPES = List.of(
            "National ID",
            "Passport",
            "Driving Licence");

    private KenyanIdType() {
    }

    public static List<String> all() {
        return TYPES;
    }

    /**
     * The stored spelling when the input names a document, otherwise null.
     *
     * <p>Accepts the Ghanaian labels that earlier rows and older clients still send, so
     * the same person does not become two drivers because a card was renamed.
     */
    public static String normalise(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        return switch (value) {
            case "nationalid", "nationalidentitycard", "idcard", "id", "nationalidentitynumber" ->
                    "National ID";
            case "passport" -> "Passport";
            case "drivinglicence", "driverslicence", "driverlicence", "licence", "license" ->
                    "Driving Licence";
            // A Ghana Card is a national identity card under another name.
            case "ghanacard", "ghanacardid" -> "National ID";
            case "voterid", "votersid" -> "National ID";
            default -> null;
        };
    }

    public static boolean isValid(String raw) {
        return normalise(raw) != null;
    }
}