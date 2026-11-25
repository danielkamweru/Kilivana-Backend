package com.kilivana.backend.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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

    @Valid
    private Map<String, Boolean> notificationPreferences;
}