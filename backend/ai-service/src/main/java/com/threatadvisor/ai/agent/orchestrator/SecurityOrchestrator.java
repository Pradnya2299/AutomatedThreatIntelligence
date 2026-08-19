package com.threatadvisor.ai.agent.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.threatadvisor.ai.agent.asset.AssetInvestigationAgent;
import com.threatadvisor.ai.agent.common.AgentExecution;
import com.threatadvisor.ai.agent.common.AgentObservability;
import com.threatadvisor.ai.agent.common.AgentStatus;
import com.threatadvisor.ai.agent.common.InvestigationStatus;
import com.threatadvisor.ai.agent.common.SecurityInvestigationContext;
import com.threatadvisor.ai.agent.dto.FinalRecommendation;
import com.threatadvisor.ai.agent.remediation.RemediationAgent;
import com.threatadvisor.ai.agent.risk.RiskAnalystAgent;
import com.threatadvisor.ai.agent.threat.ThreatIntelligenceAgent;
import com.threatadvisor.ai.kafka.EventEnvelope;
import com.threatadvisor.ai.kafka.RemediationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class SecurityOrchestrator {

    private final ThreatIntelligenceAgent threatAgent;
    private final AssetInvestigationAgent assetAgent;
    private final RiskAnalystAgent riskAgent;
    private final RemediationAgent remediationAgent;
    private final InvestigationStore store;
    private final RemediationEventPublisher publisher;
    private final ObjectMapper objectMapper;

    public SecurityOrchestrator(
            ThreatIntelligenceAgent threatAgent,
            AssetInvestigationAgent assetAgent,
            RiskAnalystAgent riskAgent,
            RemediationAgent remediationAgent,
            InvestigationStore store,
            RemediationEventPublisher publisher,
            ObjectMapper objectMapper) {
        this.threatAgent = threatAgent;
        this.assetAgent = assetAgent;
        this.riskAgent = riskAgent;
        this.remediationAgent = remediationAgent;
        this.store = store;
        this.publisher = publisher;
        this.objectMapper = objectMapper;
    }

    public SecurityInvestigationContext startInvestigation(String cveId, UUID correlationId) {
        Instant now = Instant.now();
        UUID investigationId = UUID.randomUUID();
        UUID corr = correlationId == null ? investigationId : correlationId;
        SecurityInvestigationContext context = SecurityInvestigationContext.builder()
                .investigationId(investigationId)
                .cveId(normalizeCve(cveId))
                .correlationId(corr)
                .startedAt(now)
                .status(InvestigationStatus.RUNNING)
                .build();
        AgentObservability.bind(investigationId, context.cveId(), "SecurityOrchestrator");
        AgentObservability.investigation(investigationId, InvestigationStatus.RUNNING);
        store.persist(context);
        return finish(runWorkflow(context));
    }

    public Optional<SecurityInvestigationContext> get(UUID investigationId) {
        return store.find(investigationId);
    }

    SecurityInvestigationContext runWorkflow(SecurityInvestigationContext context) {
        context = runThreatAnalysis(context);
        if (failed(context, ThreatIntelligenceAgent.NAME)) {
            return context.withStatus(InvestigationStatus.REVIEW_REQUIRED);
        }
        context = runAssetInvestigation(context);
        if (failed(context, AssetInvestigationAgent.NAME)) {
            return context.withStatus(InvestigationStatus.FAILED);
        }
        if (context.assets() == null || !context.assets().affected()) {
            context = skip(context, RiskAnalystAgent.NAME, "No affected assets; risk engine not invoked");
            context = skip(context, RemediationAgent.NAME, "No affected assets; remediation not generated");
            return context.withStatus(InvestigationStatus.COMPLETED)
                    .withRecommendation(new FinalRecommendation(
                            false,
                            null,
                            "NONE",
                            null,
                            "No inventory assets matched this CVE via the correlation engine."));
        }
        context = runRiskAnalysis(context);
        if (failed(context, RiskAnalystAgent.NAME)) {
            context = skip(context, RemediationAgent.NAME, "Risk data missing; remediation withheld");
            return context.withStatus(InvestigationStatus.FAILED);
        }
        context = runRemediation(context);
        if (failed(context, RemediationAgent.NAME)) {
            return context.withStatus(InvestigationStatus.FAILED)
                    .withRecommendation(buildFinalDecision(context, true));
        }
        return buildFinalDecision(context);
    }

    public SecurityInvestigationContext runThreatAnalysis(SecurityInvestigationContext context) {
        return threatAgent.runObserved(context, store::persist);
    }

    public SecurityInvestigationContext runAssetInvestigation(SecurityInvestigationContext context) {
        return assetAgent.runObserved(context, store::persist);
    }

    public SecurityInvestigationContext runRiskAnalysis(SecurityInvestigationContext context) {
        return riskAgent.runObserved(context, store::persist);
    }

    public SecurityInvestigationContext runRemediation(SecurityInvestigationContext context) {
        return remediationAgent.runObserved(context, store::persist);
    }

    public SecurityInvestigationContext buildFinalDecision(SecurityInvestigationContext context) {
        return context.withStatus(InvestigationStatus.COMPLETED)
                .withRecommendation(buildFinalDecision(context, false));
    }

    private FinalRecommendation buildFinalDecision(SecurityInvestigationContext context, boolean remediationFailed) {
        String priority = context.remediation() == null ? null : context.remediation().priority();
        String summary;
        if (remediationFailed) {
            summary = "Threat, asset, and risk results were preserved. Remediation generation failed.";
        } else if (context.remediation() != null && context.remediation().summary() != null) {
            summary = context.remediation().summary();
        } else {
            summary = "Investigation completed with deterministic risk "
                    + (context.risk() == null ? "n/a" : context.risk().riskLevel());
        }
        return new FinalRecommendation(
                true,
                context.risk() == null ? null : context.risk().riskScore(),
                context.risk() == null ? null : context.risk().riskLevel(),
                priority,
                summary);
    }

    private SecurityInvestigationContext skip(SecurityInvestigationContext context, String agent, String reason) {
        Instant now = Instant.now();
        AgentObservability.skipped(context.investigationId(), agent, reason);
        return store.persist(context.withExecution(new AgentExecution(agent, AgentStatus.SKIPPED, now, now, reason)));
    }

    private boolean failed(SecurityInvestigationContext context, String agent) {
        return context.executionOf(agent).map(e -> e.status() == AgentStatus.FAILED).orElse(false);
    }

    private SecurityInvestigationContext finish(SecurityInvestigationContext context) {
        Instant done = Instant.now();
        SecurityInvestigationContext complete = store.persist(context.withCompletedAt(done));
        AgentObservability.investigation(complete.investigationId(), complete.status());
        publishCompleted(complete);
        return complete;
    }

    private void publishCompleted(SecurityInvestigationContext context) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("investigationId", context.investigationId().toString());
        payload.put("cveId", context.cveId());
        payload.put("status", context.status().name());
        payload.put("affected", context.assets() != null && context.assets().affected());
        if (context.risk() != null && context.risk().riskScore() != null) {
            payload.put("riskScore", context.risk().riskScore());
            payload.put("riskLevel", context.risk().riskLevel());
        }
        if (context.remediation() != null && context.remediation().remediationPlanId() != null) {
            payload.put("remediationPlanId", context.remediation().remediationPlanId().toString());
        }
        EventEnvelope envelope = new EventEnvelope(
                UUID.randomUUID(),
                EventEnvelope.TYPE_INVESTIGATION_COMPLETED,
                1,
                Instant.now(),
                EventEnvelope.SOURCE_SERVICE,
                context.correlationId(),
                payload);
        try {
            publisher.publish(EventEnvelope.TYPE_INVESTIGATION_COMPLETED, context.investigationId().toString(), envelope);
        } catch (RuntimeException ex) {
            AgentObservability.failed(context.investigationId(), "SecurityOrchestrator", "completed event publish failed");
        }
    }

    private static String normalizeCve(String cveId) {
        if (cveId == null) {
            return null;
        }
        return cveId.trim().toUpperCase();
    }
}
