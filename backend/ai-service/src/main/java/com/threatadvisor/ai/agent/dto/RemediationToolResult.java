package com.threatadvisor.ai.agent.dto;

import java.util.List;
import java.util.UUID;

public record RemediationToolResult(
        String status,
        UUID findingId,
        UUID riskAssessmentId,
        UUID remediationPlanId,
        String priority,
        String targetVersion,
        List<String> prerequisites,
        List<String> implementationSteps,
        List<String> validationSteps,
        String rollback,
        List<String> references,
        List<String> ragSources,
        boolean ragContextUsed,
        String summary
) {
}
