package com.threatadvisor.ai.agent.common;

import com.threatadvisor.ai.agent.dto.AssetInvestigationResult;
import com.threatadvisor.ai.agent.dto.FinalRecommendation;
import com.threatadvisor.ai.agent.dto.RemediationAgentResult;
import com.threatadvisor.ai.agent.dto.RiskAnalystResult;
import com.threatadvisor.ai.agent.dto.ThreatIntelligenceResult;
import com.threatadvisor.ai.domain.Vulnerability;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class SecurityInvestigationContext {

    private static final List<String> ALL_AGENTS = List.of(
            "ThreatIntelligenceAgent",
            "AssetInvestigationAgent",
            "RiskAnalystAgent",
            "RemediationAgent");

    private final UUID investigationId;
    private final String cveId;
    private final UUID correlationId;
    private final Instant startedAt;
    private final Instant completedAt;
    private final InvestigationStatus status;
    private final InvestigationState currentState;
    private final Confidence overallConfidence;
    private final int iterationCount;
    private final Vulnerability vulnerability;
    private final ThreatIntelligenceResult threat;
    private final AssetInvestigationResult assets;
    private final RiskAnalystResult risk;
    private final RemediationAgentResult remediation;
    private final List<AgentExecution> executions;
    private final List<AgentError> errors;
    private final List<EvidenceItem> evidence;
    private final List<DecisionRecord> decisionHistory;
    private final List<ExecutionTraceEntry> executionTrace;
    private final List<String> completedAgents;
    private final List<String> pendingAgents;
    private final boolean assetDetailsAttempted;
    private final FinalRecommendation recommendation;

    private SecurityInvestigationContext(Builder builder) {
        this.investigationId = builder.investigationId;
        this.cveId = builder.cveId;
        this.correlationId = builder.correlationId;
        this.startedAt = builder.startedAt;
        this.completedAt = builder.completedAt;
        this.status = builder.status;
        this.currentState = builder.currentState;
        this.overallConfidence = builder.overallConfidence;
        this.iterationCount = builder.iterationCount;
        this.vulnerability = builder.vulnerability;
        this.threat = builder.threat;
        this.assets = builder.assets;
        this.risk = builder.risk;
        this.remediation = builder.remediation;
        this.executions = List.copyOf(builder.executions);
        this.errors = List.copyOf(builder.errors);
        this.evidence = List.copyOf(builder.evidence);
        this.decisionHistory = List.copyOf(builder.decisionHistory);
        this.executionTrace = List.copyOf(builder.executionTrace);
        this.completedAgents = List.copyOf(builder.completedAgents);
        this.pendingAgents = List.copyOf(builder.pendingAgents);
        this.assetDetailsAttempted = builder.assetDetailsAttempted;
        this.recommendation = builder.recommendation;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        Builder b = new Builder();
        b.investigationId = investigationId;
        b.cveId = cveId;
        b.correlationId = correlationId;
        b.startedAt = startedAt;
        b.completedAt = completedAt;
        b.status = status;
        b.currentState = currentState;
        b.overallConfidence = overallConfidence;
        b.iterationCount = iterationCount;
        b.vulnerability = vulnerability;
        b.threat = threat;
        b.assets = assets;
        b.risk = risk;
        b.remediation = remediation;
        b.executions = new ArrayList<>(executions);
        b.errors = new ArrayList<>(errors);
        b.evidence = new ArrayList<>(evidence);
        b.decisionHistory = new ArrayList<>(decisionHistory);
        b.executionTrace = new ArrayList<>(executionTrace);
        b.completedAgents = new ArrayList<>(completedAgents);
        b.pendingAgents = new ArrayList<>(pendingAgents);
        b.assetDetailsAttempted = assetDetailsAttempted;
        b.recommendation = recommendation;
        return b;
    }

    public SecurityInvestigationContext withStatus(InvestigationStatus next) {
        return toBuilder().status(next).build();
    }

    public SecurityInvestigationContext withCurrentState(InvestigationState next) {
        return toBuilder().currentState(next).build();
    }

    public SecurityInvestigationContext withOverallConfidence(Confidence value) {
        return toBuilder().overallConfidence(value).build();
    }

    public SecurityInvestigationContext withIterationCount(int value) {
        return toBuilder().iterationCount(value).build();
    }

    public SecurityInvestigationContext incrementIteration() {
        return toBuilder().iterationCount(iterationCount + 1).build();
    }

    public SecurityInvestigationContext withCompletedAt(Instant when) {
        return toBuilder().completedAt(when).build();
    }

    public SecurityInvestigationContext withVulnerability(Vulnerability value) {
        return toBuilder().vulnerability(value).build();
    }

    public SecurityInvestigationContext withThreat(ThreatIntelligenceResult value) {
        Builder b = toBuilder().threat(value);
        if (value != null && value.evidence() != null) {
            b.addEvidenceAll(value.evidence());
        }
        return b.build();
    }

    public SecurityInvestigationContext withAssets(AssetInvestigationResult value) {
        return toBuilder().assets(value).build();
    }

    public SecurityInvestigationContext withRisk(RiskAnalystResult value) {
        return toBuilder().risk(value).build();
    }

    public SecurityInvestigationContext withRemediation(RemediationAgentResult value) {
        return toBuilder().remediation(value).build();
    }

    public SecurityInvestigationContext withRecommendation(FinalRecommendation value) {
        return toBuilder().recommendation(value).build();
    }

    public SecurityInvestigationContext withExecution(AgentExecution execution) {
        return toBuilder().replaceExecution(execution).refreshAgentLists().build();
    }

    public SecurityInvestigationContext withError(AgentError error) {
        return toBuilder().addError(error).build();
    }

    public SecurityInvestigationContext withDecision(DecisionRecord record) {
        return toBuilder().addDecision(record).build();
    }

    public SecurityInvestigationContext withTrace(ExecutionTraceEntry entry) {
        return toBuilder().addTrace(entry).build();
    }

    public SecurityInvestigationContext withAssetDetailsAttempted(boolean value) {
        return toBuilder().assetDetailsAttempted(value).build();
    }

    public SecurityInvestigationContext withEvidenceItem(EvidenceItem item) {
        return toBuilder().addEvidenceAll(List.of(item)).build();
    }

    public Optional<AgentExecution> executionOf(String agentName) {
        return executions.stream().filter(e -> Objects.equals(agentName, e.agentName())).reduce((a, b) -> b);
    }

    public boolean agentFailed(String agentName) {
        return executionOf(agentName).map(e -> e.status() == AgentStatus.FAILED).orElse(false);
    }

    public boolean isTerminal() {
        return status == InvestigationStatus.COMPLETED
                || status == InvestigationStatus.FAILED
                || status == InvestigationStatus.REVIEW_REQUIRED;
    }

    public UUID investigationId() { return investigationId; }
    public String cveId() { return cveId; }
    public UUID correlationId() { return correlationId; }
    public Instant startedAt() { return startedAt; }
    public Instant completedAt() { return completedAt; }
    public InvestigationStatus status() { return status; }
    public InvestigationState currentState() { return currentState; }
    public Confidence overallConfidence() { return overallConfidence; }
    public int iterationCount() { return iterationCount; }
    public Vulnerability vulnerability() { return vulnerability; }
    public ThreatIntelligenceResult threat() { return threat; }
    public AssetInvestigationResult assets() { return assets; }
    public RiskAnalystResult risk() { return risk; }
    public RemediationAgentResult remediation() { return remediation; }
    public List<AgentExecution> executions() { return executions; }
    public List<AgentError> errors() { return errors; }
    public List<EvidenceItem> evidence() { return evidence; }
    public List<DecisionRecord> decisionHistory() { return decisionHistory; }
    public List<ExecutionTraceEntry> executionTrace() { return executionTrace; }
    public List<String> completedAgents() { return completedAgents; }
    public List<String> pendingAgents() { return pendingAgents; }
    public boolean assetDetailsAttempted() { return assetDetailsAttempted; }
    public FinalRecommendation recommendation() { return recommendation; }

    public static final class Builder {
        private UUID investigationId;
        private String cveId;
        private UUID correlationId;
        private Instant startedAt;
        private Instant completedAt;
        private InvestigationStatus status = InvestigationStatus.PENDING;
        private InvestigationState currentState = InvestigationState.INITIALIZED;
        private Confidence overallConfidence;
        private int iterationCount;
        private Vulnerability vulnerability;
        private ThreatIntelligenceResult threat;
        private AssetInvestigationResult assets;
        private RiskAnalystResult risk;
        private RemediationAgentResult remediation;
        private List<AgentExecution> executions = new ArrayList<>();
        private List<AgentError> errors = new ArrayList<>();
        private List<EvidenceItem> evidence = new ArrayList<>();
        private List<DecisionRecord> decisionHistory = new ArrayList<>();
        private List<ExecutionTraceEntry> executionTrace = new ArrayList<>();
        private List<String> completedAgents = new ArrayList<>();
        private List<String> pendingAgents = new ArrayList<>(ALL_AGENTS);
        private boolean assetDetailsAttempted;
        private FinalRecommendation recommendation;

        public Builder investigationId(UUID investigationId) {
            this.investigationId = investigationId;
            return this;
        }

        public Builder cveId(String cveId) {
            this.cveId = cveId;
            return this;
        }

        public Builder correlationId(UUID correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder startedAt(Instant startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public Builder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public Builder status(InvestigationStatus status) {
            this.status = status;
            return this;
        }

        public Builder currentState(InvestigationState currentState) {
            this.currentState = currentState;
            return this;
        }

        public Builder overallConfidence(Confidence overallConfidence) {
            this.overallConfidence = overallConfidence;
            return this;
        }

        public Builder iterationCount(int iterationCount) {
            this.iterationCount = iterationCount;
            return this;
        }

        public Builder vulnerability(Vulnerability vulnerability) {
            this.vulnerability = vulnerability;
            return this;
        }

        public Builder threat(ThreatIntelligenceResult threat) {
            this.threat = threat;
            return this;
        }

        public Builder assets(AssetInvestigationResult assets) {
            this.assets = assets;
            return this;
        }

        public Builder risk(RiskAnalystResult risk) {
            this.risk = risk;
            return this;
        }

        public Builder remediation(RemediationAgentResult remediation) {
            this.remediation = remediation;
            return this;
        }

        public Builder recommendation(FinalRecommendation recommendation) {
            this.recommendation = recommendation;
            return this;
        }

        public Builder assetDetailsAttempted(boolean assetDetailsAttempted) {
            this.assetDetailsAttempted = assetDetailsAttempted;
            return this;
        }

        public Builder replaceExecution(AgentExecution execution) {
            boolean replaced = false;
            List<AgentExecution> next = new ArrayList<>();
            for (AgentExecution existing : executions) {
                if (!replaced && Objects.equals(existing.agentName(), execution.agentName())
                        && existing.status() == AgentStatus.RUNNING) {
                    next.add(execution);
                    replaced = true;
                } else {
                    next.add(existing);
                }
            }
            if (!replaced) {
                next.add(execution);
            }
            this.executions = next;
            return this;
        }

        public Builder addError(AgentError error) {
            this.errors.add(error);
            return this;
        }

        public Builder addEvidenceAll(List<EvidenceItem> items) {
            if (items != null) {
                this.evidence.addAll(items);
            }
            return this;
        }

        public Builder addDecision(DecisionRecord record) {
            this.decisionHistory.add(record);
            return this;
        }

        public Builder addTrace(ExecutionTraceEntry entry) {
            this.executionTrace.add(entry);
            return this;
        }

        public Builder replaceExecutionList(List<AgentExecution> executions) {
            this.executions = executions == null ? new ArrayList<>() : new ArrayList<>(executions);
            return this;
        }

        public Builder replaceErrorList(List<AgentError> errors) {
            this.errors = errors == null ? new ArrayList<>() : new ArrayList<>(errors);
            return this;
        }

        public Builder replaceEvidenceList(List<EvidenceItem> evidence) {
            this.evidence = evidence == null ? new ArrayList<>() : new ArrayList<>(evidence);
            return this;
        }

        public Builder replaceDecisionHistory(List<DecisionRecord> history) {
            this.decisionHistory = history == null ? new ArrayList<>() : new ArrayList<>(history);
            return this;
        }

        public Builder replaceExecutionTrace(List<ExecutionTraceEntry> trace) {
            this.executionTrace = trace == null ? new ArrayList<>() : new ArrayList<>(trace);
            return this;
        }

        public Builder replaceCompletedAgents(List<String> agents) {
            this.completedAgents = agents == null ? new ArrayList<>() : new ArrayList<>(agents);
            return this;
        }

        public Builder replacePendingAgents(List<String> agents) {
            this.pendingAgents = agents == null ? new ArrayList<>() : new ArrayList<>(agents);
            return this;
        }

        public Builder refreshAgentLists() {
            List<String> done = new ArrayList<>();
            for (String name : ALL_AGENTS) {
                boolean finished = executions.stream().anyMatch(e -> name.equals(e.agentName())
                        && (e.status() == AgentStatus.COMPLETED || e.status() == AgentStatus.SKIPPED || e.status() == AgentStatus.FAILED));
                if (finished) {
                    done.add(name);
                }
            }
            this.completedAgents = done;
            List<String> pending = new ArrayList<>();
            for (String name : ALL_AGENTS) {
                if (!done.contains(name)) {
                    pending.add(name);
                }
            }
            this.pendingAgents = pending;
            return this;
        }

        public SecurityInvestigationContext build() {
            return new SecurityInvestigationContext(this);
        }
    }
}
