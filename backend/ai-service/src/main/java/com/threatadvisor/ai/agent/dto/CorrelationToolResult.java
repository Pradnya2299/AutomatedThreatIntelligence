package com.threatadvisor.ai.agent.dto;

import java.util.UUID;

public record CorrelationToolResult(
        String status,
        String cveId,
        UUID vulnerabilityId,
        UUID eventId,
        int findingsCreated,
        int findingsUpdated,
        int matchesEvaluated
) {
}
