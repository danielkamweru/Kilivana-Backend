package com.kilivana.backend.common.exception;

import lombok.Getter;

/**
 * An optional third-party integration is unreachable or unconfigured. Mapped to HTTP 503
 * by {@link GlobalExceptionHandler}, so callers can tell an integration problem apart
 * from a bad request.
 */
@Getter
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }
}