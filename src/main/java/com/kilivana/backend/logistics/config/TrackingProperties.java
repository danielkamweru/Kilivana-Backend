package com.kilivana.backend.logistics.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalised tuning knobs for live tracking. All defaults are production-sane and
 * the whole feature works out of the box; these exist so a deployment can tighten or
 * loosen validation, throttling and retention without a code change.
 */
@Component
@ConfigurationProperties(prefix = "app.tracking")
@Getter
@Setter
public class TrackingProperties {

    /** Endpoint clients connect to with a bearer token in the query string. */
    private String websocketEndpoint = "/ws";

    /** A client timestamp further from server time than this is refused as stale. */
    private int maxClockSkewMinutes = 15;

    /** A fix within this distance of the previous one is treated as a duplicate. */
    private double duplicateThresholdDegrees = 0.00005;

    /** Rolling window for the per-driver update rate limit. */
    private int rateLimitWindowMinutes = 1;

    /** Maximum fixes a driver may send per rolling window. */
    private int rateLimitMaxPerWindow = 10;

    /** Fixes older than this are pruned by the retention job. */
    private int historyRetentionDays = 30;
}