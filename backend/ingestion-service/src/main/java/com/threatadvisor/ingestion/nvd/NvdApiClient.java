package com.threatadvisor.ingestion.nvd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threatadvisor.ingestion.config.NvdProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
public class NvdApiClient {

    private static final Logger log = LoggerFactory.getLogger(NvdApiClient.class);
    private static final DateTimeFormatter NVD_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
            .withZone(ZoneOffset.UTC);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final NvdProperties properties;
    private final Object rateLock = new Object();
    private Instant lastRequestAt = Instant.EPOCH;

    public NvdApiClient(RestClient.Builder restClientBuilder, ObjectMapper objectMapper, NvdProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.getApiBaseUrl())
                .build();
    }

    public Optional<JsonNode> getCve(String cveId) {
        URI uri = UriComponentsBuilder.fromUriString(properties.getApiBaseUrl())
                .queryParam("cveId", cveId)
                .build(true)
                .toUri();
        JsonNode body = get(uri);
        JsonNode vulns = body.path("vulnerabilities");
        if (!vulns.isArray() || vulns.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(body);
    }

    public NvdCvePage getModifiedSince(Instant start, Instant end, int startIndex) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(properties.getApiBaseUrl())
                .queryParam("lastModStartDate", NVD_TIME.format(start))
                .queryParam("lastModEndDate", NVD_TIME.format(end))
                .queryParam("startIndex", startIndex)
                .queryParam("resultsPerPage", properties.getResultsPerPage());
        JsonNode body = get(builder.build(true).toUri());
        return new NvdCvePage(
                body.path("startIndex").asInt(startIndex),
                body.path("resultsPerPage").asInt(properties.getResultsPerPage()),
                body.path("totalResults").asInt(0),
                body);
    }

    private JsonNode get(URI uri) {
        throttle();
        try {
            RestClient.RequestHeadersSpec<?> spec = restClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON);
            if (properties.hasApiKey()) {
                spec = spec.header("apiKey", properties.getApiKey());
            }
            String raw = spec.retrieve().body(String.class);
            if (raw == null || raw.isBlank()) {
                throw new NvdApiException("NVD_EMPTY", HttpStatus.BAD_GATEWAY, "NVD returned an empty body");
            }
            try {
                return objectMapper.readTree(raw);
            } catch (Exception ex) {
                throw new NvdApiException("NVD_INVALID_JSON", HttpStatus.BAD_GATEWAY, "NVD returned invalid JSON", ex);
            }
        } catch (NvdApiException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new NvdApiException("NVD_NOT_FOUND", HttpStatus.NOT_FOUND, "CVE not found in NVD");
            }
            throw new NvdApiException(
                    "NVD_UNAVAILABLE",
                    HttpStatus.BAD_GATEWAY,
                    "NVD request failed with HTTP " + ex.getStatusCode().value(),
                    ex);
        } catch (RestClientException ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
            boolean timeout = message.contains("timeout") || message.contains("timed out");
            throw new NvdApiException(
                    timeout ? "NVD_TIMEOUT" : "NVD_UNAVAILABLE",
                    HttpStatus.GATEWAY_TIMEOUT,
                    timeout ? "NVD request timed out" : "NVD is unavailable",
                    ex);
        }
    }

    private void throttle() {
        long waitMs = properties.minRequestIntervalMs();
        synchronized (rateLock) {
            Instant now = Instant.now();
            long elapsed = Duration.between(lastRequestAt, now).toMillis();
            if (elapsed < waitMs) {
                try {
                    Thread.sleep(waitMs - elapsed);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new NvdApiException("NVD_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "Interrupted while rate limiting", ex);
                }
            }
            lastRequestAt = Instant.now();
            log.debug("operation=nvd.request");
        }
    }
}
