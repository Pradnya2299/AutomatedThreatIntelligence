package com.threatadvisor.ai.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AssetInvestigationResult(
        boolean affected,
        int affectedAssetCount,
        int findingsCreated,
        int findingsUpdated,
        int matchesEvaluated,
        List<AffectedAssetMatch> assets,
        List<String> matchReasons
) {
}
