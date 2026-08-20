package com.threatadvisor.ai.agent.tool;

import com.threatadvisor.ai.domain.Vulnerability;
import com.threatadvisor.ai.domain.VulnerabilityCpe;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Primary
@Component
public class RefreshingCveLookupTool implements CveLookupTool, CpeLookupTool {

    private final DatabaseCveLookupTool database;
    private final IngestionNvdGateway nvd;

    public RefreshingCveLookupTool(DatabaseCveLookupTool database, IngestionNvdGateway nvd) {
        this.database = database;
        this.nvd = nvd;
    }

    @Override
    public Optional<Vulnerability> findByCveId(String cveId) {
        Optional<Vulnerability> local = database.findByCveId(cveId);
        boolean stale = local.isEmpty()
                || local.get().getIntelligenceSource() == null
                || "SEED".equalsIgnoreCase(local.get().getIntelligenceSource());
        if (stale) {
            IngestionNvdGateway.NvdLookupBody refreshed = nvd.refresh(cveId);
            Optional<Vulnerability> after = database.findByCveId(cveId);
            if (after.isPresent()) {
                Vulnerability v = after.get();
                if (refreshed != null && refreshed.intelligenceSource() != null) {
                    v.setIntelligenceSource(refreshed.intelligenceSource());
                }
                return Optional.of(v);
            }
        }
        return local;
    }

    @Override
    public List<VulnerabilityCpe> findCpes(UUID vulnerabilityId) {
        return database.findCpes(vulnerabilityId);
    }

    @Override
    public List<VulnerabilityCpe> lookupCpes(UUID vulnerabilityId) {
        return database.lookupCpes(vulnerabilityId);
    }
}
