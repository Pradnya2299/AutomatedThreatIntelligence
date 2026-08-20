package com.threatadvisor.risk.dto;

import java.time.Instant;

public record HealthResponse(String status, String service, Instant timestamp) {
}
