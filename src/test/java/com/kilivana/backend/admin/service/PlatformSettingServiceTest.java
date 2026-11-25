package com.kilivana.backend.admin.service;

import com.kilivana.backend.admin.dto.PlatformSettingRequest;
import com.kilivana.backend.admin.dto.PlatformSettingResponse;
import com.kilivana.backend.admin.entity.AuditLog;
import com.kilivana.backend.admin.entity.PlatformSetting;
import com.kilivana.backend.admin.repository.AuditLogRepository;
import com.kilivana.backend.admin.repository.NotificationPreferenceRepository;
import com.kilivana.backend.admin.repository.PlatformSettingRepository;
import com.kilivana.backend.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlatformSettingServiceTest {

    @Mock private PlatformSettingRepository settingRepository;
    @Mock private NotificationPreferenceRepository preferenceRepository;
    @Mock private AuditLogRepository auditLogRepository;

    private NotificationPreferenceService preferenceService;
    private AuditLogService auditLogService;
    private PlatformSettingService settingService;

    private PlatformSetting defaultSetting;

    @BeforeEach
    void setUp() {
        preferenceService = new NotificationPreferenceService(preferenceRepository);
        auditLogService = new AuditLogService(auditLogRepository);
        settingService = new PlatformSettingService(settingRepository, preferenceService, auditLogService);

        defaultSetting = PlatformSetting.builder()
                .id(1L)
                .transactionFee(new BigDecimal("3.00"))
                .inspectionFee(new BigDecimal("50.00"))
                .payoutDelayDays(3)
                .build();

        when(settingRepository.findTopByOrderByIdAsc()).thenReturn(Optional.of(defaultSetting));
        when(settingRepository.save(any(PlatformSetting.class))).thenAnswer(inv -> inv.getArgument(0));

        for (var event : com.kilivana.backend.admin.enums.NotificationEvent.values()) {
            when(preferenceRepository.findByEventKey(event)).thenReturn(Optional.empty());
        }
        when(preferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Get settings returns current values and preferences")
    void getSettingsReturnsCurrentValues() {
        PlatformSettingResponse response = settingService.getSettings();
        assertEquals(new BigDecimal("3.00"), response.getTransactionFee());
        assertEquals(new BigDecimal("50.00"), response.getInspectionFee());
        assertEquals(3, response.getPayoutDelayDays());
        assertNotNull(response.getNotificationPreferences());
        assertFalse(response.getNotificationPreferences().get("ORDER_PLACED"));
        assertTrue(response.getNotificationPreferences().get("NEW_FARMER_REGISTRATION"));
    }

    @Test
    @DisplayName("Update settings persists new values")
    void updateSettingsPersistsNewValues() {
        PlatformSettingRequest request = new PlatformSettingRequest();
        request.setTransactionFee(new BigDecimal("5.00"));
        request.setInspectionFee(new BigDecimal("75.00"));
        request.setPayoutDelayDays(7);
        request.setNotificationPreferences(Map.of("ORDER_PLACED", true));

        PlatformSettingResponse response = settingService.updateSettings(request, 42L);

        assertEquals(new BigDecimal("5.00"), response.getTransactionFee());
        assertEquals(new BigDecimal("75.00"), response.getInspectionFee());
        assertEquals(7, response.getPayoutDelayDays());

        ArgumentCaptor<PlatformSetting> captor = ArgumentCaptor.forClass(PlatformSetting.class);
        verify(settingRepository).save(captor.capture());
        assertEquals(42L, captor.getValue().getUpdatedBy());
    }

    @Test
    @DisplayName("Update settings records audit log")
    void updateSettingsRecordsAuditLog() {
        PlatformSettingRequest request = new PlatformSettingRequest();
        request.setTransactionFee(new BigDecimal("3.00"));
        request.setInspectionFee(new BigDecimal("50.00"));
        request.setPayoutDelayDays(3);

        settingService.updateSettings(request, 99L);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals(99L, captor.getValue().getActorId());
        assertEquals("UPDATE_PLATFORM_SETTINGS", captor.getValue().getAction());
        assertEquals("PlatformSetting", captor.getValue().getEntityType());
    }

    @Test
    @DisplayName("Reject negative inspection fee")
    void rejectNegativeInspectionFee() {
        PlatformSettingRequest request = new PlatformSettingRequest();
        request.setTransactionFee(new BigDecimal("3.00"));
        request.setInspectionFee(new BigDecimal("-1.00"));
        request.setPayoutDelayDays(3);

        assertThrows(BadRequestException.class, () -> settingService.updateSettings(request, 1L));
    }

    @Test
    @DisplayName("Reject invalid transaction fee")
    void rejectInvalidTransactionFee() {
        PlatformSettingRequest request = new PlatformSettingRequest();
        request.setTransactionFee(new BigDecimal("200.00"));
        request.setInspectionFee(new BigDecimal("50.00"));
        request.setPayoutDelayDays(3);

        assertThrows(BadRequestException.class, () -> settingService.updateSettings(request, 1L));
    }

    @Test
    @DisplayName("Reject invalid payout delay")
    void rejectInvalidPayoutDelay() {
        PlatformSettingRequest request = new PlatformSettingRequest();
        request.setTransactionFee(new BigDecimal("3.00"));
        request.setInspectionFee(new BigDecimal("50.00"));
        request.setPayoutDelayDays(-1);

        assertThrows(BadRequestException.class, () -> settingService.updateSettings(request, 1L));
    }

    @Test
    @DisplayName("Notification preference service returns default when no record exists")
    void preferenceReturnsDefaultWhenMissing() {
        assertTrue(preferenceService.isEnabled(
                com.kilivana.backend.admin.enums.NotificationEvent.NEW_FARMER_REGISTRATION));
        assertFalse(preferenceService.isEnabled(
                com.kilivana.backend.admin.enums.NotificationEvent.ORDER_PLACED));
    }

    @Test
    @DisplayName("Update preferences persists enabled flag")
    void updatePreferencesPersistsFlag() {
        preferenceService.updatePreferences(Map.of("ORDER_PLACED", true));

        ArgumentCaptor<com.kilivana.backend.admin.entity.NotificationPreference> captor =
                ArgumentCaptor.forClass(com.kilivana.backend.admin.entity.NotificationPreference.class);
        verify(preferenceRepository).save(captor.capture());
        assertTrue(captor.getValue().getEnabled());
        assertEquals(com.kilivana.backend.admin.enums.NotificationEvent.ORDER_PLACED,
                captor.getValue().getEventKey());
    }
}