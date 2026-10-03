package com.kilivana.backend.common.enums;

/**
 * Where a payment sits between the buyer paying and the seller being settled.
 *
 * <p>HELD and SETTLED are what the admin panel shows and they are not cosmetic:
 * HELD means the money is in escrow awaiting delivery, SETTLED means it has been
 * released to the seller. Folding those into COMPLETED would lose the escrow
 * step, so they stay separate. PROCESSING and PARTIALLY_REFUNDED were dropped
 * because nothing set them and the panel has no display for either.
 */
public enum PaymentStatus {
    /** Initiated, not yet confirmed by the provider. */
    PENDING,
    /** Provider confirmed the money arrived. */
    PAID,
    /** In escrow: received but not yet released to the seller. */
    HELD,
    /** Released to the seller after delivery. */
    SETTLED,
    FAILED,
    REFUNDED;

    public boolean isRefundable() {
        return this == PAID || this == HELD || this == SETTLED;
    }
}