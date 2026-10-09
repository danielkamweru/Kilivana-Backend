package com.kilivana.backend.common.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Where an inspection job sits between assignment and completion.
 */
@Schema(description = "Where an inspection job sits between assignment and completion.")
public enum InspectionStatus {
    ASSIGNED,
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
