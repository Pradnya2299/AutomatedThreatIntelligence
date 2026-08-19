package com.threatadvisor.ingestion.nvd;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record NvdCvePage(
        int startIndex,
        int resultsPerPage,
        int totalResults,
        JsonNode raw
) {
    public boolean hasMore() {
        return startIndex + resultsPerPage < totalResults;
    }
}
