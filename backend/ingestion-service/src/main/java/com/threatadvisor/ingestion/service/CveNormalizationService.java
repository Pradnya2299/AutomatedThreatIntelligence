package com.threatadvisor.ingestion.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.threatadvisor.ingestion.domain.CveRawRecord;
import com.threatadvisor.ingestion.domain.EventProcessingRecord;
import com.threatadvisor.ingestion.domain.Vulnerability;
import com.threatadvisor.ingestion.domain.VulnerabilityCpe;
import com.threatadvisor.ingestion.kafka.CveEventPublisher;
import com.threatadvisor.ingestion.kafka.EventEnvelope;
import com.threatadvisor.ingestion.normalization.CpeExtractor;
import com.threatadvisor.ingestion.normalization.CveDocumentParser;
import com.threatadvisor.ingestion.normalization.NormalizedVulnerability;
import com.threatadvisor.ingestion.repository.CveRawRecordRepository;
import com.threatadvisor.ingestion.repository.EventProcessingRecordRepository;
import com.threatadvisor.ingestion.repository.VulnerabilityCpeRepository;
import com.threatadvisor.ingestion.repository.VulnerabilityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

@Service
public class CveNormalizationService {

    public static final String CONSUMER = "ingestion-service:cve.raw";
    private static final Logger log = LoggerFactory.getLogger(CveNormalizationService.class);

    private final ObjectMapper objectMapper;
    private final EventProcessingRecordRepository processingRecords;
    private final CveRawRecordRepository rawRecords;
    private final VulnerabilityRepository vulnerabilities;
    private final VulnerabilityCpeRepository cpes;
    private final CveEventPublisher publisher;

    public CveNormalizationService(
            ObjectMapper objectMapper,
            EventProcessingRecordRepository processingRecords,
            CveRawRecordRepository rawRecords,
            VulnerabilityRepository vulnerabilities,
            VulnerabilityCpeRepository cpes,
            CveEventPublisher publisher) {
        this.objectMapper = objectMapper;
        this.processingRecords = processingRecords;
        this.rawRecords = rawRecords;
        this.vulnerabilities = vulnerabilities;
        this.cpes = cpes;
        this.publisher = publisher;
    }

    @Transactional
    public void handleRawEvent(String message) {
        EventEnvelope envelope;
        try {
            envelope = objectMapper.readValue(message, EventEnvelope.class);
        } catch (JsonProcessingException ex) {
            log.error("operation=cve.normalize.invalid-envelope", ex);
            throw new IllegalArgumentException("Invalid cve.raw envelope");
        }
        if (!EventEnvelope.TYPE_RAW.equals(envelope.eventType()) || envelope.eventId() == null) {
            log.warn("operation=cve.normalize.ignored eventType={}", envelope.eventType());
            return;
        }
        MDC.put("eventId", envelope.eventId().toString());
        if (envelope.correlationId() != null) {
            MDC.put("correlationId", envelope.correlationId().toString());
        }

        if (processingRecords.existsByEventIdAndConsumer(envelope.eventId(), CONSUMER)) {
            log.info("operation=cve.normalize.idempotent-skip eventId={}", envelope.eventId());
            return;
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
            log.info("operation=cve.normalize.idempotent-race eventId={}", envelope.eventId());
            return;
        }

        CveRawRecord raw = rawRecords.findByEventId(envelope.eventId())
                .orElseThrow(() -> new IllegalStateException("Raw CVE row not found for event " + envelope.eventId()));
        MDC.put("cveId", raw.getExternalId());
        try {
            JsonNode payload = objectMapper.readTree(raw.getPayload());
            NormalizedVulnerability normalized = CveDocumentParser.parse(payload, raw.getSource());
            Vulnerability vulnerability = persistVulnerability(normalized, raw.getPayload());
            persistCpes(vulnerability.getId(), normalized);
            raw.setProcessingStatus("NORMALIZED");
            raw.setErrorMessage(null);
            raw.setUpdatedAt(Instant.now());
            rawRecords.save(raw);
            log.info("operation=cve.normalize.persisted cveId={} vulnerabilityId={} cpeCount={}",
                    normalized.cveId(), vulnerability.getId(), normalized.cpes().size());
            publishNormalizedAfterCommit(envelope, vulnerability);
        } catch (RuntimeException | JsonProcessingException ex) {
            raw.setProcessingStatus("FAILED");
            raw.setErrorMessage(truncate(ex.getMessage()));
            raw.setUpdatedAt(Instant.now());
            rawRecords.save(raw);
            log.error("operation=cve.normalize.failed cveId={}", raw.getExternalId(), ex);
            throw new IllegalStateException("Normalization failed for " + raw.getExternalId(), ex);
        }
    }

    private Vulnerability persistVulnerability(NormalizedVulnerability normalized, String rawJson) {
        Instant now = Instant.now();
        Vulnerability entity = vulnerabilities.findByCveId(normalized.cveId()).orElseGet(Vulnerability::new);
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
            entity.setCreatedAt(now);
        }
        entity.setCveId(normalized.cveId());
        entity.setDescription(normalized.description());
        entity.setPublishedAt(normalized.publishedAt());
        entity.setModifiedAt(normalized.modifiedAt());
        entity.setCvssScore(normalized.cvssScore());
        entity.setCvssVector(normalized.cvssVector());
        entity.setSeverity(normalized.severity());
        entity.setCwe(normalized.primaryCwe());
        entity.setAffectedVendors(toArray(normalized.vendors()));
        entity.setAffectedProducts(toArray(normalized.products()));
        entity.setExploitAvailable(normalized.exploitAvailable());
        entity.setActivelyExploited(normalized.activelyExploited());
        entity.setSource(normalized.source());
        entity.setSourceUrl(normalized.sourceUrl());
        entity.setRawSourcePayload(rawJson);
        entity.setMetadata(normalized.metadata().toString());
        entity.setCvssMetrics(normalized.cvssMetrics().toString());
        entity.setCwes(toArray(normalized.cwes()));
        entity.setReferenceUrls(toArray(normalized.referenceUrls()));
        entity.setUpdatedAt(now);
        return vulnerabilities.save(entity);
    }

    private void persistCpes(UUID vulnerabilityId, NormalizedVulnerability normalized) {
        cpes.deleteByVulnerabilityId(vulnerabilityId);
        for (CpeExtractor.ExtractedCpe extracted : normalized.cpes()) {
            VulnerabilityCpe row = new VulnerabilityCpe();
            row.setId(UUID.randomUUID());
            row.setVulnerabilityId(vulnerabilityId);
            row.setCpe(extracted.cpe());
            row.setVendor(extracted.vendor());
            row.setProduct(extracted.product());
            row.setVersionStartIncluding(extracted.versionStartIncluding());
            row.setVersionStartExcluding(extracted.versionStartExcluding());
            row.setVersionEndIncluding(extracted.versionEndIncluding());
            row.setVersionEndExcluding(extracted.versionEndExcluding());
            row.setOperatingSystem(extracted.operatingSystem());
            row.setArchitecture(extracted.architecture());
            cpes.save(row);
        }
    }

    private void publishNormalizedAfterCommit(EventEnvelope rawEnvelope, Vulnerability vulnerability) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("vulnerabilityId", vulnerability.getId().toString());
        payload.put("cveId", vulnerability.getCveId());
        payload.put("severity", vulnerability.getSeverity());
        payload.put("rawRecordEventId", rawEnvelope.eventId().toString());
        EventEnvelope envelope = new EventEnvelope(
                UUID.nameUUIDFromBytes(("cve.normalized|" + vulnerability.getCveId()).getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                EventEnvelope.TYPE_NORMALIZED,
                1,
                Instant.now(),
                EventEnvelope.SOURCE_SERVICE,
                rawEnvelope.correlationId(),
                payload
        );
        Runnable publish = () -> publisher.publish(EventEnvelope.TYPE_NORMALIZED, vulnerability.getCveId(), envelope);
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

    private static String[] toArray(java.util.List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.toArray(String[]::new);
    }

    private static String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
