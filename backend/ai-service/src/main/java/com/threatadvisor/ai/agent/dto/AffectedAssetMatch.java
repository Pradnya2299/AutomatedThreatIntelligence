package com.threatadvisor.ai.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AffectedAssetMatch(
        UUID findingId,
        UUID assetId,
        String hostname,
        String environment,
        String businessCriticality,
        boolean internetExposure,
        String matchType,
        String matchConfidence,
        String matchReason
) {
}
