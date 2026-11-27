package com.kilivana.backend.finance.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.kilivana.backend.common.enums.EnumNameMatcher;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Settlement status lifecycle.
 * Tracks the complete financial settlement process for a shipment.
 */
@Schema(description = "Settlement status for a shipment's financial lifecycle.")
public enum SettlementStatus {

    /** Payment received, awaiting delivery confirmation. */
    PENDING_PAYMENT,

    /** Payment confirmed, awaiting delivery. */
    PENDING_DELIVERY,

    /** Delivery confirmed via OTP, ready for settlement processing. */
    DELIVERY_CONFIRMED,

    /** Settlement calculations in progress. */
    SETTLEMENT_PROCESSING,

    /** Settlement completed, funds allocated to wallets. */
    SETTLED,

    /** Settlement on hold (dispute, admin review, etc.). */
    HELD,

    /** Partially refunded after settlement. */
    PARTIALLY_REFUNDED,

    /** Fully refunded. */
    REFUNDED,

    /** Settlement failed (payout failed, etc.). */
    FAILED;

    @JsonCreator
    public static SettlementStatus from(String value) {
        return EnumNameMatcher.match(SettlementStatus.class, value, "settlement status");
    }

    @JsonValue
    public String wire() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public boolean isPending() {
        return this == PENDING_PAYMENT || this == PENDING_DELIVERY;
    }

    public boolean isTerminal() {
        return this == SETTLED || this == REFUNDED || this == FAILED;
    }
}