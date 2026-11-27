package com.kilivana.backend.finance.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.kilivana.backend.common.enums.EnumNameMatcher;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Status of a wallet transaction.
 */
@Schema(description = "Status of a wallet transaction.")
public enum WalletTransactionStatus {

    /** Transaction created, awaiting processing. */
    PENDING,

    /** Transaction completed successfully. */
    COMPLETED,

    /** Transaction failed. */
    FAILED,

    /** Transaction was reversed. */
    REVERSED;

    @JsonCreator
    public static WalletTransactionStatus from(String value) {
        return EnumNameMatcher.match(WalletTransactionStatus.class, value, "wallet transaction status");
    }

    @JsonValue
    public String wire() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}