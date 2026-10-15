package com.kilivana.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point for the Kilivana Backend Spring Boot application.
 * Enables classpath scanning for configuration properties so that
 * {@code @ConfigurationProperties} classes are picked up automatically.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class KilivanaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(KilivanaBackendApplication.class, args);
    }
}
