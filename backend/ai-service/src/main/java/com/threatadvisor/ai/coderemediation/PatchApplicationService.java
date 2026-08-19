package com.threatadvisor.ai.coderemediation;

import com.threatadvisor.ai.coderemediation.git.GitProvider;
import com.threatadvisor.ai.coderemediation.llm.ContentHashes;
import com.threatadvisor.ai.coderemediation.llm.GeneratedPatch;
import com.threatadvisor.ai.coderemediation.patch.UnifiedDiffBuilder;
import com.threatadvisor.ai.coderemediation.safety.PatchSafetyGuard;
import com.threatadvisor.ai.config.AiProperties;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class PatchApplicationService {

    public record AppliedPatch(
            String unifiedDiff,
            List<PatchSafetyGuard.FileChange> fileChanges,
            List<String> violations,
            int added,
            int deleted
    ) {
        public boolean safe() {
            return violations.isEmpty();
        }
    }

    private final GitProvider gitProvider;
    private final AiProperties properties;

    public PatchApplicationService(GitProvider gitProvider, AiProperties properties) {
        this.gitProvider = gitProvider;
        this.properties = properties;
    }

    public AppliedPatch apply(Path workspace, GeneratedPatch patch, Set<String> allowedPaths) {
        List<PatchSafetyGuard.FileChange> changes = new ArrayList<>();
        StringBuilder diff = new StringBuilder();
        int added = 0;
        int deleted = 0;
        List<String> violations = new ArrayList<>();
        if (patch == null || patch.getFiles() == null || patch.getFiles().isEmpty()) {
            violations.add("Generated patch contained no files");
            return new AppliedPatch("", List.of(), violations, 0, 0);
        }
        for (GeneratedPatch.FilePatch file : patch.getFiles()) {
            if (file.getPath() == null || file.getPath().contains("..") || file.getPath().startsWith("/")) {
                violations.add("Illegal path: " + file.getPath());
                continue;
            }
            if (allowedPaths != null && !allowedPaths.contains(file.getPath())) {
                violations.add("Unexpected file outside plan/context: " + file.getPath());
                continue;
            }
            String before = gitProvider.getFile(workspace, file.getPath());
            if (before == null) {
                before = "";
            }
            String expectedHash = ContentHashes.sha256(before);
            if (file.getOriginalContentHash() == null || !expectedHash.equalsIgnoreCase(file.getOriginalContentHash())) {
                violations.add("Content hash mismatch for " + file.getPath());
                continue;
            }
            String after = file.getProposedContent() == null ? "" : file.getProposedContent();
            if (before.equals(after)) {
                continue;
            }
            gitProvider.applyPatch(workspace, file.getPath(), after);
            changes.add(new PatchSafetyGuard.FileChange(file.getPath(), before, after));
            diff.append(UnifiedDiffBuilder.build(file.getPath(), before, after)).append('\n');
            added += UnifiedDiffBuilder.added(before, after);
            deleted += UnifiedDiffBuilder.deleted(before, after);
        }
        violations.addAll(PatchSafetyGuard.violations(
                changes,
                properties.getCodeRemediation().getMaxChangedFiles(),
                properties.getCodeRemediation().getMaxChangedLines(),
                PatchSafetyGuard.DEFAULT_EXTENSIONS));
        return new AppliedPatch(diff.toString(), changes, violations, added, deleted);
    }
}
