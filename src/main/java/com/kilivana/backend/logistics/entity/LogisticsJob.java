package com.kilivana.backend.logistics.entity;

import com.kilivana.backend.common.enums.DeliveryStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "logistics_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogisticsJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private String pickupAddress;

    @Column(nullable = false)
    private String destinationAddress;

    /**
     * Addresses are free text, so the driver app cannot plot a route from them alone.
     * These are optional: a job created without them still works, it just has no pin.
     */
    private Double pickupLatitude;
    private Double pickupLongitude;
    private Double destinationLatitude;
    private Double destinationLongitude;

    @Column(columnDefinition = "TEXT")
    private String cargoDescription;

    private Integer quantity;

    private BigDecimal payoutAmount;

    private LocalDateTime scheduledPickupAt;
    private LocalDateTime scheduledDropoffAt;

    private Double distanceKm;
    private Integer estimatedMinutes;

    /**
     * BCrypt hash of the shared secret the customer reads out to the driver at handover.
     * The code itself is never stored: it is emailed to the buyer and held nowhere else,
     * so a database read cannot reveal a code that is still live. Cleared once consumed.
     */
    private String deliveryOtpHash;
    private LocalDateTime deliveryOtpExpiresAt;

    /**
     * Records that a code was presented and accepted. Without this, "the code was consumed"
     * and "this job never had a code" look identical once the hash is cleared, and a
     * second proof of delivery could be filed for a job already delivered.
     */
    private Boolean deliveryOtpVerified;

    /**
     * A six-digit code has only a million possible values, so unlimited guessing is a real
     * threat. Attempts are counted and the code is locked for a cooling-off period once the
     * limit is passed; locking rather than regenerating keeps a legitimate driver from
     * invalidating the code a customer is reading out.
     */
    private Integer deliveryOtpAttempts;
    private LocalDateTime deliveryOtpLockedUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    private Long driverId;

    @Column(columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
