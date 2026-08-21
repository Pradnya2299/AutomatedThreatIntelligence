package com.threatadvisor.ai.coderemediation;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Explicit Phase 7 lifecycle. PATCH_GENERATED cannot jump to PR_CREATED.
 */
public enum CodeRemediationState {
    DISCOVERING_REPOSITORY,
    REPOSITORY_FOUND,
    ANALYZING_CODE,
    PLAN_CREATED,
    PATCH_GENERATED,
    VALIDATING,
    PATCH_FAILED,
    PATCH_VALIDATED,
    SECURITY_VERIFIED,
    AWAITING_APPROVAL,
    APPROVED,
    REJECTED,
    CHANGES_REQUESTED,
    PR_CREATING,
    PR_CREATED,
    REVIEW_REQUIRED,
    FAILED;

    private static final Map<CodeRemediationState, Set<CodeRemediationState>> ALLOWED = Map.ofEntries(
            Map.entry(DISCOVERING_REPOSITORY, EnumSet.of(REPOSITORY_FOUND, REVIEW_REQUIRED, FAILED)),
            Map.entry(REPOSITORY_FOUND, EnumSet.of(ANALYZING_CODE, REVIEW_REQUIRED, FAILED)),
            Map.entry(ANALYZING_CODE, EnumSet.of(PLAN_CREATED, REVIEW_REQUIRED, FAILED)),
            Map.entry(PLAN_CREATED, EnumSet.of(PATCH_GENERATED, REVIEW_REQUIRED, FAILED)),
            Map.entry(PATCH_GENERATED, EnumSet.of(VALIDATING, PATCH_FAILED, REVIEW_REQUIRED, FAILED)),
            Map.entry(VALIDATING, EnumSet.of(PATCH_VALIDATED, PATCH_FAILED, REVIEW_REQUIRED, FAILED)),
            Map.entry(PATCH_FAILED, EnumSet.of(PATCH_GENERATED, REVIEW_REQUIRED, FAILED)),
            Map.entry(PATCH_VALIDATED, EnumSet.of(SECURITY_VERIFIED, REVIEW_REQUIRED, FAILED)),
            Map.entry(SECURITY_VERIFIED, EnumSet.of(AWAITING_APPROVAL, REVIEW_REQUIRED, FAILED)),
            Map.entry(AWAITING_APPROVAL, EnumSet.of(APPROVED, REJECTED, CHANGES_REQUESTED)),
            Map.entry(APPROVED, EnumSet.of(PR_CREATING, PR_CREATED, FAILED)),
            Map.entry(PR_CREATING, EnumSet.of(PR_CREATED, FAILED, REVIEW_REQUIRED)),
            Map.entry(REJECTED, EnumSet.noneOf(CodeRemediationState.class)),
            Map.entry(CHANGES_REQUESTED, EnumSet.noneOf(CodeRemediationState.class)),
            Map.entry(PR_CREATED, EnumSet.noneOf(CodeRemediationState.class)),
            Map.entry(REVIEW_REQUIRED, EnumSet.noneOf(CodeRemediationState.class)),
            Map.entry(FAILED, EnumSet.noneOf(CodeRemediationState.class))
    );

    public boolean canTransitionTo(CodeRemediationState next) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(next);
    }

    public boolean isTerminal() {
        return this == REJECTED || this == CHANGES_REQUESTED || this == PR_CREATED
                || this == REVIEW_REQUIRED || this == FAILED;
    }

    public boolean isOpen() {
        return !isTerminal();
    }
}
