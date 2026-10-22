package com.kilivana.backend.common.service;

import com.kilivana.backend.common.exception.BadRequestException;

/**
 * The password rules the API applies to credentials it accepts.
 *
 * <p>Registration and password recovery must agree on the rules, otherwise recovery
 * becomes a way to sidestep the policy a registration path enforces. The shortest
 * password accepted anywhere is therefore defined once, here, and referenced by both.
 */
public final class PasswordPolicy {

    /** Shortest password the API accepts. */
    public static final int MIN_LENGTH = 8;

    private PasswordPolicy() {
    }

    /**
     * Refuses a password that is too short to be accepted. Used by the service layer so
     * the rule holds even when a caller bypasses bean validation.
     */
    public static void requireAcceptable(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new BadRequestException("Password must be at least " + MIN_LENGTH + " characters");
        }
    }
}
