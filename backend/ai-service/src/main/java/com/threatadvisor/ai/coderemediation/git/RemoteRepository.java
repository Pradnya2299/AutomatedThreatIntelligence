package com.threatadvisor.ai.coderemediation.git;

public record RemoteRepository(
        String provider,
        String organization,
        String name,
        String defaultBranch,
        String cloneUrl
) {
}
