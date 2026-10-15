package com.kilivana.backend.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Kind of vehicle a driver delivers with.
 *
 * <p>Constrained in the database too (ck_driver_profiles_vehicle_type), because the column
 * used to be free text and already held both {@code "Truck"} and {@code "TRUCK"}.
 */
@Schema(description = "Kind of vehicle a driver delivers with.")
public enum VehicleType {
    TRUCK,
    VAN,
    PICKUP,
    MOTORBIKE;

    @JsonCreator
    public static VehicleType from(String value) {
        return EnumNameMatcher.match(VehicleType.class, value, "vehicle type");
    }

    /**
     * The spelling the panel sends and expects back: title case, with the
     * motorbike spelled the way the panel spells it, "Motorbike".
     */
    @JsonValue
    public String wire() {
        return switch (this) {
            case MOTORBIKE -> "Motorbike";
            default -> name().charAt(0) + name().substring(1).toLowerCase(java.util.Locale.ROOT);
        };
    }
}
