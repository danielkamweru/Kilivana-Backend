package com.kilivana.backend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * The caller exceeded a rate limit. Mapped to HTTP 429 by {@link GlobalExceptionHandler}.
 */
@Getter
public class TooManyRequestsException extends RuntimeException {

    private final HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;

    public TooManyRequestsException(String message) {
        super(message);
    }
}