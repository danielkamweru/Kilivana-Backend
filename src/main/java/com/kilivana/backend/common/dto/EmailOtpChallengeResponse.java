package com.kilivana.backend.common.dto;

import java.util.UUID;

/** Same shape and timing hints for eligible, unknown and already verified addresses. */
public record EmailOtpChallengeResponse(UUID challengeId,int expiresInSeconds,int resendAfterSeconds,String message) {
    public static EmailOtpChallengeResponse accepted(UUID challengeId) {
        return new EmailOtpChallengeResponse(challengeId,600,60,
                "If the account is eligible, a verification email will be sent.");
    }
}
