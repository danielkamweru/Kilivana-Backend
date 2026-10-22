package com.kilivana.backend.security;

import com.kilivana.backend.common.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthRateLimiterTest {

    private static AuthRateLimiter limiter(int loginLimit) {
        return new AuthRateLimiter(Duration.ofMinutes(1), Map.of(AuthRateLimiter.LOGIN, loginLimit));
    }

    @Test
    void attemptsInsideTheAllowanceAreAccepted() {
        AuthRateLimiter limiter = limiter(3);

        assertThat(limiter.tryRecord(AuthRateLimiter.LOGIN, "10.0.0.1")).isTrue();
        assertThat(limiter.tryRecord(AuthRateLimiter.LOGIN, "10.0.0.1")).isTrue();
        assertThat(limiter.tryRecord(AuthRateLimiter.LOGIN, "10.0.0.1")).isTrue();
    }

    @Test
    void attemptsBeyondTheAllowanceAreRefused() {
        AuthRateLimiter limiter = limiter(2);
        limiter.tryRecord(AuthRateLimiter.LOGIN, "10.0.0.1");
        limiter.tryRecord(AuthRateLimiter.LOGIN, "10.0.0.1");

        assertThat(limiter.tryRecord(AuthRateLimiter.LOGIN, "10.0.0.1")).isFalse();
    }

    @Test
    void eachCallerIsCountedSeparately() {
        AuthRateLimiter limiter = limiter(1);
        limiter.tryRecord(AuthRateLimiter.LOGIN, "10.0.0.1");

        assertThat(limiter.tryRecord(AuthRateLimiter.LOGIN, "10.0.0.2")).isTrue();
    }

    @Test
    void eachBucketIsCountedSeparately() {
        AuthRateLimiter limiter = new AuthRateLimiter(Duration.ofMinutes(1), Map.of(
                AuthRateLimiter.LOGIN, 1,
                AuthRateLimiter.FORGOT_PASSWORD, 1));
        limiter.tryRecord(AuthRateLimiter.LOGIN, "10.0.0.1");

        assertThat(limiter.tryRecord(AuthRateLimiter.FORGOT_PASSWORD, "10.0.0.1")).isTrue();
    }

    @Test
    void resettingForgivesTheAllowance() {
        AuthRateLimiter limiter = limiter(1);
        limiter.tryRecord(AuthRateLimiter.LOGIN, "10.0.0.1");

        assertThat(limiter.tryRecord(AuthRateLimiter.LOGIN, "10.0.0.1")).isFalse();
        limiter.reset(AuthRateLimiter.LOGIN, "10.0.0.1");

        assertThat(limiter.tryRecord(AuthRateLimiter.LOGIN, "10.0.0.1")).isTrue();
    }

    @Test
    void enforcingBeyondTheAllowanceIsRefused() {
        AuthRateLimiter limiter = limiter(1);
        limiter.enforce(AuthRateLimiter.LOGIN, "10.0.0.1");

        assertThatThrownBy(() -> limiter.enforce(AuthRateLimiter.LOGIN, "10.0.0.1"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void anAllowanceOfZeroDisablesTheBucket() {
        AuthRateLimiter limiter = limiter(0);

        for (int i = 0; i < 50; i++) {
            assertThat(limiter.tryRecord(AuthRateLimiter.LOGIN, "10.0.0.1")).isTrue();
        }
    }

    @Test
    void attemptsOutsideTheWindowAreForgotten() throws InterruptedException {
        AuthRateLimiter limiter = new AuthRateLimiter(Duration.ofMillis(50), Map.of(AuthRateLimiter.LOGIN, 1));
        limiter.tryRecord(AuthRateLimiter.LOGIN, "10.0.0.1");
        Thread.sleep(80);

        assertThat(limiter.tryRecord(AuthRateLimiter.LOGIN, "10.0.0.1")).isTrue();
    }
}
