package com.kilivana.backend.common.enums;

/**
 * Where an order is between "placed" and "settled".
 *
 * <p>The first stage is PLACED rather than PENDING because that is what the admin panel
 * shows for a freshly created order; calling it pending made the panel's first column
 * render as a state it has no label for.
 *
 * <p>A dispute is deliberately absent. It is a separate record with its own status and
 * resolution, so adding it here would give the same fact two homes and let them disagree.
 *
 * <p>PREPARING, READY_FOR_PICKUP and PICKED_UP are seller-side fulfilment steps between
 * confirmation and transit. The panel's pipeline does not draw them, but they are what
 * distinguishes an order nobody has started preparing from one that is about to move, so
 * they stay.
 */
public enum OrderStatus {
    PLACED,
    CONFIRMED,
    PREPARING,
    READY_FOR_PICKUP,
    PICKED_UP,
    IN_TRANSIT,
    DELIVERED,
    COMPLETED,
    CANCELLED;

    public boolean isOpen() {
        return this != COMPLETED && this != CANCELLED;
    }
}