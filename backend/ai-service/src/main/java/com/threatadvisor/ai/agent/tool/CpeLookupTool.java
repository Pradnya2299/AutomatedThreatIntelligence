package com.threatadvisor.ai.agent.tool;

import com.threatadvisor.ai.domain.VulnerabilityCpe;

import java.util.List;
import java.util.UUID;

public interface CpeLookupTool {
    List<VulnerabilityCpe> lookupCpes(UUID vulnerabilityId);
}
