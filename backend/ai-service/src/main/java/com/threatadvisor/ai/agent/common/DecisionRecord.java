package com.threatadvisor.ai.agent.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DecisionRecord(
        UUID id,
        AgentAction action,
        String reason,
        List<String> evidence,
        String result,
        Confidence confidence,
        Instant decidedAt
) {
}
