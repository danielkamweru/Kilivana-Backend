package com.kilivana.backend.common.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Outcome of an on-site inspection of a product or farm.
 */
@Schema(description = "Outcome of an on-site inspection of a product or farm.")
public enum InspectionResult {
    APPROVED,
    REJECTED,
    CHANGES_REQUIRED
}
