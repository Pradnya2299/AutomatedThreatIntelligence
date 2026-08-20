package com.threatadvisor.ai.agent.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentDecision(
        AgentAction nextAction,
        String reason,
        Confidence confidence,
        List<String> requiredEvidence,
        InvestigationStatus status,
        Instant decidedAt
) {
    public AgentDecision(AgentAction nextAction, String reason, Confidence confidence, List<String> requiredEvidence, InvestigationStatus status) {
        this(nextAction, reason, confidence, requiredEvidence, status, Instant.now());
    }
}
