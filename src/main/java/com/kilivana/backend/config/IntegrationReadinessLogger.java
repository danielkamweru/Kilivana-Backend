package com.kilivana.backend.config;

import com.kilivana.backend.common.service.ImageStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Logs the image storage provider configuration at startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntegrationReadinessLogger {

    private final ImageStorage imageStorage;

    @EventListener(ApplicationReadyEvent.class)
    public void logConfigurationState() {
        log.info("Image storage provider: {}", imageStorage.providerName());
        if (!imageStorage.isConfigured()) {
            log.warn("Image storage is NOT configured. Image uploads may fail.");
        }
    }
}