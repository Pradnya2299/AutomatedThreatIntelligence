package com.threatadvisor.ai.agent.tool;

import com.threatadvisor.ai.agent.dto.AffectedAssetMatch;

import java.util.List;
import java.util.UUID;

public interface AffectedAssetLookupTool {
    List<AffectedAssetMatch> lookup(UUID vulnerabilityId);
}
