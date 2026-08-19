package com.threatadvisor.ai.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RemediationAgentResult(
        UUID findingId,
        UUID riskAssessmentId,
        UUID remediationPlanId,
        String generationStatus,
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
