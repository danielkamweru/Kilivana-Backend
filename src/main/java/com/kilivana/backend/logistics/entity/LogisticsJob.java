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

    // Explicit getters and setters for Lombok compatibility
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }
    public String getDestinationAddress() { return destinationAddress; }
    public void setDestinationAddress(String destinationAddress) { this.destinationAddress = destinationAddress; }
    public Double getPickupLatitude() { return pickupLatitude; }
    public void setPickupLatitude(Double pickupLatitude) { this.pickupLatitude = pickupLatitude; }
    public Double getPickupLongitude() { return pickupLongitude; }
    public void setPickupLongitude(Double pickupLongitude) { this.pickupLongitude = pickupLongitude; }
    public Double getDestinationLatitude() { return destinationLatitude; }
    public void setDestinationLatitude(Double destinationLatitude) { this.destinationLatitude = destinationLatitude; }
    public Double getDestinationLongitude() { return destinationLongitude; }
    public void setDestinationLongitude(Double destinationLongitude) { this.destinationLongitude = destinationLongitude; }
    public String getCargoDescription() { return cargoDescription; }
    public void setCargoDescription(String cargoDescription) { this.cargoDescription = cargoDescription; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getPayoutAmount() { return payoutAmount; }
    public void setPayoutAmount(BigDecimal payoutAmount) { this.payoutAmount = payoutAmount; }
    public LocalDateTime getScheduledPickupAt() { return scheduledPickupAt; }
    public void setScheduledPickupAt(LocalDateTime scheduledPickupAt) { this.scheduledPickupAt = scheduledPickupAt; }
    public LocalDateTime getScheduledDropoffAt() { return scheduledDropoffAt; }
    public void setScheduledDropoffAt(LocalDateTime scheduledDropoffAt) { this.scheduledDropoffAt = scheduledDropoffAt; }
    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }
    public Integer getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(Integer estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }
    public String getDeliveryOtpHash() { return deliveryOtpHash; }
    public void setDeliveryOtpHash(String deliveryOtpHash) { this.deliveryOtpHash = deliveryOtpHash; }
    public LocalDateTime getDeliveryOtpExpiresAt() { return deliveryOtpExpiresAt; }
    public void setDeliveryOtpExpiresAt(LocalDateTime deliveryOtpExpiresAt) { this.deliveryOtpExpiresAt = deliveryOtpExpiresAt; }
    public Boolean getDeliveryOtpVerified() { return deliveryOtpVerified; }
    public void setDeliveryOtpVerified(Boolean deliveryOtpVerified) { this.deliveryOtpVerified = deliveryOtpVerified; }
    public Integer getDeliveryOtpAttempts() { return deliveryOtpAttempts; }
    public void setDeliveryOtpAttempts(Integer deliveryOtpAttempts) { this.deliveryOtpAttempts = deliveryOtpAttempts; }
    public LocalDateTime getDeliveryOtpLockedUntil() { return deliveryOtpLockedUntil; }
    public void setDeliveryOtpLockedUntil(LocalDateTime deliveryOtpLockedUntil) { this.deliveryOtpLockedUntil = deliveryOtpLockedUntil; }
    public DeliveryStatus getStatus() { return status; }
    public void setStatus(DeliveryStatus status) { this.status = status; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
