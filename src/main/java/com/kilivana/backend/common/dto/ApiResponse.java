package com.kilivana.backend.common.dto;

import java.time.LocalDateTime;

/**
 * The envelope every controller wraps its payload in, so clients always see one
 * response shape whether the call succeeded or failed.
 *
 * <p>{@code success}, {@code message} and {@code timestamp} are always present;
 * {@code data} carries the payload on success and {@code error} carries a
 * machine-readable code on failure.
 */
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

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public ErrorDetail getError() {
        return error;
    }

    public void setError(ErrorDetail error) {
        this.error = error;
    }

    /** A successful response carrying {@code data} with the default "Success" message. */
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.message = "Success";
        response.data = data;
        response.timestamp = LocalDateTime.now();
        return response;
    }

    /** A successful response with a custom message and payload. */
    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.message = message;
        response.data = data;
        response.timestamp = LocalDateTime.now();
        return response;
    }

    /** A successful response with no payload, only a message. */
    public static <T> ApiResponse<T> successMessage(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.message = message;
        response.timestamp = LocalDateTime.now();
        return response;
    }

    /** A failure with a human-readable message and no structured detail. */
    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.message = message;
        response.timestamp = LocalDateTime.now();
        return response;
    }

    /** A failure carrying a machine-readable error code in addition to the message. */
    public static <T> ApiResponse<T> error(String message, ErrorDetail error) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.message = message;
        response.error = error;
        response.timestamp = LocalDateTime.now();
        return response;
    }

    public static <T> ApiResponseBuilder<T> builder() {
        return new ApiResponseBuilder<>();
    }

    public static class ApiResponseBuilder<T> {
        private boolean success;
        private String message;
        private T data;
        private LocalDateTime timestamp;
        private ErrorDetail error;

        public ApiResponseBuilder<T> success(boolean success) {
            this.success = success;
            return this;
        }

        public ApiResponseBuilder<T> message(String message) {
            this.message = message;
            return this;
        }

        public ApiResponseBuilder<T> data(T data) {
            this.data = data;
            return this;
        }

        public ApiResponseBuilder<T> timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public ApiResponseBuilder<T> error(ErrorDetail error) {
            this.error = error;
            return this;
        }

        public ApiResponse<T> build() {
            ApiResponse<T> response = new ApiResponse<>();
            response.success = this.success;
            response.message = this.message;
            response.data = this.data;
            response.timestamp = this.timestamp != null ? this.timestamp : LocalDateTime.now();
            response.error = this.error;
            return response;
        }
    }

    /** The structured part of a failure: a stable code clients can branch on, plus context. */
    public static class ErrorDetail {
        /** Stable application error code, e.g. {@code RESOURCE_NOT_FOUND}. */
        private String code;
        /** Human-readable explanation of what went wrong. */
        private String details;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getDetails() {
            return details;
        }

        public void setDetails(String details) {
            this.details = details;
        }

        public static ErrorDetailBuilder builder() {
            return new ErrorDetailBuilder();
        }

        public static class ErrorDetailBuilder {
            private String code;
            private String details;

            public ErrorDetailBuilder code(String code) {
                this.code = code;
                return this;
            }

            public ErrorDetailBuilder details(String details) {
                this.details = details;
                return this;
            }

            public ErrorDetail build() {
                ErrorDetail errorDetail = new ErrorDetail();
                errorDetail.code = this.code;
                errorDetail.details = this.details;
                return errorDetail;
            }
        }
    }
}
