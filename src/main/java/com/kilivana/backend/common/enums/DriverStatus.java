package com.kilivana.backend.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * A driver's own working state, as opposed to {@link UserStatus}, which is the account's
 * lifecycle state and is set by an administrator.
 *
 * <p>The driver goes online and offline from their own app; an administrator can move them to
 * {@link #SUSPENDED}, which requires a reason.
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
}