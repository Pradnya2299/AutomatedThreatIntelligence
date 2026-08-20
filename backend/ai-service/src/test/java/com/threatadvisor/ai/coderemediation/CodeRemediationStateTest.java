package com.threatadvisor.ai.coderemediation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeRemediationStateTest {

    @Test
    void patchGeneratedCannotSkipToPullRequest() {
        assertFalse(CodeRemediationState.PATCH_GENERATED.canTransitionTo(CodeRemediationState.PR_CREATED));
        assertTrue(CodeRemediationState.PATCH_GENERATED.canTransitionTo(CodeRemediationState.VALIDATING));
        assertTrue(CodeRemediationState.AWAITING_APPROVAL.canTransitionTo(CodeRemediationState.APPROVED));
        assertTrue(CodeRemediationState.APPROVED.canTransitionTo(CodeRemediationState.PR_CREATING));
        assertFalse(CodeRemediationState.REJECTED.canTransitionTo(CodeRemediationState.PR_CREATED));
    }
}
