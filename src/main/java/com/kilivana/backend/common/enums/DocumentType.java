package com.kilivana.backend.common.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Kinds of identity and supporting documents a user can submit.
 */
@Schema(description = "Kinds of identity and supporting documents a user can submit.")
public enum DocumentType {
    NATIONAL_ID,
    KRA_PIN,
    LAND_TITLE,
    LEASE_AGREEMENT,
    FARM_PHOTO
}
