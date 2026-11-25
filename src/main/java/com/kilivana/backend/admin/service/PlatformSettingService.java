package com.kilivana.backend.admin.service;

import com.kilivana.backend.admin.dto.PlatformSettingRequest;
import com.kilivana.backend.admin.dto.PlatformSettingResponse;
import com.kilivana.backend.admin.entity.PlatformSetting;
import com.kilivana.backend.admin.repository.PlatformSettingRepository;
import com.kilivana.backend.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformSettingService {

    private final PlatformSettingRepository settingRepository;
    private final NotificationPreferenceService preferenceService;
    private final AuditLogService auditLogService;

    private static final BigDecimal DEFAULT_TRANSACTION_FEE = new BigDecimal("3.00");
    private static final BigDecimal DEFAULT_INSPECTION_FEE = new BigDecimal("50.00");
    private static final int DEFAULT_PAYOUT_DELAY_DAYS = 3;
    private static final BigDecimal MAX_TRANSACTION_FEE = new BigDecimal("100.00");
    private static final int MAX_PAYOUT_DELAY_DAYS = 365;

    @PostConstruct
    @Transactional
    public void initializeDefaults() {
        if (settingRepository.findTopByOrderByIdAsc().isEmpty()) {
            settingRepository.save(PlatformSetting.builder()
                    .transactionFee(DEFAULT_TRANSACTION_FEE)
                    .inspectionFee(DEFAULT_INSPECTION_FEE)
                    .payoutDelayDays(DEFAULT_PAYOUT_DELAY_DAYS)
                    .build());
            log.info("Initialized default platform settings");
        }
    }

    @Transactional(readOnly = true)
    public PlatformSettingResponse getSettings() {
        PlatformSetting setting = settingRepository.findTopByOrderByIdAsc()
                .orElseThrow(() -> new BadRequestException("Platform settings not initialized"));
        return PlatformSettingResponse.fromEntity(setting, preferenceService.getAllPreferences());
    }

    @Transactional
    public PlatformSettingResponse updateSettings(PlatformSettingRequest request, Long adminId) {
        validate(request);

        PlatformSetting setting = settingRepository.findTopByOrderByIdAsc()
                .orElseThrow(() -> new BadRequestException("Platform settings not initialized"));

        setting.setTransactionFee(request.getTransactionFee());
        setting.setInspectionFee(request.getInspectionFee());
        setting.setPayoutDelayDays(request.getPayoutDelayDays());
        setting.setUpdatedBy(adminId);
        PlatformSetting saved = settingRepository.save(setting);

        if (request.getNotificationPreferences() != null) {
            preferenceService.updatePreferences(request.getNotificationPreferences());
        }

        auditLogService.log(adminId, "UPDATE_PLATFORM_SETTINGS", "PlatformSetting", saved.getId(),
                String.format("transactionFee=%s, inspectionFee=%s, payoutDelayDays=%d",
                        saved.getTransactionFee(), saved.getInspectionFee(), saved.getPayoutDelayDays()));

        return PlatformSettingResponse.fromEntity(saved, preferenceService.getAllPreferences());
    }

    private void validate(PlatformSettingRequest request) {
        if (request.getTransactionFee() == null
                || request.getTransactionFee().compareTo(BigDecimal.ZERO) < 0
                || request.getTransactionFee().compareTo(MAX_TRANSACTION_FEE) > 0) {
            throw new BadRequestException("transactionFee must be between 0 and " + MAX_TRANSACTION_FEE);
        }
        if (request.getInspectionFee() == null
                || request.getInspectionFee().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("inspectionFee must be non-negative");
        }
        if (request.getPayoutDelayDays() == null
                || request.getPayoutDelayDays() < 0
                || request.getPayoutDelayDays() > MAX_PAYOUT_DELAY_DAYS) {
            throw new BadRequestException("payoutDelayDays must be between 0 and " + MAX_PAYOUT_DELAY_DAYS);
        }
    }
}