package com.threatadvisor.ai.coderemediation.git;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threatadvisor.ai.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * GitHub REST adapter. Remote mutations run only when {@code GITHUB_ENABLED=true} and a token is set.
 */
public class GitHubGitProvider implements GitProvider {

    private static final Logger log = LoggerFactory.getLogger(GitHubGitProvider.class);

    private final AiProperties properties;
    private final LocalWorkspaceGitProvider workspace = new LocalWorkspaceGitProvider();
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GitHubGitProvider(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getGit().getApiUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getGit().getToken())
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    @Override
    public String name() {
        return "GITHUB";
    }

    @Override
    public boolean remoteMutationsEnabled() {
        return properties.getGit().isEnabled()
                && properties.getGit().getToken() != null
                && !properties.getGit().getToken().isBlank();
    }

    @Override
    public Optional<RemoteRepository> getRepository(String organization, String repository) {
        if (!remoteMutationsEnabled()) {
            return Optional.empty();
        }
        try {
            String body = restClient.get()
                    .uri("/repos/{org}/{repo}", organization, repository)
                    .retrieve()
                    .body(String.class);
            JsonNode node = objectMapper.readTree(body);
            return Optional.of(new RemoteRepository(
                    name(),
                    organization,
                    repository,
                    node.path("default_branch").asText("main"),
                    node.path("clone_url").asText(null)));
        } catch (Exception ex) {
            log.warn("GitHub getRepository failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public String getDefaultBranch(RemoteRepository repository) {
        return workspace.getDefaultBranch(repository);
    }

    @Override
    public void createBranch(Path workspacePath, String baseBranch, String newBranch) {
        workspace.createBranch(workspacePath, baseBranch, newBranch);
    }

    @Override
    public String getFile(Path workspacePath, String path) {
        return workspace.getFile(workspacePath, path);
    }

    @Override
    public List<String> searchCode(Path workspacePath, String query) {
        return workspace.searchCode(workspacePath, query);
    }

    @Override
    public void applyPatch(Path workspacePath, String relativePath, String content) {
        workspace.applyPatch(workspacePath, relativePath, content);
    }

    @Override
    public String getDiff(Path workspacePath, String baseBranch) {
        return workspace.getDiff(workspacePath, baseBranch);
    }

    @Override
    public String commitChanges(Path workspacePath, String message) {
        return workspace.commitChanges(workspacePath, message);
    }

    @Override
    public Optional<PullRequestRef> createPullRequest(PullRequestRequest request) {
        if (!remoteMutationsEnabled()) {
            return Optional.of(PullRequestRef.skipped(name(), request.repository(), request.headBranch(), "GITHUB_DISABLED"));
        }
        try {
            String body = restClient.post()
                    .uri("/repos/{org}/{repo}/pulls", request.organization(), request.repository())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "title", request.title(),
                            "body", request.body(),
                            "head", request.headBranch(),
                            "base", request.baseBranch()))
                    .retrieve()
                    .body(String.class);
            JsonNode node = objectMapper.readTree(body);
            return Optional.of(new PullRequestRef(
                    name(),
                    request.repository(),
                    request.headBranch(),
                    null,
                    node.path("html_url").asText(null),
                    node.path("number").isInt() ? node.path("number").asInt() : null,
                    null));
        } catch (Exception ex) {
            throw new IllegalStateException("GitHub pull request creation failed", ex);
        }
    }

    @Override
    public void pushBranch(Path workspacePath, String branch) {
        if (!remoteMutationsEnabled()) {
            throw new IllegalStateException("Remote push is disabled while GITHUB_ENABLED=false");
        }
        LocalWorkspaceGitProvider.assertSafeBranch(branch);
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "push", "-u", "origin", branch);
            pb.directory(workspacePath.toFile());
            pb.environment().put("GIT_TERMINAL_PROMPT", "0");
            Process process = pb.start();
            byte[] out = process.getInputStream().readAllBytes();
            byte[] err = process.getErrorStream().readAllBytes();
            int code = process.waitFor();
            if (code != 0) {
                throw new IllegalStateException("git push exited " + code + " (output redacted)");
            }
            if (out.length == 0 && err.length == 0) {
                log.info("git push completed for isolated branch");
            }
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("git push failed", ex);
        }
    }
}
