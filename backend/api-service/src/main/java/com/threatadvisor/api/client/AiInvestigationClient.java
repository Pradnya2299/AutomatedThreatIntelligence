package com.threatadvisor.api.client;

import com.threatadvisor.api.dto.InvestigationCreateRequest;
import com.threatadvisor.api.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Component
public class AiInvestigationClient {

    private final RestClient restClient;

    public AiInvestigationClient(@Value("${app.ai-service-url:http://localhost:8084}") String aiServiceUrl) {
        this.restClient = RestClient.builder().baseUrl(aiServiceUrl).build();
    }

    public ResponseEntity<String> create(InvestigationCreateRequest request, String correlationId) {
        try {
            return restClient.post()
                    .uri("/api/v1/investigations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Correlation-Id", correlationId == null ? "" : correlationId)
                    .body(request)
                    .retrieve()
                    .toEntity(String.class);
        } catch (RestClientResponseException ex) {
            throw new ApiException(
                    HttpStatus.valueOf(ex.getStatusCode().value()),
                    "INVESTIGATION_UPSTREAM",
                    ex.getResponseBodyAsString().isBlank() ? ex.getStatusText() : "ai-service rejected the investigation");
        } catch (RestClientException ex) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI_SERVICE_UNAVAILABLE",
                    "ai-service is not reachable for investigations");
        }
    }

    public ResponseEntity<String> get(UUID id, String correlationId) {
        try {
            return restClient.get()
                    .uri("/api/v1/investigations/{id}", id)
                    .header("X-Correlation-Id", correlationId == null ? "" : correlationId)
                    .retrieve()
                    .toEntity(String.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ApiException(HttpStatus.NOT_FOUND, "INVESTIGATION_NOT_FOUND", "Unknown investigation");
            }
            throw new ApiException(
                    HttpStatus.valueOf(ex.getStatusCode().value()),
                    "INVESTIGATION_UPSTREAM",
                    "ai-service rejected the investigation lookup");
        } catch (RestClientException ex) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI_SERVICE_UNAVAILABLE",
                    "ai-service is not reachable for investigations");
        }
    }
}
