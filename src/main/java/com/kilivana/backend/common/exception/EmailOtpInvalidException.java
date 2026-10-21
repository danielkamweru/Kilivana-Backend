package com.kilivana.backend.common.exception;

/** Generic failure. The transaction must commit failed-attempt counters. */
public class EmailOtpInvalidException extends BadRequestException {
    public EmailOtpInvalidException() { super("Invalid or expired email verification challenge"); }
}
