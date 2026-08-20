package com.threatadvisor.ai.agent.dto;

import java.util.List;
import java.util.UUID;

public record AssetDetailsSnapshot(
        UUID assetId,
        String hostname,
        List<InstalledSoftware> software
) {
}
