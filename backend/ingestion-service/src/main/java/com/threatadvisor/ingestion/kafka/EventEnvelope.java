package com.threatadvisor.ingestion.kafka;

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
    public static final String SOURCE_SERVICE = "ingestion-service";
    public static final String TYPE_RAW = "cve.raw";
    public static final String TYPE_NORMALIZED = "cve.normalized";
}
