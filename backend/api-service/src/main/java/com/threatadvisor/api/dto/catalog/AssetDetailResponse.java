package com.threatadvisor.api.dto.catalog;

import java.util.List;
import java.util.UUID;

public record AssetDetailResponse(
        UUID id,
        String hostname,
        String environment,
        String operatingSystem,
        String osVersion,
        String criticality,
        boolean internetExposure,
        String status,
        List<SoftwareRow> installedSoftware,
        List<FindingListItem> findings
) {
}
