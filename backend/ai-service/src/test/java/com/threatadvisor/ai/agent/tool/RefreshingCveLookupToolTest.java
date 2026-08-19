package com.threatadvisor.ai.agent.tool;

import com.threatadvisor.ai.domain.Vulnerability;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshingCveLookupToolTest {

    @Mock DatabaseCveLookupTool database;
    @Mock IngestionNvdGateway nvd;

    @Test
    void refreshesSeedRowsFromNvd() {
        Vulnerability seed = new Vulnerability();
        seed.setId(UUID.randomUUID());
        seed.setCveId("CVE-2021-44228");
        seed.setIntelligenceSource("SEED");
        Vulnerability nvdRow = new Vulnerability();
        nvdRow.setId(seed.getId());
        nvdRow.setCveId("CVE-2021-44228");
        nvdRow.setIntelligenceSource("NVD");
        when(database.findByCveId("CVE-2021-44228")).thenReturn(Optional.of(seed), Optional.of(nvdRow));
        when(nvd.refresh("CVE-2021-44228")).thenReturn(
                new IngestionNvdGateway.NvdLookupBody("CVE-2021-44228", "UPDATED", "NVD", seed.getId(), "ok"));
        RefreshingCveLookupTool tool = new RefreshingCveLookupTool(database, nvd);
        assertEquals("NVD", tool.findByCveId("CVE-2021-44228").orElseThrow().getIntelligenceSource());
    }
}
