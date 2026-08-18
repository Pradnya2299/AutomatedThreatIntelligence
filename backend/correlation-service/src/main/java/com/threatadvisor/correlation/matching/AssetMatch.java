package com.threatadvisor.correlation.matching;

import java.util.UUID;

public record AssetMatch(
        UUID assetId,
        UUID organizationId,
        String hostname,
        String cveId,
        String vendor,
        String product,
        String installedVersion,
        CpeConstraint matchedCpe,
        MatchType matchType,
        MatchConfidence confidence,
        String explanation
) {
}
