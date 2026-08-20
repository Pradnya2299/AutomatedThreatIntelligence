package com.threatadvisor.ai.agent.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentExecution(
        String agentName,
        AgentStatus status,
        Instant startedAt,
        Instant completedAt,
        String failureReason
) {
    public static AgentExecution running(String agentName, Instant startedAt) {
        return new AgentExecution(agentName, AgentStatus.RUNNING, startedAt, null, null);
    }

    public AgentExecution completed(Instant completedAt) {
        return new AgentExecution(agentName, AgentStatus.COMPLETED, startedAt, completedAt, null);
    }

    public AgentExecution failed(Instant completedAt, String reason) {
        return new AgentExecution(agentName, AgentStatus.FAILED, startedAt, completedAt, reason);
    }

    public AgentExecution skipped(Instant completedAt, String reason) {
        return new AgentExecution(agentName, AgentStatus.SKIPPED, startedAt, completedAt, reason);
    }
}
