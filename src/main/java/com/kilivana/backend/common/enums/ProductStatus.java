package com.kilivana.backend.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The lifecycle of a product listing, as the panel draws it: a listing waits for
 * on-site verification, goes live, and can be rejected or suspended by an
 * administrator.
 *
 * <p>INACTIVE and OUT_OF_STOCK were removed: nothing set INACTIVE (suspension is
 * the panel's word for taking a listing down), and out of stock is a property of
 * the stock count, not of the listing, so a product that has sold out stays
 * ACTIVE and the panel derives the out-of-stock state from the stock it already
 * receives.
 */
@Schema(description = "The lifecycle of a product listing.")
public enum ProductStatus {
    ACTIVE,
    PENDING_APPROVAL,
    REJECTED,
    SUSPENDED;

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
