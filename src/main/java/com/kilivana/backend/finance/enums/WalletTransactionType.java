package com.kilivana.backend.finance.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.kilivana.backend.common.enums.EnumNameMatcher;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Types of wallet transactions for audit and reconciliation.
 */
@Schema(description = "Type of wallet transaction.")
public enum WalletTransactionType {

    /** Credit from a completed sale after commission deduction. */
    SALE_CREDIT,

    /** Commission deducted from gross sale amount. */
    COMMISSION_DEDUCTION,

    /** Movement of earnings from pending to available after delivery confirmation. */
    PENDING_TO_AVAILABLE,

    /** Seller requested withdrawal; funds moved from available to processing. */
    WITHDRAWAL_REQUEST,

    /** Withdrawal completed successfully; funds leave processing. */
    WITHDRAWAL_SUCCESS,

    /** Withdrawal failed or was reversed; funds returned from processing to available. */
    WITHDRAWAL_REVERSAL,

    /** Refund deducted from seller earnings. */
    REFUND,

    /** Manual adjustment by administrator (with reason). */
    ADJUSTMENT;

    @JsonCreator
    public static WalletTransactionType from(String value) {
        return EnumNameMatcher.match(WalletTransactionType.class, value, "wallet transaction type");
    }

    @JsonValue
    public String wire() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}