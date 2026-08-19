package com.threatadvisor.ai.coderemediation.git;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalWorkspaceGitProviderTest {

    @Test
    void refusesProtectedBranches() {
        assertThrows(IllegalArgumentException.class, () -> LocalWorkspaceGitProvider.assertSafeBranch("main"));
        assertThrows(IllegalArgumentException.class, () -> LocalWorkspaceGitProvider.assertSafeBranch("master"));
        assertThrows(IllegalArgumentException.class, () -> LocalWorkspaceGitProvider.assertSafeBranch("production"));
        assertThrows(IllegalArgumentException.class, () -> LocalWorkspaceGitProvider.assertSafeBranch("release/1.0"));
        assertThrows(IllegalArgumentException.class, () -> LocalWorkspaceGitProvider.assertSafeBranch("feature/cve"));
        assertDoesNotThrow(() -> LocalWorkspaceGitProvider.assertSafeBranch("ai-security/CVE-2021-44228-abcd1234"));
    }
}
