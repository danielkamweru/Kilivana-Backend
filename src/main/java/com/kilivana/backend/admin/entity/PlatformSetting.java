package com.kilivana.backend.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Singleton platform configuration: transaction fee, inspection fee, and payout delay.
 * There is exactly one row (id = 1); the service layer enforces this.
 */
@Entity
@Table(name = "platform_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal transactionFee;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal inspectionFee;

    @Column(nullable = false)
    private Integer payoutDelayDays;

    // Platform fee settings
    @Column(name = "platform_fee_type", nullable = false, length = 16)
    private String platformFeeType = "PERCENTAGE"; // PERCENTAGE, FIXED

    @Column(name = "platform_fee_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal platformFeeValue = new BigDecimal("3.00");

    @Column(name = "platform_fee_charged_to", nullable = false, length = 16)
    private String platformFeeChargedTo = "SELLER"; // SELLER, BUYER

    @Column(name = "platform_fee_by_category", columnDefinition = "JSONB")
    private Map<String, BigDecimal> platformFeeByCategory; // {"FARMER": 3.0, "SUPPLIER": 5.0}

    // Transport fee settings
    @Column(name = "transport_fee_type", nullable = false, length = 16)
    private String transportFeeType = "FIXED"; // FIXED, PER_KM

    @Column(name = "transport_fee_default", nullable = false, precision = 10, scale = 2)
    private BigDecimal transportFeeDefault = new BigDecimal("100.00");

    @Column(name = "transport_fee_per_km", precision = 10, scale = 2)
    private BigDecimal transportFeePerKm;

    @Column(name = "transport_fee_min", precision = 10, scale = 2)
    private BigDecimal transportFeeMin;

    @Column(name = "transport_fee_max", precision = 10, scale = 2)
    private BigDecimal transportFeeMax;

    @Column(name = "transport_fee_paid_to_provider", nullable = false)
    private Boolean transportFeePaidToProvider = true;

    @Column(name = "transport_fee_per_shipment", nullable = false)
    private Boolean transportFeePerShipment = true;

    // Payout settings
    @Column(name = "min_withdrawal_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal minWithdrawalAmount = new BigDecimal("100.00");

    @Column(name = "max_withdrawal_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal maxWithdrawalAmount = new BigDecimal("70000.00");

    @Column(name = "withdrawal_approval_required", nullable = false)
    private Boolean withdrawalApprovalRequired = true;

    @Column(name = "auto_payout_enabled", nullable = false)
    private Boolean autoPayoutEnabled = false;

    @Column(name = "supported_payout_methods", nullable = false, length = 255)
    private String supportedPayoutMethods = "MPESA_B2C";

    @Column(name = "payout_retry_limit", nullable = false)
    private Integer payoutRetryLimit = 3;

    private Long updatedBy;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Helper methods for backward compatibility
    public BigDecimal getEffectivePlatformFee() {
        return this.platformFeeValue != null ? this.platformFeeValue : this.transactionFee;
    }

    public String getEffectivePlatformFeeType() {
        return this.platformFeeType != null ? this.platformFeeType : "PERCENTAGE";
    }
}