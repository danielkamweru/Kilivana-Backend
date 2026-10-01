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
     * Shared secret the customer reads out to the driver on handover. Generated when the
     * job is created, cleared once a proof of delivery has consumed it.
     */
    private String deliveryOtp;
    private LocalDateTime deliveryOtpExpiresAt;

    /**
     * Records that a code was presented and accepted. Without this, "the code was consumed"
     * and "this job never had a code" look identical once deliveryOtp is cleared, and a
     * second proof of delivery could be filed for a job already delivered.
     */
    private Boolean deliveryOtpVerified;

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
