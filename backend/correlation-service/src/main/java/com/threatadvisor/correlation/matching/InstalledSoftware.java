package com.threatadvisor.correlation.matching;

import java.util.UUID;

public record InstalledSoftware(
        UUID assetId,
        UUID organizationId,
        String hostname,
        String vendor,
        String product,
        String version,
        String cpe
) {
}
