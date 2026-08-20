package com.threatadvisor.api.dto.catalog;

import java.math.BigDecimal;
import java.util.UUID;

public record RiskBreakdownDto(
        UUID id,
        BigDecimal score,
        String level,
        BigDecimal cvss,
        BigDecimal assetCriticality,
        BigDecimal internetExposure,
        BigDecimal exploitability,
        BigDecimal activeExploitation,
        String explanation
) {
}
