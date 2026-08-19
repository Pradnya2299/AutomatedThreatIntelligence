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
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
