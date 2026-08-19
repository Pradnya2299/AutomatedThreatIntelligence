package com.threatadvisor.ai.coderemediation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "code_remediation_jobs")
public class CodeRemediationJobEntity {

    @Id
    private UUID id;
    @Column(name = "investigation_id", nullable = false)
    private UUID investigationId;
    @Column(name = "cve_id", nullable = false, length = 32)
    private String cveId;
    @Column(nullable = false, length = 32)
    private String status;
    @Column(name = "current_state", nullable = false, length = 32)
    private String currentState;
    @Column(name = "initiated_by", length = 128)
    private String initiatedBy;
    @Column(name = "correlation_id")
    private UUID correlationId;
    @Column(name = "overall_confidence", length = 16)
    private String overallConfidence;
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;
    @Column(name = "review_reason")
    private String reviewReason;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "completed_at")
    private Instant completedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getInvestigationId() { return investigationId; }
    public void setInvestigationId(UUID investigationId) { this.investigationId = investigationId; }
    public String getCveId() { return cveId; }
    public void setCveId(String cveId) { this.cveId = cveId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCurrentState() { return currentState; }
    public void setCurrentState(String currentState) { this.currentState = currentState; }
    public String getInitiatedBy() { return initiatedBy; }
    public void setInitiatedBy(String initiatedBy) { this.initiatedBy = initiatedBy; }
    public UUID getCorrelationId() { return correlationId; }
    public void setCorrelationId(UUID correlationId) { this.correlationId = correlationId; }
    public String getOverallConfidence() { return overallConfidence; }
    public void setOverallConfidence(String overallConfidence) { this.overallConfidence = overallConfidence; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
    public String getReviewReason() { return reviewReason; }
    public void setReviewReason(String reviewReason) { this.reviewReason = reviewReason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
