package com.kilivana.backend.common.enums;

public enum UserRole {
    FARMER,
    BUYER,
    SUPPLIER,
    INSPECTOR,
    DRIVER,
    ADMIN,
    SUPER_ADMIN;

    /**
     * Roles that back-office staff hold. Both can reach /api/v1/admin/** and both may act
     * on any user's profile; everyone else is confined to their own.
     */
    public boolean isStaff() {
        return this == ADMIN || this == SUPER_ADMIN;
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
            case SUPER_ADMIN -> "SA";
        };
    }

    public String referenceCode(long sequence) {
        return referencePrefix() + "-" + String.format("%03d", sequence);
    }
}
