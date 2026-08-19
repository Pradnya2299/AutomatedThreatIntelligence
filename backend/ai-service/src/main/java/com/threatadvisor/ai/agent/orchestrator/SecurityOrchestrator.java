package com.threatadvisor.ai.agent.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.threatadvisor.ai.agent.asset.AssetInvestigationAgent;
import com.threatadvisor.ai.agent.common.AgentAction;
import com.threatadvisor.ai.agent.common.AgentDecision;
import com.threatadvisor.ai.agent.common.AgentExecution;
import com.threatadvisor.ai.agent.common.AgentObservability;
import com.threatadvisor.ai.agent.common.AgentStatus;
import com.threatadvisor.ai.agent.common.Confidence;
import com.threatadvisor.ai.agent.common.DecisionRecord;
import com.threatadvisor.ai.agent.common.EvidenceItem;
import com.threatadvisor.ai.agent.common.EvidenceSource;
import com.threatadvisor.ai.agent.common.ExecutionTraceEntry;
import com.threatadvisor.ai.agent.common.InvestigationConfidence;
import com.threatadvisor.ai.agent.common.InvestigationState;
import com.threatadvisor.ai.agent.common.InvestigationStatus;
import com.threatadvisor.ai.agent.common.SecurityInvestigationContext;
import com.threatadvisor.ai.agent.dto.AffectedAssetMatch;
import com.threatadvisor.ai.agent.dto.AssetDetailsSnapshot;
import com.threatadvisor.ai.agent.dto.AssetInvestigationResult;
import com.threatadvisor.ai.agent.dto.FinalRecommendation;
import com.threatadvisor.ai.agent.dto.InstalledSoftware;
import com.threatadvisor.ai.agent.remediation.RemediationAgent;
import com.threatadvisor.ai.agent.risk.RiskAnalystAgent;
import com.threatadvisor.ai.agent.threat.ThreatIntelligenceAgent;
import com.threatadvisor.ai.agent.tool.AssetDetailsTool;
import com.threatadvisor.ai.config.AiProperties;
import com.threatadvisor.ai.kafka.EventEnvelope;
import com.threatadvisor.ai.kafka.RemediationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SecurityOrchestrator {

    private final ThreatIntelligenceAgent threatAgent;
    private final AssetInvestigationAgent assetAgent;
    private final RiskAnalystAgent riskAgent;
    private final RemediationAgent remediationAgent;
    private final AssetDetailsTool assetDetailsTool;
    private final InvestigationPlanner planner;
    private final InvestigationStore store;
    private final RemediationEventPublisher publisher;
    private final ObjectMapper objectMapper;
    private final AiProperties properties;

    public SecurityOrchestrator(
            ThreatIntelligenceAgent threatAgent,
            AssetInvestigationAgent assetAgent,
            RiskAnalystAgent riskAgent,
            RemediationAgent remediationAgent,
            AssetDetailsTool assetDetailsTool,
            InvestigationPlanner planner,
            InvestigationStore store,
            RemediationEventPublisher publisher,
            ObjectMapper objectMapper,
            AiProperties properties) {
        this.threatAgent = threatAgent;
        this.assetAgent = assetAgent;
        this.riskAgent = riskAgent;
        this.remediationAgent = remediationAgent;
        this.assetDetailsTool = assetDetailsTool;
        this.planner = planner;
        this.store = store;
        this.publisher = publisher;
        this.objectMapper = objectMapper;
        this.properties = properties;
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
                .currentState(InvestigationState.INVESTIGATING)
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
        int max = Math.max(1, properties.getMaxAgentIterations());
        while (!context.isTerminal()) {
            AgentDecision decision = planner.decide(context, max);
            context = recordDecision(context, decision);
            context = switch (decision.nextAction()) {
                case RUN_THREAT_AGENT -> afterThreat(runThreatAnalysis(context.incrementIteration()));
                case RUN_ASSET_AGENT -> afterAssets(runAssetInvestigation(context.incrementIteration()));
                case RUN_RISK_AGENT -> afterRisk(runRiskAnalysis(context.incrementIteration()));
                case RUN_REMEDIATION_AGENT -> afterRemediation(runRemediation(context.incrementIteration()));
                case REQUEST_MORE_EVIDENCE -> requestMoreEvidence(context.incrementIteration(), decision);
                case COMPLETE -> complete(context, decision);
                case REVIEW_REQUIRED -> review(context, decision);
                case FAIL -> fail(context, decision);
            };
            store.persist(context);
        }
        return context;
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
        return complete(context, new AgentDecision(
                AgentAction.COMPLETE,
                "Required evidence is present and investigation is complete",
                Confidence.HIGH,
                List.of(),
                InvestigationStatus.COMPLETED));
    }

    private SecurityInvestigationContext afterThreat(SecurityInvestigationContext context) {
        if (failed(context, ThreatIntelligenceAgent.NAME)) {
            return context;
        }
        return withTrace(context, ThreatIntelligenceAgent.NAME, AgentAction.RUN_THREAT_AGENT,
                context.threat() == null ? null : context.threat().confidence())
                .withCurrentState(InvestigationState.THREAT_ANALYZED)
                .withOverallConfidence(context.threat() == null ? Confidence.LOW : context.threat().confidence());
    }

    private SecurityInvestigationContext afterAssets(SecurityInvestigationContext context) {
        if (failed(context, AssetInvestigationAgent.NAME)) {
            return context;
        }
        Confidence confidence = context.assets() == null ? Confidence.LOW : context.assets().confidence();
        return withTrace(context, AssetInvestigationAgent.NAME, AgentAction.RUN_ASSET_AGENT, confidence)
                .withCurrentState(InvestigationState.ASSETS_ANALYZED)
                .withOverallConfidence(InvestigationConfidence.min(context.overallConfidence(), confidence));
    }

    private SecurityInvestigationContext afterRisk(SecurityInvestigationContext context) {
        if (failed(context, RiskAnalystAgent.NAME)) {
            return context;
        }
        return withTrace(context, RiskAnalystAgent.NAME, AgentAction.RUN_RISK_AGENT, Confidence.HIGH)
                .withCurrentState(InvestigationState.RISK_ANALYZED)
                .withOverallConfidence(InvestigationConfidence.min(context.overallConfidence(), Confidence.HIGH));
    }

    private SecurityInvestigationContext afterRemediation(SecurityInvestigationContext context) {
        if (failed(context, RemediationAgent.NAME)) {
            return context;
        }
        Confidence confidence = context.remediation() == null ? Confidence.MEDIUM : context.remediation().confidence();
        return withTrace(context, RemediationAgent.NAME, AgentAction.RUN_REMEDIATION_AGENT, confidence)
                .withCurrentState(InvestigationState.REMEDIATION_GENERATED)
                .withOverallConfidence(InvestigationConfidence.min(context.overallConfidence(), confidence));
    }

    private SecurityInvestigationContext requestMoreEvidence(SecurityInvestigationContext context, AgentDecision decision) {
        Instant start = Instant.now();
        List<UUID> assetIds = context.assets() == null || context.assets().assets() == null
                ? List.of()
                : context.assets().assets().stream().map(AffectedAssetMatch::assetId).toList();
        List<AssetDetailsSnapshot> details = assetDetailsTool.loadDetails(assetIds);
        AssetInvestigationResult enriched = enrich(context.assets(), details);
        Instant end = Instant.now();
        SecurityInvestigationContext next = context
                .withAssets(enriched)
                .withAssetDetailsAttempted(true)
                .withCurrentState(InvestigationState.ASSETS_ANALYZED)
                .withOverallConfidence(InvestigationConfidence.min(context.overallConfidence(), enriched.confidence()))
                .withEvidenceItem(EvidenceItem.fact(
                        EvidenceSource.ASSET_INVENTORY,
                        "asset_details",
                        "Installed software versions from asset inventory",
                        "assets=" + details.size(),
                        enriched.confidence()))
                .withTrace(ExecutionTraceEntry.of(
                        context.investigationId(),
                        "AssetDetailsTool",
                        AgentAction.REQUEST_MORE_EVIDENCE,
                        AgentStatus.COMPLETED,
                        start,
                        end,
                        decision.reason(),
                        enriched.confidence(),
                        List.of("installed_version"),
                        null));
        return next;
    }

    private AssetInvestigationResult enrich(AssetInvestigationResult current, List<AssetDetailsSnapshot> details) {
        if (current == null) {
            return null;
        }
        List<AffectedAssetMatch> updated = new ArrayList<>();
        for (AffectedAssetMatch match : current.assets()) {
            String versions = details.stream()
                    .filter(d -> d.assetId().equals(match.assetId()))
                    .flatMap(d -> d.software().stream())
                    .map(InstalledSoftware::version)
                    .filter(v -> v != null && !v.isBlank())
                    .findFirst()
                    .orElse(null);
            if (versions != null && !InvestigationConfidence.hasVersion(match)) {
                updated.add(new AffectedAssetMatch(
                        match.findingId(),
                        match.assetId(),
                        match.hostname(),
                        match.environment(),
                        match.businessCriticality(),
                        match.internetExposure(),
                        match.matchType() == null ? "INVENTORY_VERSION" : match.matchType(),
                        "HIGH",
                        match.matchReason() + " Inventory version " + versions + "."));
            } else {
                updated.add(match);
            }
        }
        AssetInvestigationResult next = new AssetInvestigationResult(
                current.affected(),
                current.affectedAssetCount(),
                current.findingsCreated(),
                current.findingsUpdated(),
                current.matchesEvaluated(),
                updated,
                updated.stream().map(AffectedAssetMatch::matchReason).toList(),
                null,
                true);
        return new AssetInvestigationResult(
                next.affected(),
                next.affectedAssetCount(),
                next.findingsCreated(),
                next.findingsUpdated(),
                next.matchesEvaluated(),
                next.assets(),
                next.matchReasons(),
                InvestigationConfidence.assets(next),
                true);
    }

    private SecurityInvestigationContext complete(SecurityInvestigationContext context, AgentDecision decision) {
        if (context.assets() != null && !context.assets().affected()) {
            context = skip(context, RiskAnalystAgent.NAME, "No affected assets; risk engine not invoked");
            context = skip(context, RemediationAgent.NAME, "No affected assets; remediation not generated");
            return context
                    .withStatus(InvestigationStatus.COMPLETED)
                    .withCurrentState(InvestigationState.COMPLETED)
                    .withOverallConfidence(Confidence.HIGH)
                    .withRecommendation(new FinalRecommendation(
                            false,
                            null,
                            "NONE",
                            null,
                            "No inventory assets matched this CVE via the correlation engine."));
        }
        boolean remFailed = failed(context, RemediationAgent.NAME);
        return context
                .withStatus(InvestigationStatus.COMPLETED)
                .withCurrentState(InvestigationState.COMPLETED)
                .withRecommendation(recommendation(context, remFailed));
    }

    private SecurityInvestigationContext review(SecurityInvestigationContext context, AgentDecision decision) {
        return context
                .withStatus(InvestigationStatus.REVIEW_REQUIRED)
                .withCurrentState(InvestigationState.REVIEW_REQUIRED)
                .withOverallConfidence(decision.confidence())
                .withRecommendation(new FinalRecommendation(
                        context.assets() != null && context.assets().affected(),
                        context.risk() == null ? null : context.risk().riskScore(),
                        context.risk() == null ? null : context.risk().riskLevel(),
                        null,
                        "REVIEW_REQUIRED: " + decision.reason()));
    }

    private SecurityInvestigationContext fail(SecurityInvestigationContext context, AgentDecision decision) {
        if (failed(context, RiskAnalystAgent.NAME) && context.remediation() == null) {
            context = skip(context, RemediationAgent.NAME, "Risk data missing; remediation withheld");
        }
        boolean remFailed = failed(context, RemediationAgent.NAME);
        InvestigationState state = remFailed && context.risk() != null
                ? InvestigationState.FAILED
                : InvestigationState.FAILED;
        return context
                .withStatus(InvestigationStatus.FAILED)
                .withCurrentState(state)
                .withOverallConfidence(decision.confidence())
                .withRecommendation(recommendation(context, remFailed || context.risk() == null));
    }

    private FinalRecommendation recommendation(SecurityInvestigationContext context, boolean remediationFailed) {
        if (context.assets() != null && !context.assets().affected()) {
            return new FinalRecommendation(false, null, "NONE", null,
                    "No inventory assets matched this CVE via the correlation engine.");
        }
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

    private SecurityInvestigationContext recordDecision(SecurityInvestigationContext context, AgentDecision decision) {
        DecisionRecord record = new DecisionRecord(
                UUID.randomUUID(),
                decision.nextAction(),
                decision.reason(),
                decision.requiredEvidence() == null ? List.of() : decision.requiredEvidence(),
                decision.status() == null ? null : decision.status().name(),
                decision.confidence(),
                Instant.now());
        AgentObservability.bind(context.investigationId(), context.cveId(), "SecurityOrchestrator");
        AgentObservability.investigation(context.investigationId(), context.status());
        return context.withDecision(record).withTrace(ExecutionTraceEntry.of(
                context.investigationId(),
                "SecurityOrchestrator",
                decision.nextAction(),
                AgentStatus.COMPLETED,
                Instant.now(),
                Instant.now(),
                decision.reason(),
                decision.confidence(),
                decision.requiredEvidence(),
                null));
    }

    private SecurityInvestigationContext withTrace(
            SecurityInvestigationContext context, String agent, AgentAction action, Confidence confidence) {
        var execution = context.executionOf(agent).orElse(null);
        Instant start = execution == null ? Instant.now() : execution.startedAt();
        Instant end = execution == null || execution.completedAt() == null ? Instant.now() : execution.completedAt();
        return context.withTrace(ExecutionTraceEntry.of(
                context.investigationId(),
                agent,
                action,
                execution == null ? AgentStatus.COMPLETED : execution.status(),
                start,
                end,
                execution == null ? null : execution.failureReason(),
                confidence,
                List.of(),
                execution == null ? null : execution.failureReason()));
    }

    private SecurityInvestigationContext skip(SecurityInvestigationContext context, String agent, String reason) {
        Instant now = Instant.now();
        AgentObservability.skipped(context.investigationId(), agent, reason);
        return store.persist(context.withExecution(new AgentExecution(agent, AgentStatus.SKIPPED, now, now, reason)));
    }

    private boolean failed(SecurityInvestigationContext context, String agent) {
        return context.agentFailed(agent);
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
