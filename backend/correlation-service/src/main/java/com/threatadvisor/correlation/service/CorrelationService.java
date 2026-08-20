package com.threatadvisor.correlation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.threatadvisor.correlation.config.CorrelationProperties;
import com.threatadvisor.correlation.domain.Asset;
import com.threatadvisor.correlation.domain.AssetSoftware;
import com.threatadvisor.correlation.domain.EventProcessingRecord;
import com.threatadvisor.correlation.domain.Finding;
import com.threatadvisor.correlation.domain.Vulnerability;
import com.threatadvisor.correlation.domain.VulnerabilityCpe;
import com.threatadvisor.correlation.exception.CorrelationException;
import com.threatadvisor.correlation.kafka.EventEnvelope;
import com.threatadvisor.correlation.kafka.FindingEventPublisher;
import com.threatadvisor.correlation.matching.AssetMatch;
import com.threatadvisor.correlation.matching.CorrelationEngine;
import com.threatadvisor.correlation.matching.CpeConstraint;
import com.threatadvisor.correlation.matching.IdentityNormalizer;
import com.threatadvisor.correlation.matching.InstalledSoftware;
import com.threatadvisor.correlation.matching.MatchConfidence;
import com.threatadvisor.correlation.repository.AssetRepository;
import com.threatadvisor.correlation.repository.AssetSoftwareRepository;
import com.threatadvisor.correlation.repository.EventProcessingRecordRepository;
import com.threatadvisor.correlation.repository.FindingRepository;
import com.threatadvisor.correlation.repository.VulnerabilityCpeRepository;
import com.threatadvisor.correlation.repository.VulnerabilityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class CorrelationService {

    public static final String CONSUMER = "correlation-service:cve.normalized";
    private static final Logger log = LoggerFactory.getLogger(CorrelationService.class);
    private static final CorrelationEngine ENGINE = new CorrelationEngine();

    private final ObjectMapper objectMapper;
    private final CorrelationProperties properties;
    private final EventProcessingRecordRepository processingRecords;
    private final VulnerabilityRepository vulnerabilities;
    private final VulnerabilityCpeRepository cpes;
    private final AssetSoftwareRepository assetSoftware;
    private final AssetRepository assets;
    private final FindingRepository findings;
    private final FindingEventPublisher publisher;

    public CorrelationService(
            ObjectMapper objectMapper,
            CorrelationProperties properties,
            EventProcessingRecordRepository processingRecords,
            VulnerabilityRepository vulnerabilities,
            VulnerabilityCpeRepository cpes,
            AssetSoftwareRepository assetSoftware,
            AssetRepository assets,
            FindingRepository findings,
            FindingEventPublisher publisher) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.processingRecords = processingRecords;
        this.vulnerabilities = vulnerabilities;
        this.cpes = cpes;
        this.assetSoftware = assetSoftware;
        this.assets = assets;
        this.findings = findings;
        this.publisher = publisher;
    }

    @Transactional
    public CorrelationOutcome handleNormalizedEvent(String message) {
        EventEnvelope envelope;
        try {
            envelope = objectMapper.readValue(message, EventEnvelope.class);
        } catch (JsonProcessingException ex) {
            log.error("operation=correlate.invalid-envelope", ex);
            throw new IllegalArgumentException("Invalid cve.normalized envelope");
        }
        if (!EventEnvelope.TYPE_NORMALIZED.equals(envelope.eventType()) || envelope.eventId() == null) {
            log.warn("operation=correlate.ignored eventType={}", envelope.eventType());
            return CorrelationOutcome.ignored();
        }
        MDC.put("eventId", envelope.eventId().toString());
        if (envelope.correlationId() != null) {
            MDC.put("correlationId", envelope.correlationId().toString());
        }
        if (processingRecords.existsByEventIdAndConsumer(envelope.eventId(), CONSUMER)) {
            log.info("operation=correlate.idempotent-skip eventId={}", envelope.eventId());
            return CorrelationOutcome.duplicate(envelope.eventId());
        }
        EventProcessingRecord marker = new EventProcessingRecord();
        marker.setId(UUID.randomUUID());
        marker.setEventId(envelope.eventId());
        marker.setEventType(envelope.eventType());
        marker.setConsumer(CONSUMER);
        marker.setStatus("COMPLETED");
        marker.setProcessedAt(Instant.now());
        marker.setMetadata("{}");
        try {
            processingRecords.saveAndFlush(marker);
        } catch (DataIntegrityViolationException ex) {
            log.info("operation=correlate.idempotent-race eventId={}", envelope.eventId());
            return CorrelationOutcome.duplicate(envelope.eventId());
        }

        Vulnerability vulnerability = loadVulnerability(envelope.payload());
        MDC.put("cveId", vulnerability.getCveId());
        CorrelationOutcome outcome = correlate(vulnerability, envelope.eventId(), envelope.correlationId());
        publishAfterCommit(outcome.events());
        return outcome;
    }

    @Transactional
    public CorrelationOutcome runForCve(String cveId, UUID correlationId) {
        Vulnerability vulnerability = vulnerabilities.findByCveId(cveId)
                .orElseThrow(() -> new CorrelationException(HttpStatus.NOT_FOUND, "CVE_NOT_FOUND", "Unknown CVE " + cveId));
        UUID eventId = UUID.randomUUID();
        EventEnvelope envelope = new EventEnvelope(
                eventId,
                EventEnvelope.TYPE_NORMALIZED,
                1,
                Instant.now(),
                "correlation-service",
                correlationId == null ? UUID.randomUUID() : correlationId,
                objectMapper.createObjectNode()
                        .put("vulnerabilityId", vulnerability.getId().toString())
                        .put("cveId", vulnerability.getCveId()));
        try {
            return handleNormalizedEvent(objectMapper.writeValueAsString(envelope));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private CorrelationOutcome correlate(Vulnerability vulnerability, UUID eventId, UUID correlationId) {
        List<VulnerabilityCpe> cpeRows = cpes.findByVulnerabilityId(vulnerability.getId());
        List<CpeConstraint> constraints = cpeRows.stream().map(this::toConstraint).toList();
        List<InstalledSoftware> candidates = loadCandidates(constraints);
        List<AssetMatch> matches = ENGINE.evaluate(vulnerability.getCveId(), constraints, candidates);
        int created = 0;
        int updated = 0;
        List<EventEnvelope> events = new ArrayList<>();
        for (AssetMatch match : matches) {
            if (!shouldPersist(match.confidence())) {
                log.info("operation=correlate.skip-confidence cveId={} assetId={} confidence={}",
                        vulnerability.getCveId(), match.assetId(), match.confidence());
                continue;
            }
            UpsertResult upsert = upsertFinding(vulnerability, match);
            if (upsert.created()) {
                created++;
            } else {
                updated++;
            }
            events.add(findingCreatedEvent(upsert.finding(), match, correlationId));
            MDC.put("findingId", upsert.finding().getId().toString());
            MDC.put("assetId", match.assetId().toString());
            if (upsert.created()) {
                log.info("Created finding for {} on asset {}", vulnerability.getCveId(), match.hostname());
            } else {
                log.info("Updated finding for {} on asset {}", vulnerability.getCveId(), match.hostname());
            }
        }
        log.info("operation=correlate.completed cveId={} candidates={} matches={} created={} updated={}",
                vulnerability.getCveId(), candidates.size(), matches.size(), created, updated);
        return new CorrelationOutcome(
                "COMPLETED",
                vulnerability.getCveId(),
                vulnerability.getId(),
                eventId,
                created,
                updated,
                matches.size(),
                events);
    }

    private List<InstalledSoftware> loadCandidates(List<CpeConstraint> constraints) {
        Map<UUID, InstalledSoftware> unique = new LinkedHashMap<>();
        for (CpeConstraint constraint : constraints) {
            String vendorKey = IdentityNormalizer.key(constraint.vendorOrParsed());
            String productKey = IdentityNormalizer.key(constraint.productOrParsed());
            if (vendorKey.isEmpty() || productKey.isEmpty()) {
                continue;
            }
            for (AssetSoftware row : assetSoftware.findInstalledByNormalizedVendorProduct(vendorKey, productKey)) {
                Asset asset = assets.findById(row.getAssetId()).orElse(null);
                if (asset == null) {
                    continue;
                }
                unique.putIfAbsent(row.getId(), new InstalledSoftware(
                        asset.getId(),
                        asset.getOrganizationId(),
                        asset.getHostname(),
                        row.getVendor(),
                        row.getProduct(),
                        row.getVersion(),
                        row.getCpe()));
            }
        }
        return new ArrayList<>(unique.values());
    }

    private boolean shouldPersist(MatchConfidence confidence) {
        return switch (confidence) {
            case HIGH -> true;
            case MEDIUM -> properties.isPersistMediumConfidence();
            case LOW -> properties.isPersistLowConfidence();
        };
    }

    private UpsertResult upsertFinding(Vulnerability vulnerability, AssetMatch match) {
        Instant now = Instant.now();
        String explanationJson = explanationJson(match);
        Optional<Finding> existing = findings.findByAssetIdAndVulnerabilityId(match.assetId(), vulnerability.getId());
        if (existing.isPresent()) {
            Finding finding = existing.get();
            finding.setMatchType(match.matchType().name());
            finding.setMatchConfidence(match.confidence().name());
            finding.setMatchExplanation(explanationJson);
            finding.setUpdatedAt(now);
            return new UpsertResult(findings.save(finding), false);
        }
        Finding finding = new Finding();
        finding.setId(UUID.randomUUID());
        finding.setOrganizationId(match.organizationId());
        finding.setAssetId(match.assetId());
        finding.setVulnerabilityId(vulnerability.getId());
        finding.setStatus("OPEN");
        finding.setMatchType(match.matchType().name());
        finding.setMatchConfidence(match.confidence().name());
        finding.setMatchExplanation(explanationJson);
        finding.setDetectedAt(now);
        finding.setCreatedAt(now);
        finding.setUpdatedAt(now);
        try {
            return new UpsertResult(findings.saveAndFlush(finding), true);
        } catch (DataIntegrityViolationException ex) {
            Finding raced = findings.findByAssetIdAndVulnerabilityId(match.assetId(), vulnerability.getId())
                    .orElseThrow(() -> ex);
            raced.setMatchType(match.matchType().name());
            raced.setMatchConfidence(match.confidence().name());
            raced.setMatchExplanation(explanationJson);
            raced.setUpdatedAt(now);
            return new UpsertResult(findings.save(raced), false);
        }
    }

    private String explanationJson(AssetMatch match) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("text", match.explanation());
        node.put("matchType", match.matchType().name());
        node.put("matchConfidence", match.confidence().name());
        node.put("vendor", match.vendor());
        node.put("product", match.product());
        node.put("installedVersion", match.installedVersion());
        if (match.matchedCpe() != null && match.matchedCpe().cpe() != null) {
            node.put("cpe", match.matchedCpe().cpe());
        }
        return node.toString();
    }

    private EventEnvelope findingCreatedEvent(Finding finding, AssetMatch match, UUID correlationId) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("findingId", finding.getId().toString());
        payload.put("vulnerabilityId", finding.getVulnerabilityId().toString());
        payload.put("assetId", finding.getAssetId().toString());
        payload.put("cveId", match.cveId());
        payload.put("matchType", match.matchType().name());
        payload.put("matchConfidence", match.confidence().name());
        payload.put("matchExplanation", match.explanation());
        return new EventEnvelope(
                UUID.randomUUID(),
                EventEnvelope.TYPE_FINDING_CREATED,
                1,
                Instant.now(),
                EventEnvelope.SOURCE_SERVICE,
                correlationId == null ? UUID.randomUUID() : correlationId,
                payload);
    }

    private void publishAfterCommit(List<EventEnvelope> events) {
        if (events.isEmpty()) {
            return;
        }
        Runnable publish = () -> {
            for (EventEnvelope event : events) {
                String key = event.payload().path("findingId").asText();
                publisher.publish(EventEnvelope.TYPE_FINDING_CREATED, key, event);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish.run();
                }
            });
        } else {
            publish.run();
        }
    }

    private Vulnerability loadVulnerability(JsonNode payload) {
        if (payload != null && payload.hasNonNull("vulnerabilityId")) {
            UUID id = UUID.fromString(payload.get("vulnerabilityId").asText());
            Optional<Vulnerability> byId = vulnerabilities.findById(id);
            if (byId.isPresent()) {
                return byId.get();
            }
        }
        if (payload != null && payload.hasNonNull("cveId")) {
            return vulnerabilities.findByCveId(payload.get("cveId").asText())
                    .orElseThrow(() -> new IllegalStateException("Vulnerability not found for cveId " + payload.get("cveId").asText()));
        }
        throw new IllegalStateException("cve.normalized payload missing vulnerabilityId/cveId");
    }

    private CpeConstraint toConstraint(VulnerabilityCpe row) {
        return new CpeConstraint(
                row.getCpe(),
                row.getVendor(),
                row.getProduct(),
                row.getVersionStartIncluding(),
                row.getVersionStartExcluding(),
                row.getVersionEndIncluding(),
                row.getVersionEndExcluding());
    }

    public record CorrelationOutcome(
            String status,
            String cveId,
            UUID vulnerabilityId,
            UUID eventId,
            int findingsCreated,
            int findingsUpdated,
            int matchesEvaluated,
            List<EventEnvelope> events
    ) {
        static CorrelationOutcome ignored() {
            return new CorrelationOutcome("IGNORED", null, null, null, 0, 0, 0, List.of());
        }

        static CorrelationOutcome duplicate(UUID eventId) {
            return new CorrelationOutcome("DUPLICATE", null, null, eventId, 0, 0, 0, List.of());
        }
    }

    private record UpsertResult(Finding finding, boolean created) {
    }
}
