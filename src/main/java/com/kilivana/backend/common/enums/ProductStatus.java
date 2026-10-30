package com.kilivana.backend.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Admin moderation uses ACTIVE, PENDING_APPROVAL, REJECTED and SUSPENDED.
 * Sellers use UNLISTED to hide a published product and ARCHIVED to retire a listing.
 * Re-listing requires PENDING_APPROVAL; DELETED is terminal and retains history.
 * Out-of-stock remains a property of stock quantity rather than a lifecycle state.
 */
@Schema(description = "The lifecycle of a product listing.")
public enum ProductStatus {
    ACTIVE,
    PENDING_APPROVAL,
    REJECTED,
    SUSPENDED,
    UNLISTED,
    ARCHIVED,
    DELETED;

    @JsonCreator
    public static ProductStatus from(String value) {
        if (value == null) {
            return null;
        }
        // The panel sends "pending" for a listing awaiting verification, which is
        // PENDING_APPROVAL here; every other constant matches by name.
        if ("pending".equalsIgnoreCase(value.trim())) {
            return PENDING_APPROVAL;
        }
        return EnumNameMatcher.match(ProductStatus.class, value, "product status");
    }

    /**
     * The spelling the panel sends and expects back: {@code pending} rather than
     * {@code pending_approval}, because that is the label the panel shows.
     */
    @JsonValue
    public String wire() {
        return switch (this) {
            case PENDING_APPROVAL -> "pending";
            default -> name().toLowerCase(java.util.Locale.ROOT);
        };
    }
}
