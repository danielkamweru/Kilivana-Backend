package com.kilivana.backend.common.exception;

/**
 * The request itself is invalid: malformed input, a failed business rule or a rejected
 * constraint. Mapped to HTTP 400 by {@link GlobalExceptionHandler}.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
