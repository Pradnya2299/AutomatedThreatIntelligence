package com.threatadvisor.api.dto.catalog;

import java.util.UUID;

public record CpeRangeDto(
        UUID id,
        String cpe,
        String vendor,
        String product,
        String versionStartIncluding,
        String versionEndExcluding
) {
}
