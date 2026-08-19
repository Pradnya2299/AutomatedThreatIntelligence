package com.threatadvisor.ai.agent.remediation;

import com.threatadvisor.ai.agent.common.AgentToolException;
import com.threatadvisor.ai.agent.common.InvestigationStatus;
import com.threatadvisor.ai.agent.common.SecurityInvestigationContext;
import com.threatadvisor.ai.agent.dto.RemediationToolResult;
import com.threatadvisor.ai.agent.dto.RiskAnalystResult;
import com.threatadvisor.ai.agent.dto.RiskFactor;
import com.threatadvisor.ai.agent.tool.RemediationGenerationTool;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemediationAgentTest {

    @Mock
    private RemediationGenerationTool generationTool;
    @InjectMocks
    private RemediationAgent agent;

    @Test
    void reusesRagRemediationPipelineForPrimaryFinding() {
        UUID findingId = UUID.randomUUID();
        UUID riskId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        when(generationTool.generate(findingId, correlationId)).thenReturn(new RemediationToolResult(
                "COMPLETED",
                findingId,
                riskId,
                planId,
                "IMMEDIATE",
                "2.17.1",
                List.of("backup"),
                List.of("patch"),
                List.of("verify"),
                "rollback",
                List.of("https://nvd.nist.gov"),
                List.of("classpath:knowledge/patch-policy.md"),
                true,
                "Patch log4j"));

        SecurityInvestigationContext result = agent.execute(withRisk(findingId, riskId, correlationId));

        assertEquals(planId, result.remediation().remediationPlanId());
        assertTrue(result.remediation().ragContextUsed());
        assertEquals(List.of("classpath:knowledge/patch-policy.md"), result.remediation().ragSources());
        verify(generationTool).generate(findingId, correlationId);
    }

    @Test
    void refusesToRunWithoutRisk() {
        SecurityInvestigationContext context = SecurityInvestigationContext.builder()
                .investigationId(UUID.randomUUID())
                .cveId("CVE-2021-44228")
                .status(InvestigationStatus.RUNNING)
                .build();
        AgentToolException ex = assertThrows(AgentToolException.class, () -> agent.execute(context));
        assertEquals("RISK_REQUIRED", ex.getCode());
    }

    private static SecurityInvestigationContext withRisk(UUID findingId, UUID riskId, UUID correlationId) {
        return SecurityInvestigationContext.builder()
                .investigationId(UUID.randomUUID())
                .cveId("CVE-2021-44228")
                .correlationId(correlationId)
                .status(InvestigationStatus.RUNNING)
                .risk(new RiskAnalystResult(
                        findingId,
                        riskId,
                        new BigDecimal("91.50"),
                        "CRITICAL",
                        List.of(new RiskFactor("cvss", new BigDecimal("100"), "engine")),
                        "engine explanation",
                        List.of()))
                .build();
    }
}
