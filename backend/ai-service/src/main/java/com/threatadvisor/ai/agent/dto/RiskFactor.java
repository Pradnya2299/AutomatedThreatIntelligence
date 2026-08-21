package com.threatadvisor.ai.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RiskFactor(
        String name,
        BigDecimal score,
        String detail
) {
}
