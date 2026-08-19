package com.threatadvisor.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Browser hits to http://localhost:8080/ used to 500 because there was no mapping.
 */
@RestController
public class RootController {

    @GetMapping({"/", "/api"})
    public Map<String, Object> root() {
        return Map.of(
                "service", "api-service",
                "phase", "5A",
                "message", "SOC dashboard BFF plus CVE investigation orchestration proxy.",
                "public", List.of("GET /api/health", "GET /actuator/health"),
                "authenticated", List.of(
                        "GET /api/me",
                        "GET /api/dashboard/summary",
                        "GET /api/vulnerabilities",
                        "GET /api/findings",
                        "GET /api/assets",
                        "GET /api/remediation",
                        "POST /api/v1/investigations",
                        "GET /api/v1/investigations/{id}")
        );
    }
}
