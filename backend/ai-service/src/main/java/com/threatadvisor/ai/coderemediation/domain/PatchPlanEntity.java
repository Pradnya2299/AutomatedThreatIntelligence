package com.threatadvisor.ai.coderemediation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "patch_plans")
public class PatchPlanEntity {
    @Id
    private UUID id;
    @Column(name = "job_id", nullable = false)
    private UUID jobId;
    @Column(name = "strategy_type", nullable = false, length = 64)
    private String strategyType;
    private String rationale;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "affected_files", nullable = false, columnDefinition = "jsonb")
    private String affectedFiles;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "expected_changes", nullable = false, columnDefinition = "jsonb")
    private String expectedChanges;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String prerequisites;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String risks;
    @Column(name = "rollback_plan")
    private String rollbackPlan;
    private String confidence;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }
    public String getStrategyType() { return strategyType; }
    public void setStrategyType(String strategyType) { this.strategyType = strategyType; }
    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }
    public String getAffectedFiles() { return affectedFiles; }
    public void setAffectedFiles(String affectedFiles) { this.affectedFiles = affectedFiles; }
    public String getExpectedChanges() { return expectedChanges; }
    public void setExpectedChanges(String expectedChanges) { this.expectedChanges = expectedChanges; }
    public String getPrerequisites() { return prerequisites; }
    public void setPrerequisites(String prerequisites) { this.prerequisites = prerequisites; }
    public String getRisks() { return risks; }
    public void setRisks(String risks) { this.risks = risks; }
    public String getRollbackPlan() { return rollbackPlan; }
    public void setRollbackPlan(String rollbackPlan) { this.rollbackPlan = rollbackPlan; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
