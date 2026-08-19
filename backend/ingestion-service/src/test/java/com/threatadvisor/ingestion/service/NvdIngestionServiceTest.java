package com.threatadvisor.ingestion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threatadvisor.ingestion.config.NvdProperties;
import com.threatadvisor.ingestion.domain.Vulnerability;
import com.threatadvisor.ingestion.dto.NvdLookupResponse;
import com.threatadvisor.ingestion.exception.IngestionException;
import com.threatadvisor.ingestion.kafka.CveEventPublisher;
import com.threatadvisor.ingestion.nvd.NvdApiClient;
import com.threatadvisor.ingestion.nvd.NvdApiException;
import com.threatadvisor.ingestion.repository.NvdSyncStateRepository;
import com.threatadvisor.ingestion.repository.VulnerabilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NvdIngestionServiceTest {

    @Mock NvdApiClient nvdApiClient;
    @Mock VulnerabilityUpsertService upsertService;
    @Mock VulnerabilityRepository vulnerabilities;
    @Mock NvdSyncStateRepository syncState;
    @Mock CveIngestionService rawIngestion;
    @Mock CveEventPublisher publisher;

    private NvdIngestionService service;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        NvdProperties properties = new NvdProperties();
        properties.setLookupEnabled(true);
        properties.setCacheMaxAgeHours(24);
        service = new NvdIngestionService(
                nvdApiClient, properties, upsertService, vulnerabilities, syncState, rawIngestion, publisher, mapper);
    }

    @Test
    void usesCachedWhenNvdUnavailable() throws Exception {
        Vulnerability cached = cached("CVE-2021-44228");
        when(vulnerabilities.findByCveId("CVE-2021-44228")).thenReturn(Optional.of(cached));
        when(nvdApiClient.getCve("CVE-2021-44228"))
                .thenThrow(new NvdApiException("NVD_TIMEOUT", HttpStatus.GATEWAY_TIMEOUT, "timeout"));
        when(vulnerabilities.save(any())).thenAnswer(inv -> inv.getArgument(0));
        NvdLookupResponse response = service.lookupCve("CVE-2021-44228");
        assertEquals("NVD_CACHE", response.intelligenceSource());
        verify(upsertService, never()).upsert(any(), any(), any());
    }

    @Test
    void unavailableWithoutCacheIsFailure() {
        when(vulnerabilities.findByCveId("CVE-2099-0000")).thenReturn(Optional.empty());
        when(nvdApiClient.getCve("CVE-2099-0000"))
                .thenThrow(new NvdApiException("NVD_UNAVAILABLE", HttpStatus.BAD_GATEWAY, "down"));
        IngestionException ex = assertThrows(IngestionException.class, () -> service.lookupCve("CVE-2099-0000"));
        assertEquals("NVD_UNAVAILABLE", ex.getCode());
    }

    @Test
    void duplicateCveUpdatesExisting() throws Exception {
        String body = new String(getClass().getResourceAsStream("/nvd/cve-2021-44228.json").readAllBytes());
        when(vulnerabilities.findByCveId("CVE-2021-44228")).thenReturn(Optional.empty());
        when(nvdApiClient.getCve("CVE-2021-44228")).thenReturn(Optional.of(mapper.readTree(body)));
        Vulnerability entity = cached("CVE-2021-44228");
        when(upsertService.upsert(any(), any(), any()))
                .thenReturn(new VulnerabilityUpsertService.Result(entity, VulnerabilityUpsertService.Outcome.UPDATED));
        NvdLookupResponse response = service.lookupCve("CVE-2021-44228");
        assertEquals("UPDATED", response.status());
        assertEquals("NVD", response.intelligenceSource());
    }

    private static Vulnerability cached(String cveId) {
        Vulnerability v = new Vulnerability();
        v.setId(UUID.randomUUID());
        v.setCveId(cveId);
        v.setSource("seed");
        v.setIntelligenceSource("SEED");
        v.setIngestedAt(Instant.parse("2020-01-01T00:00:00Z"));
        v.setUpdatedAt(Instant.parse("2020-01-01T00:00:00Z"));
        v.setMetadata("{}");
        return v;
    }
}
