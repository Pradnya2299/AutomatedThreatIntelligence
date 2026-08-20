package com.threatadvisor.ai.agent.asset;

import com.threatadvisor.ai.agent.common.AgentToolException;
import com.threatadvisor.ai.agent.common.Confidence;
import com.threatadvisor.ai.agent.common.EvidenceItem;
import com.threatadvisor.ai.agent.common.EvidenceSource;
import com.threatadvisor.ai.agent.common.InvestigationConfidence;
import com.threatadvisor.ai.agent.common.SecurityAgent;
import com.threatadvisor.ai.agent.common.SecurityInvestigationContext;
import com.threatadvisor.ai.agent.dto.AffectedAssetMatch;
import com.threatadvisor.ai.agent.dto.AssetInvestigationResult;
import com.threatadvisor.ai.agent.dto.CorrelationToolResult;
import com.threatadvisor.ai.agent.tool.AffectedAssetLookupTool;
import com.threatadvisor.ai.agent.tool.CorrelationTool;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class AssetInvestigationAgent implements SecurityAgent {

    public static final String NAME = "AssetInvestigationAgent";

    private final CorrelationTool correlationTool;
    private final AffectedAssetLookupTool assetLookupTool;

    public AssetInvestigationAgent(CorrelationTool correlationTool, AffectedAssetLookupTool assetLookupTool) {
        this.correlationTool = correlationTool;
        this.assetLookupTool = assetLookupTool;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public SecurityInvestigationContext execute(SecurityInvestigationContext context) {
        UUID vulnerabilityId = context.threat() == null ? null : context.threat().vulnerabilityId();
        if (vulnerabilityId == null) {
            throw new AgentToolException("THREAT_REQUIRED", "Asset investigation requires threat intelligence first");
        }
        CorrelationToolResult correlation = correlationTool.correlate(context.cveId(), context.correlationId());
        List<AffectedAssetMatch> assets = assetLookupTool.lookup(vulnerabilityId);
        List<String> reasons = assets.stream().map(AffectedAssetMatch::matchReason).filter(r -> r != null && !r.isBlank()).toList();
        AssetInvestigationResult result = new AssetInvestigationResult(
                !assets.isEmpty(),
                assets.size(),
                correlation.findingsCreated(),
                correlation.findingsUpdated(),
                correlation.matchesEvaluated(),
                assets,
                reasons,
                null,
                false);
        Confidence confidence = InvestigationConfidence.assets(result);
        result = new AssetInvestigationResult(
                result.affected(),
                result.affectedAssetCount(),
                result.findingsCreated(),
                result.findingsUpdated(),
                result.matchesEvaluated(),
                result.assets(),
                result.matchReasons(),
                confidence,
                false);
        List<EvidenceItem> evidence = new ArrayList<>();
        evidence.add(EvidenceItem.fact(
                EvidenceSource.CORRELATION_ENGINE,
                "correlation_run",
                "Deterministic correlation against asset_software",
                "matchesEvaluated=" + correlation.matchesEvaluated() + ", affected=" + result.affected(),
                confidence));
        for (AffectedAssetMatch match : assets) {
            evidence.add(EvidenceItem.fact(
                    EvidenceSource.CORRELATION_ENGINE,
                    match.matchType() == null ? "match" : match.matchType(),
                    match.matchReason(),
                    match.hostname(),
                    "HIGH".equalsIgnoreCase(match.matchConfidence()) ? Confidence.HIGH : Confidence.MEDIUM));
        }
        return context.withAssets(result).toBuilder().addEvidenceAll(evidence).build();
    }
}
