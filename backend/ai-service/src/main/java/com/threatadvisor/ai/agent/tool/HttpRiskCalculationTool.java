package com.threatadvisor.ai.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threatadvisor.ai.agent.common.AgentToolException;
import com.threatadvisor.ai.agent.dto.RiskEngineSnapshot;
import com.threatadvisor.ai.agent.dto.RiskFactor;
import com.threatadvisor.ai.config.AiProperties;
import com.threatadvisor.ai.domain.RiskAssessment;
import com.threatadvisor.ai.repository.RiskAssessmentRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class HttpRiskCalculationTool implements RiskCalculationTool {

    private final RestClient restClient;
    private final RiskAssessmentRepository assessments;
    private final ObjectMapper objectMapper;

    public HttpRiskCalculationTool(
            AiProperties properties, RiskAssessmentRepository assessments, ObjectMapper objectMapper) {
        this.restClient = RestClient.builder().baseUrl(properties.getRiskBaseUrl()).build();
        this.assessments = assessments;
        this.objectMapper = objectMapper;
    }

    @Override
    public RiskEngineSnapshot calculate(UUID findingId, UUID correlationId) {
        RiskRunBody body;
        try {
            body = restClient.post()
                    .uri("/internal/risk/run/{findingId}", findingId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Correlation-Id", correlationId == null ? UUID.randomUUID().toString() : correlationId.toString())
                    .retrieve()
                    .body(RiskRunBody.class);
        } catch (RestClientException ex) {
            throw new AgentToolException(
                    "RISK_ENGINE_UNAVAILABLE",
                    "Risk engine call failed; score is unknown",
                    ex);
        }
        if (body == null || body.riskAssessmentId() == null) {
            throw new AgentToolException("RISK_ENGINE_EMPTY", "Risk engine returned no assessment id");
        }
        RiskAssessment stored = assessments.findById(body.riskAssessmentId())
                .or(() -> assessments.findByFindingId(findingId))
                .orElseThrow(() -> new AgentToolException(
                        "RISK_ROW_MISSING",
                        "Risk engine succeeded but risk_assessments row was not readable"));
        BigDecimal score = stored.getFinalRiskScore();
        String level = stored.getRiskLevel();
        if (body.riskScore() != null) {
            score = body.riskScore();
        }
        if (body.riskLevel() != null) {
            level = body.riskLevel();
        }
        return new RiskEngineSnapshot(
                findingId,
                stored.getId(),
                score,
                level,
                stored.getFormulaVersion(),
                factors(stored),
                explanation(stored));
    }

    private List<RiskFactor> factors(RiskAssessment stored) {
        List<RiskFactor> list = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(stored.getReasons() == null ? "{}" : stored.getReasons());
            JsonNode factors = root.path("factors");
            if (factors.isObject()) {
                factors.fields().forEachRemaining(entry -> list.add(new RiskFactor(
                        entry.getKey(),
                        entry.getValue().decimalValue(),
                        "from risk engine " + stored.getFormulaVersion())));
            }
        } catch (Exception ignored) {
            // fall through to column-level factors
        }
        if (list.isEmpty()) {
            list.add(new RiskFactor("cvss", stored.getTechnicalRisk(), "technical_risk column"));
            list.add(new RiskFactor("assetCriticality", stored.getAssetCriticalityScore(), "asset_criticality_score column"));
            list.add(new RiskFactor("internetExposure", stored.getExposureScore(), "exposure_score column"));
            list.add(new RiskFactor("exploitability", stored.getExploitabilityScore(), "exploitability_score column"));
            list.add(new RiskFactor("activeExploitation", stored.getBusinessImpactScore(), "business_impact_score column"));
        }
        return List.copyOf(list);
    }

    private String explanation(RiskAssessment stored) {
        try {
            JsonNode root = objectMapper.readTree(stored.getReasons() == null ? "{}" : stored.getReasons());
            if (root.hasNonNull("text")) {
                return root.get("text").asText();
            }
        } catch (Exception ignored) {
            // use raw
        }
        return stored.getReasons();
    }

    public record RiskRunBody(
            String status,
            UUID findingId,
            UUID riskAssessmentId,
            UUID eventId,
            BigDecimal riskScore,
            String riskLevel
    ) {
    }
}
