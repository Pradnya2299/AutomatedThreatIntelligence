package com.threatadvisor.ai.agent.tool;

import com.threatadvisor.ai.domain.Vulnerability;
import com.threatadvisor.ai.domain.VulnerabilityCpe;
import com.threatadvisor.ai.repository.VulnerabilityCpeRepository;
import com.threatadvisor.ai.repository.VulnerabilityRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DatabaseCveLookupTool implements CveLookupTool {

    private final VulnerabilityRepository vulnerabilities;
    private final VulnerabilityCpeRepository cpes;

    public DatabaseCveLookupTool(VulnerabilityRepository vulnerabilities, VulnerabilityCpeRepository cpes) {
        this.vulnerabilities = vulnerabilities;
        this.cpes = cpes;
    }

    @Override
    public Optional<Vulnerability> findByCveId(String cveId) {
        if (cveId == null || cveId.isBlank()) {
            return Optional.empty();
        }
        return vulnerabilities.findByCveIdIgnoreCase(cveId.trim());
    }

    @Override
    public List<VulnerabilityCpe> findCpes(UUID vulnerabilityId) {
        return cpes.findByVulnerabilityId(vulnerabilityId);
    }
}
