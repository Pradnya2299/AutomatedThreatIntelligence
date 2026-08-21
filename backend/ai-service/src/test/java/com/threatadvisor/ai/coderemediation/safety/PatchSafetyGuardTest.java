package com.threatadvisor.ai.coderemediation.safety;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatchSafetyGuardTest {

    @Test
    void rejectsSecretsProtectedFilesAndOversizePatches() {
        List<String> secret = PatchSafetyGuard.violations(
                List.of(new PatchSafetyGuard.FileChange("src/App.java", "", "password=\"supersecretvalue\"\n")),
                20,
                400,
                PatchSafetyGuard.DEFAULT_EXTENSIONS);
        assertTrue(secret.stream().anyMatch(v -> v.contains("secret") || v.contains("credential")));

        List<String> env = PatchSafetyGuard.violations(
                List.of(new PatchSafetyGuard.FileChange(".env", "", "x=1\n")),
                20,
                400,
                PatchSafetyGuard.DEFAULT_EXTENSIONS);
        assertTrue(env.stream().anyMatch(v -> v.contains("Protected")));

        List<String> tooMany = PatchSafetyGuard.violations(
                List.of(new PatchSafetyGuard.FileChange("a.xml", "<a/>", "<b/>")),
                0,
                400,
                PatchSafetyGuard.DEFAULT_EXTENSIONS);
        assertTrue(tooMany.stream().anyMatch(v -> v.contains("AI_MAX_CHANGED_FILES")));
        assertFalse(PatchSafetyGuard.containsSecret("<version>2.17.1</version>"));
    }
}
