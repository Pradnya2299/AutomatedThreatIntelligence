package com.threatadvisor.ai.dto;

import java.util.UUID;

public record RemediationRunResponse(
        String status,
        UUID findingId,
        UUID remediationPlanId,
        UUID riskAssessmentId
) {
}
