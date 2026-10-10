package com.kilivana.backend.common.exception;

/**
 * Credentials are missing, invalid, or the account may not authenticate (suspended,
 * inactive). Mapped to HTTP 401 by {@link GlobalExceptionHandler}.
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
