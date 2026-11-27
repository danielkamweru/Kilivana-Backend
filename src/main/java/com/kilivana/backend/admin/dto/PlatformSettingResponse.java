package com.kilivana.backend.admin.dto;

import com.kilivana.backend.admin.entity.PlatformSetting;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class PlatformSettingResponse {

    private BigDecimal transactionFee;
    private BigDecimal inspectionFee;
    private Integer payoutDelayDays;

    // Platform fee settings
    private String platformFeeType;
    private BigDecimal platformFeeValue;
    private String platformFeeChargedTo;
    private Map<String, BigDecimal> platformFeeByCategory;

    // Transport fee settings
    private String transportFeeType;
    private BigDecimal transportFeeDefault;
    private BigDecimal transportFeePerKm;
    private BigDecimal transportFeeMin;
    private BigDecimal transportFeeMax;
    private Boolean transportFeePaidToProvider;
    private Boolean transportFeePerShipment;

    // Payout settings
    private BigDecimal minWithdrawalAmount;
    private BigDecimal maxWithdrawalAmount;
    private Boolean withdrawalApprovalRequired;
    private Boolean autoPayoutEnabled;
    private String supportedPayoutMethods;
    private Integer payoutRetryLimit;

    private Map<String, Boolean> notificationPreferences;
    private LocalDateTime updatedAt;

    public static PlatformSettingResponse fromEntity(PlatformSetting setting, Map<String, Boolean> prefs) {
        return PlatformSettingResponse.builder()
                .transactionFee(setting.getTransactionFee())
                .inspectionFee(setting.getInspectionFee())
                .payoutDelayDays(setting.getPayoutDelayDays())
                .platformFeeType(setting.getPlatformFeeType())
                .platformFeeValue(setting.getPlatformFeeValue())
                .platformFeeChargedTo(setting.getPlatformFeeChargedTo())
                .platformFeeByCategory(setting.getPlatformFeeByCategory())
                .transportFeeType(setting.getTransportFeeType())
                .transportFeeDefault(setting.getTransportFeeDefault())
                .transportFeePerKm(setting.getTransportFeePerKm())
                .transportFeeMin(setting.getTransportFeeMin())
                .transportFeeMax(setting.getTransportFeeMax())
                .transportFeePaidToProvider(setting.getTransportFeePaidToProvider())
                .transportFeePerShipment(setting.getTransportFeePerShipment())
                .minWithdrawalAmount(setting.getMinWithdrawalAmount())
                .maxWithdrawalAmount(setting.getMaxWithdrawalAmount())
                .withdrawalApprovalRequired(setting.getWithdrawalApprovalRequired())
                .autoPayoutEnabled(setting.getAutoPayoutEnabled())
                .supportedPayoutMethods(setting.getSupportedPayoutMethods())
                .payoutRetryLimit(setting.getPayoutRetryLimit())
                .notificationPreferences(prefs)
                .updatedAt(setting.getUpdatedAt())
                .build();
    }
}