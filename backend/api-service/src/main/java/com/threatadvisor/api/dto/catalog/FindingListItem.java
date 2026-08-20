package com.threatadvisor.api.dto.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FindingListItem(
        UUID id,
        UUID assetId,
        String hostname,
        UUID vulnerabilityId,
        String cveId,
        String matchType,
        String matchConfidence,
        BigDecimal riskScore,
        String riskLevel,
        String status,
        Instant detectedAt
) {
}
