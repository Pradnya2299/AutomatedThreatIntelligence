package com.threatadvisor.api.controller;

import com.threatadvisor.api.dto.HealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
public class HealthController {

    private final String serviceName;

    public HealthController(@Value("${spring.application.name:unknown}") String serviceName) {
        this.serviceName = serviceName;
    }

    @GetMapping("/api/health")
    public HealthResponse health() {
        return new HealthResponse("UP", serviceName, Instant.now());
    }
}
