package com.threatadvisor.ai.domain;

import java.util.List;

public record RemediationContext(
        String cveId,
        String vulnerabilityDescription,
        String severity,
        String cvss,
        String product,
        String installedVersion,
        String versionRange,
        String hostname,
        String operatingSystem,
        String environment,
        String businessCriticality,
        boolean internetExposed,
        String installedSoftware,
        String riskScore,
        String riskLevel,
        String riskFactors,
        String riskExplanation,
        List<KnowledgeChunkHit> knowledge
) {
}
