package com.threatadvisor.ai.agent.orchestrator;

import com.threatadvisor.ai.agent.asset.AssetInvestigationAgent;
import com.threatadvisor.ai.agent.common.AgentAction;
import com.threatadvisor.ai.agent.common.AgentDecision;
import com.threatadvisor.ai.agent.common.Confidence;
import com.threatadvisor.ai.agent.common.InvestigationStatus;
import com.threatadvisor.ai.agent.common.SecurityInvestigationContext;
import com.threatadvisor.ai.agent.remediation.RemediationAgent;
import com.threatadvisor.ai.agent.risk.RiskAnalystAgent;
import com.threatadvisor.ai.agent.threat.ThreatIntelligenceAgent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InvestigationPlanner {

    public AgentDecision decide(SecurityInvestigationContext context, int maxIterations) {
        if (context.iterationCount() >= maxIterations) {
            return new AgentDecision(
                    AgentAction.REVIEW_REQUIRED,
                    "Maximum agent iterations reached (" + maxIterations + ")",
                    Confidence.LOW,
                    List.of("bounded_loop"),
                    InvestigationStatus.REVIEW_REQUIRED);
        }
        if (context.threat() == null) {
            if (context.agentFailed(ThreatIntelligenceAgent.NAME)) {
                return new AgentDecision(
                        AgentAction.REVIEW_REQUIRED,
                        "Threat intelligence failed; unknown CVE or lookup error",
                        Confidence.LOW,
                        List.of("cve_row"),
                        InvestigationStatus.REVIEW_REQUIRED);
            }
            return new AgentDecision(
                    AgentAction.RUN_THREAT_AGENT,
                    "Threat intelligence is missing",
                    Confidence.MEDIUM,
                    List.of("cve_row", "cpe"),
                    InvestigationStatus.RUNNING);
        }
        if (context.threat().confidence() == Confidence.LOW) {
            return new AgentDecision(
                    AgentAction.REVIEW_REQUIRED,
                    "Threat evidence confidence is LOW",
                    Confidence.LOW,
                    List.of("cvss", "affected_products"),
                    InvestigationStatus.REVIEW_REQUIRED);
        }
        if (context.assets() == null) {
            if (context.agentFailed(AssetInvestigationAgent.NAME)) {
                return new AgentDecision(
                        AgentAction.FAIL,
                        "Asset correlation failed; exposure is unknown",
                        Confidence.LOW,
                        List.of("correlation"),
                        InvestigationStatus.FAILED);
            }
            return new AgentDecision(
                    AgentAction.RUN_ASSET_AGENT,
                    "Affected asset evidence is missing",
                    Confidence.MEDIUM,
                    List.of("correlation_matches"),
                    InvestigationStatus.RUNNING);
        }
        if (context.assets().affected() && context.assets().confidence() == Confidence.LOW) {
            if (!context.assetDetailsAttempted()) {
                return new AgentDecision(
                        AgentAction.REQUEST_MORE_EVIDENCE,
                        "Asset correlation confidence is LOW; requesting installed-version details",
                        Confidence.LOW,
                        List.of("installed_version"),
                        InvestigationStatus.RUNNING);
            }
            return new AgentDecision(
                    AgentAction.REVIEW_REQUIRED,
                    "Asset evidence remains LOW after additional inventory lookup",
                    Confidence.LOW,
                    List.of("installed_version"),
                    InvestigationStatus.REVIEW_REQUIRED);
        }
        if (!context.assets().affected()) {
            return new AgentDecision(
                    AgentAction.COMPLETE,
                    "Correlation completed with no affected assets",
                    Confidence.HIGH,
                    List.of(),
                    InvestigationStatus.COMPLETED);
        }
        if (context.risk() == null) {
            if (context.agentFailed(RiskAnalystAgent.NAME)) {
                return new AgentDecision(
                        AgentAction.FAIL,
                        "Risk engine failed; remediation withheld",
                        Confidence.LOW,
                        List.of("risk_score"),
                        InvestigationStatus.FAILED);
            }
            return new AgentDecision(
                    AgentAction.RUN_RISK_AGENT,
                    "Threat and asset evidence are sufficient for deterministic risk",
                    context.assets().confidence(),
                    List.of("risk_score"),
                    InvestigationStatus.RUNNING);
        }
        if (context.remediation() == null) {
            if (context.agentFailed(RemediationAgent.NAME)) {
                return new AgentDecision(
                        AgentAction.FAIL,
                        "Remediation generation failed; prior evidence preserved",
                        context.risk() == null ? Confidence.MEDIUM : Confidence.HIGH,
                        List.of("remediation_plan"),
                        InvestigationStatus.FAILED);
            }
            if (!remediationGate(context)) {
                return new AgentDecision(
                        AgentAction.REVIEW_REQUIRED,
                        "Remediation gate failed: threat, assets, and deterministic risk are required",
                        Confidence.LOW,
                        List.of("threat", "assets", "risk"),
                        InvestigationStatus.REVIEW_REQUIRED);
            }
            return new AgentDecision(
                    AgentAction.RUN_REMEDIATION_AGENT,
                    "Risk assessment completed",
                    Confidence.HIGH,
                    List.of("rag_context"),
                    InvestigationStatus.RUNNING);
        }
        return new AgentDecision(
                AgentAction.COMPLETE,
                "Required evidence is present and investigation is complete",
                Confidence.HIGH,
                List.of(),
                InvestigationStatus.COMPLETED);
    }

    public static boolean remediationGate(SecurityInvestigationContext context) {
        return context.threat() != null
                && context.assets() != null
                && context.risk() != null
                && context.threat().confidence() != Confidence.LOW
                && context.assets().confidence() != Confidence.LOW
                && context.risk().riskScore() != null;
    }
}
