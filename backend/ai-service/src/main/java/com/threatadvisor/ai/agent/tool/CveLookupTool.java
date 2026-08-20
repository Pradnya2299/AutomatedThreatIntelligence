package com.threatadvisor.ai.agent.tool;

import com.threatadvisor.ai.domain.Vulnerability;
import com.threatadvisor.ai.domain.VulnerabilityCpe;

import java.util.List;
import java.util.Optional;

public interface CveLookupTool {
    Optional<Vulnerability> findByCveId(String cveId);

    List<VulnerabilityCpe> findCpes(java.util.UUID vulnerabilityId);
}
