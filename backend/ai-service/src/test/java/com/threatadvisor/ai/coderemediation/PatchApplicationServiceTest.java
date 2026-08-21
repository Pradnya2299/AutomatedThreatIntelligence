package com.threatadvisor.ai.coderemediation;

import com.threatadvisor.ai.coderemediation.git.LocalWorkspaceGitProvider;
import com.threatadvisor.ai.coderemediation.llm.ContentHashes;
import com.threatadvisor.ai.coderemediation.llm.GeneratedPatch;
import com.threatadvisor.ai.config.AiProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatchApplicationServiceTest {

    @TempDir
    Path workspace;

    @Test
    void rejectsHashMismatchAndPathTraversal() throws Exception {
        Files.writeString(workspace.resolve("pom.xml"), "<project/>");
        PatchApplicationService service = new PatchApplicationService(new LocalWorkspaceGitProvider(), new AiProperties());
        GeneratedPatch patch = new GeneratedPatch();
        patch.setSummary("x");
        patch.setConfidence("HIGH");
        GeneratedPatch.FilePatch file = new GeneratedPatch.FilePatch();
        file.setPath("pom.xml");
        file.setOperation("MODIFY");
        file.setOriginalContentHash("deadbeef");
        file.setProposedContent("<project>patched</project>");
        patch.setFiles(List.of(file));
        var applied = service.apply(workspace, patch, Set.of("pom.xml"));
        assertFalse(applied.safe());
        assertTrue(applied.violations().stream().anyMatch(v -> v.contains("hash")));

        GeneratedPatch.FilePatch traversal = new GeneratedPatch.FilePatch();
        traversal.setPath("../secrets.env");
        traversal.setOperation("MODIFY");
        traversal.setOriginalContentHash(ContentHashes.sha256(""));
        traversal.setProposedContent("x");
        patch.setFiles(List.of(traversal));
        var illegal = service.apply(workspace, patch, Set.of("pom.xml"));
        assertTrue(illegal.violations().stream().anyMatch(v -> v.toLowerCase().contains("path") || v.contains("outside")));
    }

    @Test
    void appliesWhenHashMatches() throws Exception {
        String original = "<project><version>1</version></project>\n";
        Files.writeString(workspace.resolve("pom.xml"), original);
        PatchApplicationService service = new PatchApplicationService(new LocalWorkspaceGitProvider(), new AiProperties());
        GeneratedPatch patch = new GeneratedPatch();
        patch.setSummary("ok");
        patch.setConfidence("HIGH");
        GeneratedPatch.FilePatch file = new GeneratedPatch.FilePatch();
        file.setPath("pom.xml");
        file.setOperation("MODIFY");
        file.setOriginalContentHash(ContentHashes.sha256(original));
        file.setProposedContent("<project><version>2</version></project>\n");
        patch.setFiles(List.of(file));
        var applied = service.apply(workspace, patch, Set.of("pom.xml"));
        assertTrue(applied.safe());
        assertTrue(applied.unifiedDiff().contains("2"));
    }
}
