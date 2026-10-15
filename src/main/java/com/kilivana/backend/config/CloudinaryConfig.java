package com.kilivana.backend.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Wires the Cloudinary client from environment-supplied credentials. When any
 * of the three values is absent the bean is still created, but the
 * {@link com.kilivana.backend.common.service.ImageStorage} implementation
 * reports itself as unconfigured and uploads fail fast with 503 — see
 * {@link IntegrationReadinessLogger}.
 */
@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    /**
     * Builds the Cloudinary client from environment properties. Credentials
     * come from the platform, so they are never committed; a missing cloud
     * name leaves the client unconfigured and uploads fail with 503.
     */
    @Bean
    public Cloudinary cloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", cloudName);
        config.put("api_key", apiKey);
        config.put("api_secret", apiSecret);
        config.put("secure", "true");
        return new Cloudinary(config);
    }
}