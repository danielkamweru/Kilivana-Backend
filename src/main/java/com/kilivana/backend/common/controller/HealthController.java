package com.kilivana.backend.common.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.Map;

@Tag(name = "Health & System", description = "Service liveness and readiness information")
@RestController
public class HealthController {

    @GetMapping("/")
    @Operation(summary = "Service health", description = "Liveness probe. Public, requires no authentication.")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Kilivana Backend is running");
    }

    @GetMapping("/api/v1/health")
    @Operation(summary = "API health",
            description = "Liveness probe for the deployment's health check. Public, requires no authentication. "
                    + "Returns the service state so a monitoring system can verify the backend is up.")
    public ResponseEntity<Map<String, Object>> apiHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "application", "kilivana-backend",
                "timestamp", LocalDateTime.now().toString()));
    }

    @GetMapping("/favicon.ico")
    @Operation(summary = "Favicon", description = "Returns no content so browsers stop requesting a missing asset.")
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.noContent().build();
    }
}
