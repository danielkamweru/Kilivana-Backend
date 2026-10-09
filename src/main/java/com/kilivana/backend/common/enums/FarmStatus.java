package com.kilivana.backend.common.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Whether a farm is actively farmed, resting, or taken out of use.
 */
@Schema(description = "Whether a farm is actively farmed, resting, or taken out of use.")
public enum FarmStatus {
    ACTIVE,
    FALLOW,
    INACTIVE
}
