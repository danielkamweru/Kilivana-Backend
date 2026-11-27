package com.kilivana.backend.finance.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.kilivana.backend.common.enums.EnumNameMatcher;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Delivery OTP verification status.
 */
@Schema(description = "Delivery OTP verification status.")
public enum DeliveryOtpStatus {

    /** OTP requested, not yet sent. */
    PENDING,

    /** OTP sent to buyer. */
    SENT,

    /** OTP verified successfully. */
    VERIFIED,

    /** OTP expired. */
    EXPIRED,

    /** OTP verification failed (too many attempts). */
    FAILED,

    /** OTP used for delivery confirmation. */
    USED;

    @JsonCreator
    public static DeliveryOtpStatus from(String value) {
        return EnumNameMatcher.match(DeliveryOtpStatus.class, value, "delivery OTP status");
    }

    @JsonValue
    public String wire() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public boolean isActive() {
        return this == PENDING || this == SENT;
    }

    public boolean isTerminal() {
        return this == VERIFIED || this == EXPIRED || this == FAILED || this == USED;
    }
}