package com.threatadvisor.ai.coderemediation.git;

public record PullRequestRequest(
        String organization,
        String repository,
        String title,
        String body,
        String headBranch,
        String baseBranch
) {
}
