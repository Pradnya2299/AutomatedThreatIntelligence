package com.threatadvisor.ingestion.dto;

import java.util.UUID;

public record NvdLookupResponse(
        String cveId,
        String status,
        String intelligenceSource,
        UUID vulnerabilityId,
        String message
) {
}
