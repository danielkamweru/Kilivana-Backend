package com.kilivana.backend.common.exception;

import com.kilivana.backend.common.dto.ApiResponse;
import lombok.Getter;

@Getter
public class ConflictException extends RuntimeException {

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
