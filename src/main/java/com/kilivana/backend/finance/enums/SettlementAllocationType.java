package com.kilivana.backend.finance.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.kilivana.backend.common.enums.EnumNameMatcher;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Types of settlement allocations.
 */
@Schema(description = "Type of settlement allocation.")
public enum SettlementAllocationType {

    /** Seller's net earnings from product sales. */
    SELLER_EARNINGS,

    /** Transport fee paid to transport provider. */
    TRANSPORT_FEE,

    /** Inspection fee paid to inspector/platform. */
    INSPECTION_FEE,

    /** Platform commission from the sale. */
    PLATFORM_COMMISSION,

    /** Platform revenue (includes commission, fees, etc.). */
    PLATFORM_REVENUE,

    /** Refund to buyer. */
    BUYER_REFUND,

    /** Adjustment (admin correction). */
    ADJUSTMENT;

    @JsonCreator
    public static SettlementAllocationType from(String value) {
        return EnumNameMatcher.match(SettlementAllocationType.class, value, "settlement allocation type");
    }

    @JsonValue
    public String wire() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}