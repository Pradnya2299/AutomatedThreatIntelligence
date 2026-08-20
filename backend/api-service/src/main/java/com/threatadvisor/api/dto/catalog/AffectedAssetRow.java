package com.threatadvisor.api.dto.catalog;

import java.math.BigDecimal;
import java.util.UUID;

public record AffectedAssetRow(
        UUID assetId,
        UUID findingId,
        String hostname,
        String environment,
        String installedVersion,
        String criticality,
        boolean internetExposure,
        BigDecimal riskScore,
        String riskLevel
) {
}
