package com.threatadvisor.ai.controller;

import com.threatadvisor.ai.config.CorrelationIdFilter;
import com.threatadvisor.ai.dto.RemediationRunResponse;
import com.threatadvisor.ai.rag.KnowledgeIngestionService;
import com.threatadvisor.ai.service.RemediationGenerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal/ai")
public class AiController {

    private final RemediationGenerationService generationService;
    private final KnowledgeIngestionService knowledgeIngestionService;

    public AiController(
            RemediationGenerationService generationService,
            KnowledgeIngestionService knowledgeIngestionService) {
        this.generationService = generationService;
        this.knowledgeIngestionService = knowledgeIngestionService;
    }

    @PostMapping("/remediation/{findingId}")
    public ResponseEntity<RemediationRunResponse> remediate(
            @PathVariable UUID findingId,
            @RequestHeader(value = CorrelationIdFilter.HEADER, required = false) UUID correlationId) {
        RemediationGenerationService.Outcome outcome = generationService.runForFinding(findingId, correlationId);
        return ResponseEntity.ok(new RemediationRunResponse(
                outcome.status(),
                outcome.findingId(),
                outcome.remediationPlanId(),
                outcome.riskAssessmentId()));
    }

    @PostMapping("/knowledge/ingest")
    public Map<String, Object> ingest() {
        int documents = knowledgeIngestionService.ingestClasspath();
        return Map.of("status", "INGESTED", "documents", documents);
    }
}
