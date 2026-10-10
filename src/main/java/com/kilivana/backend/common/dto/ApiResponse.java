package com.kilivana.backend.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * The envelope every controller wraps its payload in, so clients always see one
 * response shape whether the call succeeded or failed.
 *
 * <p>{@code success}, {@code message} and {@code timestamp} are always present;
 * {@code data} carries the payload on success and {@code error} carries a
 * machine-readable code on failure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    /** Whether the request succeeded. */
    private boolean success;
    /** Human-readable outcome, e.g. "Success" or the failure reason. */
    private String message;
    /** The payload, or {@code null} when the call returned nothing. */
    private T data;
    /** When the server produced the response. */
    private LocalDateTime timestamp;
    /** Structured failure details, present only when {@code success} is false. */
    private ErrorDetail error;

    /** A successful response carrying {@code data} with the default "Success" message. */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
            .message("Success")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /** A successful response with a custom message and payload. */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /** A successful response with no payload, only a message. */
    public static <T> ApiResponse<T> successMessage(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /** A failure with a human-readable message and no structured detail. */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /** A failure carrying a machine-readable error code in addition to the message. */
    public static <T> ApiResponse<T> error(String message, ErrorDetail error) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .error(error)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    /** The structured part of a failure: a stable code clients can branch on, plus context. */
    public static class ErrorDetail {
        /** Stable application error code, e.g. {@code RESOURCE_NOT_FOUND}. */
        private String code;
        /** Human-readable explanation of what went wrong. */
        private String details;
    }
}
