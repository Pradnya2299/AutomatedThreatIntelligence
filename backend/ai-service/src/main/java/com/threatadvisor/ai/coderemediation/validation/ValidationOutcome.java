package com.threatadvisor.ai.coderemediation.validation;

public record ValidationOutcome(
        String command,
        Integer exitCode,
        long durationMs,
        String stdoutSummary,
        String stderrSummary,
        String status
) {
}
