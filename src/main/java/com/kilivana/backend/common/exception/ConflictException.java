package com.kilivana.backend.common.exception;

import com.kilivana.backend.common.dto.ApiResponse;
import lombok.Getter;

/**
 * The request conflicts with existing state, typically a duplicate unique value.
 * Mapped to HTTP 409 by {@link GlobalExceptionHandler}.
 *
 * <p>Carries an application error code so clients can tell one kind of conflict from
 * another; it defaults to {@code CONFLICT}.
 */
@Getter
public class ConflictException extends RuntimeException {

    /** Stable code clients can branch on, e.g. {@code EMAIL_EXISTS}. */
    private final String errorCode;

    public ConflictException(String message) {
        super(message);
        this.errorCode = "CONFLICT";
    }

    public ConflictException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ConflictException(String message, ApiResponse.ErrorDetail errorDetail) {
        super(message);
        this.errorCode = errorDetail != null ? errorDetail.getCode() : "CONFLICT";
    }
}
