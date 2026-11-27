package com.kilivana.backend.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class PlatformSettingRequest {

    @NotNull(message = "transactionFee is required")
    @DecimalMin(value = "0.0", message = "transactionFee must be non-negative")
    private BigDecimal transactionFee;

    @NotNull(message = "inspectionFee is required")
    @DecimalMin(value = "0.0", message = "inspectionFee must be non-negative")
    private BigDecimal inspectionFee;

    @NotNull(message = "payoutDelayDays is required")
    @Min(value = 0, message = "payoutDelayDays must be non-negative")
    private Integer payoutDelayDays;

    // Platform fee settings
    @Pattern(regexp = "PERCENTAGE|FIXED", message = "platformFeeType must be PERCENTAGE or FIXED")
    private String platformFeeType;

    @DecimalMin(value = "0.0", message = "platformFeeValue must be non-negative")
    private BigDecimal platformFeeValue;

    @Pattern(regexp = "SELLER|BUYER", message = "platformFeeChargedTo must be SELLER or BUYER")
    private String platformFeeChargedTo;

    private Map<String, BigDecimal> platformFeeByCategory;

    // Transport fee settings
    @Pattern(regexp = "FIXED|PER_KM", message = "transportFeeType must be FIXED or PER_KM")
    private String transportFeeType;

    @DecimalMin(value = "0.0", message = "transportFeeDefault must be non-negative")
    private BigDecimal transportFeeDefault;

    @DecimalMin(value = "0.0", message = "transportFeePerKm must be non-negative")
    private BigDecimal transportFeePerKm;

    @DecimalMin(value = "0.0", message = "transportFeeMin must be non-negative")
    private BigDecimal transportFeeMin;

    @DecimalMin(value = "0.0", message = "transportFeeMax must be non-negative")
    private BigDecimal transportFeeMax;

    private Boolean transportFeePaidToProvider;

    private Boolean transportFeePerShipment;

    // Payout settings
    @DecimalMin(value = "0.0", message = "minWithdrawalAmount must be non-negative")
    private BigDecimal minWithdrawalAmount;

    @DecimalMin(value = "0.0", message = "maxWithdrawalAmount must be non-negative")
    private BigDecimal maxWithdrawalAmount;

    private Boolean withdrawalApprovalRequired;

    private Boolean autoPayoutEnabled;

    private String supportedPayoutMethods;

    @Min(value = 0, message = "payoutRetryLimit must be non-negative")
    private Integer payoutRetryLimit;

    @Valid
    private Map<String, Boolean> notificationPreferences;
}