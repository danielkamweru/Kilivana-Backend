package com.kilivana.backend.common.service;

import com.kilivana.backend.common.exception.EmailOtpCooldownException;
import com.kilivana.backend.common.exception.EmailOtpRateLimitException;
import com.kilivana.backend.common.repository.EmailOtpRateLimitRepository;
import com.kilivana.backend.security.EmailOtpSecurity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.context.request.*;
import org.springframework.beans.factory.annotation.Value;
import java.time.*;

/**
 * Rate limits for email-verification OTP sends, backed by the {@code email_otp_rate_limits}
 * table with pessimistic locking so concurrent callers cannot share a budget.
 *
 * <p>Three buckets are enforced per send:
 * <ul>
 *   <li>per-IP hourly allowance, so one noisy network cannot flood everyone</li>
 *   <li>per-email hourly allowance, so a single address cannot burn its own budget</li>
 *   <li>a short cooldown between two sends to the same address, so a stuck UI cannot
 *       hammer the mailer</li>
 * </ul>
 *
 * <p>The limits are externalised so a deployment can tune them without a code change;
 * a value of zero disables that bucket entirely. The defaults match the previous
 * hard-coded behaviour.
 */
@Service
@RequiredArgsConstructor
public class EmailOtpRequestLimiter {
    private final EmailOtpRateLimitRepository buckets;
    private final EmailOtpSecurity security;

    /** Max OTP-send attempts per client IP per rolling hour. Zero disables the IP bucket. */
    @Value("${app.email-verification.send-ip-max-per-hour:60}")
    private int sendIpMaxPerHour;

    /** Max OTP-send attempts per email per rolling hour. Zero disables the email bucket. */
    @Value("${app.email-verification.send-email-max-per-hour:5}")
    private int sendEmailMaxPerHour;

    /** Minimum gap between two OTP sends for the same email, in seconds. Zero removes the cooldown. */
    @Value("${app.email-verification.send-cooldown-seconds:60}")
    private int sendCooldownSeconds;

    /** Commits limits even if a later send or registration fails. All addresses get identical buckets. */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = EmailOtpRateLimitException.class)
    public java.util.UUID acquireSend(String email) {
        consume("send-ip", clientAddress(), sendIpMaxPerHour, 0);
        var bucket = consume("send-email", EmailOtpSecurity.normalize(email), sendEmailMaxPerHour, sendCooldownSeconds);
        var id = java.util.UUID.randomUUID();
        bucket.setLatestChallengeId(id);
        buckets.save(bucket);
        return id;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = EmailOtpRateLimitException.class)
    public void acquireVerification() {
        consume("verify-ip", clientAddress(), 120, 0);
    }

    private com.kilivana.backend.common.entity.EmailOtpRateLimit consume(
            String scope, String identity, int maximum, int cooldownSeconds) {
        Instant now = security.now();
        String key = security.bucketKey(scope, identity);
        buckets.ensureBucket(key, now.plusSeconds(3600));
        var bucket = buckets.lockBucket(key).orElseThrow();
        if (!bucket.getResetAt().isAfter(now)) {
            bucket.setRequests(0);
            bucket.setResetAt(now.plusSeconds(3600));
        }
        if (bucket.getRequests() >= maximum) {
            throw new EmailOtpRateLimitException(Duration.between(now, bucket.getResetAt()).getSeconds() + 1);
        }
        if (cooldownSeconds > 0
                && bucket.getLastRequestedAt() != null
                && bucket.getLastRequestedAt().plusSeconds(cooldownSeconds).isAfter(now)) {
            throw new EmailOtpCooldownException(
                    Duration.between(now, bucket.getLastRequestedAt().plusSeconds(cooldownSeconds)).getSeconds() + 1);
        }
        bucket.setRequests(bucket.getRequests() + 1);
        bucket.setLastRequestedAt(now);
        buckets.save(bucket);
        return bucket;
    }

    private String clientAddress() {
        var attrs = RequestContextHolder.getRequestAttributes();
        return attrs instanceof ServletRequestAttributes servlet
                ? servlet.getRequest().getRemoteAddr()
                : "internal";
    }
}