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
@Table(name = "patch_executions")
public class PatchExecutionEntity {
    @Id
    private UUID id;
    @Column(name = "job_id", nullable = false)
    private UUID jobId;
    @Column(name = "patch_plan_id")
    private UUID patchPlanId;
    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;
    @Column(nullable = false, length = 32)
    private String status;
    @Column(name = "unified_diff")
    private String unifiedDiff;
    @Column(name = "files_changed", nullable = false)
    private int filesChanged;
    @Column(name = "lines_added", nullable = false)
    private int linesAdded;
    @Column(name = "lines_deleted", nullable = false)
    private int linesDeleted;
    @Column(name = "safety_status", length = 32)
    private String safetyStatus;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "safety_violations", nullable = false, columnDefinition = "jsonb")
    private String safetyViolations;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }
    public UUID getPatchPlanId() { return patchPlanId; }
    public void setPatchPlanId(UUID patchPlanId) { this.patchPlanId = patchPlanId; }
    public int getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(int attemptNumber) { this.attemptNumber = attemptNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getUnifiedDiff() { return unifiedDiff; }
    public void setUnifiedDiff(String unifiedDiff) { this.unifiedDiff = unifiedDiff; }
    public int getFilesChanged() { return filesChanged; }
    public void setFilesChanged(int filesChanged) { this.filesChanged = filesChanged; }
    public int getLinesAdded() { return linesAdded; }
    public void setLinesAdded(int linesAdded) { this.linesAdded = linesAdded; }
    public int getLinesDeleted() { return linesDeleted; }
    public void setLinesDeleted(int linesDeleted) { this.linesDeleted = linesDeleted; }
    public String getSafetyStatus() { return safetyStatus; }
    public void setSafetyStatus(String safetyStatus) { this.safetyStatus = safetyStatus; }
    public String getSafetyViolations() { return safetyViolations; }
    public void setSafetyViolations(String safetyViolations) { this.safetyViolations = safetyViolations; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
