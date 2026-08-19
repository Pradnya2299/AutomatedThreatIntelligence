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

    private final UUID investigationId;
    private final String cveId;
    private final UUID correlationId;
    private final Instant startedAt;
    private final Instant completedAt;
    private final InvestigationStatus status;
    private final Vulnerability vulnerability;
    private final ThreatIntelligenceResult threat;
    private final AssetInvestigationResult assets;
    private final RiskAnalystResult risk;
    private final RemediationAgentResult remediation;
    private final List<AgentExecution> executions;
    private final List<AgentError> errors;
    private final List<EvidenceItem> evidence;
    private final FinalRecommendation recommendation;

    private SecurityInvestigationContext(Builder builder) {
        this.investigationId = builder.investigationId;
        this.cveId = builder.cveId;
        this.correlationId = builder.correlationId;
        this.startedAt = builder.startedAt;
        this.completedAt = builder.completedAt;
        this.status = builder.status;
        this.vulnerability = builder.vulnerability;
        this.threat = builder.threat;
        this.assets = builder.assets;
        this.risk = builder.risk;
        this.remediation = builder.remediation;
        this.executions = List.copyOf(builder.executions);
        this.errors = List.copyOf(builder.errors);
        this.evidence = List.copyOf(builder.evidence);
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
        b.vulnerability = vulnerability;
        b.threat = threat;
        b.assets = assets;
        b.risk = risk;
        b.remediation = remediation;
        b.executions = new ArrayList<>(executions);
        b.errors = new ArrayList<>(errors);
        b.evidence = new ArrayList<>(evidence);
        b.recommendation = recommendation;
        return b;
    }

    public SecurityInvestigationContext withStatus(InvestigationStatus next) {
        return toBuilder().status(next).build();
    }

    public SecurityInvestigationContext withCompletedAt(Instant when) {
        return toBuilder().completedAt(when).build();
    }

    public SecurityInvestigationContext withVulnerability(Vulnerability value) {
        return toBuilder().vulnerability(value).build();
    }

    public SecurityInvestigationContext withThreat(ThreatIntelligenceResult value) {
        return toBuilder().threat(value).addEvidenceAll(value == null ? List.of() : value.evidence()).build();
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
        return toBuilder().replaceExecution(execution).build();
    }

    public SecurityInvestigationContext withError(AgentError error) {
        return toBuilder().addError(error).build();
    }

    public Optional<AgentExecution> executionOf(String agentName) {
        return executions.stream().filter(e -> Objects.equals(agentName, e.agentName())).reduce((a, b) -> b);
    }

    public UUID investigationId() { return investigationId; }
    public String cveId() { return cveId; }
    public UUID correlationId() { return correlationId; }
    public Instant startedAt() { return startedAt; }
    public Instant completedAt() { return completedAt; }
    public InvestigationStatus status() { return status; }
    public Vulnerability vulnerability() { return vulnerability; }
    public ThreatIntelligenceResult threat() { return threat; }
    public AssetInvestigationResult assets() { return assets; }
    public RiskAnalystResult risk() { return risk; }
    public RemediationAgentResult remediation() { return remediation; }
    public List<AgentExecution> executions() { return executions; }
    public List<AgentError> errors() { return errors; }
    public List<EvidenceItem> evidence() { return evidence; }
    public FinalRecommendation recommendation() { return recommendation; }

    public static final class Builder {
        private UUID investigationId;
        private String cveId;
        private UUID correlationId;
        private Instant startedAt;
        private Instant completedAt;
        private InvestigationStatus status = InvestigationStatus.PENDING;
        private Vulnerability vulnerability;
        private ThreatIntelligenceResult threat;
        private AssetInvestigationResult assets;
        private RiskAnalystResult risk;
        private RemediationAgentResult remediation;
        private List<AgentExecution> executions = new ArrayList<>();
        private List<AgentError> errors = new ArrayList<>();
        private List<EvidenceItem> evidence = new ArrayList<>();
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

        public SecurityInvestigationContext build() {
            return new SecurityInvestigationContext(this);
        }
    }
}
