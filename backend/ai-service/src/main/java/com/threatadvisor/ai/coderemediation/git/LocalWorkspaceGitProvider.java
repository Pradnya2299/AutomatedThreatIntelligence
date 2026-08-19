package com.threatadvisor.ai.coderemediation.git;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Isolated workspace Git operations. Never mutates a remote when GitHub is disabled.
 */
public class LocalWorkspaceGitProvider implements GitProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalWorkspaceGitProvider.class);

    @Override
    public String name() {
        return "LOCAL_WORKSPACE";
    }

    @Override
    public boolean remoteMutationsEnabled() {
        return false;
    }

    @Override
    public Optional<RemoteRepository> getRepository(String organization, String repository) {
        return Optional.empty();
    }

    @Override
    public String getDefaultBranch(RemoteRepository repository) {
        return repository == null || repository.defaultBranch() == null ? "main" : repository.defaultBranch();
    }

    @Override
    public void createBranch(Path workspace, String baseBranch, String newBranch) {
        assertSafeBranch(newBranch);
        try {
            if (!Files.isDirectory(workspace.resolve(".git"))) {
                run(workspace, "git", "init", "-b", newBranch);
                run(workspace, "git", "add", "-A");
                run(workspace, "git", "-c", "user.email=ai-security@local", "-c", "user.name=AI Security",
                        "commit", "-m", "chore: isolated workspace snapshot", "--allow-empty");
                return;
            }
            run(workspace, "git", "checkout", "-B", newBranch);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to create isolated branch " + newBranch, ex);
        }
    }

    @Override
    public String getFile(Path workspace, String path) {
        try {
            Path resolved = workspace.resolve(path).normalize();
            if (!resolved.startsWith(workspace.normalize())) {
                throw new IllegalArgumentException("Path escapes workspace");
            }
            if (!Files.exists(resolved) || Files.isDirectory(resolved)) {
                return null;
            }
            return Files.readString(resolved);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read " + path, ex);
        }
    }

    @Override
    public List<String> searchCode(Path workspace, String query) {
        List<String> hits = new ArrayList<>();
        if (query == null || query.isBlank()) {
            return hits;
        }
        try (Stream<Path> walk = Files.walk(workspace)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> !p.toString().contains("/.git/"))
                    .forEach(p -> {
                        try {
                            String text = Files.readString(p);
                            if (text.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) {
                                hits.add(workspace.relativize(p).toString().replace('\\', '/'));
                            }
                        } catch (IOException ignored) {
                            // skip unreadable / binary
                        }
                    });
        } catch (IOException ex) {
            throw new IllegalStateException("Workspace search failed", ex);
        }
        hits.sort(Comparator.naturalOrder());
        return hits;
    }

    @Override
    public void applyPatch(Path workspace, String relativePath, String content) {
        try {
            Path resolved = workspace.resolve(relativePath).normalize();
            if (!resolved.startsWith(workspace.normalize())) {
                throw new IllegalArgumentException("Path escapes workspace");
            }
            Files.createDirectories(resolved.getParent() == null ? workspace : resolved.getParent());
            Files.writeString(resolved, content, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to write " + relativePath, ex);
        }
    }

    @Override
    public String getDiff(Path workspace, String baseBranch) {
        try {
            if (!Files.isDirectory(workspace.resolve(".git"))) {
                return "";
            }
            return run(workspace, "git", "diff", "--cached") + run(workspace, "git", "diff");
        } catch (Exception ex) {
            log.warn("git diff unavailable: {}", ex.getMessage());
            return "";
        }
    }

    @Override
    public String commitChanges(Path workspace, String message) {
        try {
            run(workspace, "git", "add", "-A");
            run(workspace, "git", "-c", "user.email=ai-security@local", "-c", "user.name=AI Security",
                    "commit", "-m", message);
            return run(workspace, "git", "rev-parse", "HEAD").trim();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to commit isolated patch", ex);
        }
    }

    @Override
    public Optional<PullRequestRef> createPullRequest(PullRequestRequest request) {
        return Optional.of(PullRequestRef.skipped(
                name(),
                request.repository(),
                request.headBranch(),
                "GITHUB_DISABLED"));
    }

    @Override
    public void pushBranch(Path workspace, String branch) {
        throw new IllegalStateException("Remote push is disabled while GITHUB_ENABLED=false");
    }

    public static void assertSafeBranch(String branch) {
        if (branch == null || branch.isBlank()) {
            throw new IllegalArgumentException("Branch is required");
        }
        String lower = branch.toLowerCase(Locale.ROOT);
        if (lower.equals("main") || lower.equals("master") || lower.equals("production")
                || lower.startsWith("release") || lower.startsWith("prod")) {
            throw new IllegalArgumentException("Refusing to use protected branch: " + branch);
        }
        if (!lower.startsWith("ai-security/")) {
            throw new IllegalArgumentException("AI branches must start with ai-security/");
        }
    }

    private String run(Path workspace, String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workspace.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int code = process.waitFor();
        if (code != 0) {
            throw new IllegalStateException(String.join(" ", command) + " exited " + code + ": " + output);
        }
        return output;
    }
}
