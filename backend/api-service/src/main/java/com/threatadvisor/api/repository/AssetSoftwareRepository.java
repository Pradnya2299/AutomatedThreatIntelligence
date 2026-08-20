package com.threatadvisor.api.repository;

import com.threatadvisor.api.domain.AssetSoftware;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssetSoftwareRepository extends JpaRepository<AssetSoftware, UUID> {
    List<AssetSoftware> findByAssetIdAndInstallationStatus(UUID assetId, String status);
}
