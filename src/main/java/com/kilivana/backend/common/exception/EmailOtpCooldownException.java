package com.kilivana.backend.common.exception;

/** The email was already sent recently; this is distinct from an exhausted hourly limit. */
public class EmailOtpCooldownException extends EmailOtpRateLimitException {
    public EmailOtpCooldownException(long retryAfterSeconds) {
        super(retryAfterSeconds);
    }
}
