package com.threatadvisor.ai.coderemediation.git;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Git host abstraction. Agents depend on this, not on GitHub APIs.
 */
public interface GitProvider {

    String name();

    boolean remoteMutationsEnabled();

    Optional<RemoteRepository> getRepository(String organization, String repository);

    String getDefaultBranch(RemoteRepository repository);

    void createBranch(Path workspace, String baseBranch, String newBranch);

    String getFile(Path workspace, String path);

    List<String> searchCode(Path workspace, String query);

    void applyPatch(Path workspace, String relativePath, String content);

    String getDiff(Path workspace, String baseBranch);

    String commitChanges(Path workspace, String message);

    Optional<PullRequestRef> createPullRequest(PullRequestRequest request);

    void pushBranch(Path workspace, String branch);
}
