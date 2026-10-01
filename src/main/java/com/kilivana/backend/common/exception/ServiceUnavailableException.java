package com.kilivana.backend.common.exception;

import lombok.Getter;

@Getter
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }
}