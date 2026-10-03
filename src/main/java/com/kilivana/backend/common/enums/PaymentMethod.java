package com.kilivana.backend.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * How money moves, as the admin panel names it.
 *
 * <p>M-Pesa is the dominant Kenyan mobile wallet; bank transfer covers the larger
 * corporate and cooperative orders, and card covers online checkout. Stored as
 * STRING so a new provider never invalidates a row written by an older build.
 */
public enum PaymentMethod {
    MPESA,
    BANK,
    CARD;

    /**
     * Accepts what callers actually send. The panel sends lower case, and older rows
     * still carry the Ghanaian and generic labels they were created with.
     */
    public static PaymentMethod from(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        return switch (value) {
            case "MPESA", "M_PESA", "SENDGRID" -> MPESA;
            // The seeded rows predate this enum and used Ghanaian mobile money names.
            case "MTN_MOMO", "MTN", "MOMO", "MOBILE_MONEY", "VODACOM", "AIRTEL" -> MPESA;
            case "BANK", "BANK_TRANSFER", "BANKTRANSFER", "RTGS", "SWIFT" -> BANK;
            case "CARD", "CREDIT_CARD", "DEBIT_CARD", "VISA", "MASTERCARD" -> CARD;
            default -> null;
        };
    }

    /** The spelling the panel sends and expects back, e.g. {@code mpesa}. */
    @JsonValue
    public String wire() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
