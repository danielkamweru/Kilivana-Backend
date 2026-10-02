package com.kilivana.backend.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Kind of vehicle a driver delivers with.
 *
 * <p>Constrained in the database too (ck_driver_profiles_vehicle_type), because the column
 * used to be free text and already held both {@code "Truck"} and {@code "TRUCK"}.
 */
public enum VehicleType {
    TRUCK,
    VAN,
    PICKUP,
    MOTORBIKE;

    @JsonCreator
    public static VehicleType from(String value) {
        return EnumNameMatcher.match(VehicleType.class, value, "vehicle type");
    }
}