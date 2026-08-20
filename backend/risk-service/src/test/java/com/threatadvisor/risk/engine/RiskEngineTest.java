package com.threatadvisor.risk.engine;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskEngineTest {

    @Test
    void mapsCvssLinearly() {
        assertEquals(new BigDecimal("0.00"), RiskEngine.cvss(new BigDecimal("0.0")));
        assertEquals(new BigDecimal("39.00"), RiskEngine.cvss(new BigDecimal("3.9")));
        assertEquals(new BigDecimal("40.00"), RiskEngine.cvss(new BigDecimal("4.0")));
        assertEquals(new BigDecimal("69.00"), RiskEngine.cvss(new BigDecimal("6.9")));
        assertEquals(new BigDecimal("70.00"), RiskEngine.cvss(new BigDecimal("7.0")));
        assertEquals(new BigDecimal("89.00"), RiskEngine.cvss(new BigDecimal("8.9")));
        assertEquals(new BigDecimal("90.00"), RiskEngine.cvss(new BigDecimal("9.0")));
        assertEquals(new BigDecimal("98.00"), RiskEngine.cvss(new BigDecimal("9.8")));
        assertEquals(new BigDecimal("100.00"), RiskEngine.cvss(new BigDecimal("10.0")));
        assertEquals(new BigDecimal("0.00"), RiskEngine.cvss(null));
    }

    @Test
    void mapsAssetCriticality() {
        assertEquals(new BigDecimal("25.00"), RiskEngine.assetCriticality("LOW"));
        assertEquals(new BigDecimal("50.00"), RiskEngine.assetCriticality("MEDIUM"));
        assertEquals(new BigDecimal("75.00"), RiskEngine.assetCriticality("HIGH"));
        assertEquals(new BigDecimal("100.00"), RiskEngine.assetCriticality("CRITICAL"));
        assertEquals(new BigDecimal("0.00"), RiskEngine.assetCriticality(null));
    }

    @Test
    void mapsInternetExposureBoolean() {
        assertEquals(new BigDecimal("0.00"), RiskEngine.internetExposure(false));
        assertEquals(new BigDecimal("100.00"), RiskEngine.internetExposure(true));
    }

    @Test
    void mapsExploitabilityBoolean() {
        assertEquals(new BigDecimal("0.00"), RiskEngine.exploitability(false));
        assertEquals(new BigDecimal("0.00"), RiskEngine.exploitability(null));
        assertEquals(new BigDecimal("75.00"), RiskEngine.exploitability(true));
    }

    @Test
    void mapsActiveExploitation() {
        assertEquals(new BigDecimal("0.00"), RiskEngine.activeExploitation(false));
        assertEquals(new BigDecimal("0.00"), RiskEngine.activeExploitation(null));
        assertEquals(new BigDecimal("100.00"), RiskEngine.activeExploitation(true));
    }

    @Test
    void mapsSeverityBoundaries() {
        assertEquals("LOW", RiskEngine.riskLevel(new BigDecimal("0.00")));
        assertEquals("LOW", RiskEngine.riskLevel(new BigDecimal("24.99")));
        assertEquals("MEDIUM", RiskEngine.riskLevel(new BigDecimal("25.00")));
        assertEquals("MEDIUM", RiskEngine.riskLevel(new BigDecimal("49.99")));
        assertEquals("HIGH", RiskEngine.riskLevel(new BigDecimal("50.00")));
        assertEquals("HIGH", RiskEngine.riskLevel(new BigDecimal("74.99")));
        assertEquals("CRITICAL", RiskEngine.riskLevel(new BigDecimal("75.00")));
        assertEquals("CRITICAL", RiskEngine.riskLevel(new BigDecimal("100.00")));
    }

    @Test
    void criticalInternetFacingActiveExploitIsCritical() {
        RiskResult result = RiskEngine.calculate(new RiskInput(
                new BigDecimal("9.8"), "CRITICAL", true, true, true));
        // 98*0.4 + 100*0.25 + 100*0.15 + 75*0.1 + 100*0.1 = 96.70
        assertEquals(new BigDecimal("96.70"), result.score());
        assertEquals("CRITICAL", result.level());
        assertEquals(new BigDecimal("98.00"), result.cvss());
        assertEquals(new BigDecimal("100.00"), result.assetCriticality());
        assertEquals(new BigDecimal("100.00"), result.internetExposure());
        assertEquals(new BigDecimal("75.00"), result.exploitability());
        assertEquals(new BigDecimal("100.00"), result.activeExploitation());
        assertTrue(result.explanation().contains("CRITICAL (96.70)"));
        assertTrue(result.explanation().contains("CVSS score of 9.8"));
        assertTrue(result.explanation().contains("internet-facing"));
        assertTrue(result.explanation().contains("active exploitation is known"));
    }

    @Test
    void mediumInternalAssetIsLower() {
        RiskResult result = RiskEngine.calculate(new RiskInput(
                new BigDecimal("5.4"), "MEDIUM", false, false, false));
        // 54*0.4 + 50*0.25 + 0 + 0 + 0 = 34.10
        assertEquals(new BigDecimal("34.10"), result.score());
        assertEquals("MEDIUM", result.level());
        assertTrue(result.explanation().contains("not internet-facing"));
        assertTrue(result.explanation().contains("MEDIUM (34.10)"));
    }
}
