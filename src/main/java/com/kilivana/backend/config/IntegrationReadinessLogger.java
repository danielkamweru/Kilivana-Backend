package com.kilivana.backend.config;

import com.kilivana.backend.common.service.ImageStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Warns at startup when an optional integration is not configured. Both SendGrid and
 * Cloudinary degrade to a clear 503 at request time, but a startup warning makes a missing
 * key obvious before anyone spends time debugging a failed upload or a delivery code that
 * never reaches the buyer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntegrationReadinessLogger {

    private final ImageStorage cloudinaryService;

    @EventListener(ApplicationReadyEvent.class)
    public void logConfigurationState() {
        if (cloudinaryService.isConfigured()) {
            log.info("Cloudinary image storage is configured");
        } else {
            log.warn("Cloudinary is NOT configured. Every image upload will fail with 503 "
                    + "SERVICE_UNAVAILABLE until CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY and "
                    + "CLOUDINARY_API_SECRET are set.");
        }
    }
}