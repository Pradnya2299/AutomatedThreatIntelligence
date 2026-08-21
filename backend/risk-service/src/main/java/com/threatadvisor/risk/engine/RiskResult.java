package com.threatadvisor.risk.engine;

import java.math.BigDecimal;

public record RiskResult(
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
