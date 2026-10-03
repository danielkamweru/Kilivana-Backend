package com.kilivana.backend.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Where a delivery job is between assignment and handover. The richer states here
 * are the job's own; the order the panel shows reads the simplified
 * assigned / picked-up / delivered view of them.
 */
public enum DeliveryStatus {
    PENDING_ASSIGNMENT,
    ASSIGNED,
    ACCEPTED,
    EN_ROUTE_TO_PICKUP,
    ARRIVED_AT_PICKUP,
    PICKED_UP,
    IN_TRANSIT,
    ARRIVED_AT_DESTINATION,
    DELIVERED,
    CANCELLED,
    FAILED;

    @JsonCreator
    public static DeliveryStatus from(String value) {
        return EnumNameMatcher.match(DeliveryStatus.class, value, "delivery status");
    }

    /** The spelling the panel sends and expects back, e.g. {@code picked_up}. */
    @JsonValue
    public String wire() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
