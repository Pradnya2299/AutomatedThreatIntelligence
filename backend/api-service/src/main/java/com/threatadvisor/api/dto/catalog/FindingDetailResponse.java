package com.threatadvisor.api.dto.catalog;

import java.util.UUID;

public record FindingDetailResponse(
        UUID id,
        String status,
        String cveId,
        UUID vulnerabilityId,
        UUID assetId,
        String hostname,
        String environment,
        String operatingSystem,
        String matchType,
        String matchConfidence,
        String matchExplanation,
        RiskBreakdownDto risk,
        RemediationPlanDto remediation
) {
}
