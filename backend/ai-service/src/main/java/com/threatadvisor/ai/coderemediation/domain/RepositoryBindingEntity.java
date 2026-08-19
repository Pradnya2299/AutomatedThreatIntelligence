package com.threatadvisor.ai.coderemediation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "remediation_repository_bindings")
public class RepositoryBindingEntity {
    @Id
    private UUID id;
    @Column(name = "organization_id")
    private UUID organizationId;
    @Column(name = "asset_id")
    private UUID assetId;
    private String hostname;
    @Column(name = "application_name")
    private String applicationName;
    @Column(nullable = false, length = 32)
    private String provider;
    private String organization;
    @Column(nullable = false)
    private String repository;
    @Column(name = "repository_url", nullable = false, length = 1024)
    private String repositoryUrl;
    @Column(name = "default_branch", nullable = false)
    private String defaultBranch;
    private String technology;
    @Column(name = "build_system")
    private String buildSystem;
    @Column(nullable = false, length = 16)
    private String confidence;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getAssetId() { return assetId; }
    public void setAssetId(UUID assetId) { this.assetId = assetId; }
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }
    public String getRepository() { return repository; }
    public void setRepository(String repository) { this.repository = repository; }
    public String getRepositoryUrl() { return repositoryUrl; }
    public void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; }
    public String getDefaultBranch() { return defaultBranch; }
    public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; }
    public String getTechnology() { return technology; }
    public void setTechnology(String technology) { this.technology = technology; }
    public String getBuildSystem() { return buildSystem; }
    public void setBuildSystem(String buildSystem) { this.buildSystem = buildSystem; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
}
