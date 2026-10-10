package com.kilivana.backend.logistics.config;

import com.kilivana.backend.logistics.service.LocationRateLimiter;
import com.kilivana.backend.logistics.service.LocationValidator;
import com.kilivana.backend.logistics.service.TrackingBroadcastService;
import com.kilivana.backend.logistics.service.TrackingAuthorizationService;
import com.kilivana.backend.logistics.service.DriverLocationService;
import com.kilivana.backend.logistics.service.TrackingStopService;
import com.kilivana.backend.logistics.repository.DriverLatestLocationRepository;
import com.kilivana.backend.logistics.repository.DriverLocationRepository;
import com.kilivana.backend.logistics.repository.LogisticsJobRepository;
import com.kilivana.backend.common.service.UserReferenceCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;

/**
 * Wires the tracking beans together from externalised properties so a deployment can
 * tune validation, throttling and retention without touching code.
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class TrackingConfig {

    private final TrackingProperties properties;
    private final DriverLatestLocationRepository latestRepository;
    private final DriverLocationRepository locationRepository;
    private final LogisticsJobRepository logisticsJobRepository;
    private final TrackingAuthorizationService authorizationService;
    private final TrackingBroadcastService broadcastService;

    @Bean
    public LocationValidator locationValidator() {
        return new LocationValidator(
                Duration.ofMinutes(properties.getMaxClockSkewMinutes()),
                Duration.ofMinutes(1), // minimum interval is enforced by the rate limiter
                properties.getDuplicateThresholdDegrees());
    }

    @Bean
    public LocationRateLimiter locationRateLimiter() {
        return new LocationRateLimiter(
                Duration.ofMinutes(properties.getRateLimitWindowMinutes()),
                properties.getRateLimitMaxPerWindow());
    }

    @Bean
    public DriverLocationService driverLocationService() {
        return new DriverLocationService(
                logisticsJobRepository,
                locationRepository,
                latestRepository,
                locationValidator(),
                locationRateLimiter(),
                authorizationService,
                broadcastService);
    }

    @Bean
    public TrackingStopService trackingStopService() {
        return new TrackingStopService(logisticsJobRepository, broadcastService);
    }

    /** Prunes the high-frequency stream on a fixed cadence so it cannot grow without bound. */
    @Scheduled(fixedDelay = 3600_000, initialDelay = 3600_000)
    public void pruneStaleFixes() {
        long cutoff = System.currentTimeMillis()
                - Duration.ofDays(properties.getHistoryRetentionDays()).toMillis();
        int deleted = locationRepository.deleteByRecordedAtBefore(
                java.time.Instant.ofEpochMilli(cutoff));
        if (deleted > 0) {
            org.slf4j.LoggerFactory.getLogger(TrackingConfig.class)
                    .info("Pruned {} stale location fixes older than {} days",
                            deleted, properties.getHistoryRetentionDays());
        }
    }
}