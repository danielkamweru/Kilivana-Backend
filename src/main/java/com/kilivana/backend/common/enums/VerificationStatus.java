package com.kilivana.backend.common.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Whether a user's email address has been confirmed.
 */
@Schema(description = "Whether a user's email address has been confirmed.")
public enum VerificationStatus {
    PENDING,
    UNDER_REVIEW,
    VERIFIED,
    REJECTED,
    NOT_REQUIRED
}
