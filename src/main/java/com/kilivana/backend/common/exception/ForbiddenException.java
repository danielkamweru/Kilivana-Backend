package com.kilivana.backend.common.exception;

/**
 * The caller is authenticated but lacks the role or ownership the operation requires.
 * Mapped to HTTP 403 by {@link GlobalExceptionHandler}.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
