package com.threatadvisor.ai.agent.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threatadvisor.ai.agent.common.AgentToolException;
import com.threatadvisor.ai.agent.dto.RemediationToolResult;
import com.threatadvisor.ai.domain.RemediationPlan;
import com.threatadvisor.ai.repository.RemediationPlanRepository;
import com.threatadvisor.ai.service.RemediationGenerationService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class ExistingRemediationGenerationTool implements RemediationGenerationTool {

    private final RemediationGenerationService generationService;
    private final RemediationPlanRepository plans;
    private final ObjectMapper objectMapper;

    public ExistingRemediationGenerationTool(
            RemediationGenerationService generationService,
            RemediationPlanRepository plans,
            ObjectMapper objectMapper) {
        this.generationService = generationService;
        this.plans = plans;
        this.objectMapper = objectMapper;
    }

    @Override
    public RemediationToolResult generate(UUID findingId, UUID correlationId) {
        RemediationGenerationService.Outcome outcome;
        try {
            outcome = generationService.runForFinding(findingId, correlationId);
        } catch (RuntimeException ex) {
            throw new AgentToolException("REMEDIATION_FAILED", ex.getMessage(), ex);
        }
        if ("FAILED".equals(outcome.status())) {
            throw new AgentToolException("REMEDIATION_FAILED", "Existing RAG/remediation pipeline returned FAILED");
        }
        RemediationPlan plan = outcome.remediationPlanId() == null
                ? null
                : plans.findById(outcome.remediationPlanId()).orElse(null);
        List<String> ragSources = ragSources(plan);
        return new RemediationToolResult(
                outcome.status(),
                outcome.findingId(),
                outcome.riskAssessmentId(),
                outcome.remediationPlanId(),
                plan == null ? null : plan.getPriority(),
                plan == null ? null : plan.getPatchVersion(),
                readStringList(plan == null ? null : plan.getPrerequisites()),
                readStringList(plan == null ? null : plan.getImplementationSteps()),
                readStringList(plan == null ? null : plan.getVerificationSteps()),
                plan == null ? null : plan.getRollbackPlan(),
                readStringList(plan == null ? null : plan.getReferenceUrls()),
                ragSources,
                !ragSources.isEmpty(),
                plan == null ? null : plan.getSummary());
    }

    private List<String> ragSources(RemediationPlan plan) {
        if (plan == null || plan.getRetrievedContext() == null || plan.getRetrievedContext().isBlank()) {
            return List.of();
        }
        try {
            JsonNode array = objectMapper.readTree(plan.getRetrievedContext());
            List<String> sources = new ArrayList<>();
            if (array.isArray()) {
                for (JsonNode node : array) {
                    if (node.hasNonNull("source")) {
                        sources.add(node.get("source").asText());
                    } else if (node.hasNonNull("title")) {
                        sources.add(node.get("title").asText());
                    }
                }
            }
            return List.copyOf(sources);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }
}
