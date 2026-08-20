package com.threatadvisor.ai.repository;

import com.threatadvisor.ai.domain.AssetSoftware;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssetSoftwareRepository extends JpaRepository<AssetSoftware, UUID> {
    List<AssetSoftware> findByAssetIdAndInstallationStatus(UUID assetId, String installationStatus);
}
