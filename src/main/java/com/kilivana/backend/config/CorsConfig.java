package com.kilivana.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Configures cross-origin requests. The allowed origins are driven by the
 * {@code cors.allowed-origins} property so deployments can lock the API down
 * to their own frontend, while local development always keeps the Angular dev
 * server and ngrok preview tunnels accessible.
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:*}")
    private String corsAllowedOrigins;

    @Bean
    /** Registers the CORS policy for every path so preflight and actual
     *  cross-origin requests are accepted from the configured origins. */
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> allowedOrigins = new ArrayList<>(Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());

        if (allowedOrigins.contains("*")) {
            config.setAllowedOriginPatterns(List.of("*", "http://localhost:4200",
                    "https://*.ngrok-free.dev", "https://*.ngrok.io"));
        } else {
            if (!allowedOrigins.contains("http://localhost:4200")) {
                allowedOrigins.add("http://localhost:4200");
            }
            for (String origin : List.of("https://*.ngrok-free.dev", "https://*.ngrok.io")) {
                if (!allowedOrigins.contains(origin)) {
                    allowedOrigins.add(origin);
                }
            }
            config.setAllowedOriginPatterns(allowedOrigins);
        }

        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*", "ngrok-skip-browser-warning"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
