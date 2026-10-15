package com.kilivana.backend.common.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * How a farm or land parcel is held by the person farming it.
 */
@Schema(description = "How a farm or land parcel is held by the person farming it.")
public enum OwnershipType {
    OWNED,
    LEASED,
    COMMUNAL
}
