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
@Table(name = "findings")
public class Finding {
    @Id
    private UUID id;
    @Column(name = "asset_id", nullable = false)
    private UUID assetId;
    @Column(name = "vulnerability_id", nullable = false)
    private UUID vulnerabilityId;
    @Column(nullable = false, length = 32)
    private String status;
    @Column(name = "match_type", length = 32)
    private String matchType;
    @Column(name = "match_confidence", length = 16)
    private String matchConfidence;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "match_explanation", nullable = false, columnDefinition = "jsonb")
    private String matchExplanation;
    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    public UUID getId() { return id; }
    public UUID getAssetId() { return assetId; }
    public UUID getVulnerabilityId() { return vulnerabilityId; }
    public String getStatus() { return status; }
    public String getMatchType() { return matchType; }
    public String getMatchConfidence() { return matchConfidence; }
    public String getMatchExplanation() { return matchExplanation; }
    public Instant getDetectedAt() { return detectedAt; }
}
