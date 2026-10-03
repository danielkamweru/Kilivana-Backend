package com.kilivana.backend.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * A driver's own working state, as opposed to {@link UserStatus}, which is the
 * account's lifecycle state and is set by an administrator.
 *
 * <p>The driver goes online and offline from their own app; an administrator can
 * move them to {@link #SUSPENDED}, which requires a reason.
 */
public enum DriverStatus {
    AVAILABLE,
    ON_DELIVERY,
    OFFLINE,
    SUSPENDED;

    @JsonCreator
    public static DriverStatus from(String value) {
        return EnumNameMatcher.match(DriverStatus.class, value, "driver status");
    }

    /**
     * The spelling the panel sends and expects back. ON_DELIVERY is the one
     * constant whose wire name is not simply its own name lower-cased: the panel
     * writes it with a hyphen, as "on-delivery".
     */
    @JsonValue
    public String wire() {
        return switch (this) {
            case ON_DELIVERY -> "on-delivery";
            default -> name().toLowerCase(java.util.Locale.ROOT);
        };
    }
}
