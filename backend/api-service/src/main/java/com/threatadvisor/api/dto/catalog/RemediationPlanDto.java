package com.threatadvisor.api.dto.catalog;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RemediationPlanDto(
        UUID id,
        UUID findingId,
        UUID riskAssessmentId,
        String cveId,
        String hostname,
        String status,
        String priority,
        String summary,
        String recommendedAction,
        String targetVersion,
        List<String> affectedComponents,
        List<String> prerequisites,
        List<String> implementationSteps,
        List<String> validationSteps,
        String rollbackPlan,
        Boolean downtimeExpected,
        String reasoning,
        List<String> references,
        String modelName,
        boolean demoAi,
        Instant createdAt
) {
}
