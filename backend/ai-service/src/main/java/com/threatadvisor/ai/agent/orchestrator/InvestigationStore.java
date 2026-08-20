package com.threatadvisor.ai.agent.orchestrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threatadvisor.ai.agent.common.AgentError;
import com.threatadvisor.ai.agent.common.AgentExecution;
import com.threatadvisor.ai.agent.common.Confidence;
import com.threatadvisor.ai.agent.common.DecisionRecord;
import com.threatadvisor.ai.agent.common.EvidenceItem;
import com.threatadvisor.ai.agent.common.ExecutionTraceEntry;
import com.threatadvisor.ai.agent.common.InvestigationState;
import com.threatadvisor.ai.agent.common.InvestigationStatus;
import com.threatadvisor.ai.agent.common.SecurityInvestigationContext;
import com.threatadvisor.ai.agent.dto.AssetInvestigationResult;
import com.threatadvisor.ai.agent.dto.FinalRecommendation;
import com.threatadvisor.ai.agent.dto.RemediationAgentResult;
import com.threatadvisor.ai.agent.dto.RiskAnalystResult;
import com.threatadvisor.ai.agent.dto.ThreatIntelligenceResult;
import com.threatadvisor.ai.domain.SecurityInvestigation;
import com.threatadvisor.ai.repository.SecurityInvestigationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class InvestigationStore {

    private static final String EMPTY_OBJECT = "{}";
    private static final String EMPTY_ARRAY = "[]";

    private final SecurityInvestigationRepository repository;
    private final ObjectMapper objectMapper;

    public InvestigationStore(SecurityInvestigationRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SecurityInvestigationContext persist(SecurityInvestigationContext context) {
        Instant now = Instant.now();
        SecurityInvestigation row = repository.findById(context.investigationId()).orElseGet(SecurityInvestigation::new);
        if (row.getId() == null) {
            row.setId(context.investigationId());
            row.setCreatedAt(context.startedAt() == null ? now : context.startedAt());
        }
        row.setCveId(context.cveId());
        row.setStatus(context.status().name());
        row.setCorrelationId(context.correlationId());
        row.setThreatResult(write(context.threat(), EMPTY_OBJECT));
        row.setAssetResult(write(context.assets(), EMPTY_OBJECT));
        row.setRiskResult(write(context.risk(), EMPTY_OBJECT));
        row.setRemediationResult(write(context.remediation(), EMPTY_OBJECT));
        row.setExecutions(write(context.executions(), EMPTY_ARRAY));
        row.setErrors(write(context.errors(), EMPTY_ARRAY));
        row.setEvidence(write(context.evidence(), EMPTY_ARRAY));
        row.setRecommendation(write(context.recommendation(), EMPTY_OBJECT));
        row.setCurrentState(context.currentState() == null
                ? InvestigationState.INITIALIZED.name()
                : context.currentState().name());
        row.setOverallConfidence(context.overallConfidence() == null ? null : context.overallConfidence().name());
        row.setIterationCount(context.iterationCount());
        row.setDecisionHistory(write(context.decisionHistory(), EMPTY_ARRAY));
        row.setExecutionTrace(write(context.executionTrace(), EMPTY_ARRAY));
        row.setCompletedAgents(write(context.completedAgents(), EMPTY_ARRAY));
        row.setPendingAgents(write(context.pendingAgents(), EMPTY_ARRAY));
        row.setUpdatedAt(now);
        row.setCompletedAt(context.completedAt());
        repository.save(row);
        return context;
    }

    @Transactional(readOnly = true)
    public Optional<SecurityInvestigationContext> find(UUID id) {
        return repository.findById(id).map(this::toContext);
    }

    private SecurityInvestigationContext toContext(SecurityInvestigation row) {
        return SecurityInvestigationContext.builder()
                .investigationId(row.getId())
                .cveId(row.getCveId())
                .correlationId(row.getCorrelationId())
                .startedAt(row.getCreatedAt())
                .completedAt(row.getCompletedAt())
                .status(InvestigationStatus.valueOf(row.getStatus()))
                .currentState(parseState(row.getCurrentState()))
                .overallConfidence(parseConfidence(row.getOverallConfidence()))
                .iterationCount(row.getIterationCount())
                .threat(read(row.getThreatResult(), ThreatIntelligenceResult.class))
                .assets(read(row.getAssetResult(), AssetInvestigationResult.class))
                .risk(read(row.getRiskResult(), RiskAnalystResult.class))
                .remediation(read(row.getRemediationResult(), RemediationAgentResult.class))
                .recommendation(read(row.getRecommendation(), FinalRecommendation.class))
                .replaceExecutionList(readList(row.getExecutions(), new TypeReference<List<AgentExecution>>() {}))
                .replaceErrorList(readList(row.getErrors(), new TypeReference<List<AgentError>>() {}))
                .replaceEvidenceList(readList(row.getEvidence(), new TypeReference<List<EvidenceItem>>() {}))
                .replaceDecisionHistory(readList(row.getDecisionHistory(), new TypeReference<List<DecisionRecord>>() {}))
                .replaceExecutionTrace(readList(row.getExecutionTrace(), new TypeReference<List<ExecutionTraceEntry>>() {}))
                .replaceCompletedAgents(readList(row.getCompletedAgents(), new TypeReference<List<String>>() {}))
                .replacePendingAgents(readList(row.getPendingAgents(), new TypeReference<List<String>>() {}))
                .refreshAgentLists()
                .build();
    }

    private <T> T read(String json, Class<T> type) {
        if (json == null || json.isBlank() || EMPTY_OBJECT.equals(json.strip())) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            return null;
        }
    }

    private <T> List<T> readList(String json, TypeReference<List<T>> type) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<T> value = objectMapper.readValue(json, type);
            return value == null ? List.of() : value;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private static InvestigationState parseState(String value) {
        if (value == null || value.isBlank()) {
            return InvestigationState.INITIALIZED;
        }
        try {
            return InvestigationState.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return InvestigationState.INITIALIZED;
        }
    }

    private static Confidence parseConfidence(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Confidence.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String write(Object value, String empty) {
        if (value == null) {
            return empty;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return empty;
        }
    }
}
