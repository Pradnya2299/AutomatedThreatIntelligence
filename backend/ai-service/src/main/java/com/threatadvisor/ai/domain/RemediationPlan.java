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
@Table(name = "remediation_plans")
public class RemediationPlan {
    @Id
    private UUID id;
    @Column(name = "finding_id", nullable = false)
    private UUID findingId;
    @Column(name = "risk_assessment_id")
    private UUID riskAssessmentId;
    @Column(nullable = false, length = 32)
    private String status;
    @Column(length = 16)
    private String priority;
    @Column(columnDefinition = "text")
    private String summary;
    @Column(columnDefinition = "text")
    private String reason;
    @Column(name = "recommended_action", columnDefinition = "text")
    private String recommendedAction;
    @Column(name = "patch_version", length = 128)
    private String patchVersion;
    @Column(name = "temporary_mitigation", columnDefinition = "text")
    private String temporaryMitigation;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "verification_steps", nullable = false, columnDefinition = "jsonb")
    private String verificationSteps;
    @Column(name = "rollback_plan", columnDefinition = "text")
    private String rollbackPlan;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "retrieved_context", nullable = false, columnDefinition = "jsonb")
    private String retrievedContext;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "model_raw_output", columnDefinition = "jsonb")
    private String modelRawOutput;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "affected_components", nullable = false, columnDefinition = "jsonb")
    private String affectedComponents;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String prerequisites;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "implementation_steps", nullable = false, columnDefinition = "jsonb")
    private String implementationSteps;
    @Column(name = "downtime_expected")
    private Boolean downtimeExpected;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reference_urls", nullable = false, columnDefinition = "jsonb")
    private String referenceUrls;
    @Column(name = "model_name", length = 128)
    private String modelName;
    @Column(name = "prompt_version", length = 32)
    private String promptVersion;
    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getFindingId() { return findingId; }
    public void setFindingId(UUID findingId) { this.findingId = findingId; }
    public UUID getRiskAssessmentId() { return riskAssessmentId; }
    public void setRiskAssessmentId(UUID riskAssessmentId) { this.riskAssessmentId = riskAssessmentId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
    public String getPatchVersion() { return patchVersion; }
    public void setPatchVersion(String patchVersion) { this.patchVersion = patchVersion; }
    public String getTemporaryMitigation() { return temporaryMitigation; }
    public void setTemporaryMitigation(String temporaryMitigation) { this.temporaryMitigation = temporaryMitigation; }
    public String getVerificationSteps() { return verificationSteps; }
    public void setVerificationSteps(String verificationSteps) { this.verificationSteps = verificationSteps; }
    public String getRollbackPlan() { return rollbackPlan; }
    public void setRollbackPlan(String rollbackPlan) { this.rollbackPlan = rollbackPlan; }
    public String getRetrievedContext() { return retrievedContext; }
    public void setRetrievedContext(String retrievedContext) { this.retrievedContext = retrievedContext; }
    public String getModelRawOutput() { return modelRawOutput; }
    public void setModelRawOutput(String modelRawOutput) { this.modelRawOutput = modelRawOutput; }
    public String getAffectedComponents() { return affectedComponents; }
    public void setAffectedComponents(String affectedComponents) { this.affectedComponents = affectedComponents; }
    public String getPrerequisites() { return prerequisites; }
    public void setPrerequisites(String prerequisites) { this.prerequisites = prerequisites; }
    public String getImplementationSteps() { return implementationSteps; }
    public void setImplementationSteps(String implementationSteps) { this.implementationSteps = implementationSteps; }
    public Boolean getDowntimeExpected() { return downtimeExpected; }
    public void setDowntimeExpected(Boolean downtimeExpected) { this.downtimeExpected = downtimeExpected; }
    public String getReferenceUrls() { return referenceUrls; }
    public void setReferenceUrls(String referenceUrls) { this.referenceUrls = referenceUrls; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
