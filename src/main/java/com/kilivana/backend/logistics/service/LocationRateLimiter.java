package com.kilivana.backend.logistics.service;

import com.kilivana.backend.common.exception.TooManyRequestsException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-driver rate limiting for location updates, backed by an in-memory sliding window.
 *
 * <p>Each driver gets their own deque of accepted timestamps. On every accepted update the
 * current instant is pushed and every entry older than the window is evicted; the update is
 * refused once the window would hold more than the configured maximum.
 *
 * <p>This is intentionally simple and local: it guards against a misbehaving client or a
 * retry storm, not against a determined attacker, who would simply rotate identities. A
 * distributed deployment would want a shared store; the configuration is externalised so
 * that swap is a property change rather than a code change.
 */
public class LocationRateLimiter {

    private final Map<Long, Deque<Instant>> windows = new ConcurrentHashMap<>();
    private final Duration window;
    private final int maxPerWindow;

    public LocationRateLimiter(Duration window, int maxPerWindow) {
        this.window = window;
        this.maxPerWindow = maxPerWindow;
    }

    /**
     * Records an accepted update and returns true, or returns false (without recording)
     * when the driver has already sent too many updates in the window.
     */
    public boolean tryRecord(Long driverId) {
        Deque<Instant> deque = windows.computeIfAbsent(driverId, k -> new ArrayDeque<>());
        Instant now = Instant.now();
        Instant cutoff = now.minus(window);
        while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
            deque.removeFirst();
        }
        if (deque.size() >= maxPerWindow) {
            return false;
        }
        deque.addLast(now);
        return true;
    }

    public void enforce(Long driverId) {
        if (!tryRecord(driverId)) {
            throw new TooManyRequestsException("Too many location updates. Please slow down.");
        }
    }
}