package com.kilivana.backend.logistics.fulfilment.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import com.kilivana.backend.common.enums.EnumNameMatcher;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The lifecycle of a shipment, independent of the customer order and independent
 * of any individual farmer fulfilment record.
 * <p>
 * A shipment groups one or more farmer pickups and one buyer delivery into a
 * single route serviced by one driver and one vehicle.
 */
@Schema(description = "The lifecycle of a shipment, independent of the customer order.")
public enum ShipmentStatus {
    /** Shipment created but no driver assigned yet. */
    PENDING,
    /** A driver and vehicle have been assigned to the shipment. */
    ASSIGNED,
    /** The driver has accepted the shipment. */
    DRIVER_ACCEPTED,
    /** The driver is en route to or collecting from farmer pickups. */
    PICKING_UP,
    /** The driver is en route to the buyer after all pickups. */
    IN_TRANSIT,
    /** The buyer has received all goods in the shipment. */
    DELIVERED,
    /** The shipment encountered a failure (e.g. missed pickup, damaged goods). */
    FAILED,
    /** The shipment was cancelled before completion. */
    CANCELLED;

    @JsonCreator
    public static ShipmentStatus from(String value) {
        return EnumNameMatcher.match(ShipmentStatus.class, value, "shipment status");
    }

    @JsonValue
    public String wire() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == FAILED || this == CANCELLED;
    }

    public boolean isOpen() {
        return !isTerminal();
    }
}
