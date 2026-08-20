package com.threatadvisor.ai.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.threatadvisor.ai.agent.common.Confidence;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AssetInvestigationResult(
        boolean affected,
        int affectedAssetCount,
        int findingsCreated,
        int findingsUpdated,
        int matchesEvaluated,
        List<AffectedAssetMatch> assets,
        List<String> matchReasons,
        Confidence confidence,
        boolean detailsEnriched
) {
    public AssetInvestigationResult(
            boolean affected,
            int affectedAssetCount,
            int findingsCreated,
            int findingsUpdated,
            int matchesEvaluated,
            List<AffectedAssetMatch> assets,
            List<String> matchReasons) {
        this(affected, affectedAssetCount, findingsCreated, findingsUpdated, matchesEvaluated, assets, matchReasons,
                null, false);
    }
}
