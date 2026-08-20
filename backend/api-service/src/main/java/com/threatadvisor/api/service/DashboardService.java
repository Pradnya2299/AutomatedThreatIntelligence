package com.threatadvisor.api.service;

import com.threatadvisor.api.dto.dashboard.DashboardSummaryResponse;
import com.threatadvisor.api.repository.CatalogQueryRepository;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final CatalogQueryRepository queries;

    public DashboardService(CatalogQueryRepository queries) {
        this.queries = queries;
    }

    public DashboardSummaryResponse summary() {
        DashboardSummaryResponse counts = queries.counts();
        return new DashboardSummaryResponse(
                counts.criticalVulnerabilities(),
                counts.highVulnerabilities(),
                counts.mediumVulnerabilities(),
                counts.lowVulnerabilities(),
                counts.affectedAssets(),
                counts.criticalFindings(),
                counts.pendingRemediations(),
                counts.aiRemediationPlans(),
                counts.riskDistribution(),
                queries.topVulnerabilities(8),
                queries.recentActivity(12));
    }
}
