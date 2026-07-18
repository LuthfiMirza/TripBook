package com.tripbook.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Liveness endpoint. Kept trivial and dependency-free so it answers even when
 * downstream services are degraded.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final String instanceId;

    public HealthController(@Value("${INSTANCE_ID:local}") String instanceId) {
        this.instanceId = instanceId;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "instanceId", instanceId);
    }
}
