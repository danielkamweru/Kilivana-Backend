package com.kilivana.backend.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Where an order is between "placed" and "settled", exactly as the admin panel
 * draws its pipeline: Placed, Confirmed, In Transit, Delivered, Completed, with
 * Disputed and Cancelled as the two side exits.
 *
 * <p>DISPUTED is a real order state here because the panel marks the order
 * disputed when a buyer raises one and holds the payment until it is resolved;
 * the dispute record keeps its own status, but the order has to show it too.
 *
 * <p>PREPARING, READY_FOR_PICKUP and PICKED_UP were removed: nothing ever moved
 * an order into them (the fulfilment steps live on the logistics job's
 * {@link DeliveryStatus}), and the panel has no column to render them in.
 */
public enum OrderStatus {
    PLACED,
    CONFIRMED,
    IN_TRANSIT,
    DELIVERED,
    COMPLETED,
    DISPUTED,
    CANCELLED;

    @JsonCreator
    public static OrderStatus from(String value) {
        return EnumNameMatcher.match(OrderStatus.class, value, "order status");
    }

    /** The spelling the panel sends and expects back, e.g. {@code in_transit}. */
    @JsonValue
    public String wire() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public boolean isOpen() {
        return this != COMPLETED && this != CANCELLED;
    }
}
