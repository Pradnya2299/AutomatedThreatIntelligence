package com.threatadvisor.correlation.dto;

import java.util.UUID;

public record CorrelationRunResponse(
        String status,
        String cveId,
        UUID vulnerabilityId,
        UUID eventId,
        int findingsCreated,
        int findingsUpdated,
        int matchesEvaluated
) {
}
