package com.threatadvisor.ai.agent.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExecutionTraceEntry(
        UUID executionId,
        UUID investigationId,
        String agentName,
        AgentAction action,
        AgentStatus status,
        Instant startTime,
        Instant endTime,
        Long durationMs,
        String reason,
        Confidence confidence,
        List<String> evidenceProduced,
        String error
) {
    public static ExecutionTraceEntry of(
            UUID investigationId,
            String agentName,
            AgentAction action,
            AgentStatus status,
            Instant start,
            Instant end,
            String reason,
            Confidence confidence,
            List<String> evidenceProduced,
            String error) {
        Long duration = start == null || end == null ? null : Duration.between(start, end).toMillis();
        return new ExecutionTraceEntry(
                UUID.randomUUID(),
                investigationId,
                agentName,
                action,
                status,
                start,
                end,
                duration,
                reason,
                confidence,
                evidenceProduced == null ? List.of() : evidenceProduced,
                error);
    }
}
