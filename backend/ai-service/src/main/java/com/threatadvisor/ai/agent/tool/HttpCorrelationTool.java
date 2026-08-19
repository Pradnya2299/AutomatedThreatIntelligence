package com.threatadvisor.ai.agent.tool;

import com.threatadvisor.ai.agent.common.AgentToolException;
import com.threatadvisor.ai.agent.dto.CorrelationToolResult;
import com.threatadvisor.ai.config.AiProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class HttpCorrelationTool implements CorrelationTool {

    private final RestClient restClient;

    public HttpCorrelationTool(AiProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.getCorrelationBaseUrl())
                .build();
    }

    @Override
    public CorrelationToolResult correlate(String cveId, UUID correlationId) {
        try {
            CorrelationRunBody body = restClient.post()
                    .uri("/internal/correlation/run/{cveId}", cveId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Correlation-Id", correlationId == null ? UUID.randomUUID().toString() : correlationId.toString())
                    .retrieve()
                    .body(CorrelationRunBody.class);
            if (body == null) {
                throw new AgentToolException("CORRELATION_EMPTY", "Correlation engine returned an empty body");
            }
            return new CorrelationToolResult(
                    body.status(),
                    body.cveId(),
                    body.vulnerabilityId(),
                    body.eventId(),
                    body.findingsCreated(),
                    body.findingsUpdated(),
                    body.matchesEvaluated());
        } catch (AgentToolException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new AgentToolException(
                    "CORRELATION_UNAVAILABLE",
                    "Correlation engine call failed; exposure is unknown (not 'not affected')",
                    ex);
        }
    }

    public record CorrelationRunBody(
            String status,
            String cveId,
            UUID vulnerabilityId,
            UUID eventId,
            int findingsCreated,
            int findingsUpdated,
            int matchesEvaluated
    ) {
    }
}
