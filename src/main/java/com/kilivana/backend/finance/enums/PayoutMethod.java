package com.kilivana.backend.finance.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.kilivana.backend.common.enums.EnumNameMatcher;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Method for seller payouts.
 */
@Schema(description = "Payout method for seller withdrawals.")
public enum PayoutMethod {

    /** M-Pesa B2C payout to registered phone number. */
    MPESA,

    /** Bank transfer (future support). */
    BANK_TRANSFER;

    @JsonCreator
    public static PayoutMethod from(String value) {
        return EnumNameMatcher.match(PayoutMethod.class, value, "payout method");
    }

    @JsonValue
    public String wire() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}