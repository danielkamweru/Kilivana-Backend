package com.kilivana.backend.finance.entity;

import com.kilivana.backend.finance.enums.DeliveryOtpStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks delivery OTP requests and verifications per shipment.
 * Separate from LogisticsJob OTP to enable shipment-level tracking.
 */
@Entity
@Table(name = "delivery_otp_verifications",
        indexes = {
                @Index(name = "idx_delivery_otp_shipment", columnList = "shipment_id"),
                @Index(name = "idx_delivery_otp_driver", columnList = "driver_id"),
                @Index(name = "idx_delivery_otp_buyer", columnList = "buyer_id"),
                @Index(name = "idx_delivery_otp_status", columnList = "status"),
                @Index(name = "idx_delivery_otp_settlement", columnList = "settlement_id")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryOtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UUID for external references. */
    @Column(nullable = false, updatable = false, unique = true)
    private UUID uuid;

    /** The shipment this OTP is for. */
    @Column(name = "shipment_id", nullable = false)
    private Long shipmentId;

    /** The parent order. */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** The driver who requested the OTP. */
    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    /** The buyer who receives the OTP. */
    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    // ==================== OTP DETAILS ====================

    /** BCrypt hash of the OTP. Never stored in plaintext. */
    @Column(name = "otp_hash", nullable = false, length = 255)
    private String otpHash;

    /** When the OTP expires. Null means no expiry (configurable). */
    @Column(name = "otp_expires_at")
    private LocalDateTime otpExpiresAt;

    /** Number of failed verification attempts. */
    @Column(name = "otp_attempts", nullable = false)
    @Builder.Default
    private Integer otpAttempts = 0;

    /** When the OTP is locked due to too many failed attempts. */
    @Column(name = "otp_locked_until")
    private LocalDateTime otpLockedUntil;

    /** Whether the OTP has been verified. */
    @Column(name = "otp_verified", nullable = false)
    @Builder.Default
    private Boolean otpVerified = false;

    /** When the OTP was verified. */
    @Column(name = "otp_verified_at")
    private LocalDateTime otpVerifiedAt;

    // ==================== STATUS ====================

    /** Current status of the OTP verification. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private DeliveryOtpStatus status = DeliveryOtpStatus.PENDING;

    // ==================== TIMESTAMPS ====================

    /** When the driver requested the OTP. */
    @Column(name = "requested_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime requestedAt;

    /** When the OTP was sent to the buyer. */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /** When the OTP was verified. */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    /** When the OTP was used (delivery confirmed). */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    // ==================== REFERENCES ====================

    /** Reference to the settlement created after verification. */
    @Column(name = "settlement_id")
    private Long settlementId;

    /** Reference to the logistics job (if applicable). */
    @Column(name = "logistics_job_id")
    private Long logisticsJobId;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }

    // ==================== HELPER METHODS ====================

    public boolean isExpired() {
        return otpExpiresAt != null && LocalDateTime.now().isAfter(otpExpiresAt);
    }

    public boolean isLocked() {
        return otpLockedUntil != null && LocalDateTime.now().isBefore(otpLockedUntil);
    }

    public boolean canVerify() {
        return status == DeliveryOtpStatus.SENT
                && !isExpired()
                && !isLocked()
                && !otpVerified;
    }
}