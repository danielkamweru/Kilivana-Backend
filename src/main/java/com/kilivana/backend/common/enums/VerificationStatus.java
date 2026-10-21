package com.kilivana.backend.common.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Administrator verification/approval state; email confirmation is tracked separately by User.emailVerified.
 */
@Schema(description = "Administrator verification/approval state; email confirmation is tracked separately by User.emailVerified.")
public enum VerificationStatus {
    PENDING,
    UNDER_REVIEW,
    VERIFIED,
    REJECTED,
    NOT_REQUIRED
}
