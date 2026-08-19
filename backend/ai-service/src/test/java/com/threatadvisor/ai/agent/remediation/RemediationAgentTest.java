package com.threatadvisor.ai.agent.remediation;

import com.threatadvisor.ai.agent.common.AgentToolException;
import com.threatadvisor.ai.agent.common.Confidence;
import com.threatadvisor.ai.agent.common.InvestigationStatus;
import com.threatadvisor.ai.agent.common.SecurityInvestigationContext;
import com.threatadvisor.ai.agent.dto.AssetInvestigationResult;
import com.threatadvisor.ai.agent.dto.KnowledgeSearchResult;
import com.threatadvisor.ai.agent.dto.RemediationToolResult;
import com.threatadvisor.ai.agent.dto.RiskAnalystResult;
import com.threatadvisor.ai.agent.dto.RiskFactor;
import com.threatadvisor.ai.agent.dto.ThreatIntelligenceResult;
import com.threatadvisor.ai.agent.dto.AffectedAssetMatch;
import com.threatadvisor.ai.agent.tool.KnowledgeSearchTool;
import com.threatadvisor.ai.agent.tool.PolicyRetrievalTool;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemediationAgentTest {

    @Mock
    private RemediationGenerationTool generationTool;
    @Mock
    private KnowledgeSearchTool knowledgeSearchTool;
    @Mock
    private PolicyRetrievalTool policyRetrievalTool;
    @InjectMocks
    private RemediationAgent agent;

    @Test
    void reusesRagRemediationPipelineForPrimaryFinding() {
        UUID findingId = UUID.randomUUID();
        UUID riskId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        when(knowledgeSearchTool.search(any())).thenReturn(new KnowledgeSearchResult(List.of()));
        when(policyRetrievalTool.retrievePolicies(any())).thenReturn(new KnowledgeSearchResult(List.of()));
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
        assertEquals("REMEDIATION_GATE", ex.getCode());
    }

    private static SecurityInvestigationContext withRisk(UUID findingId, UUID riskId, UUID correlationId) {
        AffectedAssetMatch match = new AffectedAssetMatch(
                findingId, UUID.randomUUID(), "web-prod-01", "PROD", "CRITICAL", true,
                "EXACT_VERSION_MATCH", "HIGH", "2.14.1");
        return SecurityInvestigationContext.builder()
                .investigationId(UUID.randomUUID())
                .cveId("CVE-2021-44228")
                .correlationId(correlationId)
                .status(InvestigationStatus.RUNNING)
                .threat(new ThreatIntelligenceResult(
                        "CVE-2021-44228", UUID.randomUUID(), "CRITICAL", new BigDecimal("10.0"),
                        "NO_EXPLOIT_FLAG_IN_SOURCE", false, false, List.of(), "summary", List.of(), Confidence.HIGH))
                .assets(new AssetInvestigationResult(true, 1, 1, 0, 4, List.of(match), List.of("2.14.1"), Confidence.HIGH, false))
                .risk(new RiskAnalystResult(
                        findingId,
                        riskId,
                        new BigDecimal("91.50"),
                        "CRITICAL",
                        List.of(new RiskFactor("cvss", new BigDecimal("100"), "engine")),
                        "engine explanation",
                        List.of(),
                        Confidence.HIGH))
                .build();
    }
}
