package com.threatadvisor.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.threatadvisor.ingestion.domain.CveRawRecord;
import com.threatadvisor.ingestion.dto.IngestionResponse;
import com.threatadvisor.ingestion.exception.IngestionException;
import com.threatadvisor.ingestion.kafka.CveEventPublisher;
import com.threatadvisor.ingestion.kafka.EventEnvelope;
import com.threatadvisor.ingestion.normalization.CveDocumentParser;
import com.threatadvisor.ingestion.repository.CveRawRecordRepository;
import com.threatadvisor.ingestion.validation.CveIdValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class CveIngestionService {

    public static final int MAX_PAYLOAD_BYTES = 512 * 1024;
    private static final Logger log = LoggerFactory.getLogger(CveIngestionService.class);
    private static final Pattern SOURCE = Pattern.compile("^[a-zA-Z0-9._-]{1,64}$");

    private final CveRawRecordRepository rawRecords;
    private final ObjectMapper objectMapper;
    private final CveEventPublisher publisher;

    public CveIngestionService(
            CveRawRecordRepository rawRecords,
            ObjectMapper objectMapper,
            CveEventPublisher publisher) {
        this.rawRecords = rawRecords;
        this.objectMapper = objectMapper;
        this.publisher = publisher;
    }

    @Transactional
    public IngestionResponse ingest(byte[] body, String source, UUID correlationId) {
        if (body == null || body.length == 0) {
            throw new IngestionException(HttpStatus.BAD_REQUEST, "EMPTY_PAYLOAD", "Request body is required");
        }
        if (body.length > MAX_PAYLOAD_BYTES) {
            throw new IngestionException(HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE", "CVE payload exceeds 512 KiB");
        }
        String normalizedSource = normalizeSource(source);
        UUID corr = correlationId == null ? UUID.randomUUID() : correlationId;
        MDC.put("correlationId", corr.toString());

        JsonNode payload;
        try {
            payload = objectMapper.readTree(body);
        } catch (Exception ex) {
            throw new IngestionException(HttpStatus.BAD_REQUEST, "INVALID_JSON", "Body is not valid JSON");
        }

        String cveId = CveIdValidator.requireValid(CveDocumentParser.extractCveId(payload));
        CveDocumentParser.parse(payload, normalizedSource);
        MDC.put("cveId", cveId);

        String hash = sha256(body);
        UUID eventId = stableEventId(normalizedSource, cveId);
        MDC.put("eventId", eventId.toString());

        var existing = rawRecords.findBySourceAndExternalId(normalizedSource, cveId);
        if (existing.isPresent()) {
            CveRawRecord record = existing.get();
            log.info("operation=cve.ingest.duplicate cveId={} eventId={} status={}", cveId, record.getEventId(), record.getProcessingStatus());
            if ("ACCEPTED".equals(record.getProcessingStatus())) {
                publishRawAfterCommit(record, payload);
            }
            return new IngestionResponse(cveId, "DUPLICATE", record.getEventId(), record.getCorrelationId());
        }

        Instant now = Instant.now();
        CveRawRecord record = new CveRawRecord();
        record.setId(UUID.randomUUID());
        record.setSource(normalizedSource);
        record.setExternalId(cveId);
        record.setReceivedAt(now);
        record.setPayload(new String(body, StandardCharsets.UTF_8));
        record.setPayloadHash(hash);
        record.setProcessingStatus("ACCEPTED");
        record.setEventId(eventId);
        record.setCorrelationId(corr);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        try {
            rawRecords.saveAndFlush(record);
        } catch (DataIntegrityViolationException ex) {
            CveRawRecord raced = rawRecords.findBySourceAndExternalId(normalizedSource, cveId).orElseThrow();
            log.info("operation=cve.ingest.race cveId={} eventId={}", cveId, raced.getEventId());
            return new IngestionResponse(cveId, "DUPLICATE", raced.getEventId(), raced.getCorrelationId());
        }

        log.info("operation=cve.ingest.accepted cveId={} eventId={} bytes={}", cveId, eventId, body.length);
        publishRawAfterCommit(record, payload);
        return new IngestionResponse(cveId, "ACCEPTED", eventId, corr);
    }

    private void publishRawAfterCommit(CveRawRecord record, JsonNode payload) {
        ObjectNode eventPayload = objectMapper.createObjectNode();
        eventPayload.put("rawRecordId", record.getId().toString());
        eventPayload.put("cveId", record.getExternalId());
        eventPayload.put("source", record.getSource());
        eventPayload.put("payloadHash", record.getPayloadHash());
        eventPayload.put("receivedAt", record.getReceivedAt().toString());
        eventPayload.set("raw", payload);
        EventEnvelope envelope = new EventEnvelope(
                record.getEventId(),
                EventEnvelope.TYPE_RAW,
                1,
                Instant.now(),
                EventEnvelope.SOURCE_SERVICE,
                record.getCorrelationId(),
                eventPayload
        );
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publisher.publish(EventEnvelope.TYPE_RAW, record.getExternalId(), envelope);
                }
            });
        } else {
            publisher.publish(EventEnvelope.TYPE_RAW, record.getExternalId(), envelope);
        }
    }

    private static String normalizeSource(String source) {
        String value = source == null || source.isBlank() ? "manual" : source.trim();
        if (!SOURCE.matcher(value).matches()) {
            throw new IngestionException(HttpStatus.BAD_REQUEST, "INVALID_SOURCE", "source must match [a-zA-Z0-9._-]{1,64}");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    static UUID stableEventId(String source, String cveId) {
        return UUID.nameUUIDFromBytes(("cve.raw|" + source + "|" + cveId).getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256(byte[] body) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(body);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
