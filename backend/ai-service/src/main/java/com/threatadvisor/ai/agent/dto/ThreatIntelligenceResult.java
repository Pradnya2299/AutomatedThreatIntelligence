package com.threatadvisor.ai.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.threatadvisor.ai.agent.common.Confidence;
import com.threatadvisor.ai.agent.common.EvidenceItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ThreatIntelligenceResult(
        String cveId,
        UUID vulnerabilityId,
        String severity,
        BigDecimal cvssScore,
        String exploitability,
        Boolean exploitAvailable,
        Boolean activelyExploited,
        List<AffectedProduct> affectedProducts,
        String summary,
        List<EvidenceItem> evidence,
        Confidence confidence
) {
    public ThreatIntelligenceResult(
            String cveId,
            UUID vulnerabilityId,
            String severity,
            BigDecimal cvssScore,
            String exploitability,
            Boolean exploitAvailable,
            Boolean activelyExploited,
            List<AffectedProduct> affectedProducts,
            String summary,
            List<EvidenceItem> evidence) {
        this(cveId, vulnerabilityId, severity, cvssScore, exploitability, exploitAvailable, activelyExploited,
                affectedProducts, summary, evidence, null);
    }
}
