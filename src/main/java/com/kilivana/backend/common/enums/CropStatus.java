package com.kilivana.backend.common.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Where a crop is in its growing cycle, from the plan through to the outcome.
 */
@Schema(description = "Where a crop is in its growing cycle, from the plan through to the outcome.")
public enum CropStatus {
    PLANNED,
    GROWING,
    HARVESTED,
    FAILED
}
