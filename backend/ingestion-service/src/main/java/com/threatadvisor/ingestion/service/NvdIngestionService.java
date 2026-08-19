package com.threatadvisor.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.threatadvisor.ingestion.config.NvdProperties;
import com.threatadvisor.ingestion.domain.NvdSyncState;
import com.threatadvisor.ingestion.domain.Vulnerability;
import com.threatadvisor.ingestion.dto.NvdLookupResponse;
import com.threatadvisor.ingestion.exception.IngestionException;
import com.threatadvisor.ingestion.kafka.CveEventPublisher;
import com.threatadvisor.ingestion.kafka.EventEnvelope;
import com.threatadvisor.ingestion.normalization.CveDocumentParser;
import com.threatadvisor.ingestion.normalization.NormalizedVulnerability;
import com.threatadvisor.ingestion.nvd.NvdApiClient;
import com.threatadvisor.ingestion.nvd.NvdApiException;
import com.threatadvisor.ingestion.nvd.NvdCvePage;
import com.threatadvisor.ingestion.nvd.NvdDocumentAdapter;
import com.threatadvisor.ingestion.repository.NvdSyncStateRepository;
import com.threatadvisor.ingestion.repository.VulnerabilityRepository;
import com.threatadvisor.ingestion.validation.CveIdValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class NvdIngestionService {

    private static final Logger log = LoggerFactory.getLogger(NvdIngestionService.class);
    private static final String STATE_ID = "default";

    private final NvdApiClient nvdApiClient;
    private final NvdProperties properties;
    private final VulnerabilityUpsertService upsertService;
    private final VulnerabilityRepository vulnerabilities;
    private final NvdSyncStateRepository syncState;
    private final CveIngestionService rawIngestion;
    private final CveEventPublisher publisher;
    private final ObjectMapper objectMapper;

    public NvdIngestionService(
            NvdApiClient nvdApiClient,
            NvdProperties properties,
            VulnerabilityUpsertService upsertService,
            VulnerabilityRepository vulnerabilities,
            NvdSyncStateRepository syncState,
            CveIngestionService rawIngestion,
            CveEventPublisher publisher,
            ObjectMapper objectMapper) {
        this.nvdApiClient = nvdApiClient;
        this.properties = properties;
        this.upsertService = upsertService;
        this.vulnerabilities = vulnerabilities;
        this.syncState = syncState;
        this.rawIngestion = rawIngestion;
        this.publisher = publisher;
        this.objectMapper = objectMapper;
    }

    public NvdLookupResponse lookupCve(String cveId) {
        String id = CveIdValidator.requireValid(cveId);
        Optional<Vulnerability> cached = vulnerabilities.findByCveId(id);
        if (!properties.isLookupEnabled()) {
            return cached.map(v -> new NvdLookupResponse(id, "CACHED", sourceOrSeed(v), v.getId(), "NVD lookup disabled"))
                    .orElseThrow(() -> new IngestionException(HttpStatus.NOT_FOUND, "CVE_NOT_FOUND", "No local CVE and NVD lookup is disabled"));
        }
        if (cached.isPresent() && !isStale(cached.get())) {
            return new NvdLookupResponse(id, "CACHED", sourceOrSeed(cached.get()), cached.get().getId(), "Local NVD row is current");
        }
        try {
            JsonNode envelope = nvdApiClient.getCve(id)
                    .orElseThrow(() -> new NvdApiException("NVD_NOT_FOUND", HttpStatus.NOT_FOUND, "CVE not found in NVD"));
            VulnerabilityUpsertService.Result result = ingestEnvelope(envelope, UUID.randomUUID());
            return new NvdLookupResponse(
                    id,
                    result.outcome().name(),
                    "NVD",
                    result.vulnerability().getId(),
                    "Retrieved from NIST NVD");
        } catch (NvdApiException ex) {
            if (cached.isPresent()) {
                log.warn("operation=nvd.lookup.cache-fallback cveId={} code={}", id, ex.getCode());
                Vulnerability v = cached.get();
                v.setIntelligenceSource("NVD_CACHE");
                vulnerabilities.save(v);
                return new NvdLookupResponse(id, "NVD_CACHE", "NVD_CACHE", v.getId(), "NVD unavailable; using cached CVE");
            }
            if ("NVD_NOT_FOUND".equals(ex.getCode())) {
                throw new IngestionException(HttpStatus.NOT_FOUND, "NVD_NOT_FOUND", "NVD has no record for " + id);
            }
            throw new IngestionException(HttpStatus.BAD_GATEWAY, ex.getCode(), "NVD is unavailable and no cached CVE exists");
        }
    }

    public NvdSyncState synchronizeIncremental() {
        NvdSyncState state = syncState.findById(STATE_ID).orElseGet(() -> {
            NvdSyncState created = new NvdSyncState();
            created.setId(STATE_ID);
            created.setLastStatus("NEVER_RUN");
            created.setUpdatedAt(Instant.now());
            return created;
        });
        Instant end = Instant.now();
        Instant start = state.getLastSuccessfulSyncAt() == null
                ? end.minus(Duration.ofHours(Math.max(1, properties.getSyncInitialLookbackHours())))
                : state.getLastSuccessfulSyncAt().minus(Duration.ofMinutes(5));
        state.setLastAttemptAt(end);
        state.setLastModStartAt(start);
        state.setLastModEndAt(end);
        int created = 0;
        int updated = 0;
        int unchanged = 0;
        int pages = 0;
        try {
            int startIndex = 0;
            NvdCvePage page;
            do {
                page = nvdApiClient.getModifiedSince(start, end, startIndex);
                pages++;
                for (JsonNode document : NvdDocumentAdapter.extractCveDocuments(page.raw())) {
                    VulnerabilityUpsertService.Result result = ingestDocument(document, page.raw(), UUID.randomUUID());
                    switch (result.outcome()) {
                        case CREATED -> created++;
                        case UPDATED -> updated++;
                        case UNCHANGED -> unchanged++;
                    }
                }
                startIndex = page.startIndex() + page.resultsPerPage();
            } while (page.hasMore());
            state.setCreatedCount(created);
            state.setUpdatedCount(updated);
            state.setUnchangedCount(unchanged);
            state.setPageCount(pages);
            state.setLastSuccessfulSyncAt(end);
            state.setLastStatus("SUCCESS");
            state.setLastError(null);
            log.info("operation=nvd.sync.success created={} updated={} unchanged={} pages={}", created, updated, unchanged, pages);
        } catch (RuntimeException ex) {
            state.setLastStatus("FAILED");
            state.setLastError(ex.getMessage() == null ? ex.getClass().getSimpleName() : truncate(ex.getMessage()));
            log.error("operation=nvd.sync.failed", ex);
        }
        state.setUpdatedAt(Instant.now());
        return syncState.save(state);
    }

    private VulnerabilityUpsertService.Result ingestEnvelope(JsonNode envelope, UUID correlationId) {
        var documents = NvdDocumentAdapter.extractCveDocuments(envelope);
        if (documents.isEmpty()) {
            throw new NvdApiException("NVD_NOT_FOUND", HttpStatus.NOT_FOUND, "NVD envelope contained no CVE documents");
        }
        VulnerabilityUpsertService.Result last = null;
        for (JsonNode document : documents) {
            last = ingestDocument(document, envelope, correlationId);
        }
        return last;
    }

    private VulnerabilityUpsertService.Result ingestDocument(JsonNode document, JsonNode envelope, UUID correlationId) {
        NormalizedVulnerability normalized = CveDocumentParser.parse(document, "nvd");
        String rawJson = envelope.toString();
        VulnerabilityUpsertService.Result result = upsertService.upsert(normalized, rawJson, "NVD");
        try {
            rawIngestion.ingest(document.toString().getBytes(StandardCharsets.UTF_8), "nvd", correlationId);
        } catch (RuntimeException ex) {
            log.warn("operation=nvd.raw-publish-skipped cveId={}", normalized.cveId());
        }
        if (result.outcome() != VulnerabilityUpsertService.Outcome.UNCHANGED) {
            publishNormalized(result.vulnerability(), correlationId);
        }
        log.info("operation=nvd.upsert cveId={} outcome={}", normalized.cveId(), result.outcome());
        return result;
    }

    private void publishNormalized(Vulnerability vulnerability, UUID correlationId) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("vulnerabilityId", vulnerability.getId().toString());
        payload.put("cveId", vulnerability.getCveId());
        payload.put("severity", vulnerability.getSeverity());
        payload.put("source", "nvd");
        EventEnvelope envelope = new EventEnvelope(
                UUID.nameUUIDFromBytes(("cve.normalized|nvd|" + vulnerability.getCveId() + "|" + vulnerability.getUpdatedAt())
                        .getBytes(StandardCharsets.UTF_8)),
                EventEnvelope.TYPE_NORMALIZED,
                1,
                Instant.now(),
                EventEnvelope.SOURCE_SERVICE,
                correlationId,
                payload
        );
        publisher.publish(EventEnvelope.TYPE_NORMALIZED, vulnerability.getCveId(), envelope);
    }

    private boolean isStale(Vulnerability vulnerability) {
        Instant reference = vulnerability.getIngestedAt() != null
                ? vulnerability.getIngestedAt()
                : vulnerability.getUpdatedAt();
        if (reference == null) {
            return true;
        }
        if (!"NVD".equalsIgnoreCase(vulnerability.getIntelligenceSource())
                && !"nvd".equalsIgnoreCase(vulnerability.getSource())) {
            return true;
        }
        return reference.isBefore(Instant.now().minus(Duration.ofHours(Math.max(1, properties.getCacheMaxAgeHours()))));
    }

    private static String sourceOrSeed(Vulnerability vulnerability) {
        if (vulnerability.getIntelligenceSource() != null) {
            return vulnerability.getIntelligenceSource();
        }
        return "nvd".equalsIgnoreCase(vulnerability.getSource()) ? "NVD" : "SEED";
    }

    private static String truncate(String message) {
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
