package com.kilivana.backend.logistics.service;

import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.logistics.dto.LocationUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Bounds, clock skew and duplicate detection are pure logic, so they are exercised here
 * without a database. The rate limiter is tested alongside because it is likewise stateless
 * beyond its own window.
 */
class LocationValidatorTest {

    private LocationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new LocationValidator(
                Duration.ofMinutes(15),
                Duration.ofSeconds(1),
                0.00005);
    }

    @Test
    void acceptsWellFormedFix() {
        LocationUpdateRequest request = new LocationUpdateRequest();
        request.setTripId(1L);
        request.setLatitude(-1.2921);
        request.setLongitude(36.8219);
        validator.validateBounds(request);
    }

    @Test
    void rejectsLatitudeAboveNinety() {
        LocationUpdateRequest request = new LocationUpdateRequest();
        request.setTripId(1L);
        request.setLatitude(91.0);
        request.setLongitude(0.0);
        assertThatThrownBy(() -> validator.validateBounds(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsLatitudeBelowMinusNinety() {
        LocationUpdateRequest request = new LocationUpdateRequest();
        request.setTripId(1L);
        request.setLatitude(-91.0);
        request.setLongitude(0.0);
        assertThatThrownBy(() -> validator.validateBounds(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsLongitudeOutsideRange() {
        LocationUpdateRequest request = new LocationUpdateRequest();
        request.setTripId(1L);
        request.setLatitude(0.0);
        request.setLongitude(181.0);
        assertThatThrownBy(() -> validator.validateBounds(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsNegativeSpeed() {
        LocationUpdateRequest request = new LocationUpdateRequest();
        request.setTripId(1L);
        request.setLatitude(0.0);
        request.setLongitude(0.0);
        request.setSpeedKmh(-5.0);
        assertThatThrownBy(() -> validator.validateBounds(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsBearingAbove360() {
        LocationUpdateRequest request = new LocationUpdateRequest();
        request.setTripId(1L);
        request.setLatitude(0.0);
        request.setLongitude(0.0);
        request.setBearing(361.0);
        assertThatThrownBy(() -> validator.validateBounds(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void acceptsNullTimestamp() {
        assertThat(validator.validateTimestamp(null)).isNull();
        assertThat(validator.validateTimestamp("")).isNull();
    }

    @Test
    void acceptsCurrentTimestamp() {
        assertThat(validator.validateTimestamp(java.time.LocalDateTime.now().toString()))
                .isNotNull();
    }

    @Test
    void rejectsUnparseableTimestamp() {
        assertThatThrownBy(() -> validator.validateTimestamp("not-a-date"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsTimestampFarInPast() {
        String stale = java.time.LocalDateTime.now().minusHours(3).toString();
        assertThatThrownBy(() -> validator.validateTimestamp(stale))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void detectsDuplicatePosition() {
        assertThat(validator.isDuplicate(1.0, 2.0, 1.0, 2.0)).isTrue();
        assertThat(validator.isDuplicate(1.0, 2.0, 1.00001, 2.0)).isTrue();
    }

    @Test
    void doesNotFlagDistinctPositionAsDuplicate() {
        assertThat(validator.isDuplicate(1.0, 2.0, 1.1, 2.0)).isFalse();
        assertThat(validator.isDuplicate(1.0, 2.0, null, null)).isFalse();
    }
}

class LocationRateLimiterTest {

    @Test
    void allowsFirstFixesAndRejectsExcess() {
        LocationRateLimiter limiter = new LocationRateLimiter(Duration.ofSeconds(60), 3);
        assertThat(limiter.tryRecord(1L)).isTrue();
        assertThat(limiter.tryRecord(1L)).isTrue();
        assertThat(limiter.tryRecord(1L)).isTrue();
        // Fourth fix inside the window is refused.
        assertThat(limiter.tryRecord(1L)).isFalse();
    }

    @Test
    void isolatesPerDriver() {
        LocationRateLimiter limiter = new LocationRateLimiter(Duration.ofSeconds(60), 1);
        assertThat(limiter.tryRecord(1L)).isTrue();
        // A different driver is not throttled by driver 1's window.
        assertThat(limiter.tryRecord(2L)).isTrue();
        // Driver 1 is now throttled.
        assertThat(limiter.tryRecord(1L)).isFalse();
    }

    @Test
    void throwsWhenOverLimit() {
        LocationRateLimiter limiter = new LocationRateLimiter(Duration.ofSeconds(60), 1);
        limiter.tryRecord(1L);
        assertThatThrownBy(() -> limiter.enforce(1L))
                .isInstanceOf(com.kilivana.backend.common.exception.TooManyRequestsException.class);
    }
}