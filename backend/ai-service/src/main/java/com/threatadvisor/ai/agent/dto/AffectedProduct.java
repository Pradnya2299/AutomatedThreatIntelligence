package com.threatadvisor.ai.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AffectedProduct(
        String vendor,
        String product,
        String versionStartIncluding,
        String versionEndExcluding,
        String cpe
) {
}
