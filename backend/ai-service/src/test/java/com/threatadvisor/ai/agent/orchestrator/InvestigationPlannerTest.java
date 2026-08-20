package com.threatadvisor.ai.agent.orchestrator;

import com.threatadvisor.ai.agent.asset.AssetInvestigationAgent;
import com.threatadvisor.ai.agent.common.AgentAction;
import com.threatadvisor.ai.agent.common.AgentExecution;
import com.threatadvisor.ai.agent.common.AgentStatus;
import com.threatadvisor.ai.agent.common.Confidence;
import com.threatadvisor.ai.agent.common.InvestigationStatus;
import com.threatadvisor.ai.agent.common.SecurityInvestigationContext;
import com.threatadvisor.ai.agent.dto.AffectedAssetMatch;
import com.threatadvisor.ai.agent.dto.AssetInvestigationResult;
import com.threatadvisor.ai.agent.dto.FindingRiskScore;
import com.threatadvisor.ai.agent.dto.RiskAnalystResult;
import com.threatadvisor.ai.agent.dto.ThreatIntelligenceResult;
import com.threatadvisor.ai.agent.remediation.RemediationAgent;
import com.threatadvisor.ai.agent.risk.RiskAnalystAgent;
import com.threatadvisor.ai.agent.threat.ThreatIntelligenceAgent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvestigationPlannerTest {

    private final InvestigationPlanner planner = new InvestigationPlanner();

    @Test
    void startsWithThreatWhenNothingIsKnown() {
        var decision = planner.decide(base().build(), 10);
        assertEquals(AgentAction.RUN_THREAT_AGENT, decision.nextAction());
    }

    @Test
    void maxIterationsRequiresReview() {
        var decision = planner.decide(base().iterationCount(10).build(), 10);
        assertEquals(AgentAction.REVIEW_REQUIRED, decision.nextAction());
        assertEquals(InvestigationStatus.REVIEW_REQUIRED, decision.status());
    }

    @Test
    void threatFailureRequiresReview() {
        var ctx = base()
                .replaceExecutionList(List.of(failed(ThreatIntelligenceAgent.NAME)))
                .build();
        assertEquals(AgentAction.REVIEW_REQUIRED, planner.decide(ctx, 10).nextAction());
    }

    @Test
    void lowThreatConfidenceRequiresReview() {
        var ctx = base().threat(threat(Confidence.LOW)).build();
        assertEquals(AgentAction.REVIEW_REQUIRED, planner.decide(ctx, 10).nextAction());
    }

    @Test
    void correlationFailureFailsWithoutClaimingNotAffected() {
        var ctx = base()
                .threat(threat(Confidence.HIGH))
                .replaceExecutionList(List.of(failed(AssetInvestigationAgent.NAME)))
                .build();
        assertEquals(AgentAction.FAIL, planner.decide(ctx, 10).nextAction());
    }

    @Test
    void lowAssetConfidenceRequestsMoreEvidenceOnce() {
        var ctx = base().threat(threat(Confidence.HIGH)).assets(assets(Confidence.LOW, true)).build();
        assertEquals(AgentAction.REQUEST_MORE_EVIDENCE, planner.decide(ctx, 10).nextAction());
        var after = ctx.withAssetDetailsAttempted(true);
        assertEquals(AgentAction.REVIEW_REQUIRED, planner.decide(after, 10).nextAction());
    }

    @Test
    void notAffectedCompletesWithoutRisk() {
        var ctx = base().threat(threat(Confidence.HIGH)).assets(assets(Confidence.HIGH, false)).build();
        assertEquals(AgentAction.COMPLETE, planner.decide(ctx, 10).nextAction());
    }

    @Test
    void riskRunsOnlyAfterSufficientAssetEvidence() {
        var ctx = base().threat(threat(Confidence.HIGH)).assets(assets(Confidence.HIGH, true)).build();
        assertEquals(AgentAction.RUN_RISK_AGENT, planner.decide(ctx, 10).nextAction());
    }

    @Test
    void riskFailureFailsAndRemediationGateBlocks() {
        var ctx = base()
                .threat(threat(Confidence.HIGH))
                .assets(assets(Confidence.HIGH, true))
                .replaceExecutionList(List.of(failed(RiskAnalystAgent.NAME)))
                .build();
        assertEquals(AgentAction.FAIL, planner.decide(ctx, 10).nextAction());
        assertFalse(InvestigationPlanner.remediationGate(ctx));
    }

    @Test
    void remediationRunsOnlyWhenGatePasses() {
        var ctx = withRisk().build();
        assertTrue(InvestigationPlanner.remediationGate(ctx));
        assertEquals(AgentAction.RUN_REMEDIATION_AGENT, planner.decide(ctx, 10).nextAction());
    }

    @Test
    void remediationFailurePreservesPriorEvidence() {
        var ctx = withRisk()
                .replaceExecutionList(List.of(failed(RemediationAgent.NAME)))
                .build();
        assertEquals(AgentAction.FAIL, planner.decide(ctx, 10).nextAction());
    }

    private static SecurityInvestigationContext.Builder base() {
        return SecurityInvestigationContext.builder()
                .investigationId(UUID.randomUUID())
                .cveId("CVE-2021-44228")
                .status(InvestigationStatus.RUNNING);
    }

    private static SecurityInvestigationContext.Builder withRisk() {
        UUID findingId = UUID.randomUUID();
        return base()
                .threat(threat(Confidence.HIGH))
                .assets(assets(Confidence.HIGH, true))
                .risk(new RiskAnalystResult(
                        findingId,
                        UUID.randomUUID(),
                        new BigDecimal("91.50"),
                        "CRITICAL",
                        List.of(),
                        "engine",
                        List.of(new FindingRiskScore(findingId, UUID.randomUUID(), UUID.randomUUID(),
                                new BigDecimal("91.50"), "CRITICAL", List.of(), "engine")),
                        Confidence.HIGH));
    }

    private static ThreatIntelligenceResult threat(Confidence confidence) {
        return new ThreatIntelligenceResult(
                "CVE-2021-44228", UUID.randomUUID(), "CRITICAL", new BigDecimal("10.0"),
                "NO_EXPLOIT_FLAG_IN_SOURCE", false, false, List.of(), "summary", List.of(), confidence);
    }

    private static AssetInvestigationResult assets(Confidence confidence, boolean affected) {
        if (!affected) {
            return new AssetInvestigationResult(false, 0, 0, 0, 3, List.of(), List.of(), confidence, false);
        }
        AffectedAssetMatch match = new AffectedAssetMatch(
                UUID.randomUUID(), UUID.randomUUID(), "web-prod-01", "PROD", "CRITICAL", true,
                "EXACT_VERSION_MATCH", "HIGH", "2.14.1");
        return new AssetInvestigationResult(true, 1, 1, 0, 4, List.of(match), List.of("2.14.1"), confidence, false);
    }

    private static AgentExecution failed(String agent) {
        Instant now = Instant.now();
        return new AgentExecution(agent, AgentStatus.FAILED, now, now, "failed");
    }
}
