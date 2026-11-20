package com.kilivana.backend.logistics.fulfilment.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The preparation state of a single farmer's fulfilment within a multi-farmer order.
 * <p>
 * A seller fulfilment record represents one farmer's portion of a customer order.
 * The farmer advances it from PENDING to COLLECTED as products are made ready
 * for the driver to collect.
 */
@Schema(description = "The preparation state of a farmer's fulfilment within an order.")
public enum FulfilmentStatus {
    /** The fulfilment record exists but the farmer has not yet acknowledged it. */
    PENDING,
    /** The farmer has accepted the fulfilment request and begun preparation. */
    ACCEPTED,
    /** The farmer is actively preparing/packing the products. */
    PREPARING,
    /** The farmer confirms products are ready for collection at the farm. */
    READY_FOR_PICKUP,
    /** The driver has collected the products from the farmer. */
    COLLECTED,
    /** The farmer or an administrator cancelled this fulfilment portion. */
    CANCELLED;

    @JsonCreator
    public static FulfilmentStatus from(String value) {
        return EnumNameMatcher.match(FulfilmentStatus.class, value, "fulfilment status");
    }

    @JsonValue
    public String wire() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public boolean isOpen() {
        return this != COLLECTED && this != CANCELLED;
    }
}
