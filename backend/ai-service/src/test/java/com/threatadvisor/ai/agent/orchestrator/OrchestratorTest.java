package com.threatadvisor.ai.agent.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threatadvisor.ai.agent.asset.AssetInvestigationAgent;
import com.threatadvisor.ai.agent.common.AgentStatus;
import com.threatadvisor.ai.agent.common.AgentToolException;
import com.threatadvisor.ai.agent.common.InvestigationStatus;
import com.threatadvisor.ai.agent.dto.AffectedAssetMatch;
import com.threatadvisor.ai.agent.dto.CorrelationToolResult;
import com.threatadvisor.ai.agent.dto.RemediationToolResult;
import com.threatadvisor.ai.agent.dto.RiskEngineSnapshot;
import com.threatadvisor.ai.agent.dto.RiskFactor;
import com.threatadvisor.ai.agent.remediation.RemediationAgent;
import com.threatadvisor.ai.agent.risk.RiskAnalystAgent;
import com.threatadvisor.ai.agent.threat.ThreatIntelligenceAgent;
import com.threatadvisor.ai.agent.tool.AffectedAssetLookupTool;
import com.threatadvisor.ai.agent.tool.CorrelationTool;
import com.threatadvisor.ai.agent.tool.CveLookupTool;
import com.threatadvisor.ai.agent.tool.RemediationGenerationTool;
import com.threatadvisor.ai.agent.tool.RiskCalculationTool;
import com.threatadvisor.ai.agent.tool.VulnerabilityContextTool;
import com.threatadvisor.ai.domain.Vulnerability;
import com.threatadvisor.ai.kafka.RemediationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrchestratorTest {

    @Mock
    private CveLookupTool cveLookupTool;
    @Mock
    private VulnerabilityContextTool contextTool;
    @Mock
    private CorrelationTool correlationTool;
    @Mock
    private AffectedAssetLookupTool assetLookupTool;
    @Mock
    private RiskCalculationTool riskCalculationTool;
    @Mock
    private RemediationGenerationTool remediationGenerationTool;
    @Mock
    private InvestigationStore store;
    @Mock
    private RemediationEventPublisher publisher;

    private SecurityOrchestrator orchestrator;
    private Vulnerability vulnerability;
    private UUID findingId;
    private UUID assetId;

    @BeforeEach
    void setUp() {
        when(store.persist(any())).thenAnswer(invocation -> invocation.getArgument(0));
        vulnerability = new Vulnerability();
        vulnerability.setId(UUID.randomUUID());
        vulnerability.setCveId("CVE-2021-44228");
        vulnerability.setSeverity("CRITICAL");
        vulnerability.setCvssScore(new BigDecimal("10.0"));
        vulnerability.setDescription("RCE");
        findingId = UUID.randomUUID();
        assetId = UUID.randomUUID();
        orchestrator = new SecurityOrchestrator(
                new ThreatIntelligenceAgent(cveLookupTool, contextTool),
                new AssetInvestigationAgent(correlationTool, assetLookupTool),
                new RiskAnalystAgent(riskCalculationTool),
                new RemediationAgent(remediationGenerationTool),
                store,
                publisher,
                new ObjectMapper());
    }

    @Test
    void runsAgentsInOrderAndPassesTypedContext() {
        stubHappyPath();

        var result = orchestrator.startInvestigation("cve-2021-44228", UUID.randomUUID());

        InOrder order = inOrder(cveLookupTool, correlationTool, assetLookupTool, riskCalculationTool, remediationGenerationTool);
        order.verify(cveLookupTool).findByCveId("CVE-2021-44228");
        order.verify(correlationTool).correlate(any(), any());
        order.verify(assetLookupTool).lookup(vulnerability.getId());
        order.verify(riskCalculationTool).calculate(findingId, result.correlationId());
        order.verify(remediationGenerationTool).generate(findingId, result.correlationId());

        assertEquals(InvestigationStatus.COMPLETED, result.status());
        assertEquals(new BigDecimal("91.50"), result.risk().riskScore());
        assertEquals("web-prod-01", result.assets().assets().getFirst().hostname());
        assertTrue(result.remediation().ragContextUsed());
        assertEquals("IMMEDIATE", result.recommendation().priority());
        verify(contextTool).summarize(vulnerability);
        verify(publisher).publish(any(), any(), any());
    }

    @Test
    void threatFailureStopsWorkflow() {
        when(cveLookupTool.findByCveId(any())).thenReturn(Optional.empty());

        var result = orchestrator.startInvestigation("CVE-1999-0001", null);

        assertEquals(InvestigationStatus.REVIEW_REQUIRED, result.status());
        verify(correlationTool, never()).correlate(any(), any());
        verify(riskCalculationTool, never()).calculate(any(), any());
        verify(remediationGenerationTool, never()).generate(any(), any());
        assertEquals(AgentStatus.FAILED, result.executionOf(ThreatIntelligenceAgent.NAME).orElseThrow().status());
    }

    @Test
    void assetFailureDoesNotProduceNotAffected() {
        when(cveLookupTool.findByCveId(any())).thenReturn(Optional.of(vulnerability));
        when(cveLookupTool.findCpes(any())).thenReturn(List.of());
        when(contextTool.summarize(any())).thenReturn("summary");
        when(correlationTool.correlate(any(), any()))
                .thenThrow(new AgentToolException("CORRELATION_UNAVAILABLE", "down"));

        var result = orchestrator.startInvestigation("CVE-2021-44228", UUID.randomUUID());

        assertEquals(InvestigationStatus.FAILED, result.status());
        assertNull(result.assets());
        verify(riskCalculationTool, never()).calculate(any(), any());
        verify(remediationGenerationTool, never()).generate(any(), any());
    }

    @Test
    void riskFailureSkipsRemediationAndKeepsAssetResult() {
        when(cveLookupTool.findByCveId(any())).thenReturn(Optional.of(vulnerability));
        when(cveLookupTool.findCpes(any())).thenReturn(List.of());
        when(contextTool.summarize(any())).thenReturn("summary");
        stubCorrelationAndAssets();
        when(riskCalculationTool.calculate(any(), any()))
                .thenThrow(new AgentToolException("RISK_ENGINE_UNAVAILABLE", "down"));

        var result = orchestrator.startInvestigation("CVE-2021-44228", UUID.randomUUID());

        assertEquals(InvestigationStatus.FAILED, result.status());
        assertTrue(result.assets().affected());
        assertEquals(AgentStatus.SKIPPED, result.executionOf(RemediationAgent.NAME).orElseThrow().status());
        verify(remediationGenerationTool, never()).generate(any(), any());
    }

    @Test
    void remediationFailurePreservesThreatAssetAndRisk() {
        stubHappyPath();
        when(remediationGenerationTool.generate(any(), any()))
                .thenThrow(new AgentToolException("REMEDIATION_FAILED", "model down"));

        var result = orchestrator.startInvestigation("CVE-2021-44228", UUID.randomUUID());

        assertEquals(InvestigationStatus.FAILED, result.status());
        assertNotNull(result.threat());
        assertTrue(result.assets().affected());
        assertEquals(new BigDecimal("91.50"), result.risk().riskScore());
        assertEquals(AgentStatus.FAILED, result.executionOf(RemediationAgent.NAME).orElseThrow().status());
    }

    @Test
    void notAffectedSkipsRiskAndRemediationWithoutLlm() {
        when(cveLookupTool.findByCveId(any())).thenReturn(Optional.of(vulnerability));
        when(cveLookupTool.findCpes(any())).thenReturn(List.of());
        when(contextTool.summarize(any())).thenReturn("summary");
        when(correlationTool.correlate(any(), any())).thenReturn(new CorrelationToolResult(
                "COMPLETED", "CVE-2021-44228", vulnerability.getId(), UUID.randomUUID(), 0, 0, 9));
        when(assetLookupTool.lookup(vulnerability.getId())).thenReturn(List.of());

        var result = orchestrator.startInvestigation("CVE-2021-44228", UUID.randomUUID());

        assertEquals(InvestigationStatus.COMPLETED, result.status());
        assertFalse(result.assets().affected());
        assertEquals(AgentStatus.SKIPPED, result.executionOf(RiskAnalystAgent.NAME).orElseThrow().status());
        assertEquals(AgentStatus.SKIPPED, result.executionOf(RemediationAgent.NAME).orElseThrow().status());
        verify(riskCalculationTool, never()).calculate(any(), any());
        verify(remediationGenerationTool, never()).generate(any(), any());
    }

    private void stubHappyPath() {
        when(cveLookupTool.findByCveId(any())).thenReturn(Optional.of(vulnerability));
        when(cveLookupTool.findCpes(any())).thenReturn(List.of());
        when(contextTool.summarize(any())).thenReturn("summary");
        stubCorrelationAndAssets();
        when(riskCalculationTool.calculate(any(), any())).thenReturn(new RiskEngineSnapshot(
                findingId,
                UUID.randomUUID(),
                new BigDecimal("91.50"),
                "CRITICAL",
                "v1",
                List.of(new RiskFactor("cvss", new BigDecimal("100"), "engine")),
                "engine explanation"));
        when(remediationGenerationTool.generate(any(), any())).thenReturn(new RemediationToolResult(
                "COMPLETED",
                findingId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "IMMEDIATE",
                "2.17.1",
                List.of(),
                List.of("patch"),
                List.of("verify"),
                "rollback",
                List.of(),
                List.of("policy.md"),
                true,
                "Patch now"));
    }

    private void stubCorrelationAndAssets() {
        when(correlationTool.correlate(any(), any())).thenReturn(new CorrelationToolResult(
                "COMPLETED", "CVE-2021-44228", vulnerability.getId(), UUID.randomUUID(), 1, 0, 4));
        when(assetLookupTool.lookup(vulnerability.getId())).thenReturn(List.of(new AffectedAssetMatch(
                findingId, assetId, "web-prod-01", "PROD", "CRITICAL", true,
                "EXACT_VERSION_MATCH", "HIGH", "deterministic")));
    }
}
