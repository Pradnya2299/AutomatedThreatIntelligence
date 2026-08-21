package com.threatadvisor.ai.agent.tool;

import com.threatadvisor.ai.agent.dto.InstalledSoftware;

import java.util.List;
import java.util.UUID;

public interface AssetInventoryTool {
    List<InstalledSoftware> listInstalledSoftware(UUID assetId);
}
