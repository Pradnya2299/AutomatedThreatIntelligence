package com.threatadvisor.api.client;

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
public class AiCodeRemediationClient {

    private final RestClient restClient;

    public AiCodeRemediationClient(@Value("${app.ai-service-url:http://localhost:8084}") String aiServiceUrl) {
        this.restClient = RestClient.builder().baseUrl(aiServiceUrl).build();
    }

    public ResponseEntity<String> start(UUID investigationId, String body, String correlationId) {
        return post("/api/v1/investigations/" + investigationId + "/remediation", body, correlationId);
    }

    public ResponseEntity<String> latest(UUID investigationId, String correlationId) {
        return get("/api/v1/investigations/" + investigationId + "/remediation", correlationId);
    }

    public ResponseEntity<String> get(UUID id, String correlationId) {
        return get("/api/v1/remediations/" + id, correlationId);
    }

    public ResponseEntity<String> diff(UUID id, String correlationId) {
        return get("/api/v1/remediations/" + id + "/diff", correlationId);
    }

    public ResponseEntity<String> validations(UUID id, String correlationId) {
        return get("/api/v1/remediations/" + id + "/validations", correlationId);
    }

    public ResponseEntity<String> pullRequest(UUID id, String correlationId) {
        return get("/api/v1/remediations/" + id + "/pull-request", correlationId);
    }

    public ResponseEntity<String> approve(UUID id, String body, String correlationId) {
        return post("/api/v1/remediations/" + id + "/approve", body, correlationId);
    }

    public ResponseEntity<String> reject(UUID id, String body, String correlationId) {
        return post("/api/v1/remediations/" + id + "/reject", body, correlationId);
    }

    public ResponseEntity<String> requestChanges(UUID id, String body, String correlationId) {
        return post("/api/v1/remediations/" + id + "/request-changes", body, correlationId);
    }

    private ResponseEntity<String> get(String path, String correlationId) {
        try {
            return restClient.get()
                    .uri(path)
                    .header("X-Correlation-Id", correlationId == null ? "" : correlationId)
                    .retrieve()
                    .toEntity(String.class);
        } catch (RestClientResponseException ex) {
            throw map(ex);
        } catch (RestClientException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE", "ai-service is not reachable for code remediation");
        }
    }

    private ResponseEntity<String> post(String path, String body, String correlationId) {
        try {
            return restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Correlation-Id", correlationId == null ? "" : correlationId)
                    .body(body == null || body.isBlank() ? "{}" : body)
                    .retrieve()
                    .toEntity(String.class);
        } catch (RestClientResponseException ex) {
            throw map(ex);
        } catch (RestClientException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE", "ai-service is not reachable for code remediation");
        }
    }

    private static ApiException map(RestClientResponseException ex) {
        if (ex.getStatusCode().value() == 404) {
            return new ApiException(HttpStatus.NOT_FOUND, "REMEDIATION_NOT_FOUND", "Unknown code remediation");
        }
        if (ex.getStatusCode().value() == 409) {
            return new ApiException(HttpStatus.CONFLICT, "CODE_REMEDIATION_CONFLICT",
                    firstNonBlank(readMessage(ex), "Code remediation cannot proceed"));
        }
        return new ApiException(
                HttpStatus.valueOf(ex.getStatusCode().value()),
                "REMEDIATION_UPSTREAM",
                firstNonBlank(readMessage(ex), "ai-service rejected the code remediation request"));
    }

    private static String readMessage(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return ex.getStatusText();
        }
        int msg = body.indexOf("\"message\"");
        if (msg >= 0) {
            int colon = body.indexOf(':', msg);
            int start = body.indexOf('"', colon + 1);
            int end = start >= 0 ? body.indexOf('"', start + 1) : -1;
            if (start >= 0 && end > start) {
                return body.substring(start + 1, end);
            }
        }
        return body.length() > 280 ? body.substring(0, 277) + "..." : body;
    }

    private static String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
