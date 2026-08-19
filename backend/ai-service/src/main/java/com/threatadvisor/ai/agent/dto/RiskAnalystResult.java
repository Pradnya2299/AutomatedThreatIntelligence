package com.threatadvisor.ai.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RiskAnalystResult(
        UUID primaryFindingId,
        UUID primaryRiskAssessmentId,
        BigDecimal riskScore,
        String riskLevel,
        List<RiskFactor> factors,
        String explanation,
        List<FindingRiskScore> findingScores
) {
}
