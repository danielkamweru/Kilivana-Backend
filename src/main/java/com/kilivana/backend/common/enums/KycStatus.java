package com.kilivana.backend.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Whether a driver's licence and national ID have been checked by an administrator.
 *
 * <p>Separate from {@link VerificationStatus}, which tracks whether a user's <em>email</em> has
 * been confirmed. A driver can have a confirmed email and an unverified licence, so the two
 * cannot share a column.
 */
public enum KycStatus {
    PENDING,
    VERIFIED;

    @JsonCreator
    public static KycStatus from(String value) {
        return EnumNameMatcher.match(KycStatus.class, value, "KYC status");
    }
}