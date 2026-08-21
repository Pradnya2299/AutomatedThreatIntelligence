package com.threatadvisor.ai.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FindingRiskScore(
        UUID findingId,
        UUID assetId,
        UUID riskAssessmentId,
        BigDecimal riskScore,
        String riskLevel,
        List<RiskFactor> factors,
        String calculationDetails
) {
}
