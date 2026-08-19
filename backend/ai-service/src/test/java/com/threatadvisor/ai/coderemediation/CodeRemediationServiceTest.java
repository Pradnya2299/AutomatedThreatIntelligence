package com.threatadvisor.ai.coderemediation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threatadvisor.ai.agent.common.Confidence;
import com.threatadvisor.ai.agent.common.InvestigationStatus;
import com.threatadvisor.ai.agent.common.SecurityInvestigationContext;
import com.threatadvisor.ai.agent.dto.AffectedAssetMatch;
import com.threatadvisor.ai.agent.dto.AffectedProduct;
import com.threatadvisor.ai.agent.dto.AssetInvestigationResult;
import com.threatadvisor.ai.agent.dto.RemediationAgentResult;
import com.threatadvisor.ai.agent.dto.RiskAnalystResult;
import com.threatadvisor.ai.agent.dto.ThreatIntelligenceResult;
import com.threatadvisor.ai.agent.orchestrator.SecurityOrchestrator;
import com.threatadvisor.ai.coderemediation.domain.ApprovalRequestEntity;
import com.threatadvisor.ai.coderemediation.domain.CodePullRequestEntity;
import com.threatadvisor.ai.coderemediation.domain.CodeRemediationJobEntity;
import com.threatadvisor.ai.coderemediation.domain.PatchChangeEntity;
import com.threatadvisor.ai.coderemediation.domain.PatchExecutionEntity;
import com.threatadvisor.ai.coderemediation.domain.PatchPlanEntity;
import com.threatadvisor.ai.coderemediation.domain.RemediationTargetEntity;
import com.threatadvisor.ai.coderemediation.domain.RepositoryBindingEntity;
import com.threatadvisor.ai.coderemediation.domain.SecurityVerificationEntity;
import com.threatadvisor.ai.coderemediation.domain.ValidationRunEntity;
import com.threatadvisor.ai.coderemediation.dto.ApprovalDecisionRequest;
import com.threatadvisor.ai.coderemediation.dto.CodeRemediationRequest;
import com.threatadvisor.ai.coderemediation.dto.CodeRemediationResponse;
import com.threatadvisor.ai.coderemediation.git.LocalWorkspaceGitProvider;
import com.threatadvisor.ai.coderemediation.repo.ApprovalRequestRepository;
import com.threatadvisor.ai.coderemediation.repo.CodePullRequestRepository;
import com.threatadvisor.ai.coderemediation.repo.CodeRemediationAuditRepository;
import com.threatadvisor.ai.coderemediation.repo.CodeRemediationJobRepository;
import com.threatadvisor.ai.coderemediation.repo.PatchChangeRepository;
import com.threatadvisor.ai.coderemediation.repo.PatchExecutionRepository;
import com.threatadvisor.ai.coderemediation.repo.PatchPlanRepository;
import com.threatadvisor.ai.coderemediation.repo.RemediationTargetRepository;
import com.threatadvisor.ai.coderemediation.repo.RepositoryBindingRepository;
import com.threatadvisor.ai.coderemediation.repo.SecurityVerificationRepository;
import com.threatadvisor.ai.coderemediation.repo.ValidationRunRepository;
import com.threatadvisor.ai.config.AiProperties;
import com.threatadvisor.ai.exception.AiException;
import com.threatadvisor.ai.kafka.RemediationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodeRemediationServiceTest {

    @Mock SecurityOrchestrator orchestrator;
    @Mock RemediationEventPublisher events;
    @Mock CodeRemediationJobRepository jobs;
    @Mock RepositoryBindingRepository bindings;
    @Mock RemediationTargetRepository targets;
    @Mock PatchPlanRepository plans;
    @Mock PatchExecutionRepository executions;
    @Mock PatchChangeRepository changes;
    @Mock ValidationRunRepository validations;
    @Mock SecurityVerificationRepository verifications;
    @Mock ApprovalRequestRepository approvals;
    @Mock CodePullRequestRepository pullRequests;
    @Mock CodeRemediationAuditRepository audit;

    private CodeRemediationService service;
    private RemediationTargetEntity lastTarget;
    private PatchPlanEntity lastPlan;
    private PatchExecutionEntity lastExecution;
    private final java.util.ArrayList<PatchChangeEntity> lastChanges = new java.util.ArrayList<>();
    private final java.util.ArrayList<ValidationRunEntity> lastValidations = new java.util.ArrayList<>();
    private SecurityVerificationEntity lastVerification;
    private ApprovalRequestEntity lastApproval;

    @BeforeEach
    void setup() {
        ObjectMapper mapper = new ObjectMapper();
        AiProperties properties = new AiProperties();
        CodeRemediationAssembler assembler = new CodeRemediationAssembler(
                mapper, jobs, targets, plans, executions, changes, validations, verifications, approvals, pullRequests, audit);
        service = new CodeRemediationService(
                orchestrator, properties, new LocalWorkspaceGitProvider(),
                new com.threatadvisor.ai.coderemediation.validation.DefaultValidationRunner(properties),
                mapper, events, assembler, jobs, bindings, targets, plans, executions, changes,
                validations, verifications, approvals, pullRequests);
        lastChanges.clear();
        lastValidations.clear();
        lastTarget = null;
        lastPlan = null;
        lastExecution = null;
        lastVerification = null;
        lastApproval = null;
        lenient().when(jobs.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(targets.save(any())).thenAnswer(inv -> {
            lastTarget = inv.getArgument(0);
            return lastTarget;
        });
        lenient().when(plans.save(any())).thenAnswer(inv -> {
            lastPlan = inv.getArgument(0);
            return lastPlan;
        });
        lenient().when(executions.save(any())).thenAnswer(inv -> {
            lastExecution = inv.getArgument(0);
            return lastExecution;
        });
        lenient().when(changes.save(any())).thenAnswer(inv -> {
            lastChanges.add(inv.getArgument(0));
            return inv.getArgument(0);
        });
        lenient().when(validations.save(any())).thenAnswer(inv -> {
            lastValidations.add(inv.getArgument(0));
            return inv.getArgument(0);
        });
        lenient().when(verifications.save(any())).thenAnswer(inv -> {
            lastVerification = inv.getArgument(0);
            return lastVerification;
        });
        lenient().when(approvals.save(any())).thenAnswer(inv -> {
            lastApproval = inv.getArgument(0);
            return lastApproval;
        });
        lenient().when(pullRequests.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(audit.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(jobs.findByInvestigationIdOrderByCreatedAtDesc(any())).thenReturn(List.of());
        lenient().when(targets.findFirstByJobId(any())).thenAnswer(inv -> Optional.ofNullable(lastTarget));
        lenient().when(plans.findFirstByJobIdOrderByCreatedAtDesc(any())).thenAnswer(inv -> Optional.ofNullable(lastPlan));
        lenient().when(executions.findFirstByJobIdOrderByAttemptNumberDesc(any())).thenAnswer(inv -> Optional.ofNullable(lastExecution));
        lenient().when(changes.findByPatchExecutionId(any())).thenAnswer(inv -> List.copyOf(lastChanges));
        lenient().when(validations.findByPatchExecutionIdOrderByCreatedAtAsc(any())).thenAnswer(inv -> List.copyOf(lastValidations));
        lenient().when(verifications.findFirstByPatchExecutionIdOrderByCreatedAtDesc(any())).thenAnswer(inv -> Optional.ofNullable(lastVerification));
        lenient().when(approvals.findFirstByJobIdOrderByCreatedAtDesc(any())).thenAnswer(inv -> Optional.ofNullable(lastApproval));
        lenient().when(pullRequests.findFirstByJobIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());
        lenient().doNothing().when(events).publish(any(), any(), any());
    }

    @Test
    void mavenFixtureProducesPatchAndWaitsForApproval() {
        UUID investigationId = UUID.randomUUID();
        when(orchestrator.get(investigationId)).thenReturn(Optional.of(completedInvestigation(investigationId)));
        CodeRemediationResponse response = service.start(
                investigationId,
                new CodeRemediationRequest("analyst", "LOCAL_WORKSPACE", "northwind", "payment-service",
                        "local://payment-service", "main"),
                "analyst");
        assertEquals("AWAITING_APPROVAL", response.currentState());
        assertEquals("PATCH_READY_FOR_REVIEW", response.status());
        assertNotNull(response.patch());
        assertTrue(response.patch().unifiedDiff().contains("2.17.1"));
        assertTrue(response.patch().unifiedDiff().contains("2.14.1"));
        assertEquals("PASSED", response.patch().safetyStatus());
        assertEquals("PATCH_NOT_VERIFIED", response.securityVerification().result());
    }

    @Test
    void emptyRepositoryIsReviewRequired() {
        UUID investigationId = UUID.randomUUID();
        when(orchestrator.get(investigationId)).thenReturn(Optional.of(completedInvestigation(investigationId)));
        CodeRemediationResponse response = service.start(
                investigationId,
                new CodeRemediationRequest("analyst", "LOCAL_WORKSPACE", null, "empty-service",
                        "local://empty-service", "main"),
                "analyst");
        assertEquals("REVIEW_REQUIRED", response.currentState());
        assertTrue(response.reviewReason().toLowerCase().contains("no reliable"));
        assertTrue(response.patch() == null || response.patch().unifiedDiff() == null || response.patch().unifiedDiff().isBlank());
    }

    @Test
    void missingRepositoryMappingIsReviewRequired() {
        UUID investigationId = UUID.randomUUID();
        when(orchestrator.get(investigationId)).thenReturn(Optional.of(completedInvestigation(investigationId)));
        when(bindings.findByHostnameIgnoreCase("nw-prod-app-01")).thenReturn(List.of());
        CodeRemediationResponse response = service.start(investigationId, new CodeRemediationRequest(null, null, null, null, null, null), "analyst");
        assertEquals("REVIEW_REQUIRED", response.currentState());
        assertTrue(response.reviewReason().contains("does not guess"));
    }

    @Test
    void incompleteInvestigationIsRejected() {
        UUID investigationId = UUID.randomUUID();
        when(orchestrator.get(investigationId)).thenReturn(Optional.of(
                SecurityInvestigationContext.builder()
                        .investigationId(investigationId)
                        .cveId("CVE-2021-44228")
                        .status(InvestigationStatus.REVIEW_REQUIRED)
                        .build()));
        assertThrows(AiException.class, () -> service.start(investigationId, null, "analyst"));
    }

    @Test
    void rejectDoesNotCreatePullRequest() {
        UUID investigationId = UUID.randomUUID();
        when(orchestrator.get(investigationId)).thenReturn(Optional.of(completedInvestigation(investigationId)));
        CodeRemediationResponse started = service.start(
                investigationId,
                new CodeRemediationRequest("analyst", "LOCAL_WORKSPACE", "northwind", "payment-service",
                        "local://payment-service", "main"),
                "analyst");
        CodeRemediationJobEntity job = new CodeRemediationJobEntity();
        job.setId(started.remediationId());
        job.setInvestigationId(investigationId);
        job.setCveId("CVE-2021-44228");
        job.setStatus("PATCH_READY_FOR_REVIEW");
        job.setCurrentState("AWAITING_APPROVAL");
        when(jobs.findById(started.remediationId())).thenReturn(Optional.of(job));
        when(approvals.findFirstByJobIdOrderByCreatedAtDesc(started.remediationId())).thenReturn(Optional.of(pending(started.remediationId())));
        CodeRemediationResponse rejected = service.decide(started.remediationId(), "REJECT",
                new ApprovalDecisionRequest("analyst", "no"), "analyst");
        assertEquals("REJECTED", rejected.currentState());
        assertNull(rejected.pullRequest());
    }

    @Test
    void duplicateOpenJobIsConflict() {
        UUID investigationId = UUID.randomUUID();
        when(orchestrator.get(investigationId)).thenReturn(Optional.of(completedInvestigation(investigationId)));
        CodeRemediationJobEntity open = new CodeRemediationJobEntity();
        open.setCurrentState("AWAITING_APPROVAL");
        when(jobs.findByInvestigationIdOrderByCreatedAtDesc(investigationId)).thenReturn(List.of(open));
        assertThrows(AiException.class, () -> service.start(investigationId, null, "analyst"));
    }

    private static ApprovalRequestEntity pending(UUID jobId) {
        ApprovalRequestEntity entity = new ApprovalRequestEntity();
        entity.setId(UUID.randomUUID());
        entity.setJobId(jobId);
        entity.setStatus("PENDING");
        return entity;
    }

    private static SecurityInvestigationContext completedInvestigation(UUID id) {
        return SecurityInvestigationContext.builder()
                .investigationId(id)
                .cveId("CVE-2021-44228")
                .status(InvestigationStatus.COMPLETED)
                .threat(new ThreatIntelligenceResult(
                        "CVE-2021-44228",
                        UUID.randomUUID(),
                        "CRITICAL",
                        new BigDecimal("10.0"),
                        "NETWORK",
                        true,
                        true,
                        List.of(new AffectedProduct("apache", "log4j", "2.0.0", "2.17.0", "cpe")),
                        "Log4j",
                        List.of(),
                        Confidence.HIGH))
                .assets(new AssetInvestigationResult(
                        true, 1, 1, 0, 1,
                        List.of(new AffectedAssetMatch(UUID.randomUUID(), UUID.randomUUID(), "nw-prod-app-01",
                                "PRODUCTION", "CRITICAL", false, "VERSION_RANGE_MATCH", "HIGH", "log4j 2.14.1")),
                        List.of(),
                        Confidence.HIGH,
                        true))
                .risk(new RiskAnalystResult(
                        UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("97.5"), "CRITICAL",
                        List.of(), "engine", List.of(), Confidence.HIGH))
                .remediation(new RemediationAgentResult(
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "COMPLETED", "IMMEDIATE",
                        "2.17.1", List.of(), List.of(), List.of(), "rollback", List.of(), List.of(), true,
                        "demo", Confidence.HIGH))
                .build();
    }
}
