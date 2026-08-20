package com.threatadvisor.ai.coderemediation.dto;

import jakarta.validation.constraints.Size;

public record CodeRemediationRequest(
        @Size(max = 128) String initiatedBy,
        @Size(max = 32) String provider,
        @Size(max = 255) String organization,
        @Size(max = 255) String repository,
        @Size(max = 1024) String repositoryUrl,
        @Size(max = 128) String defaultBranch
) {
}
