package com.kilivana.backend.common.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Account lifecycle state of a user, set by an administrator.
 */
@Schema(description = "Account lifecycle state of a user, set by an administrator.")
public enum UserStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    PENDING_VERIFICATION
}
