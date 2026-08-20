package com.threatadvisor.ai.agent.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threatadvisor.ai.agent.asset.AssetInvestigationAgent;
import com.threatadvisor.ai.agent.common.AgentAction;
import com.threatadvisor.ai.agent.common.AgentStatus;
import com.threatadvisor.ai.agent.common.AgentToolException;
import com.threatadvisor.ai.agent.common.Confidence;
import com.threatadvisor.ai.agent.common.InvestigationState;
import com.threatadvisor.ai.agent.common.InvestigationStatus;
import com.threatadvisor.ai.agent.dto.AffectedAssetMatch;
import com.threatadvisor.ai.agent.dto.AssetDetailsSnapshot;
import com.threatadvisor.ai.agent.dto.CorrelationToolResult;
import com.threatadvisor.ai.agent.dto.InstalledSoftware;
import com.threatadvisor.ai.agent.dto.KnowledgeSearchResult;
import com.threatadvisor.ai.agent.dto.RemediationToolResult;
import com.threatadvisor.ai.agent.dto.RiskEngineSnapshot;
import com.threatadvisor.ai.agent.dto.RiskFactor;
import com.threatadvisor.ai.agent.remediation.RemediationAgent;
import com.threatadvisor.ai.agent.risk.RiskAnalystAgent;
import com.threatadvisor.ai.agent.threat.ThreatIntelligenceAgent;
import com.threatadvisor.ai.agent.tool.AffectedAssetLookupTool;
import com.threatadvisor.ai.agent.tool.AssetDetailsTool;
import com.threatadvisor.ai.agent.tool.CorrelationTool;
import com.threatadvisor.ai.agent.tool.CpeLookupTool;
import com.threatadvisor.ai.agent.tool.CveLookupTool;
import com.threatadvisor.ai.agent.tool.KnowledgeSearchTool;
import com.threatadvisor.ai.agent.tool.PolicyRetrievalTool;
import com.threatadvisor.ai.agent.tool.RemediationGenerationTool;
import com.threatadvisor.ai.agent.tool.RiskCalculationTool;
import com.threatadvisor.ai.agent.tool.VulnerabilityContextTool;
import com.threatadvisor.ai.config.AiProperties;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrchestratorTest {

    @Mock
    private CveLookupTool cveLookupTool;
    @Mock
    private CpeLookupTool cpeLookupTool;
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
    private KnowledgeSearchTool knowledgeSearchTool;
    @Mock
    private PolicyRetrievalTool policyRetrievalTool;
    @Mock
    private AssetDetailsTool assetDetailsTool;
    @Mock
    private InvestigationStore store;
    @Mock
    private RemediationEventPublisher publisher;

    private AiProperties properties;
    private SecurityOrchestrator orchestrator;
    private Vulnerability vulnerability;
    private UUID findingId;
    private UUID assetId;

    @BeforeEach
    void setUp() {
        when(store.persist(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(cpeLookupTool.lookupCpes(any())).thenReturn(List.of());
        lenient().when(knowledgeSearchTool.search(any())).thenReturn(new KnowledgeSearchResult(List.of()));
        lenient().when(policyRetrievalTool.retrievePolicies(any())).thenReturn(new KnowledgeSearchResult(List.of()));
        lenient().when(assetDetailsTool.loadDetails(any())).thenReturn(List.of());
        vulnerability = new Vulnerability();
        vulnerability.setId(UUID.randomUUID());
        vulnerability.setCveId("CVE-2021-44228");
        vulnerability.setSeverity("CRITICAL");
        vulnerability.setCvssScore(new BigDecimal("10.0"));
        vulnerability.setDescription("RCE");
        vulnerability.setAffectedProducts(new String[] {"log4j"});
        vulnerability.setAffectedVendors(new String[] {"apache"});
        vulnerability.setExploitAvailable(false);
        vulnerability.setActivelyExploited(false);
        findingId = UUID.randomUUID();
        assetId = UUID.randomUUID();
        properties = new AiProperties();
        properties.setMaxAgentIterations(10);
        orchestrator = newOrchestrator();
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
        assertEquals(InvestigationState.COMPLETED, result.currentState());
        assertEquals(new BigDecimal("91.50"), result.risk().riskScore());
        assertEquals("web-prod-01", result.assets().assets().getFirst().hostname());
        assertTrue(result.remediation().ragContextUsed());
        assertEquals("IMMEDIATE", result.recommendation().priority());
        assertEquals(AgentAction.RUN_THREAT_AGENT, result.decisionHistory().getFirst().action());
        assertTrue(result.decisionHistory().stream().anyMatch(d -> d.action() == AgentAction.RUN_ASSET_AGENT));
        assertTrue(result.decisionHistory().stream().anyMatch(d -> d.action() == AgentAction.RUN_RISK_AGENT));
        assertTrue(result.decisionHistory().stream().anyMatch(d -> d.action() == AgentAction.RUN_REMEDIATION_AGENT));
        assertTrue(result.decisionHistory().stream().anyMatch(d -> d.action() == AgentAction.COMPLETE));
        assertTrue(result.executionTrace().stream().anyMatch(t -> "SecurityOrchestrator".equals(t.agentName())));
        assertFalse(result.evidence().isEmpty());
        assertTrue(result.iterationCount() <= 10);
        verify(contextTool).summarize(vulnerability);
        verify(publisher).publish(any(), any(), any());
    }

    @Test
    void threatFailureStopsWorkflow() {
        when(cveLookupTool.findByCveId(any())).thenReturn(Optional.empty());

        var result = orchestrator.startInvestigation("CVE-1999-0001", null);

        assertEquals(InvestigationStatus.REVIEW_REQUIRED, result.status());
        assertEquals(InvestigationState.REVIEW_REQUIRED, result.currentState());
        verify(correlationTool, never()).correlate(any(), any());
        verify(riskCalculationTool, never()).calculate(any(), any());
        verify(remediationGenerationTool, never()).generate(any(), any());
        assertEquals(AgentStatus.FAILED, result.executionOf(ThreatIntelligenceAgent.NAME).orElseThrow().status());
    }

    @Test
    void unknownCveRequiresReview() {
        when(cveLookupTool.findByCveId(any())).thenReturn(Optional.empty());
        var result = orchestrator.startInvestigation("CVE-2099-0000", null);
        assertEquals(InvestigationStatus.REVIEW_REQUIRED, result.status());
        assertTrue(result.recommendation().summary().contains("REVIEW_REQUIRED"));
    }

    @Test
    void threatConfidenceLowRequiresReview() {
        vulnerability.setCvssScore(null);
        vulnerability.setAffectedProducts(null);
        when(cveLookupTool.findByCveId(any())).thenReturn(Optional.of(vulnerability));
        when(contextTool.summarize(any())).thenReturn("incomplete");

        var result = orchestrator.startInvestigation("CVE-2021-44228", UUID.randomUUID());

        assertEquals(InvestigationStatus.REVIEW_REQUIRED, result.status());
        assertEquals(Confidence.LOW, result.threat().confidence());
        verify(correlationTool, never()).correlate(any(), any());
        verify(riskCalculationTool, never()).calculate(any(), any());
    }

    @Test
    void assetFailureDoesNotProduceNotAffected() {
        when(cveLookupTool.findByCveId(any())).thenReturn(Optional.of(vulnerability));
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
    void assetCorrelationLowRequestsMoreEvidenceThenReviewsIfStillLow() {
        when(cveLookupTool.findByCveId(any())).thenReturn(Optional.of(vulnerability));
        when(contextTool.summarize(any())).thenReturn("summary");
        when(correlationTool.correlate(any(), any())).thenReturn(new CorrelationToolResult(
                "COMPLETED", "CVE-2021-44228", vulnerability.getId(), UUID.randomUUID(), 1, 0, 4));
        when(assetLookupTool.lookup(vulnerability.getId())).thenReturn(List.of(new AffectedAssetMatch(
                findingId, assetId, "web-prod-01", "PROD", "CRITICAL", true,
                "PARTIAL", "LOW", "product name only, no installed version")));

        var result = orchestrator.startInvestigation("CVE-2021-44228", UUID.randomUUID());

        assertEquals(InvestigationStatus.REVIEW_REQUIRED, result.status());
        assertTrue(result.assetDetailsAttempted());
        assertTrue(result.decisionHistory().stream().anyMatch(d -> d.action() == AgentAction.REQUEST_MORE_EVIDENCE));
        verify(assetDetailsTool).loadDetails(any());
        verify(riskCalculationTool, never()).calculate(any(), any());
        verify(remediationGenerationTool, never()).generate(any(), any());
    }

    @Test
    void missingAssetVersionIsEnrichedThenRiskRuns() {
        when(cveLookupTool.findByCveId(any())).thenReturn(Optional.of(vulnerability));
        when(contextTool.summarize(any())).thenReturn("summary");
        when(correlationTool.correlate(any(), any())).thenReturn(new CorrelationToolResult(
                "COMPLETED", "CVE-2021-44228", vulnerability.getId(), UUID.randomUUID(), 1, 0, 4));
        when(assetLookupTool.lookup(vulnerability.getId())).thenReturn(List.of(new AffectedAssetMatch(
                findingId, assetId, "web-prod-01", "PROD", "CRITICAL", true,
                "PRODUCT_MATCH", "MEDIUM", "CPE product match without version")));
        when(assetDetailsTool.loadDetails(any())).thenReturn(List.of(new AssetDetailsSnapshot(
                assetId, "web-prod-01", List.of(new InstalledSoftware("apache", "log4j", "2.14.1")))));
        stubRiskAndRemediation();

        var result = orchestrator.startInvestigation("CVE-2021-44228", UUID.randomUUID());

        assertEquals(InvestigationStatus.COMPLETED, result.status());
        assertEquals(Confidence.HIGH, result.assets().confidence());
        assertTrue(result.assetDetailsAttempted());
        verify(riskCalculationTool).calculate(findingId, result.correlationId());
        verify(remediationGenerationTool).generate(findingId, result.correlationId());
    }

    @Test
    void riskFailureSkipsRemediationAndKeepsAssetResult() {
        when(cveLookupTool.findByCveId(any())).thenReturn(Optional.of(vulnerability));
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

    @Test
    void remediationCannotRunWithoutDeterministicRisk() {
        when(cveLookupTool.findByCveId(any())).thenReturn(Optional.of(vulnerability));
        when(contextTool.summarize(any())).thenReturn("summary");
        stubCorrelationAndAssets();
        when(riskCalculationTool.calculate(any(), any()))
                .thenThrow(new AgentToolException("RISK_ENGINE_UNAVAILABLE", "down"));

        var result = orchestrator.startInvestigation("CVE-2021-44228", UUID.randomUUID());

        assertNull(result.remediation());
        verify(remediationGenerationTool, never()).generate(any(), any());
        assertFalse(InvestigationPlanner.remediationGate(result));
    }

    @Test
    void maximumIterationsYieldsReviewRequiredWithoutInfiniteLoop() {
        properties.setMaxAgentIterations(1);
        orchestrator = newOrchestrator();
        when(cveLookupTool.findByCveId(any())).thenReturn(Optional.of(vulnerability));
        when(contextTool.summarize(any())).thenReturn("summary");

        var result = orchestrator.startInvestigation("CVE-2021-44228", UUID.randomUUID());

        assertEquals(InvestigationStatus.REVIEW_REQUIRED, result.status());
        assertTrue(result.decisionHistory().stream().anyMatch(d ->
                d.action() == AgentAction.REVIEW_REQUIRED && d.reason().contains("Maximum agent iterations")));
        assertTrue(result.iterationCount() <= 1);
        verify(correlationTool, never()).correlate(any(), any());
    }

    private SecurityOrchestrator newOrchestrator() {
        return new SecurityOrchestrator(
                new ThreatIntelligenceAgent(cveLookupTool, cpeLookupTool, contextTool),
                new AssetInvestigationAgent(correlationTool, assetLookupTool),
                new RiskAnalystAgent(riskCalculationTool),
                new RemediationAgent(remediationGenerationTool, knowledgeSearchTool, policyRetrievalTool),
                assetDetailsTool,
                new InvestigationPlanner(),
                store,
                publisher,
                new ObjectMapper(),
                properties);
    }

    private void stubHappyPath() {
        when(cveLookupTool.findByCveId(any())).thenReturn(Optional.of(vulnerability));
        when(contextTool.summarize(any())).thenReturn("summary");
        stubCorrelationAndAssets();
        stubRiskAndRemediation();
    }

    private void stubRiskAndRemediation() {
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
