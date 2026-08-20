package com.threatadvisor.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assets")
public class Asset {
    @Id
    private UUID id;
    @Column(nullable = false)
    private String hostname;
    @Column(name = "operating_system", length = 128)
    private String operatingSystem;
    @Column(name = "os_version", length = 64)
    private String osVersion;
    @Column(nullable = false, length = 32)
    private String environment;
    @Column(name = "business_criticality", nullable = false, length = 32)
    private String businessCriticality;
    @Column(name = "internet_exposure", nullable = false)
    private boolean internetExposure;
    @Column(nullable = false, length = 32)
    private String status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public String getHostname() { return hostname; }
    public String getOperatingSystem() { return operatingSystem; }
    public String getOsVersion() { return osVersion; }
    public String getEnvironment() { return environment; }
    public String getBusinessCriticality() { return businessCriticality; }
    public boolean isInternetExposure() { return internetExposure; }
    public String getStatus() { return status; }
}
