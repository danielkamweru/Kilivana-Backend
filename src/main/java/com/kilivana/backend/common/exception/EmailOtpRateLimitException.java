package com.kilivana.backend.common.exception;

public class EmailOtpRateLimitException extends TooManyRequestsException {
    private final long retryAfterSeconds;
    public EmailOtpRateLimitException(long retryAfterSeconds) {
        super("Too many verification requests. Please try again later.");
        this.retryAfterSeconds=Math.max(1,retryAfterSeconds);
    }
    public long getRetryAfterSeconds() { return retryAfterSeconds; }
}
