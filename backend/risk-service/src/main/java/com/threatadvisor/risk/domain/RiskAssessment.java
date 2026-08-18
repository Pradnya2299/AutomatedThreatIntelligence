package com.threatadvisor.risk.domain;

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

    @Column(name = "formula_version", nullable = false, length = 32)
    private String formulaVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String reasons;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getFindingId() {
        return findingId;
    }

    public void setFindingId(UUID findingId) {
        this.findingId = findingId;
    }

    public BigDecimal getTechnicalRisk() {
        return technicalRisk;
    }

    public void setTechnicalRisk(BigDecimal technicalRisk) {
        this.technicalRisk = technicalRisk;
    }

    public BigDecimal getExploitabilityScore() {
        return exploitabilityScore;
    }

    public void setExploitabilityScore(BigDecimal exploitabilityScore) {
        this.exploitabilityScore = exploitabilityScore;
    }

    public BigDecimal getExposureScore() {
        return exposureScore;
    }

    public void setExposureScore(BigDecimal exposureScore) {
        this.exposureScore = exposureScore;
    }

    public BigDecimal getBusinessImpactScore() {
        return businessImpactScore;
    }

    public void setBusinessImpactScore(BigDecimal businessImpactScore) {
        this.businessImpactScore = businessImpactScore;
    }

    public BigDecimal getAssetCriticalityScore() {
        return assetCriticalityScore;
    }

    public void setAssetCriticalityScore(BigDecimal assetCriticalityScore) {
        this.assetCriticalityScore = assetCriticalityScore;
    }

    public BigDecimal getFinalRiskScore() {
        return finalRiskScore;
    }

    public void setFinalRiskScore(BigDecimal finalRiskScore) {
        this.finalRiskScore = finalRiskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getFormulaVersion() {
        return formulaVersion;
    }

    public void setFormulaVersion(String formulaVersion) {
        this.formulaVersion = formulaVersion;
    }

    public String getReasons() {
        return reasons;
    }

    public void setReasons(String reasons) {
        this.reasons = reasons;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(Instant calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}
