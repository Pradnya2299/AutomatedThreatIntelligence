package com.threatadvisor.ai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "security_investigations")
public class SecurityInvestigation {

    @Id
    private UUID id;
    @Column(name = "cve_id", nullable = false, length = 32)
    private String cveId;
    @Column(nullable = false, length = 32)
    private String status;
    @Column(name = "correlation_id")
    private UUID correlationId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "threat_result", nullable = false, columnDefinition = "jsonb")
    private String threatResult;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "asset_result", nullable = false, columnDefinition = "jsonb")
    private String assetResult;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "risk_result", nullable = false, columnDefinition = "jsonb")
    private String riskResult;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "remediation_result", nullable = false, columnDefinition = "jsonb")
    private String remediationResult;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String executions;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String errors;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String evidence;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String recommendation;
    @Column(name = "current_state", nullable = false, length = 32)
    private String currentState;
    @Column(name = "overall_confidence", length = 16)
    private String overallConfidence;
    @Column(name = "iteration_count", nullable = false)
    private int iterationCount;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "decision_history", nullable = false, columnDefinition = "jsonb")
    private String decisionHistory;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "execution_trace", nullable = false, columnDefinition = "jsonb")
    private String executionTrace;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "completed_agents", nullable = false, columnDefinition = "jsonb")
    private String completedAgents;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pending_agents", nullable = false, columnDefinition = "jsonb")
    private String pendingAgents;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "completed_at")
    private Instant completedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCveId() { return cveId; }
    public void setCveId(String cveId) { this.cveId = cveId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public UUID getCorrelationId() { return correlationId; }
    public void setCorrelationId(UUID correlationId) { this.correlationId = correlationId; }
    public String getThreatResult() { return threatResult; }
    public void setThreatResult(String threatResult) { this.threatResult = threatResult; }
    public String getAssetResult() { return assetResult; }
    public void setAssetResult(String assetResult) { this.assetResult = assetResult; }
    public String getRiskResult() { return riskResult; }
    public void setRiskResult(String riskResult) { this.riskResult = riskResult; }
    public String getRemediationResult() { return remediationResult; }
    public void setRemediationResult(String remediationResult) { this.remediationResult = remediationResult; }
    public String getExecutions() { return executions; }
    public void setExecutions(String executions) { this.executions = executions; }
    public String getErrors() { return errors; }
    public void setErrors(String errors) { this.errors = errors; }
    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    public String getCurrentState() { return currentState; }
    public void setCurrentState(String currentState) { this.currentState = currentState; }
    public String getOverallConfidence() { return overallConfidence; }
    public void setOverallConfidence(String overallConfidence) { this.overallConfidence = overallConfidence; }
    public int getIterationCount() { return iterationCount; }
    public void setIterationCount(int iterationCount) { this.iterationCount = iterationCount; }
    public String getDecisionHistory() { return decisionHistory; }
    public void setDecisionHistory(String decisionHistory) { this.decisionHistory = decisionHistory; }
    public String getExecutionTrace() { return executionTrace; }
    public void setExecutionTrace(String executionTrace) { this.executionTrace = executionTrace; }
    public String getCompletedAgents() { return completedAgents; }
    public void setCompletedAgents(String completedAgents) { this.completedAgents = completedAgents; }
    public String getPendingAgents() { return pendingAgents; }
    public void setPendingAgents(String pendingAgents) { this.pendingAgents = pendingAgents; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
