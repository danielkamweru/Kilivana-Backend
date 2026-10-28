package com.kilivana.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Configures cross-origin requests. The allowed origins are driven by the
 * {@code cors.allowed-origins} property so deployments can lock the API down
 * to their own frontend. Local development defaults are declared in
 * application.properties; public tunnel and deployment origins must be added
 * explicitly through CORS_ALLOWED_ORIGINS.
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins}")
    private String corsAllowedOrigins;

    @Bean
    /** Registers the CORS policy for every path so preflight and actual
     *  cross-origin requests are accepted from the configured origins. */
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> allowedOrigins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        config.setAllowedOriginPatterns(allowedOrigins);

        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // ngrok-skip-browser-warning lets tunneled browsers skip ngrok's interstitial.
        config.setAllowedHeaders(Arrays.asList("*", "ngrok-skip-browser-warning"));
        // Browsers reject credentialed requests when the allowed origin is a literal "*",
        // which is why the wildcard case above uses patterns instead.
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of("Retry-After"));
        // Cache the preflight response so repeat cross-origin calls skip the OPTIONS round trip.
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
