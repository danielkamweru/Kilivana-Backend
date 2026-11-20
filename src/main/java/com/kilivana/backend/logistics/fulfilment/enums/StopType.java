package com.kilivana.backend.logistics.fulfilment.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import com.kilivana.backend.common.enums.EnumNameMatcher;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Whether a shipment stop is a farmer pickup or the buyer delivery.
 */
@Schema(description = "Whether a shipment stop is a farmer pickup or the buyer delivery.")
public enum StopType {
    PICKUP,
    DELIVERY;

    @JsonCreator
    public static StopType from(String value) {
        return EnumNameMatcher.match(StopType.class, value, "stop type");
    }

    @JsonValue
    public String wire() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
