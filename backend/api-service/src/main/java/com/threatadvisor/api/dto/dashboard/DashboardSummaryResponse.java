package com.threatadvisor.api.dto.dashboard;

import java.util.List;

public record DashboardSummaryResponse(
        long criticalVulnerabilities,
        long highVulnerabilities,
        long mediumVulnerabilities,
        long lowVulnerabilities,
        long affectedAssets,
        long criticalFindings,
        long pendingRemediations,
        long aiRemediationPlans,
        RiskDistribution riskDistribution,
        List<TopVulnerabilityRow> topVulnerabilities,
        List<ActivityItem> recentActivity
) {
    public record RiskDistribution(long critical, long high, long medium, long low) {
    }

    public record TopVulnerabilityRow(
            String cveId,
            String severity,
            java.math.BigDecimal cvss,
            long affectedAssets,
            java.math.BigDecimal riskScore,
            String riskLevel,
            String status,
            String aiRecommendation
    ) {
    }

    public record ActivityItem(
            String type,
            String message,
            String cveId,
            String findingId,
            java.time.Instant occurredAt
    ) {
    }
}
