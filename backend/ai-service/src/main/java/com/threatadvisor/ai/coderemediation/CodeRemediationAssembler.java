package com.threatadvisor.ai.coderemediation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threatadvisor.ai.agent.common.Confidence;
import com.threatadvisor.ai.coderemediation.domain.ApprovalRequestEntity;
import com.threatadvisor.ai.coderemediation.domain.CodePullRequestEntity;
import com.threatadvisor.ai.coderemediation.domain.CodeRemediationAuditEntity;
import com.threatadvisor.ai.coderemediation.domain.CodeRemediationJobEntity;
import com.threatadvisor.ai.coderemediation.domain.PatchChangeEntity;
import com.threatadvisor.ai.coderemediation.domain.PatchExecutionEntity;
import com.threatadvisor.ai.coderemediation.domain.PatchPlanEntity;
import com.threatadvisor.ai.coderemediation.domain.RemediationTargetEntity;
import com.threatadvisor.ai.coderemediation.domain.SecurityVerificationEntity;
import com.threatadvisor.ai.coderemediation.domain.ValidationRunEntity;
import com.threatadvisor.ai.coderemediation.dto.CodeRemediationResponse;
import com.threatadvisor.ai.coderemediation.model.CodeFinding;
import com.threatadvisor.ai.coderemediation.model.RemediationStrategy;
import com.threatadvisor.ai.coderemediation.model.RepositoryTarget;
import com.threatadvisor.ai.coderemediation.repo.ApprovalRequestRepository;
import com.threatadvisor.ai.coderemediation.repo.CodePullRequestRepository;
import com.threatadvisor.ai.coderemediation.repo.CodeRemediationAuditRepository;
import com.threatadvisor.ai.coderemediation.repo.CodeRemediationJobRepository;
import com.threatadvisor.ai.coderemediation.repo.PatchChangeRepository;
import com.threatadvisor.ai.coderemediation.repo.PatchExecutionRepository;
import com.threatadvisor.ai.coderemediation.repo.PatchPlanRepository;
import com.threatadvisor.ai.coderemediation.repo.RemediationTargetRepository;
import com.threatadvisor.ai.coderemediation.repo.SecurityVerificationRepository;
import com.threatadvisor.ai.coderemediation.repo.ValidationRunRepository;
import com.threatadvisor.ai.coderemediation.validation.ValidationOutcome;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CodeRemediationAssembler {

    private final ObjectMapper objectMapper;
    private final CodeRemediationJobRepository jobs;
    private final RemediationTargetRepository targets;
    private final PatchPlanRepository plans;
    private final PatchExecutionRepository executions;
    private final PatchChangeRepository changes;
    private final ValidationRunRepository validations;
    private final SecurityVerificationRepository verifications;
    private final ApprovalRequestRepository approvals;
    private final CodePullRequestRepository pullRequests;
    private final CodeRemediationAuditRepository audit;

    public CodeRemediationAssembler(
            ObjectMapper objectMapper,
            CodeRemediationJobRepository jobs,
            RemediationTargetRepository targets,
            PatchPlanRepository plans,
            PatchExecutionRepository executions,
            PatchChangeRepository changes,
            ValidationRunRepository validations,
            SecurityVerificationRepository verifications,
            ApprovalRequestRepository approvals,
            CodePullRequestRepository pullRequests,
            CodeRemediationAuditRepository audit) {
        this.objectMapper = objectMapper;
        this.jobs = jobs;
        this.targets = targets;
        this.plans = plans;
        this.executions = executions;
        this.changes = changes;
        this.validations = validations;
        this.verifications = verifications;
        this.approvals = approvals;
        this.pullRequests = pullRequests;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public Optional<CodeRemediationResponse> load(UUID id, boolean githubEnabled) {
        return jobs.findById(id).map(job -> toResponse(job, githubEnabled));
    }

    @Transactional(readOnly = true)
    public Optional<CodeRemediationResponse> loadLatest(UUID investigationId, boolean githubEnabled) {
        return jobs.findFirstByInvestigationIdOrderByCreatedAtDesc(investigationId)
                .map(job -> toResponse(job, githubEnabled));
    }

    public CodeRemediationResponse toResponse(CodeRemediationJobEntity job, boolean githubEnabled) {
        RepositoryTarget target = targets.findFirstByJobId(job.getId())
                .map(this::toTarget)
                .orElse(null);
        PatchPlanEntity plan = plans.findFirstByJobIdOrderByCreatedAtDesc(job.getId()).orElse(null);
        PatchExecutionEntity execution = executions.findFirstByJobIdOrderByAttemptNumberDesc(job.getId()).orElse(null);
        List<CodeRemediationResponse.FileChangeView> fileViews = new ArrayList<>();
        List<ValidationOutcome> validationViews = List.of();
        CodeRemediationResponse.VerificationView verificationView = null;
        if (execution != null) {
            fileViews = changes.findByPatchExecutionId(execution.getId()).stream()
                    .map(c -> new CodeRemediationResponse.FileChangeView(
                            c.getFilePath(), c.getChangeType(), c.getBeforeExcerpt(), c.getAfterExcerpt()))
                    .toList();
            validationViews = validations.findByPatchExecutionIdOrderByCreatedAtAsc(execution.getId()).stream()
                    .map(v -> new ValidationOutcome(
                            v.getCommand(), v.getExitCode(), v.getDurationMs() == null ? 0 : v.getDurationMs(),
                            v.getStdoutSummary(), v.getStderrSummary(), v.getStatus()))
                    .toList();
            verificationView = verifications.findFirstByPatchExecutionIdOrderByCreatedAtDesc(execution.getId())
                    .map(v -> new CodeRemediationResponse.VerificationView(
                            v.getResult(), v.getDetails(), readList(v.getEvidence())))
                    .orElse(null);
        }
        RemediationStrategy strategy = plan == null ? null : new RemediationStrategy(
                RemediationStrategyType.valueOf(plan.getStrategyType()),
                plan.getRationale(),
                readList(plan.getAffectedFiles()),
                readList(plan.getExpectedChanges()),
                plan.getConfidence() == null ? null : Confidence.valueOf(plan.getConfidence()),
                readList(plan.getPrerequisites()),
                readList(plan.getRisks()),
                plan.getRollbackPlan());
        CodeRemediationResponse.PatchPlanView planView = plan == null ? null : new CodeRemediationResponse.PatchPlanView(
                plan.getId(),
                plan.getStrategyType(),
                plan.getRationale(),
                readList(plan.getAffectedFiles()),
                readList(plan.getExpectedChanges()),
                readList(plan.getPrerequisites()),
                readList(plan.getRisks()),
                plan.getRollbackPlan(),
                plan.getConfidence());
        PatchExecutionEntity exec = execution;
        List<CodeRemediationResponse.FileChangeView> files = fileViews;
        CodeRemediationResponse.PatchView patchView = exec == null ? null : new CodeRemediationResponse.PatchView(
                exec.getId(),
                exec.getAttemptNumber(),
                exec.getStatus(),
                exec.getUnifiedDiff(),
                exec.getFilesChanged(),
                exec.getLinesAdded(),
                exec.getLinesDeleted(),
                exec.getSafetyStatus(),
                readList(exec.getSafetyViolations()),
                files);
        CodeRemediationResponse.ApprovalView approvalView = approvals.findFirstByJobIdOrderByCreatedAtDesc(job.getId())
                .map(a -> new CodeRemediationResponse.ApprovalView(
                        a.getStatus(), a.getDecidedBy(), a.getComment(), a.getDecidedAt()))
                .orElse(null);
        CodeRemediationResponse.PullRequestView prView = pullRequests.findFirstByJobIdOrderByCreatedAtDesc(job.getId())
                .map(p -> new CodeRemediationResponse.PullRequestView(
                        p.getProvider(), p.getRepository(), p.getBranch(), p.getCommitSha(),
                        p.getPullRequestUrl(), p.getPullRequestNumber(), p.getSkippedReason()))
                .orElse(null);
        return new CodeRemediationResponse(
                job.getId(),
                job.getInvestigationId(),
                job.getCveId(),
                job.getStatus(),
                job.getCurrentState(),
                job.getOverallConfidence(),
                job.getReviewReason(),
                job.getAttemptCount(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                target,
                List.of(),
                strategy,
                planView,
                patchView,
                validationViews,
                verificationView,
                approvalView,
                prView,
                githubEnabled);
    }

    public void audit(UUID jobId, String agent, String action, String tool, String detailJson) {
        CodeRemediationAuditEntity row = new CodeRemediationAuditEntity();
        row.setId(UUID.randomUUID());
        row.setJobId(jobId);
        row.setAgentName(agent);
        row.setAction(action);
        row.setToolName(tool);
        row.setDetail(detailJson == null ? "{}" : detailJson);
        row.setCreatedAt(Instant.now());
        audit.save(row);
    }

    private RepositoryTarget toTarget(RemediationTargetEntity entity) {
        return new RepositoryTarget(
                entity.getProvider(),
                entity.getOrganization(),
                entity.getRepository(),
                entity.getDefaultBranch(),
                entity.getTechnology(),
                entity.getBuildSystem(),
                entity.getRepositoryUrl(),
                entity.getAiBranch(),
                entity.getWorkspacePath(),
                entity.getConfidence() == null ? null : Confidence.valueOf(entity.getConfidence()),
                readList(entity.getEvidence()));
    }

    public String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public List<String> readList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }
}
