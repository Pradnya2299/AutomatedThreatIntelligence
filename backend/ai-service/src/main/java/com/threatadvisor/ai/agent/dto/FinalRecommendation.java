package com.threatadvisor.ai.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FinalRecommendation(
        boolean exposed,
        BigDecimal organizationalRiskScore,
        String riskLevel,
        String priority,
        String summary
) {
}
