package com.threatadvisor.api.dto.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RemediationListItem(
        UUID id,
        String cveId,
        String hostname,
        BigDecimal riskScore,
        String riskLevel,
        String priority,
        String status,
        Instant createdAt
) {
}
