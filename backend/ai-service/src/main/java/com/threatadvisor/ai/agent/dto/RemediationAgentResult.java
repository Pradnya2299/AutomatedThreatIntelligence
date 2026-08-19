package com.threatadvisor.ai.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.threatadvisor.ai.agent.common.Confidence;

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
        String summary,
        Confidence confidence
) {
    public RemediationAgentResult(
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
            String summary) {
        this(findingId, riskAssessmentId, remediationPlanId, generationStatus, priority, targetVersion, prerequisites,
                implementationSteps, validationSteps, rollback, references, ragSources, ragContextUsed, summary, null);
    }
}
