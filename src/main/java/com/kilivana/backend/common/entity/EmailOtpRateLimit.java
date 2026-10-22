package com.kilivana.backend.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

/** HMAC bucket key; no email address or IP is stored here. */
@Entity @Table(name="email_otp_rate_limits") @Getter @Setter
public class EmailOtpRateLimit {
    @Id @Column(length=64) private String bucketKey;
    @Column(nullable=false) private Instant resetAt;
    @Column(nullable=false) private int requests;
    private Instant lastRequestedAt;
    private java.util.UUID latestChallengeId;
}
