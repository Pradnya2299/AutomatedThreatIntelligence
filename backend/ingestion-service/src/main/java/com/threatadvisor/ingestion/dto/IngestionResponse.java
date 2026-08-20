package com.threatadvisor.ingestion.dto;

import java.util.UUID;

public record IngestionResponse(String cveId, String status, UUID eventId, UUID correlationId) {
}
