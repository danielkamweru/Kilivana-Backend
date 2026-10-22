package com.kilivana.backend.security;

import com.kilivana.backend.common.exception.TooManyRequestsException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting for the credential endpoints, backed by an in-memory sliding window.
 *
 * <p>Login, password recovery and password reset are the endpoints an attacker drives
 * directly: one guesses credentials, the other two request recovery mail or replay a
 * token. Each endpoint has its own bucket and its own allowance, because a burst of
 * recovery requests is far more suspicious than a burst of logins.
 *
 * <p>Every caller is keyed by its own bucket and client address, so one noisy client
 * cannot lock everybody else out. A successful login clears that caller's login bucket,
 * so an honest client that retries is never punished for it.
 *
 * <p>This is intentionally simple and local: it blunts brute force and mail flooding, it
 * does not stop a determined attacker who rotates addresses. A distributed deployment
 * would want a shared store; the configuration is externalised so that swap is a
 * property change rather than a code change.
 */
public class AuthRateLimiter {

    /** Bucket for password and email login attempts. */
    public static final String LOGIN = "login";
    /** Bucket for password recovery requests. */
    public static final String FORGOT_PASSWORD = "forgot-password";
    /** Bucket for password reset submissions. */
    public static final String RESET_PASSWORD = "reset-password";

    private final Map<String, Deque<Instant>> windows = new ConcurrentHashMap<>();
    private final Duration window;
    private final Map<String, Integer> maxAttempts;

    public AuthRateLimiter(Duration window, Map<String, Integer> maxAttempts) {
        this.window = window;
        this.maxAttempts = Map.copyOf(maxAttempts);
    }

    /**
     * Records an attempt and returns true, or returns false (without recording) when the
     * caller has already spent its allowance for that bucket inside the window.
     */
    public boolean tryRecord(String bucket, String key) {
        int limit = maxAttempts.getOrDefault(bucket, 0);
        if (limit <= 0) {
            return true;
        }
        Deque<Instant> deque = windows.computeIfAbsent(bucket + ":" + key, k -> new ArrayDeque<>());
        Instant now = Instant.now();
        Instant cutoff = now.minus(window);
        while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
            deque.removeFirst();
        }
        if (deque.size() >= limit) {
            return false;
        }
        deque.addLast(now);
        return true;
    }

    /**
     * Refuses the attempt when the caller has exhausted its allowance for that bucket.
     */
    public void enforce(String bucket, String key) {
        if (!tryRecord(bucket, key)) {
            throw new TooManyRequestsException("Too many attempts. Please wait and try again.");
        }
    }

    /**
     * Forgets the attempts recorded for a bucket and caller. Used after a successful
     * login so an honest client keeps its allowance.
     */
    public void reset(String bucket, String key) {
        windows.remove(bucket + ":" + key);
    }
}
