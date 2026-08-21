package com.threatadvisor.risk.engine;

import java.math.BigDecimal;

public record RiskInput(
        BigDecimal cvssBase,
        String businessCriticality,
        boolean internetExposed,
        Boolean exploitAvailable,
        Boolean activelyExploited
) {
}
