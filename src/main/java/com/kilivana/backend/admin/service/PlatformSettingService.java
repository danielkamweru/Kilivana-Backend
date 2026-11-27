package com.kilivana.backend.admin.service;

import com.kilivana.backend.admin.dto.PlatformSettingRequest;
import com.kilivana.backend.admin.dto.PlatformSettingResponse;
import com.kilivana.backend.admin.entity.PlatformSetting;
import com.kilivana.backend.admin.repository.PlatformSettingRepository;
import com.kilivana.backend.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

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
    private static final BigDecimal MAX_FEE_VALUE = new BigDecimal("100000.00");

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeDefaults() {
        try {
            if (settingRepository.findTopByOrderByIdAsc().isEmpty()) {
                settingRepository.save(PlatformSetting.builder()
                        .transactionFee(DEFAULT_TRANSACTION_FEE)
                        .inspectionFee(DEFAULT_INSPECTION_FEE)
                        .payoutDelayDays(DEFAULT_PAYOUT_DELAY_DAYS)
                        .build());
                log.info("Initialized default platform settings");
            }
        } catch (Exception e) {
            log.warn("Could not initialize platform settings (schema may not be ready yet): {}", e.getMessage());
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

        // Update core settings
        if (request.getTransactionFee() != null) {
            setting.setTransactionFee(request.getTransactionFee());
        }
        if (request.getInspectionFee() != null) {
            setting.setInspectionFee(request.getInspectionFee());
        }
        if (request.getPayoutDelayDays() != null) {
            setting.setPayoutDelayDays(request.getPayoutDelayDays());
        }

        // Update platform fee settings
        if (request.getPlatformFeeType() != null) {
            setting.setPlatformFeeType(request.getPlatformFeeType());
        }
        if (request.getPlatformFeeValue() != null) {
            setting.setPlatformFeeValue(request.getPlatformFeeValue());
        }
        if (request.getPlatformFeeChargedTo() != null) {
            setting.setPlatformFeeChargedTo(request.getPlatformFeeChargedTo());
        }
        if (request.getPlatformFeeByCategory() != null) {
            setting.setPlatformFeeByCategory(request.getPlatformFeeByCategory());
        }

        // Update transport fee settings
        if (request.getTransportFeeType() != null) {
            setting.setTransportFeeType(request.getTransportFeeType());
        }
        if (request.getTransportFeeDefault() != null) {
            setting.setTransportFeeDefault(request.getTransportFeeDefault());
        }
        if (request.getTransportFeePerKm() != null) {
            setting.setTransportFeePerKm(request.getTransportFeePerKm());
        }
        if (request.getTransportFeeMin() != null) {
            setting.setTransportFeeMin(request.getTransportFeeMin());
        }
        if (request.getTransportFeeMax() != null) {
            setting.setTransportFeeMax(request.getTransportFeeMax());
        }
        if (request.getTransportFeePaidToProvider() != null) {
            setting.setTransportFeePaidToProvider(request.getTransportFeePaidToProvider());
        }
        if (request.getTransportFeePerShipment() != null) {
            setting.setTransportFeePerShipment(request.getTransportFeePerShipment());
        }

        // Update payout settings
        if (request.getMinWithdrawalAmount() != null) {
            setting.setMinWithdrawalAmount(request.getMinWithdrawalAmount());
        }
        if (request.getMaxWithdrawalAmount() != null) {
            setting.setMaxWithdrawalAmount(request.getMaxWithdrawalAmount());
        }
        if (request.getWithdrawalApprovalRequired() != null) {
            setting.setWithdrawalApprovalRequired(request.getWithdrawalApprovalRequired());
        }
        if (request.getAutoPayoutEnabled() != null) {
            setting.setAutoPayoutEnabled(request.getAutoPayoutEnabled());
        }
        if (request.getSupportedPayoutMethods() != null) {
            setting.setSupportedPayoutMethods(request.getSupportedPayoutMethods());
        }
        if (request.getPayoutRetryLimit() != null) {
            setting.setPayoutRetryLimit(request.getPayoutRetryLimit());
        }

        setting.setUpdatedBy(adminId);
        PlatformSetting saved = settingRepository.save(setting);

        if (request.getNotificationPreferences() != null) {
            preferenceService.updatePreferences(request.getNotificationPreferences());
        }

        auditLogService.log(adminId, "UPDATE_PLATFORM_SETTINGS", "PlatformSetting", saved.getId(),
                String.format("transactionFee=%s, inspectionFee=%s, payoutDelayDays=%d, platformFeeType=%s, platformFeeValue=%s, transportFeeType=%s, transportFeeDefault=%s",
                        saved.getTransactionFee(), saved.getInspectionFee(), saved.getPayoutDelayDays(),
                        saved.getPlatformFeeType(), saved.getPlatformFeeValue(),
                        saved.getTransportFeeType(), saved.getTransportFeeDefault()));

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

        // Validate platform fee
        if (request.getPlatformFeeValue() != null
                && (request.getPlatformFeeValue().compareTo(BigDecimal.ZERO) < 0
                || request.getPlatformFeeValue().compareTo(MAX_FEE_VALUE) > 0)) {
            throw new BadRequestException("platformFeeValue must be between 0 and " + MAX_FEE_VALUE);
        }
        if (request.getPlatformFeeType() != null
                && !"PERCENTAGE".equals(request.getPlatformFeeType())
                && !"FIXED".equals(request.getPlatformFeeType())) {
            throw new BadRequestException("platformFeeType must be PERCENTAGE or FIXED");
        }
        if (request.getPlatformFeeChargedTo() != null
                && !"SELLER".equals(request.getPlatformFeeChargedTo())
                && !"BUYER".equals(request.getPlatformFeeChargedTo())) {
            throw new BadRequestException("platformFeeChargedTo must be SELLER or BUYER");
        }
        if (request.getPlatformFeeByCategory() != null) {
            for (Map.Entry<String, BigDecimal> entry : request.getPlatformFeeByCategory().entrySet()) {
                if (entry.getValue() == null || entry.getValue().compareTo(BigDecimal.ZERO) < 0
                        || entry.getValue().compareTo(new BigDecimal("100")) > 0) {
                    throw new BadRequestException("platformFeeByCategory values must be between 0 and 100");
                }
            }
        }

        // Validate transport fee
        if (request.getTransportFeeDefault() != null
                && (request.getTransportFeeDefault().compareTo(BigDecimal.ZERO) < 0
                || request.getTransportFeeDefault().compareTo(MAX_FEE_VALUE) > 0)) {
            throw new BadRequestException("transportFeeDefault must be between 0 and " + MAX_FEE_VALUE);
        }
        if (request.getTransportFeePerKm() != null
                && request.getTransportFeePerKm().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("transportFeePerKm must be non-negative");
        }
        if (request.getTransportFeeMin() != null
                && request.getTransportFeeMin().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("transportFeeMin must be non-negative");
        }
        if (request.getTransportFeeMax() != null
                && request.getTransportFeeMax().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("transportFeeMax must be non-negative");
        }
        if (request.getTransportFeeMin() != null && request.getTransportFeeMax() != null
                && request.getTransportFeeMin().compareTo(request.getTransportFeeMax()) > 0) {
            throw new BadRequestException("transportFeeMin cannot exceed transportFeeMax");
        }
        if (request.getTransportFeeType() != null
                && !"FIXED".equals(request.getTransportFeeType())
                && !"PER_KM".equals(request.getTransportFeeType())) {
            throw new BadRequestException("transportFeeType must be FIXED or PER_KM");
        }

        // Validate payout settings
        if (request.getMinWithdrawalAmount() != null
                && request.getMinWithdrawalAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("minWithdrawalAmount must be non-negative");
        }
        if (request.getMaxWithdrawalAmount() != null
                && request.getMaxWithdrawalAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("maxWithdrawalAmount must be non-negative");
        }
        if (request.getMinWithdrawalAmount() != null && request.getMaxWithdrawalAmount() != null
                && request.getMinWithdrawalAmount().compareTo(request.getMaxWithdrawalAmount()) > 0) {
            throw new BadRequestException("minWithdrawalAmount cannot exceed maxWithdrawalAmount");
        }
        if (request.getPayoutRetryLimit() != null && request.getPayoutRetryLimit() < 0) {
            throw new BadRequestException("payoutRetryLimit must be non-negative");
        }
    }

    // Helper method for other services to get effective platform fee
    @Transactional(readOnly = true)
    public BigDecimal getEffectivePlatformFeeValue() {
        PlatformSetting setting = settingRepository.findTopByOrderByIdAsc()
                .orElseThrow(() -> new BadRequestException("Platform settings not initialized"));
        return setting.getEffectivePlatformFee();
    }

    @Transactional(readOnly = true)
    public String getEffectivePlatformFeeType() {
        PlatformSetting setting = settingRepository.findTopByOrderByIdAsc()
                .orElseThrow(() -> new BadRequestException("Platform settings not initialized"));
        return setting.getEffectivePlatformFeeType();
    }
}