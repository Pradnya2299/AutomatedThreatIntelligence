package com.threatadvisor.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.threatadvisor.ai.config.AiProperties;
import com.threatadvisor.ai.domain.Asset;
import com.threatadvisor.ai.domain.AssetSoftware;
import com.threatadvisor.ai.domain.EventProcessingRecord;
import com.threatadvisor.ai.domain.Finding;
import com.threatadvisor.ai.domain.KnowledgeChunkHit;
import com.threatadvisor.ai.domain.RemediationContext;
import com.threatadvisor.ai.domain.RemediationPlan;
import com.threatadvisor.ai.domain.RiskAssessment;
import com.threatadvisor.ai.domain.Vulnerability;
import com.threatadvisor.ai.domain.VulnerabilityCpe;
import com.threatadvisor.ai.dto.RemediationAiResponse;
import com.threatadvisor.ai.exception.AiException;
import com.threatadvisor.ai.kafka.EventEnvelope;
import com.threatadvisor.ai.kafka.RemediationEventPublisher;
import com.threatadvisor.ai.llm.AiRemediationService;
import com.threatadvisor.ai.llm.DemoAiRemediationService;
import com.threatadvisor.ai.prompt.PromptBuilder;
import com.threatadvisor.ai.rag.EmbeddingService;
import com.threatadvisor.ai.repository.AssetRepository;
import com.threatadvisor.ai.repository.AssetSoftwareRepository;
import com.threatadvisor.ai.repository.EventProcessingRecordRepository;
import com.threatadvisor.ai.repository.FindingRepository;
import com.threatadvisor.ai.repository.RemediationPlanRepository;
import com.threatadvisor.ai.repository.RiskAssessmentRepository;
import com.threatadvisor.ai.repository.VulnerabilityCpeRepository;
import com.threatadvisor.ai.repository.VulnerabilityRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RemediationGenerationService {

    public static final String CONSUMER = "ai-service:risk.calculated";
    private static final Logger log = LoggerFactory.getLogger(RemediationGenerationService.class);

    private final ObjectMapper objectMapper;
    private final AiProperties properties;
    private final Validator validator;
    private final AiRemediationService ai;
    private final EmbeddingService embeddings;
    private final EventProcessingRecordRepository processingRecords;
    private final FindingRepository findings;
    private final AssetRepository assets;
    private final AssetSoftwareRepository software;
    private final VulnerabilityRepository vulnerabilities;
    private final VulnerabilityCpeRepository cpes;
    private final RiskAssessmentRepository risks;
    private final RemediationPlanRepository plans;
    private final RemediationEventPublisher publisher;
    private final RemediationResponseCache cache;

    public RemediationGenerationService(
            ObjectMapper objectMapper,
            AiProperties properties,
            Validator validator,
            AiRemediationService ai,
            EmbeddingService embeddings,
            EventProcessingRecordRepository processingRecords,
            FindingRepository findings,
            AssetRepository assets,
            AssetSoftwareRepository software,
            VulnerabilityRepository vulnerabilities,
            VulnerabilityCpeRepository cpes,
            RiskAssessmentRepository risks,
            RemediationPlanRepository plans,
            RemediationEventPublisher publisher,
            RemediationResponseCache cache) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.validator = validator;
        this.ai = ai;
        this.embeddings = embeddings;
        this.processingRecords = processingRecords;
        this.findings = findings;
        this.assets = assets;
        this.software = software;
        this.vulnerabilities = vulnerabilities;
        this.cpes = cpes;
        this.risks = risks;
        this.plans = plans;
        this.publisher = publisher;
        this.cache = cache;
    }

    @Transactional
    public Outcome handleRiskCalculated(String message) {
        EventEnvelope envelope;
        try {
            envelope = objectMapper.readValue(message, EventEnvelope.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid risk.calculated envelope");
        }
        if (!EventEnvelope.TYPE_RISK_CALCULATED.equals(envelope.eventType()) || envelope.eventId() == null) {
            return Outcome.ignored();
        }
        MDC.put("eventId", envelope.eventId().toString());
        if (envelope.correlationId() != null) {
            MDC.put("correlationId", envelope.correlationId().toString());
        }
        if (processingRecords.existsByEventIdAndConsumer(envelope.eventId(), CONSUMER)) {
            log.info("operation=ai.idempotent-skip eventId={}", envelope.eventId());
            return Outcome.duplicate(envelope.eventId());
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
            return Outcome.duplicate(envelope.eventId());
        }
        UUID findingId = readUuid(envelope.payload(), "findingId");
        UUID riskId = envelope.payload() != null && envelope.payload().hasNonNull("riskAssessmentId")
                ? UUID.fromString(envelope.payload().get("riskAssessmentId").asText())
                : null;
        return generate(findingId, riskId, envelope.correlationId());
    }

    @Transactional
    public Outcome runForFinding(UUID findingId, UUID correlationId) {
        Finding finding = findings.findById(findingId)
                .orElseThrow(() -> new AiException(HttpStatus.NOT_FOUND, "FINDING_NOT_FOUND", "Unknown finding"));
        RiskAssessment risk = risks.findByFindingId(finding.getId())
                .orElseThrow(() -> new AiException(HttpStatus.NOT_FOUND, "RISK_NOT_FOUND", "No risk assessment"));
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("findingId", finding.getId().toString());
        payload.put("riskAssessmentId", risk.getId().toString());
        EventEnvelope envelope = new EventEnvelope(
                UUID.randomUUID(),
                EventEnvelope.TYPE_RISK_CALCULATED,
                1,
                Instant.now(),
                "ai-service",
                correlationId == null ? UUID.randomUUID() : correlationId,
                payload);
        try {
            return handleRiskCalculated(objectMapper.writeValueAsString(envelope));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Outcome generate(UUID findingId, UUID riskAssessmentId, UUID correlationId) {
        Finding finding = findings.findById(findingId)
                .orElseThrow(() -> new IllegalStateException("Finding not found " + findingId));
        RiskAssessment risk = riskAssessmentId != null
                ? risks.findById(riskAssessmentId).orElseThrow()
                : risks.findByFindingId(findingId).orElseThrow();
        MDC.put("findingId", finding.getId().toString());
        MDC.put("riskAssessmentId", risk.getId().toString());
        MDC.put("assetId", finding.getAssetId().toString());

        var existing = plans.findByFindingIdAndRiskAssessmentId(finding.getId(), risk.getId());
        if (existing.isPresent() && "GENERATED".equals(existing.get().getStatus())) {
            log.info("operation=ai.skip-existing-plan remediationPlanId={}", existing.get().getId());
            return new Outcome("SKIPPED", finding.getId(), existing.get().getId(), risk.getId(), null);
        }

        Asset asset = assets.findById(finding.getAssetId()).orElseThrow();
        Vulnerability vulnerability = vulnerabilities.findById(finding.getVulnerabilityId()).orElseThrow();
        MDC.put("cveId", vulnerability.getCveId());
        List<AssetSoftware> installed = software.findByAssetIdAndInstallationStatus(asset.getId(), "INSTALLED");
        List<VulnerabilityCpe> cpeRows = cpes.findByVulnerabilityId(vulnerability.getId());

        RemediationContext base = buildContext(finding, asset, vulnerability, risk, installed, cpeRows, List.of());
        List<KnowledgeChunkHit> hits;
        try {
            hits = embeddings.search(PromptBuilder.retrievalQuery(base), properties.getTopK());
        } catch (RuntimeException ex) {
            log.error("operation=ai.rag.failed", ex);
            hits = List.of();
        }
        RemediationContext context = buildContext(finding, asset, vulnerability, risk, installed, cpeRows, hits);

        RemediationAiResponse response = cache.get(finding.getId(), risk.getId());
        boolean calledModel = false;
        if (response == null) {
            try {
                response = ai.generate(context);
                calledModel = true;
            } catch (RuntimeException ex) {
                log.error("operation=ai.generate.failed", ex);
                persistFailed(existing.orElse(null), finding, risk, truncate(ex.getMessage()));
                return new Outcome("FAILED", finding.getId(), null, risk.getId(), null);
            }
        }
        Set<ConstraintViolation<RemediationAiResponse>> violations = validator.validate(response);
        if (!violations.isEmpty()) {
            persistFailed(existing.orElse(null), finding, risk, "Invalid structured response");
            return new Outcome("FAILED", finding.getId(), null, risk.getId(), null);
        }
        cache.put(finding.getId(), risk.getId(), response);
        RemediationPlan plan = persistGenerated(existing.orElse(null), finding, risk, response, hits);
        MDC.put("remediationPlanId", plan.getId().toString());
        log.info("Generated remediation plan for {} on asset {} demoMode={} modelCall={}",
                vulnerability.getCveId(), asset.getHostname(), properties.isDemoMode(), calledModel);
        publishAfterCommit(plan, finding, vulnerability, risk, correlationId);
        return new Outcome("COMPLETED", finding.getId(), plan.getId(), risk.getId(), response);
    }

    private RemediationContext buildContext(
            Finding finding,
            Asset asset,
            Vulnerability vulnerability,
            RiskAssessment risk,
            List<AssetSoftware> installed,
            List<VulnerabilityCpe> cpeRows,
            List<KnowledgeChunkHit> hits) {
        String softwareSummary = installed.stream()
                .map(s -> s.getVendor() + " " + s.getProduct() + " " + s.getVersion())
                .collect(Collectors.joining(", "));
        String product = cpeRows.stream().map(VulnerabilityCpe::getProduct).filter(v -> v != null).findFirst()
                .orElse(installed.isEmpty() ? null : installed.getFirst().getProduct());
        String version = installed.isEmpty() ? null : installed.getFirst().getVersion();
        String range = cpeRows.stream()
                .map(c -> String.valueOf(c.getVersionStartIncluding()) + " to " + c.getVersionEndExcluding())
                .findFirst()
                .orElse(null);
        String factors = "cvss=" + risk.getTechnicalRisk()
                + ", criticality=" + risk.getAssetCriticalityScore()
                + ", exposure=" + risk.getExposureScore()
                + ", exploit=" + risk.getExploitabilityScore()
                + ", active=" + risk.getBusinessImpactScore();
        String explanation = risk.getReasons();
        try {
            JsonNode reasons = objectMapper.readTree(risk.getReasons());
            if (reasons.has("text")) {
                explanation = reasons.get("text").asText();
            }
        } catch (Exception ignored) {
            // keep raw JSON
        }
        return new RemediationContext(
                vulnerability.getCveId(),
                vulnerability.getDescription(),
                vulnerability.getSeverity(),
                vulnerability.getCvssScore() == null ? null : vulnerability.getCvssScore().toPlainString(),
                product,
                version,
                range,
                asset.getHostname(),
                asset.getOperatingSystem(),
                asset.getEnvironment(),
                asset.getBusinessCriticality(),
                asset.isInternetExposure(),
                softwareSummary,
                risk.getFinalRiskScore().toPlainString(),
                risk.getRiskLevel(),
                factors,
                explanation,
                hits);
    }

    private RemediationPlan persistGenerated(
            RemediationPlan existing,
            Finding finding,
            RiskAssessment risk,
            RemediationAiResponse response,
            List<KnowledgeChunkHit> hits) {
        Instant now = Instant.now();
        RemediationPlan plan = existing == null ? new RemediationPlan() : existing;
        if (plan.getId() == null) {
            plan.setId(UUID.randomUUID());
            plan.setCreatedAt(now);
        }
        plan.setFindingId(finding.getId());
        plan.setRiskAssessmentId(risk.getId());
        plan.setStatus("GENERATED");
        plan.setPriority(response.getPriority());
        plan.setSummary(response.getSummary());
        plan.setReason(response.getReasoning());
        plan.setRecommendedAction(response.getRecommendedAction());
        plan.setPatchVersion(truncate(response.getTargetVersion(), 128));
        plan.setVerificationSteps(toJson(response.getValidationSteps()));
        plan.setImplementationSteps(toJson(response.getImplementationSteps()));
        plan.setPrerequisites(toJson(response.getPrerequisites()));
        plan.setAffectedComponents(toJson(response.getAffectedComponents()));
        plan.setReferenceUrls(toJson(response.getReferences()));
        plan.setRollbackPlan(response.getRollbackPlan());
        plan.setDowntimeExpected(response.getDowntimeExpected());
        plan.setRetrievedContext(retrievedJson(hits));
        plan.setModelRawOutput(toJson(response));
        plan.setModelName(properties.isDemoMode() ? DemoAiRemediationService.MODEL_NAME : properties.getChatModel());
        plan.setPromptVersion(properties.getPromptVersion());
        plan.setRejectionReason(null);
        plan.setUpdatedAt(now);
        return plans.save(plan);
    }

    private void persistFailed(RemediationPlan existing, Finding finding, RiskAssessment risk, String error) {
        Instant now = Instant.now();
        RemediationPlan plan = existing == null ? new RemediationPlan() : existing;
        if (plan.getId() == null) {
            plan.setId(UUID.randomUUID());
            plan.setCreatedAt(now);
            plan.setVerificationSteps("[]");
            plan.setRetrievedContext("[]");
            plan.setAffectedComponents("[]");
            plan.setPrerequisites("[]");
            plan.setImplementationSteps("[]");
            plan.setReferenceUrls("[]");
        }
        plan.setFindingId(finding.getId());
        plan.setRiskAssessmentId(risk.getId());
        plan.setStatus("FAILED");
        plan.setRejectionReason(truncate(error, 2000));
        plan.setUpdatedAt(now);
        plans.save(plan);
    }

    private void publishAfterCommit(
            RemediationPlan plan, Finding finding, Vulnerability vulnerability, RiskAssessment risk, UUID correlationId) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("remediationPlanId", plan.getId().toString());
        payload.put("findingId", finding.getId().toString());
        payload.put("riskAssessmentId", risk.getId().toString());
        payload.put("cveId", vulnerability.getCveId());
        payload.put("assetId", finding.getAssetId().toString());
        payload.put("priority", plan.getPriority());
        payload.put("summary", plan.getSummary());
        payload.put("recommendedAction", plan.getRecommendedAction());
        payload.put("status", plan.getStatus());
        EventEnvelope event = new EventEnvelope(
                UUID.randomUUID(),
                EventEnvelope.TYPE_REMEDIATION_GENERATED,
                1,
                Instant.now(),
                EventEnvelope.SOURCE_SERVICE,
                correlationId == null ? UUID.randomUUID() : correlationId,
                payload);
        Runnable publish = () -> publisher.publish(
                EventEnvelope.TYPE_REMEDIATION_GENERATED,
                plan.getId().toString(),
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

    private String retrievedJson(List<KnowledgeChunkHit> hits) {
        ArrayNode array = objectMapper.createArrayNode();
        for (KnowledgeChunkHit hit : hits) {
            ObjectNode node = array.addObject();
            node.put("title", hit.title());
            node.put("source", hit.source());
            node.put("content", hit.content());
        }
        return array.toString();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    private static UUID readUuid(JsonNode payload, String field) {
        if (payload == null || !payload.hasNonNull(field)) {
            throw new IllegalStateException("payload missing " + field);
        }
        return UUID.fromString(payload.get(field).asText());
    }

    private static String truncate(String value) {
        return truncate(value, 500);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    public record Outcome(String status, UUID findingId, UUID remediationPlanId, UUID riskAssessmentId, RemediationAiResponse response) {
        static Outcome ignored() {
            return new Outcome("IGNORED", null, null, null, null);
        }

        static Outcome duplicate(UUID eventId) {
            return new Outcome("DUPLICATE", null, null, eventId, null);
        }
    }
}
