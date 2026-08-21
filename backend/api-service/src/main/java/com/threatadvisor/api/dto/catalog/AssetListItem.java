package com.threatadvisor.api.dto.catalog;

import java.util.UUID;

public record AssetListItem(
        UUID id,
        String hostname,
        String environment,
        String operatingSystem,
        String criticality,
        boolean internetExposure,
        String installedSoftware,
        long openFindings
) {
}
