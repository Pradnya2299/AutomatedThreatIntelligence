package com.threatadvisor.ai.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threatadvisor.ai.agent.orchestrator.SecurityOrchestrator;
import com.threatadvisor.ai.domain.EventProcessingRecord;
import com.threatadvisor.ai.repository.EventProcessingRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
public class InvestigationRequestedListener {

    public static final String CONSUMER = "ai-service:security.investigation.requested";
    private static final Logger log = LoggerFactory.getLogger(InvestigationRequestedListener.class);

    private final ObjectMapper objectMapper;
    private final EventProcessingRecordRepository processingRecords;
    private final SecurityOrchestrator orchestrator;

    public InvestigationRequestedListener(
            ObjectMapper objectMapper,
            EventProcessingRecordRepository processingRecords,
            SecurityOrchestrator orchestrator) {
        this.objectMapper = objectMapper;
        this.processingRecords = processingRecords;
        this.orchestrator = orchestrator;
    }

    @KafkaListener(topics = EventEnvelope.TYPE_INVESTIGATION_REQUESTED, groupId = "ai-service")
    public void onRequested(String message) {
        log.info("operation=security.investigation.requested.received bytes={}", message == null ? 0 : message.length());
        try {
            handle(message);
        } catch (RuntimeException ex) {
            log.error("operation=investigation.consume.failed", ex);
        }
    }

    @Transactional
    void handle(String message) {
        EventEnvelope envelope;
        try {
            envelope = objectMapper.readValue(message, EventEnvelope.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid security.investigation.requested envelope");
        }
        if (!EventEnvelope.TYPE_INVESTIGATION_REQUESTED.equals(envelope.eventType()) || envelope.eventId() == null) {
            return;
        }
        if (processingRecords.existsByEventIdAndConsumer(envelope.eventId(), CONSUMER)) {
            log.info("operation=investigation.idempotent-skip eventId={}", envelope.eventId());
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
            return;
        }
        if (envelope.payload() == null || !envelope.payload().hasNonNull("cveId")) {
            throw new IllegalStateException("security.investigation.requested payload missing cveId");
        }
        String cveId = envelope.payload().get("cveId").asText();
        orchestrator.startInvestigation(cveId, envelope.correlationId());
    }
}
