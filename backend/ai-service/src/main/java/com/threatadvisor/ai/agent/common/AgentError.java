package com.threatadvisor.ai.agent.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentError(
        String agentName,
        String code,
        String message,
        Instant occurredAt
) {
}
