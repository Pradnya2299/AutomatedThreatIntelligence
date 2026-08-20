package com.threatadvisor.ai.agent.risk;

import com.threatadvisor.ai.agent.common.AgentToolException;
import com.threatadvisor.ai.agent.common.InvestigationStatus;
import com.threatadvisor.ai.agent.common.SecurityInvestigationContext;
import com.threatadvisor.ai.agent.dto.AffectedAssetMatch;
import com.threatadvisor.ai.agent.dto.AssetInvestigationResult;
import com.threatadvisor.ai.agent.dto.RiskEngineSnapshot;
import com.threatadvisor.ai.agent.dto.RiskFactor;
import com.threatadvisor.ai.agent.tool.RiskCalculationTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskAnalystAgentTest {

    @Mock
    private RiskCalculationTool riskCalculationTool;
    @InjectMocks
    private RiskAnalystAgent agent;

    @Test
    void scoreComesFromRiskEngineTool() {
        UUID findingId = UUID.randomUUID();
        UUID assessmentId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        when(riskCalculationTool.calculate(findingId, correlationId)).thenReturn(new RiskEngineSnapshot(
                findingId,
                assessmentId,
                new BigDecimal("91.50"),
                "CRITICAL",
                "v1",
                List.of(new RiskFactor("cvss", new BigDecimal("100.00"), "from engine")),
                "engine explanation"));

        SecurityInvestigationContext result = agent.execute(exposed(findingId, correlationId));

        assertEquals(new BigDecimal("91.50"), result.risk().riskScore());
        assertEquals("CRITICAL", result.risk().riskLevel());
        assertEquals(assessmentId, result.risk().primaryRiskAssessmentId());
        assertEquals("cvss", result.risk().factors().getFirst().name());
        verify(riskCalculationTool).calculate(findingId, correlationId);
    }

    @Test
    void doesNotInventScoreWhenEngineFails() {
        UUID findingId = UUID.randomUUID();
        when(riskCalculationTool.calculate(any(), any()))
                .thenThrow(new AgentToolException("RISK_ENGINE_UNAVAILABLE", "down"));
        assertThrows(AgentToolException.class, () -> agent.execute(exposed(findingId, UUID.randomUUID())));
    }

    @Test
    void refusesToRunWithoutAffectedAssets() {
        SecurityInvestigationContext context = SecurityInvestigationContext.builder()
                .investigationId(UUID.randomUUID())
                .cveId("CVE-2021-44228")
                .status(InvestigationStatus.RUNNING)
                .assets(new AssetInvestigationResult(false, 0, 0, 0, 3, List.of(), List.of()))
                .build();
        AgentToolException ex = assertThrows(AgentToolException.class, () -> agent.execute(context));
        assertEquals("NOT_EXPOSED", ex.getCode());
    }

    private static SecurityInvestigationContext exposed(UUID findingId, UUID correlationId) {
        AffectedAssetMatch match = new AffectedAssetMatch(
                findingId, UUID.randomUUID(), "web-prod-01", "PROD", "CRITICAL", true,
                "EXACT_VERSION_MATCH", "HIGH", "reason");
        return SecurityInvestigationContext.builder()
                .investigationId(UUID.randomUUID())
                .cveId("CVE-2021-44228")
                .correlationId(correlationId)
                .status(InvestigationStatus.RUNNING)
                .assets(new AssetInvestigationResult(true, 1, 1, 0, 4, List.of(match), List.of("reason")))
                .build();
    }
}
