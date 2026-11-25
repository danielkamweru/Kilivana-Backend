package com.kilivana.backend.admin.service;

import com.kilivana.backend.admin.entity.NotificationPreference;
import com.kilivana.backend.admin.enums.NotificationEvent;
import com.kilivana.backend.admin.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    private static final Map<NotificationEvent, Boolean> DEFAULTS = new EnumMap<>(NotificationEvent.class);
    static {
        DEFAULTS.put(NotificationEvent.NEW_FARMER_REGISTRATION, true);
        DEFAULTS.put(NotificationEvent.KYC_SUBMITTED, true);
        DEFAULTS.put(NotificationEvent.DISPUTE_OPENED, true);
        DEFAULTS.put(NotificationEvent.ORDER_PLACED, false);
        DEFAULTS.put(NotificationEvent.PAYOUT_PROCESSED, true);
    }

    @PostConstruct
    @Transactional
    public void initializeDefaults() {
        for (NotificationEvent event : NotificationEvent.values()) {
            if (preferenceRepository.findByEventKey(event).isEmpty()) {
                preferenceRepository.save(NotificationPreference.builder()
                        .eventKey(event)
                        .enabled(DEFAULTS.get(event))
                        .build());
                log.info("Initialized default notification preference: {} = {}", event, DEFAULTS.get(event));
            }
        }
    }

    @Transactional(readOnly = true)
    public boolean isEnabled(NotificationEvent event) {
        return preferenceRepository.findByEventKey(event)
                .map(NotificationPreference::getEnabled)
                .orElse(DEFAULTS.getOrDefault(event, false));
    }

    @Transactional(readOnly = true)
    public Map<String, Boolean> getAllPreferences() {
        Map<String, Boolean> result = new java.util.LinkedHashMap<>();
        for (NotificationEvent event : NotificationEvent.values()) {
            boolean enabled = preferenceRepository.findByEventKey(event)
                    .map(NotificationPreference::getEnabled)
                    .orElse(DEFAULTS.getOrDefault(event, false));
            result.put(event.name(), enabled);
        }
        return result;
    }

    @Transactional
    public void updatePreferences(Map<String, Boolean> updates) {
        if (updates == null || updates.isEmpty()) return;
        for (Map.Entry<String, Boolean> entry : updates.entrySet()) {
            try {
                NotificationEvent event = NotificationEvent.valueOf(entry.getKey());
                NotificationPreference pref = preferenceRepository.findByEventKey(event)
                        .orElseGet(() -> NotificationPreference.builder()
                                .eventKey(event)
                                .enabled(DEFAULTS.getOrDefault(event, false))
                                .build());
                pref.setEnabled(entry.getValue());
                preferenceRepository.save(pref);
                log.info("Updated notification preference: {} = {}", event, entry.getValue());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown notification event key: {}", entry.getKey());
            }
        }
    }
}