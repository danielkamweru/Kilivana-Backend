package com.kilivana.backend.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

/** Who is selling on the platform. */
@Schema(description = "Who is selling on the platform.")
public enum SellerType {
    FARMER,
    SUPPLIER;

    @JsonCreator
    public static SellerType from(String value) {
        return EnumNameMatcher.match(SellerType.class, value, "seller type");
    }

    /** The spelling the panel sends and expects back, e.g. {@code farmer}. */
    @JsonValue
    public String wire() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
