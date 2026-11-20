package com.kilivana.backend.logistics.fulfilment.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The status of an individual stop along a shipment route.
 */
@Schema(description = "The status of an individual stop along a shipment route.")
public enum StopStatus {
    PENDING,
    ARRIVED,
    COMPLETED,
    FAILED;

    @JsonCreator
    public static StopStatus from(String value) {
        return EnumNameMatcher.match(StopStatus.class, value, "stop status");
    }

    @JsonValue
    public String wire() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
