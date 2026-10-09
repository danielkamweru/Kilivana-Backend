package com.kilivana.backend.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Where a payment sits between the buyer paying and the seller being settled.
 *
 * <p>HELD and SETTLED are what the admin panel shows and they are not cosmetic:
 * HELD means the money is in escrow awaiting delivery, SETTLED means it has been
 * released to the seller. Folding those into COMPLETED would lose the escrow
 * step, so they stay separate. PROCESSING, PARTIALLY_REFUNDED and FAILED were
 * dropped because nothing set them, the panel has no display for any of them, and
 * the panel's own union has no FAILED either.
 */
@Schema(description = "Where a payment sits between the buyer paying and the seller being settled.")
public enum PaymentStatus {
    /** Initiated, not yet confirmed by the provider. */
    PENDING,
    /** Provider confirmed the money arrived. */
    PAID,
    /** In escrow: received but not yet released to the seller. */
    HELD,
    /** Released to the seller after delivery. */
    SETTLED,
    REFUNDED;

    @JsonCreator
    public static PaymentStatus from(String value) {
        return EnumNameMatcher.match(PaymentStatus.class, value, "payment status");
    }

    /** The spelling the panel sends and expects back, e.g. {@code settled}. */
    @JsonValue
    public String wire() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public boolean isRefundable() {
        return this == PAID || this == HELD || this == SETTLED;
    }
}
