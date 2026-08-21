package com.threatadvisor.risk.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RiskRunResponse(
        String status,
        UUID findingId,
        UUID riskAssessmentId,
        UUID eventId,
        BigDecimal riskScore,
        String riskLevel
) {
}
