package com.threatadvisor.ai.domain;

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
    public void setId(UUID id) { this.id = id; }
    public UUID getAssetId() { return assetId; }
    public void setAssetId(UUID assetId) { this.assetId = assetId; }
    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }
    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getInstallationStatus() { return installationStatus; }
    public void setInstallationStatus(String installationStatus) { this.installationStatus = installationStatus; }
}
