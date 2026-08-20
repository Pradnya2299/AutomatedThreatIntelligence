package com.threatadvisor.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "asset_software")
public class AssetSoftware {
    @Id
    private UUID id;
    @Column(name = "asset_id", nullable = false)
    private UUID assetId;
    @Column(nullable = false)
    private String vendor;
    @Column(nullable = false)
    private String product;
    @Column(length = 128)
    private String version;
    @Column(name = "installation_status", nullable = false, length = 32)
    private String installationStatus;

    public UUID getId() { return id; }
    public UUID getAssetId() { return assetId; }
    public String getVendor() { return vendor; }
    public String getProduct() { return product; }
    public String getVersion() { return version; }
    public String getInstallationStatus() { return installationStatus; }
}
