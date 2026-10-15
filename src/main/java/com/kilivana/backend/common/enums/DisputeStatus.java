package com.kilivana.backend.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Where a dispute sits between being raised and being settled.
 */
@Schema(description = "Where a dispute sits between being raised and being settled.")
public enum DisputeStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;

    @JsonCreator
    public static DisputeStatus from(String value) {
        return EnumNameMatcher.match(DisputeStatus.class, value, "dispute status");
    }

    /** The spelling the panel sends and expects back, e.g. {@code in_progress}. */
    @JsonValue
    public String wire() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
