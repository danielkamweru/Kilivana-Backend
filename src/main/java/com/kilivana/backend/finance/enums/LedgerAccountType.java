package com.kilivana.backend.finance.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.kilivana.backend.common.enums.EnumNameMatcher;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Account types for the double-entry financial ledger.
 */
@Schema(description = "Account type in the financial ledger.")
public enum LedgerAccountType {

    // Assets
    /** Funds received from buyers but not yet settled by payment provider. */
    PAYMENT_CLEARING,

    /** Funds settled by payment provider and available for disbursement. */
    SETTLED_FUNDS,

    // Liabilities
    /** Amounts owed to sellers (farmers/suppliers). */
    SELLER_PAYABLES,

    /** Amounts owed to buyers for refunds. */
    REFUND_PAYABLE,

    // Revenue
    /** Platform commission earned from seller transactions. */
    PLATFORM_COMMISSION_REVENUE,

    /** Other platform revenue (fees, subscriptions, etc.). */
    OTHER_PLATFORM_REVENUE,

    // Expenses
    /** Payment processing fees paid to providers. */
    PAYMENT_PROCESSING_EXPENSES,

    /** Other approved platform operating expenses. */
    OTHER_PLATFORM_EXPENSES;

    @JsonCreator
    public static LedgerAccountType from(String value) {
        return EnumNameMatcher.match(LedgerAccountType.class, value, "ledger account type");
    }

    @JsonValue
    public String wire() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public boolean isDebitNormal() {
        // Assets and Expenses have normal debit balance
        return this == PAYMENT_CLEARING
                || this == SETTLED_FUNDS
                || this == PAYMENT_PROCESSING_EXPENSES
                || this == OTHER_PLATFORM_EXPENSES;
    }

    public boolean isCreditNormal() {
        // Liabilities and Revenue have normal credit balance
        return this == SELLER_PAYABLES
                || this == REFUND_PAYABLE
                || this == PLATFORM_COMMISSION_REVENUE
                || this == OTHER_PLATFORM_REVENUE;
    }
}