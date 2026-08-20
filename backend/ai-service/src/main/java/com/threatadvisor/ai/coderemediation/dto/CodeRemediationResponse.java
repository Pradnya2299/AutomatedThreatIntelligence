package com.threatadvisor.ai.coderemediation.dto;

import com.threatadvisor.ai.coderemediation.model.CodeFinding;
import com.threatadvisor.ai.coderemediation.model.RemediationStrategy;
import com.threatadvisor.ai.coderemediation.model.RepositoryTarget;
import com.threatadvisor.ai.coderemediation.validation.ValidationOutcome;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CodeRemediationResponse(
        UUID remediationId,
        UUID investigationId,
        String cveId,
        String status,
        String currentState,
        String confidence,
        String reviewReason,
        int attemptCount,
        Instant createdAt,
        Instant updatedAt,
        RepositoryTarget repository,
        List<CodeFinding> codeFindings,
        RemediationStrategy strategy,
        PatchPlanView plan,
        PatchView patch,
        List<ValidationOutcome> validations,
        VerificationView securityVerification,
        ApprovalView approval,
        PullRequestView pullRequest,
        boolean githubEnabled,
        String intelligenceMode,
        String intelligenceSource,
        String modelName,
        String promptVersion,
        Object codeAnalysis,
        Object llmPatchPlan
) {
    public record PatchPlanView(
            UUID id,
            String strategyType,
            String rationale,
            List<String> affectedFiles,
            List<String> expectedChanges,
            List<String> prerequisites,
            List<String> risks,
            String rollbackPlan,
            String confidence
    ) {
    }

    public record PatchView(
            UUID id,
            int attemptNumber,
            String status,
            String unifiedDiff,
            int filesChanged,
            int linesAdded,
            int linesDeleted,
            String safetyStatus,
            List<String> safetyViolations,
            List<FileChangeView> files
    ) {
    }

    public record FileChangeView(String file, String changeType, String beforeExcerpt, String afterExcerpt) {
    }

    public record VerificationView(String result, String details, List<String> evidence) {
    }

    public record ApprovalView(String status, String decidedBy, String comment, Instant decidedAt) {
    }

    public record PullRequestView(
            String provider,
            String repository,
            String branch,
            String commitSha,
            String pullRequestUrl,
            Integer pullRequestNumber,
            String skippedReason
    ) {
    }
}
