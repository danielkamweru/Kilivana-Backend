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
    private Map<String, Boolean> notificationPreferences;
    private LocalDateTime updatedAt;

    public static PlatformSettingResponse fromEntity(PlatformSetting setting, Map<String, Boolean> prefs) {
        return PlatformSettingResponse.builder()
                .transactionFee(setting.getTransactionFee())
                .inspectionFee(setting.getInspectionFee())
                .payoutDelayDays(setting.getPayoutDelayDays())
                .notificationPreferences(prefs)
                .updatedAt(setting.getUpdatedAt())
                .build();
    }
}