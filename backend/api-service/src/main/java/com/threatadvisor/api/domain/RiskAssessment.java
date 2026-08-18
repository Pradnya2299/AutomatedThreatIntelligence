package com.threatadvisor.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "risk_assessments")
public class RiskAssessment {
    @Id
    private UUID id;
    @Column(name = "finding_id", nullable = false, unique = true)
    private UUID findingId;
    @Column(name = "technical_risk", nullable = false, precision = 5, scale = 2)
    private BigDecimal technicalRisk;
    @Column(name = "exploitability_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal exploitabilityScore;
    @Column(name = "exposure_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal exposureScore;
    @Column(name = "business_impact_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal businessImpactScore;
    @Column(name = "asset_criticality_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal assetCriticalityScore;
    @Column(name = "final_risk_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal finalRiskScore;
    @Column(name = "risk_level", nullable = false, length = 16)
    private String riskLevel;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String reasons;
    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    public UUID getId() { return id; }
    public UUID getFindingId() { return findingId; }
    public BigDecimal getTechnicalRisk() { return technicalRisk; }
    public BigDecimal getExploitabilityScore() { return exploitabilityScore; }
    public BigDecimal getExposureScore() { return exposureScore; }
    public BigDecimal getBusinessImpactScore() { return businessImpactScore; }
    public BigDecimal getAssetCriticalityScore() { return assetCriticalityScore; }
    public BigDecimal getFinalRiskScore() { return finalRiskScore; }
    public String getRiskLevel() { return riskLevel; }
    public String getReasons() { return reasons; }
    public Instant getCalculatedAt() { return calculatedAt; }
}
