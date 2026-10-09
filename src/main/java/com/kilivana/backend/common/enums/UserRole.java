package com.kilivana.backend.common.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Role a user holds on the platform, which decides what they may do.
 */
@Schema(description = "Role a user holds on the platform, which decides what they may do.")
public enum UserRole {
    FARMER,
    BUYER,
    SUPPLIER,
    INSPECTOR,
    DRIVER,
    ADMIN;

    /**
     * Roles that back-office staff hold. Only an administrator may reach {@code /api/v1/admin/**}
     * and act on another user's profile; everyone else is confined to their own.
     *
     * <p>There used to be a second staff role, {@code SUPER_ADMIN}, with exactly the same
     * permissions. It was removed because two roles with identical authority is one role too
     * many: nothing could be permitted to one but not the other, so every permission check had to
     * name both and a grant to one silently became a grant to the other.
     */
    public boolean isStaff() {
        return this == ADMIN;
    }

    /**
     * Prefix for the human-facing reference code shown in the admin app, e.g. {@code F-001}.
     * Kept next to the role so a new role cannot be added without deciding its prefix.
     */
    public String referencePrefix() {
        return switch (this) {
            case FARMER -> "F";
            case BUYER -> "B";
            case SUPPLIER -> "S";
            case DRIVER -> "DA";
            case INSPECTOR -> "IN";
            case ADMIN -> "AD";
        };
    }

    public String referenceCode(long sequence) {
        return referencePrefix() + "-" + String.format("%03d", sequence);
    }
}