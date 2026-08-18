package com.threatadvisor.ai.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventEnvelope(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant timestamp,
        String source,
        UUID correlationId,
        JsonNode payload
) {
    public static final String SOURCE_SERVICE = "ai-service";
    public static final String TYPE_RISK_CALCULATED = "risk.calculated";
    public static final String TYPE_REMEDIATION_GENERATED = "remediation.generated";
}
