package com.kilivana.backend.common.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** A hashed, single-use password recovery credential. */
@Entity
@Table(name = "password_reset_tokens", indexes = {
        @Index(name = "idx_password_reset_token_hash", columnList = "tokenHash", unique = true),
        @Index(name = "idx_password_reset_user", columnList = "userId")})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PasswordResetToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false, unique = true, length = 100)
    private String tokenHash;
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
