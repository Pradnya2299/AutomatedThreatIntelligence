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
                "phase", "2A",
                "message", "Infrastructure API only. Dashboard resource APIs are not implemented yet.",
                "public", List.of("GET /api/health", "GET /actuator/health"),
                "authenticated", List.of("GET /api/me")
        );
    }
}
