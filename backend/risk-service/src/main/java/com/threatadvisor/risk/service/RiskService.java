package com.threatadvisor.risk.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.threatadvisor.risk.domain.Asset;
import com.threatadvisor.risk.domain.EventProcessingRecord;
import com.threatadvisor.risk.domain.Finding;
import com.threatadvisor.risk.domain.RiskAssessment;
import com.threatadvisor.risk.domain.Vulnerability;
import com.threatadvisor.risk.engine.RiskEngine;
import com.threatadvisor.risk.engine.RiskInput;
import com.threatadvisor.risk.engine.RiskResult;
import com.threatadvisor.risk.exception.RiskException;
import com.threatadvisor.risk.kafka.EventEnvelope;
import com.threatadvisor.risk.kafka.RiskEventPublisher;
import com.threatadvisor.risk.repository.AssetRepository;
import com.threatadvisor.risk.repository.EventProcessingRecordRepository;
import com.threatadvisor.risk.repository.FindingRepository;
import com.threatadvisor.risk.repository.RiskAssessmentRepository;
import com.threatadvisor.risk.repository.VulnerabilityRepository;
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
import java.util.UUID;

@Service
public class RiskService {

    public static final String CONSUMER = "risk-service:finding.created";
    private static final Logger log = LoggerFactory.getLogger(RiskService.class);

    private final ObjectMapper objectMapper;
    private final EventProcessingRecordRepository processingRecords;
    private final FindingRepository findings;
    private final AssetRepository assets;
    private final VulnerabilityRepository vulnerabilities;
    private final RiskAssessmentRepository assessments;
    private final RiskEventPublisher publisher;

    public RiskService(
            ObjectMapper objectMapper,
            EventProcessingRecordRepository processingRecords,
            FindingRepository findings,
            AssetRepository assets,
            VulnerabilityRepository vulnerabilities,
            RiskAssessmentRepository assessments,
            RiskEventPublisher publisher) {
        this.objectMapper = objectMapper;
        this.processingRecords = processingRecords;
        this.findings = findings;
        this.assets = assets;
        this.vulnerabilities = vulnerabilities;
        this.assessments = assessments;
        this.publisher = publisher;
    }

    @Transactional
    public RiskOutcome handleFindingCreated(String message) {
        EventEnvelope envelope;
        try {
            envelope = objectMapper.readValue(message, EventEnvelope.class);
        } catch (JsonProcessingException ex) {
            log.error("operation=risk.invalid-envelope", ex);
            throw new IllegalArgumentException("Invalid finding.created envelope");
        }
        if (!EventEnvelope.TYPE_FINDING_CREATED.equals(envelope.eventType()) || envelope.eventId() == null) {
            log.warn("operation=risk.ignored eventType={}", envelope.eventType());
            return RiskOutcome.ignored();
        }
        MDC.put("eventId", envelope.eventId().toString());
        if (envelope.correlationId() != null) {
            MDC.put("correlationId", envelope.correlationId().toString());
        }
        if (processingRecords.existsByEventIdAndConsumer(envelope.eventId(), CONSUMER)) {
            log.info("operation=risk.idempotent-skip eventId={}", envelope.eventId());
            return RiskOutcome.duplicate(envelope.eventId());
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
            log.info("operation=risk.idempotent-race eventId={}", envelope.eventId());
            return RiskOutcome.duplicate(envelope.eventId());
        }

        UUID findingId = readUuid(envelope.payload(), "findingId");
        Finding finding = findings.findById(findingId)
                .orElseThrow(() -> new IllegalStateException("Finding not found: " + findingId));
        Asset asset = assets.findById(finding.getAssetId())
                .orElseThrow(() -> new IllegalStateException("Asset not found: " + finding.getAssetId()));
        Vulnerability vulnerability = vulnerabilities.findById(finding.getVulnerabilityId())
                .orElseThrow(() -> new IllegalStateException("Vulnerability not found: " + finding.getVulnerabilityId()));

        MDC.put("findingId", finding.getId().toString());
        MDC.put("assetId", asset.getId().toString());
        MDC.put("cveId", vulnerability.getCveId());

        RiskResult result = RiskEngine.calculate(new RiskInput(
                vulnerability.getCvssScore(),
                asset.getBusinessCriticality(),
                asset.isInternetExposure(),
                vulnerability.getExploitAvailable(),
                vulnerability.getActivelyExploited()));
        RiskAssessment assessment = upsert(finding.getId(), result);
        EventEnvelope outbound = riskCalculatedEvent(assessment, finding, vulnerability, result, envelope.correlationId());
        publishAfterCommit(outbound);
        log.info("Calculated {} risk {} for finding {} CVE {}",
                result.level(), result.score(), finding.getId(), vulnerability.getCveId());
        return new RiskOutcome("COMPLETED", finding.getId(), assessment.getId(), envelope.eventId(), result);
    }

    @Transactional
    public RiskOutcome runForFinding(UUID findingId, UUID correlationId) {
        if (!findings.existsById(findingId)) {
            throw new RiskException(HttpStatus.NOT_FOUND, "FINDING_NOT_FOUND", "Unknown finding " + findingId);
        }
        ObjectNode payload = objectMapper.createObjectNode().put("findingId", findingId.toString());
        EventEnvelope envelope = new EventEnvelope(
                UUID.randomUUID(),
                EventEnvelope.TYPE_FINDING_CREATED,
                1,
                Instant.now(),
                "risk-service",
                correlationId == null ? UUID.randomUUID() : correlationId,
                payload);
        try {
            return handleFindingCreated(objectMapper.writeValueAsString(envelope));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private RiskAssessment upsert(UUID findingId, RiskResult result) {
        Instant now = Instant.now();
        String reasons = reasonsJson(result);
        RiskAssessment assessment = assessments.findByFindingId(findingId).orElseGet(() -> {
            RiskAssessment created = new RiskAssessment();
            created.setId(UUID.randomUUID());
            created.setFindingId(findingId);
            return created;
        });
        assessment.setTechnicalRisk(result.cvss());
        assessment.setAssetCriticalityScore(result.assetCriticality());
        assessment.setExposureScore(result.internetExposure());
        assessment.setExploitabilityScore(result.exploitability());
        assessment.setBusinessImpactScore(result.activeExploitation());
        assessment.setFinalRiskScore(result.score());
        assessment.setRiskLevel(result.level());
        assessment.setFormulaVersion(RiskEngine.FORMULA_VERSION);
        assessment.setReasons(reasons);
        assessment.setCalculatedAt(now);
        try {
            return assessments.saveAndFlush(assessment);
        } catch (DataIntegrityViolationException ex) {
            RiskAssessment raced = assessments.findByFindingId(findingId).orElseThrow(() -> ex);
            raced.setTechnicalRisk(result.cvss());
            raced.setAssetCriticalityScore(result.assetCriticality());
            raced.setExposureScore(result.internetExposure());
            raced.setExploitabilityScore(result.exploitability());
            raced.setBusinessImpactScore(result.activeExploitation());
            raced.setFinalRiskScore(result.score());
            raced.setRiskLevel(result.level());
            raced.setFormulaVersion(RiskEngine.FORMULA_VERSION);
            raced.setReasons(reasons);
            raced.setCalculatedAt(now);
            return assessments.save(raced);
        }
    }

    private String reasonsJson(RiskResult result) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("text", result.explanation());
        ObjectNode factors = root.putObject("factors");
        factors.put("cvss", result.cvss());
        factors.put("assetCriticality", result.assetCriticality());
        factors.put("internetExposure", result.internetExposure());
        factors.put("exploitability", result.exploitability());
        factors.put("activeExploitation", result.activeExploitation());
        return root.toString();
    }

    private EventEnvelope riskCalculatedEvent(
            RiskAssessment assessment,
            Finding finding,
            Vulnerability vulnerability,
            RiskResult result,
            UUID correlationId) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("riskAssessmentId", assessment.getId().toString());
        payload.put("findingId", finding.getId().toString());
        payload.put("vulnerabilityId", finding.getVulnerabilityId().toString());
        payload.put("assetId", finding.getAssetId().toString());
        payload.put("cveId", vulnerability.getCveId());
        payload.put("riskScore", result.score());
        payload.put("riskLevel", result.level());
        ObjectNode factors = payload.putObject("factors");
        factors.put("cvss", result.cvss());
        factors.put("assetCriticality", result.assetCriticality());
        factors.put("internetExposure", result.internetExposure());
        factors.put("exploitability", result.exploitability());
        factors.put("activeExploitation", result.activeExploitation());
        payload.put("explanation", result.explanation());
        return new EventEnvelope(
                UUID.randomUUID(),
                EventEnvelope.TYPE_RISK_CALCULATED,
                1,
                Instant.now(),
                EventEnvelope.SOURCE_SERVICE,
                correlationId == null ? UUID.randomUUID() : correlationId,
                payload);
    }

    private void publishAfterCommit(EventEnvelope event) {
        Runnable publish = () -> publisher.publish(
                EventEnvelope.TYPE_RISK_CALCULATED,
                event.payload().path("findingId").asText(),
                event);
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

    private static UUID readUuid(JsonNode payload, String field) {
        if (payload == null || !payload.hasNonNull(field)) {
            throw new IllegalStateException("finding.created payload missing " + field);
        }
        return UUID.fromString(payload.get(field).asText());
    }

    public record RiskOutcome(
            String status,
            UUID findingId,
            UUID riskAssessmentId,
            UUID eventId,
            RiskResult result
    ) {
        static RiskOutcome ignored() {
            return new RiskOutcome("IGNORED", null, null, null, null);
        }

        static RiskOutcome duplicate(UUID eventId) {
            return new RiskOutcome("DUPLICATE", null, null, eventId, null);
        }
    }
}
