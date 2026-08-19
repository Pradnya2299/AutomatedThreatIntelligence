package com.threatadvisor.ai.agent.tool;

import com.threatadvisor.ai.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class IngestionNvdGateway {

    private static final Logger log = LoggerFactory.getLogger(IngestionNvdGateway.class);

    private final RestClient restClient;
    private final boolean enabled;

    public IngestionNvdGateway(AiProperties properties) {
        this.enabled = properties.isNvdLookupEnabled();
        this.restClient = RestClient.builder()
                .baseUrl(properties.getIngestionBaseUrl())
                .build();
    }

    public NvdLookupBody refresh(String cveId) {
        if (!enabled) {
            return null;
        }
        try {
            return restClient.post()
                    .uri("/internal/ingestion/nvd/cves/{cveId}", cveId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(NvdLookupBody.class);
        } catch (RestClientException ex) {
            log.warn("operation=nvd.lookup.unavailable cveId={}", cveId);
            return null;
        }
    }

    public record NvdLookupBody(
            String cveId,
            String status,
            String intelligenceSource,
            UUID vulnerabilityId,
            String message
    ) {
    }
}
