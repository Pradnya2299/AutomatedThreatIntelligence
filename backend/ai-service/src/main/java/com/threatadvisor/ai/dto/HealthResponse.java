package com.threatadvisor.ai.dto;

import java.time.Instant;

public record HealthResponse(String status, String service, Instant timestamp) {
}
