package com.kilivana.backend.common.entity;

import com.kilivana.backend.common.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

/** One mutable challenge per account. Never contains a plaintext code. */
@Entity @Table(name="email_verification_challenges") @Getter @Setter
public class EmailVerificationChallenge {
    @Id private Long userId;
    @Column(nullable=false, unique=true) private UUID challengeId;
    @Column(nullable=false) private String email;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private UserRole role;
    @Column(nullable=false) private String purpose;
    @Column(nullable=false, length=64) private String codeHash;
    @Column(nullable=false) private Instant expiresAt;
    private Instant usedAt;
    @Column(nullable=false) private int failedAttempts;
    @Column(nullable=false) private Instant attemptsResetAt;
}
