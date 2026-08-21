package com.threatadvisor.ai.agent.tool;

import com.threatadvisor.ai.agent.dto.AssetDetailsSnapshot;

import java.util.List;
import java.util.UUID;

public interface AssetDetailsTool {
    List<AssetDetailsSnapshot> loadDetails(List<UUID> assetIds);
}
