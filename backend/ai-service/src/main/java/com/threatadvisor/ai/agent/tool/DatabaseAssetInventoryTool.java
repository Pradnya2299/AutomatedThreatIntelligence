package com.threatadvisor.ai.agent.tool;

import com.threatadvisor.ai.agent.dto.AssetDetailsSnapshot;
import com.threatadvisor.ai.agent.dto.InstalledSoftware;
import com.threatadvisor.ai.domain.Asset;
import com.threatadvisor.ai.repository.AssetRepository;
import com.threatadvisor.ai.repository.AssetSoftwareRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class DatabaseAssetInventoryTool implements AssetInventoryTool, AssetDetailsTool {

    private final AssetRepository assets;
    private final AssetSoftwareRepository software;

    public DatabaseAssetInventoryTool(AssetRepository assets, AssetSoftwareRepository software) {
        this.assets = assets;
        this.software = software;
    }

    @Override
    public List<InstalledSoftware> listInstalledSoftware(UUID assetId) {
        return software.findByAssetIdAndInstallationStatus(assetId, "INSTALLED").stream()
                .map(row -> new InstalledSoftware(row.getVendor(), row.getProduct(), row.getVersion()))
                .toList();
    }

    @Override
    public List<AssetDetailsSnapshot> loadDetails(List<UUID> assetIds) {
        List<AssetDetailsSnapshot> snapshots = new ArrayList<>();
        if (assetIds == null) {
            return List.of();
        }
        for (UUID assetId : assetIds) {
            Asset asset = assets.findById(assetId).orElse(null);
            List<InstalledSoftware> installed = listInstalledSoftware(assetId);
            snapshots.add(new AssetDetailsSnapshot(
                    assetId,
                    asset == null ? null : asset.getHostname(),
                    installed));
        }
        return List.copyOf(snapshots);
    }
}
