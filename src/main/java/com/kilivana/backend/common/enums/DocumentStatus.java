package com.kilivana.backend.common.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Outcome of an administrator's review of a submitted document.
 */
@Schema(description = "Outcome of an administrator's review of a submitted document.")
public enum DocumentStatus {
    PENDING,
    APPROVED,
    REJECTED
}
