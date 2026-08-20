package com.threatadvisor.ai.agent.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RiskEngineSnapshot(
        UUID findingId,
        UUID riskAssessmentId,
        BigDecimal riskScore,
        String riskLevel,
        String formulaVersion,
        List<RiskFactor> factors,
        String explanation
) {
}
