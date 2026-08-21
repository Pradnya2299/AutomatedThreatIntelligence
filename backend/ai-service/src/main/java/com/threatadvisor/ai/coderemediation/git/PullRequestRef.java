package com.threatadvisor.ai.coderemediation.git;

public record PullRequestRef(
        String provider,
        String repository,
        String branch,
        String commitSha,
        String pullRequestUrl,
        Integer pullRequestNumber,
        String skippedReason
) {
    public static PullRequestRef skipped(String provider, String repository, String branch, String reason) {
        return new PullRequestRef(provider, repository, branch, null, null, null, reason);
    }
}
