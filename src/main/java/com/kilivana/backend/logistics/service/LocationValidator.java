package com.kilivana.backend.logistics.service;

import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.logistics.dto.LocationUpdateRequest;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * Rejects a location update before it ever reaches the database. Nothing here touches
 * state, so it is trivially unit-testable without mocks.
 *
 * <p>The checks are deliberately cheap and ordered from cheapest to most expensive: bounds
 * first, then clock skew, then duplicate position. A driver sitting still still has to pass
 * them all.
 */
public class LocationValidator {

    private static final double EPSILON = 0.00005; // ~5 m, guards against a 0.0/0.0 "same point" loop

    private final Duration maxClockSkew;
    private final Duration minInterval;
    private final double duplicateThresholdDegrees;

    public LocationValidator(Duration maxClockSkew, Duration minInterval, double duplicateThresholdDegrees) {
        this.maxClockSkew = maxClockSkew;
        this.minInterval = minInterval;
        this.duplicateThresholdDegrees = duplicateThresholdDegrees;
    }

    /**
     * Validates the bounds of a single fix. Does not look at the database, so it can run
     * before the job is even loaded.
     */
    public void validateBounds(LocationUpdateRequest request) {
        if (request.getLatitude() < -90 || request.getLatitude() > 90) {
            throw new BadRequestException("latitude must be between -90 and 90");
        }
        if (request.getLongitude() < -180 || request.getLongitude() > 180) {
            throw new BadRequestException("longitude must be between -180 and 180");
        }
        if (request.getSpeedKmh() != null && (request.getSpeedKmh() < 0 || request.getSpeedKmh() > 300)) {
            throw new BadRequestException("speedKmh must be between 0 and 300");
        }
        if (request.getBearing() != null && (request.getBearing() < 0 || request.getBearing() > 360)) {
            throw new BadRequestException("bearing must be between 0 and 360");
        }
        if (request.getAccuracyMetres() != null && (request.getAccuracyMetres() < 0 || request.getAccuracyMetres() > 500)) {
            throw new BadRequestException("accuracyMetres must be between 0 and 500");
        }
    }

    /**
     * Parses the client-supplied timestamp and rejects it when it is unparseable or further
     * from now than the configured skew allows. A stale fix (device clock wrong, or the
     * update queued on a bad connection) is refused rather than stored, because a position
     * older than the last accepted one would invert the history.
     */
    public LocalDateTime validateTimestamp(String clientTimestamp) {
        if (clientTimestamp == null || clientTimestamp.isBlank()) {
            return null;
        }
        try {
            LocalDateTime parsed = LocalDateTime.parse(clientTimestamp);
            Instant clientInstant = parsed.atZone(ZoneId.systemDefault()).toInstant();
            Instant now = Instant.now();
            Duration skew = Duration.between(clientInstant, now).abs();
            if (skew.compareTo(maxClockSkew) > 0) {
                throw new BadRequestException("clientTimestamp is too far from server time");
            }
            return parsed;
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("clientTimestamp must be ISO-8601, e.g. 2026-10-09T12:00:00Z");
        }
    }

    /**
     * Rejects a fix that is essentially identical to the previous one. This is the cheap
     * half of throttling: a parked driver or a client re-sending the last packet should not
     * fill the stream with duplicate rows.
     */
    public boolean isDuplicate(Double lat, Double lon, Double prevLat, Double prevLon) {
        if (prevLat == null || prevLon == null) {
            return false;
        }
        return Math.abs(lat - prevLat) < duplicateThresholdDegrees
                && Math.abs(lon - prevLon) < duplicateThresholdDegrees;
    }
}