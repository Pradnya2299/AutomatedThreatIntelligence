package com.threatadvisor.api.domain;

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
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "verification_steps", nullable = false, columnDefinition = "jsonb")
    private String verificationSteps;
    @Column(name = "rollback_plan", columnDefinition = "text")
    private String rollbackPlan;
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
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public UUID getFindingId() { return findingId; }
    public UUID getRiskAssessmentId() { return riskAssessmentId; }
    public String getStatus() { return status; }
    public String getPriority() { return priority; }
    public String getSummary() { return summary; }
    public String getReason() { return reason; }
    public String getRecommendedAction() { return recommendedAction; }
    public String getPatchVersion() { return patchVersion; }
    public String getVerificationSteps() { return verificationSteps; }
    public String getRollbackPlan() { return rollbackPlan; }
    public String getAffectedComponents() { return affectedComponents; }
    public String getPrerequisites() { return prerequisites; }
    public String getImplementationSteps() { return implementationSteps; }
    public Boolean getDowntimeExpected() { return downtimeExpected; }
    public String getReferenceUrls() { return referenceUrls; }
    public String getModelName() { return modelName; }
    public Instant getCreatedAt() { return createdAt; }
}
