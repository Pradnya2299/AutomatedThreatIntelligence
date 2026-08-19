package com.threatadvisor.ai.coderemediation.model;

import com.threatadvisor.ai.agent.common.Confidence;

import java.util.List;

public record RepositoryTarget(
        String provider,
        String organization,
        String repository,
        String defaultBranch,
        String technology,
        String buildSystem,
        String repositoryUrl,
        String aiBranch,
        String workspacePath,
        Confidence confidence,
        List<String> evidence
) {
}
