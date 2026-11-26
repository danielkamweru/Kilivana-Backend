package com.kilivana.backend.finance.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.kilivana.backend.common.enums.EnumNameMatcher;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Status of a seller withdrawal/payout request.
 */
@Schema(description = "Status of a seller payout/withdrawal request.")
public enum PayoutStatus {

    /** Withdrawal requested by seller, awaiting admin review. */
    PENDING,

    /** Admin approved the withdrawal. */
    APPROVED,

    /** Payout is being processed (B2C initiated). */
    PROCESSING,

    /** Payout confirmed successful by provider. */
    PAID,

    /** Payout failed at provider level. */
    FAILED,

    /** Admin rejected the withdrawal request. */
    REJECTED,

    /** Payout placed on hold (e.g., compliance review). */
    ON_HOLD,

    /** Payout was reversed after being marked paid. */
    REVERSED;

    @JsonCreator
    public static PayoutStatus from(String value) {
        return EnumNameMatcher.match(PayoutStatus.class, value, "payout status");
    }

    @JsonValue
    public String wire() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public boolean isTerminal() {
        return this == PAID || this == FAILED || this == REJECTED || this == REVERSED;
    }

    public boolean canTransitionTo(PayoutStatus next) {
        return switch (this) {
            case PENDING -> next == APPROVED || next == REJECTED || next == ON_HOLD;
            case APPROVED -> next == PROCESSING || next == ON_HOLD || next == REJECTED;
            case PROCESSING -> next == PAID || next == FAILED || next == ON_HOLD;
            case PAID -> next == REVERSED;
            case FAILED -> next == PENDING || next == ON_HOLD; // retry or hold
            case REJECTED -> next == PENDING; // seller can request again
            case ON_HOLD -> next == APPROVED || next == REJECTED || next == PROCESSING;
            case REVERSED -> false;
        };
    }
}