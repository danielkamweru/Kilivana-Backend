package com.kilivana.backend.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Whether a driver's licence and national ID have been checked by an administrator.
 *
 * <p>Separate from {@link VerificationStatus}, which tracks whether a user's <em>email</em> has
 * been confirmed. A driver can have a confirmed email and an unverified licence, so the two
 * cannot share a column.
 */
@Schema(description = "Whether a driver's licence and national ID have been checked by an administrator.")
public enum KycStatus {
    PENDING,
    VERIFIED;

    @JsonCreator
    public static KycStatus from(String value) {
        return EnumNameMatcher.match(KycStatus.class, value, "KYC status");
    }

    /** The spelling the panel sends and expects back, e.g. {@code verified}. */
    @JsonValue
    public String wire() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
