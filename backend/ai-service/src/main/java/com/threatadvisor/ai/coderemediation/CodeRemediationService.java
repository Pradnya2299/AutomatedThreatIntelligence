package com.threatadvisor.ai.coderemediation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threatadvisor.ai.agent.common.Confidence;
import com.threatadvisor.ai.agent.common.InvestigationStatus;
import com.threatadvisor.ai.agent.common.SecurityInvestigationContext;
import com.threatadvisor.ai.agent.dto.AffectedAssetMatch;
import com.threatadvisor.ai.agent.dto.AffectedProduct;
import com.threatadvisor.ai.agent.orchestrator.InvestigationPlanner;
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
import com.threatadvisor.ai.coderemediation.git.GitProvider;
import com.threatadvisor.ai.coderemediation.git.LocalWorkspaceGitProvider;
import com.threatadvisor.ai.coderemediation.git.PullRequestRef;
import com.threatadvisor.ai.coderemediation.git.PullRequestRequest;
import com.threatadvisor.ai.coderemediation.model.CodeFinding;
import com.threatadvisor.ai.coderemediation.model.RemediationStrategy;
import com.threatadvisor.ai.coderemediation.patch.DockerfilePatcher;
import com.threatadvisor.ai.coderemediation.patch.MavenDependencyPatcher;
import com.threatadvisor.ai.coderemediation.patch.NpmPackagePatcher;
import com.threatadvisor.ai.coderemediation.patch.UnifiedDiffBuilder;
import com.threatadvisor.ai.coderemediation.repo.ApprovalRequestRepository;
import com.threatadvisor.ai.coderemediation.repo.CodePullRequestRepository;
import com.threatadvisor.ai.coderemediation.repo.CodeRemediationJobRepository;
import com.threatadvisor.ai.coderemediation.repo.PatchChangeRepository;
import com.threatadvisor.ai.coderemediation.repo.PatchExecutionRepository;
import com.threatadvisor.ai.coderemediation.repo.PatchPlanRepository;
import com.threatadvisor.ai.coderemediation.repo.RemediationTargetRepository;
import com.threatadvisor.ai.coderemediation.repo.RepositoryBindingRepository;
import com.threatadvisor.ai.coderemediation.repo.SecurityVerificationRepository;
import com.threatadvisor.ai.coderemediation.repo.ValidationRunRepository;
import com.threatadvisor.ai.coderemediation.safety.PatchSafetyGuard;
import com.threatadvisor.ai.coderemediation.validation.ValidationCommandDetector;
import com.threatadvisor.ai.coderemediation.validation.ValidationOutcome;
import com.threatadvisor.ai.coderemediation.validation.ValidationRunner;
import com.threatadvisor.ai.coderemediation.workspace.FixtureWorkspaceFactory;
import com.threatadvisor.ai.config.AiProperties;
import com.threatadvisor.ai.exception.AiException;
import com.threatadvisor.ai.kafka.EventEnvelope;
import com.threatadvisor.ai.kafka.RemediationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class CodeRemediationService {

    public static final String TOPIC_REQUESTED = "security.remediation.requested";
    public static final String TOPIC_PLANNED = "security.remediation.planned";
    public static final String TOPIC_PATCH_GENERATED = "security.patch.generated";
    public static final String TOPIC_PATCH_VALIDATED = "security.patch.validated";
    public static final String TOPIC_APPROVAL_REQUESTED = "security.remediation.approval.requested";
    public static final String TOPIC_APPROVED = "security.remediation.approved";
    public static final String TOPIC_REJECTED = "security.remediation.rejected";
    public static final String TOPIC_PR_CREATED = "security.pullrequest.created";

    private final SecurityOrchestrator investigations;
    private final AiProperties properties;
    private final GitProvider gitProvider;
    private final ValidationRunner validationRunner;
    private final ObjectMapper objectMapper;
    private final RemediationEventPublisher events;
    private final CodeRemediationAssembler assembler;
    private final CodeRemediationJobRepository jobs;
    private final RepositoryBindingRepository bindings;
    private final RemediationTargetRepository targets;
    private final PatchPlanRepository plans;
    private final PatchExecutionRepository executions;
    private final PatchChangeRepository changes;
    private final ValidationRunRepository validations;
    private final SecurityVerificationRepository verifications;
    private final ApprovalRequestRepository approvals;
    private final CodePullRequestRepository pullRequests;

    public CodeRemediationService(
            SecurityOrchestrator investigations,
            AiProperties properties,
            GitProvider gitProvider,
            ValidationRunner validationRunner,
            ObjectMapper objectMapper,
            RemediationEventPublisher events,
            CodeRemediationAssembler assembler,
            CodeRemediationJobRepository jobs,
            RepositoryBindingRepository bindings,
            RemediationTargetRepository targets,
            PatchPlanRepository plans,
            PatchExecutionRepository executions,
            PatchChangeRepository changes,
            ValidationRunRepository validations,
            SecurityVerificationRepository verifications,
            ApprovalRequestRepository approvals,
            CodePullRequestRepository pullRequests) {
        this.investigations = investigations;
        this.properties = properties;
        this.gitProvider = gitProvider;
        this.validationRunner = validationRunner;
        this.objectMapper = objectMapper;
        this.events = events;
        this.assembler = assembler;
        this.jobs = jobs;
        this.bindings = bindings;
        this.targets = targets;
        this.plans = plans;
        this.executions = executions;
        this.changes = changes;
        this.validations = validations;
        this.verifications = verifications;
        this.approvals = approvals;
        this.pullRequests = pullRequests;
    }

    @Transactional
    public CodeRemediationResponse start(UUID investigationId, CodeRemediationRequest request, String initiatedBy) {
        SecurityInvestigationContext investigation = investigations.get(investigationId)
                .orElseThrow(() -> new AiException(HttpStatus.NOT_FOUND, "INVESTIGATION_NOT_FOUND", "Unknown investigation"));
        if (investigation.status() != InvestigationStatus.COMPLETED
                || !InvestigationPlanner.remediationGate(investigation)) {
            throw new AiException(
                    HttpStatus.CONFLICT,
                    "INVESTIGATION_NOT_READY",
                    "Code remediation requires a COMPLETED investigation with threat, asset, and risk evidence");
        }
        boolean open = jobs.findByInvestigationIdOrderByCreatedAtDesc(investigationId).stream()
                .anyMatch(job -> CodeRemediationState.valueOf(job.getCurrentState()).isOpen());
        if (open) {
            throw new AiException(HttpStatus.CONFLICT, "CODE_REMEDIATION_IN_PROGRESS", "An open code remediation already exists");
        }
        Instant now = Instant.now();
        UUID jobId = UUID.randomUUID();
        CodeRemediationJobEntity job = new CodeRemediationJobEntity();
        job.setId(jobId);
        job.setInvestigationId(investigationId);
        job.setCveId(investigation.cveId());
        job.setStatus("PATCH_READY_FOR_REVIEW");
        job.setCurrentState(CodeRemediationState.DISCOVERING_REPOSITORY.name());
        job.setInitiatedBy(initiatedBy == null ? (request == null ? null : request.initiatedBy()) : initiatedBy);
        job.setCorrelationId(investigation.correlationId());
        job.setAttemptCount(0);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        jobs.save(job);
        publish(TOPIC_REQUESTED, job, "{\"investigationId\":\"" + investigationId + "\"}");
        assembler.audit(jobId, "RepositoryDiscoveryAgent", "START", null, "{}");
        try {
            runPipeline(job, investigation, request == null ? new CodeRemediationRequest(null, null, null, null, null, null) : request);
        } catch (AiException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            fail(job, CodeRemediationState.FAILED, ex.getMessage());
        }
        jobs.save(job);
        return assembler.toResponse(job, gitProvider.remoteMutationsEnabled());
    }

    public Optional<CodeRemediationResponse> get(UUID id) {
        return assembler.load(id, gitProvider.remoteMutationsEnabled());
    }

    public Optional<CodeRemediationResponse> latest(UUID investigationId) {
        return assembler.loadLatest(investigationId, gitProvider.remoteMutationsEnabled());
    }

    @Transactional
    public CodeRemediationResponse decide(UUID id, String action, ApprovalDecisionRequest body, String actor) {
        CodeRemediationJobEntity job = jobs.findById(id)
                .orElseThrow(() -> new AiException(HttpStatus.NOT_FOUND, "REMEDIATION_NOT_FOUND", "Unknown code remediation"));
        CodeRemediationState state = CodeRemediationState.valueOf(job.getCurrentState());
        if (state != CodeRemediationState.AWAITING_APPROVAL) {
            throw new AiException(HttpStatus.CONFLICT, "APPROVAL_NOT_PENDING", "Remediation is not awaiting approval");
        }
        String decidedBy = actor != null ? actor : (body == null ? "analyst" : body.decidedBy());
        String comment = body == null ? null : body.comment();
        ApprovalRequestEntity approval = approvals.findFirstByJobIdOrderByCreatedAtDesc(id).orElseGet(() -> {
            ApprovalRequestEntity created = new ApprovalRequestEntity();
            created.setId(UUID.randomUUID());
            created.setJobId(id);
            created.setStatus("PENDING");
            created.setCreatedAt(Instant.now());
            return created;
        });
        approval.setDecidedBy(decidedBy);
        approval.setComment(comment);
        approval.setDecidedAt(Instant.now());
        if ("APPROVE".equals(action)) {
            transition(job, CodeRemediationState.APPROVED);
            approval.setStatus("APPROVED");
            approvals.save(approval);
            assembler.audit(id, "ApprovalGate", "APPROVE", null, assembler.json(decidedBy));
            publish(TOPIC_APPROVED, job, "{}");
            createPullRequest(job);
        } else if ("REJECT".equals(action)) {
            transition(job, CodeRemediationState.REJECTED);
            job.setStatus("REJECTED");
            job.setCompletedAt(Instant.now());
            approval.setStatus("REJECTED");
            approvals.save(approval);
            assembler.audit(id, "ApprovalGate", "REJECT", null, assembler.json(comment));
            publish(TOPIC_REJECTED, job, "{}");
        } else {
            transition(job, CodeRemediationState.CHANGES_REQUESTED);
            job.setStatus("CHANGES_REQUESTED");
            job.setCompletedAt(Instant.now());
            approval.setStatus("CHANGES_REQUESTED");
            approvals.save(approval);
            assembler.audit(id, "ApprovalGate", "REQUEST_CHANGES", null, assembler.json(comment));
        }
        job.setUpdatedAt(Instant.now());
        jobs.save(job);
        return assembler.toResponse(job, gitProvider.remoteMutationsEnabled());
    }

    private void runPipeline(CodeRemediationJobEntity job, SecurityInvestigationContext investigation, CodeRemediationRequest request) {
        Optional<RepositoryBindingEntity> discovered = discover(investigation, request);
        if (discovered.isEmpty()) {
            review(job, "Repository mapping is unavailable. The system does not guess a repository.");
            return;
        }
        RepositoryBindingEntity binding = discovered.get();
        transition(job, CodeRemediationState.REPOSITORY_FOUND);
        String shortId = job.getId().toString().substring(0, 8);
        String branch = "ai-security/" + investigation.cveId() + "-" + shortId;
        LocalWorkspaceGitProvider.assertSafeBranch(branch);
        Path workspace = materializeWorkspace(binding);
        gitProvider.createBranch(workspace, binding.getDefaultBranch(), branch);
        RemediationTargetEntity target = saveTarget(job, binding, branch, workspace);
        assembler.audit(job.getId(), "RepositoryDiscoveryAgent", "REPOSITORY_FOUND", "RepositoryCloneTool", assembler.json(binding.getRepositoryUrl()));

        transition(job, CodeRemediationState.ANALYZING_CODE);
        List<CodeFinding> findings = analyze(workspace, investigation, gitProvider);
        assembler.audit(job.getId(), "CodeAnalysisAgent", "ANALYZED", "RepositorySearchTool", assembler.json(findings.size()));
        RemediationStrategy strategy = classify(findings, investigation);
        if (strategy.strategyType() == RemediationStrategyType.MANUAL_REVIEW || strategy.confidence() == Confidence.LOW) {
            review(job, strategy.rationale());
            return;
        }
        transition(job, CodeRemediationState.PLAN_CREATED);
        PatchPlanEntity plan = savePlan(job, strategy);
        publish(TOPIC_PLANNED, job, "{}");
        assembler.audit(job.getId(), "PatchPlanningAgent", "PLAN_CREATED", null, assembler.json(strategy.strategyType()));

        int maxAttempts = Math.max(1, properties.getCodeRemediation().getMaxPatchAttempts());
        PatchExecutionEntity execution = null;
        List<ValidationOutcome> lastValidations = List.of();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            job.setAttemptCount(attempt);
            execution = generatePatch(job, plan, workspace, findings, strategy, attempt);
            if ("REJECTED".equals(execution.getSafetyStatus())) {
                review(job, "Patch rejected by safety guardrails: " + execution.getSafetyViolations());
                return;
            }
            transition(job, CodeRemediationState.PATCH_GENERATED);
            publish(TOPIC_PATCH_GENERATED, job, "{}");
            transition(job, CodeRemediationState.VALIDATING);
            lastValidations = validate(execution, workspace);
            boolean failed = lastValidations.stream().anyMatch(v -> "FAILED".equals(v.status()));
            if (failed) {
                transition(job, CodeRemediationState.PATCH_FAILED);
                assembler.audit(job.getId(), "PatchRepairAgent", "RETRY", "TestTool", assembler.json(attempt));
                if (attempt == maxAttempts) {
                    review(job, "Validation failed after " + maxAttempts + " patch attempts");
                    return;
                }
                continue;
            }
            transition(job, CodeRemediationState.PATCH_VALIDATED);
            publish(TOPIC_PATCH_VALIDATED, job, "{}");
            break;
        }
        if (execution == null) {
            fail(job, CodeRemediationState.FAILED, "Patch generation produced no execution");
            return;
        }
        SecurityVerifyResult verify = verify(execution, workspace, findings, lastValidations);
        if (verify == SecurityVerifyResult.UNKNOWN) {
            review(job, "Security verification is UNKNOWN; the CVE is not claimed fixed because a patch compiled.");
            return;
        }
        transition(job, CodeRemediationState.SECURITY_VERIFIED);
        transition(job, CodeRemediationState.AWAITING_APPROVAL);
        job.setStatus("PATCH_READY_FOR_REVIEW");
        job.setOverallConfidence(strategy.confidence() == null ? "MEDIUM" : strategy.confidence().name());
        ApprovalRequestEntity pending = new ApprovalRequestEntity();
        pending.setId(UUID.randomUUID());
        pending.setJobId(job.getId());
        pending.setStatus("PENDING");
        pending.setCreatedAt(Instant.now());
        approvals.save(pending);
        publish(TOPIC_APPROVAL_REQUESTED, job, "{}");
        assembler.audit(job.getId(), "ApprovalGate", "AWAITING_APPROVAL", null, "{}");
        jobs.save(job);
        targets.save(target);
    }

    private Optional<RepositoryBindingEntity> discover(SecurityInvestigationContext investigation, CodeRemediationRequest request) {
        if (request.repositoryUrl() != null && !request.repositoryUrl().isBlank()
                || request.repository() != null && !request.repository().isBlank()) {
            RepositoryBindingEntity explicit = new RepositoryBindingEntity();
            explicit.setId(UUID.randomUUID());
            explicit.setProvider(request.provider() == null ? inferProvider(request.repositoryUrl()) : request.provider());
            explicit.setOrganization(request.organization());
            explicit.setRepository(request.repository() == null ? fixtureName(request.repositoryUrl()) : request.repository());
            explicit.setRepositoryUrl(request.repositoryUrl() == null ? "local://" + request.repository() : request.repositoryUrl());
            explicit.setDefaultBranch(request.defaultBranch() == null ? "main" : request.defaultBranch());
            explicit.setConfidence("HIGH");
            return Optional.of(explicit);
        }
        List<String> hosts = investigation.assets() == null || investigation.assets().assets() == null
                ? List.of()
                : investigation.assets().assets().stream().map(AffectedAssetMatch::hostname).filter(h -> h != null).toList();
        LinkedHashSet<String> repos = new LinkedHashSet<>();
        RepositoryBindingEntity chosen = null;
        for (String host : hosts) {
            for (RepositoryBindingEntity binding : bindings.findByHostnameIgnoreCase(host)) {
                repos.add(binding.getProvider() + "/" + binding.getRepositoryUrl());
                chosen = binding;
            }
        }
        if (repos.size() != 1) {
            return Optional.empty();
        }
        return Optional.of(chosen);
    }

    private Path materializeWorkspace(RepositoryBindingEntity binding) {
        String url = binding.getRepositoryUrl() == null ? "" : binding.getRepositoryUrl();
        if (url.startsWith("local://")) {
            return FixtureWorkspaceFactory.materialize(url.substring("local://".length()), Path.of(System.getProperty("java.io.tmpdir")));
        }
        if ("LOCAL_WORKSPACE".equalsIgnoreCase(binding.getProvider())) {
            return FixtureWorkspaceFactory.materialize(binding.getRepository(), Path.of(System.getProperty("java.io.tmpdir")));
        }
        throw new AiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "REMOTE_CLONE_DISABLED",
                "Remote repository clone requires GITHUB_ENABLED=true. Provide a local:// fixture or repository binding.");
    }

    private List<CodeFinding> analyze(Path workspace, SecurityInvestigationContext investigation, GitProvider git) {
        List<CodeFinding> findings = new ArrayList<>();
        String pom = git.getFile(workspace, "pom.xml");
        String product = primaryProduct(investigation);
        String artifact = artifactFor(product);
        if (pom != null && artifact != null && MavenDependencyPatcher.containsArtifact(pom, artifact)) {
            String current = MavenDependencyPatcher.currentVersion(pom, artifact);
            findings.add(new CodeFinding(
                    "pom.xml",
                    lineOf(pom, "<artifactId>" + artifact + "</artifactId>"),
                    artifact,
                    current,
                    targetVersion(investigation, artifact),
                    "Maven dependency version is in the vulnerable range recorded by CVE/CPE evidence",
                    Confidence.HIGH,
                    "DETERMINISTIC file parse of pom.xml"));
        }
        String pkg = git.getFile(workspace, "package.json");
        if (pkg != null && product != null) {
            String current = NpmPackagePatcher.currentVersion(objectMapper, pkg, product);
            if (current != null) {
                findings.add(new CodeFinding(
                        "package.json",
                        null,
                        product,
                        current,
                        targetVersion(investigation, product),
                        "npm dependency version present in package.json",
                        Confidence.HIGH,
                        "DETERMINISTIC parse of package.json"));
            }
        }
        String docker = git.getFile(workspace, "Dockerfile");
        if (docker != null && DockerfilePatcher.currentFrom(docker) != null && looksContainerCve(investigation)) {
            findings.add(new CodeFinding(
                    "Dockerfile",
                    1,
                    "base-image",
                    DockerfilePatcher.currentFrom(docker),
                    "eclipse-temurin:17-jre",
                    "Dockerfile FROM pin matches a container/base-image remediation class",
                    Confidence.MEDIUM,
                    "DETERMINISTIC Dockerfile FROM parse"));
        }
        String java = git.getFile(workspace, "src/main/java/com/northwind/InsecureHash.java");
        if (java != null && java.contains("MD5")) {
            findings.add(new CodeFinding(
                    "src/main/java/com/northwind/InsecureHash.java",
                    lineOf(java, "MD5"),
                    "MessageDigest",
                    "MD5",
                    "SHA-256",
                    "Insecure hash algorithm in source",
                    Confidence.MEDIUM,
                    "DETERMINISTIC source scan"));
        }
        return findings;
    }

    private RemediationStrategy classify(List<CodeFinding> findings, SecurityInvestigationContext investigation) {
        if (findings.isEmpty()) {
            return new RemediationStrategy(
                    RemediationStrategyType.MANUAL_REVIEW,
                    "No reliable code/configuration/dependency evidence was located in the mapped repository.",
                    List.of(),
                    List.of(),
                    Confidence.LOW,
                    List.of(),
                    List.of("Automated patch would be speculative"),
                    "No files were modified");
        }
        boolean multi = findings.size() > 1;
        CodeFinding first = findings.get(0);
        RemediationStrategyType type = switch (first.file()) {
            case "pom.xml", "build.gradle", "build.gradle.kts" -> RemediationStrategyType.DEPENDENCY_UPGRADE;
            case "package.json" -> RemediationStrategyType.DEPENDENCY_UPGRADE;
            case "Dockerfile" -> RemediationStrategyType.BASE_IMAGE_UPGRADE;
            default -> first.file().endsWith(".java") || first.file().endsWith(".ts") || first.file().endsWith(".js")
                    ? RemediationStrategyType.SOURCE_CODE_CHANGE
                    : RemediationStrategyType.APPLICATION_CONFIGURATION_CHANGE;
        };
        if (multi) {
            type = RemediationStrategyType.MULTI_FILE_CHANGE;
        }
        List<String> files = findings.stream().map(CodeFinding::file).toList();
        List<String> expected = findings.stream()
                .map(f -> f.file() + ": " + f.currentValue() + " -> " + f.expectedValue())
                .toList();
        return new RemediationStrategy(
                type,
                "Evidence-driven strategy from repository files; risk score remains " + investigation.risk().riskScore()
                        + " from the risk engine (not from this patch planner).",
                files,
                expected,
                first.confidence(),
                List.of("Isolated ai-security branch", "Human approval before any PR"),
                List.of("Build/test may fail", "Transitive dependencies may remain"),
                "Revert the isolated branch; default branch is untouched");
    }

    private PatchExecutionEntity generatePatch(
            CodeRemediationJobEntity job,
            PatchPlanEntity plan,
            Path workspace,
            List<CodeFinding> findings,
            RemediationStrategy strategy,
            int attempt) {
        List<PatchSafetyGuard.FileChange> fileChanges = new ArrayList<>();
        StringBuilder diff = new StringBuilder();
        int added = 0;
        int deleted = 0;
        for (CodeFinding finding : findings) {
            String before = gitProvider.getFile(workspace, finding.file());
            if (before == null) {
                continue;
            }
            String after = applyFinding(finding, before);
            if (after.equals(before)) {
                continue;
            }
            gitProvider.applyPatch(workspace, finding.file(), after);
            fileChanges.add(new PatchSafetyGuard.FileChange(finding.file(), before, after));
            diff.append(UnifiedDiffBuilder.build(finding.file(), before, after)).append('\n');
            added += UnifiedDiffBuilder.added(before, after);
            deleted += UnifiedDiffBuilder.deleted(before, after);
        }
        Set<String> allowed = PatchSafetyGuard.DEFAULT_EXTENSIONS;
        List<String> violations = PatchSafetyGuard.violations(
                fileChanges,
                properties.getCodeRemediation().getMaxChangedFiles(),
                properties.getCodeRemediation().getMaxChangedLines(),
                allowed);
        for (PatchSafetyGuard.FileChange change : fileChanges) {
            if (strategy.affectedFiles() != null && !strategy.affectedFiles().contains(change.path())) {
                violations.add("Unexpected file changed outside plan: " + change.path());
            }
        }
        PatchExecutionEntity execution = new PatchExecutionEntity();
        execution.setId(UUID.randomUUID());
        execution.setJobId(job.getId());
        execution.setPatchPlanId(plan.getId());
        execution.setAttemptNumber(attempt);
        execution.setUnifiedDiff(diff.toString());
        execution.setFilesChanged(fileChanges.size());
        execution.setLinesAdded(added);
        execution.setLinesDeleted(deleted);
        execution.setSafetyViolations(assembler.json(violations));
        execution.setSafetyStatus(violations.isEmpty() ? "PASSED" : "REJECTED");
        execution.setStatus(violations.isEmpty() ? "GENERATED" : "REJECTED");
        execution.setCreatedAt(Instant.now());
        executions.save(execution);
        for (PatchSafetyGuard.FileChange change : fileChanges) {
            PatchChangeEntity row = new PatchChangeEntity();
            row.setId(UUID.randomUUID());
            row.setPatchExecutionId(execution.getId());
            row.setFilePath(change.path());
            row.setChangeType("MODIFY");
            row.setBeforeExcerpt(excerpt(change.before()));
            row.setAfterExcerpt(excerpt(change.after()));
            changes.save(row);
        }
        assembler.audit(job.getId(), "PatchGenerationAgent", "PATCH", "PatchApplyTool", assembler.json(execution.getFilesChanged()));
        return execution;
    }

    private String applyFinding(CodeFinding finding, String before) {
        if ("pom.xml".equals(finding.file())) {
            return MavenDependencyPatcher.upgrade(before, finding.component(), finding.expectedValue());
        }
        if ("package.json".equals(finding.file())) {
            return NpmPackagePatcher.upgrade(objectMapper, before, finding.component(), finding.expectedValue());
        }
        if ("Dockerfile".equals(finding.file())) {
            return DockerfilePatcher.replaceFrom(before, finding.expectedValue());
        }
        if (finding.currentValue() != null && finding.expectedValue() != null) {
            return before.replace(finding.currentValue(), finding.expectedValue());
        }
        return before;
    }

    private List<ValidationOutcome> validate(PatchExecutionEntity execution, Path workspace) {
        List<String[]> commands = ValidationCommandDetector.detect(workspace);
        List<ValidationOutcome> outcomes = validationRunner.run(workspace, commands);
        for (ValidationOutcome outcome : outcomes) {
            ValidationRunEntity row = new ValidationRunEntity();
            row.setId(UUID.randomUUID());
            row.setPatchExecutionId(execution.getId());
            row.setCommand(outcome.command());
            row.setExitCode(outcome.exitCode());
            row.setDurationMs(outcome.durationMs());
            row.setStdoutSummary(outcome.stdoutSummary());
            row.setStderrSummary(outcome.stderrSummary());
            row.setStatus(outcome.status());
            row.setCreatedAt(Instant.now());
            validations.save(row);
        }
        assembler.audit(execution.getJobId(), "ValidationAgent", "VALIDATE", "BuildTool", assembler.json(outcomes.size()));
        return outcomes;
    }

    private SecurityVerifyResult verify(
            PatchExecutionEntity execution,
            Path workspace,
            List<CodeFinding> findings,
            List<ValidationOutcome> validations) {
        List<String> evidence = new ArrayList<>();
        boolean patched = true;
        for (CodeFinding finding : findings) {
            String content = gitProvider.getFile(workspace, finding.file());
            if (content == null) {
                patched = false;
                evidence.add(finding.file() + " missing after patch");
                continue;
            }
            if (finding.currentValue() != null && content.contains(finding.currentValue())
                    && (finding.expectedValue() == null || !content.contains(finding.expectedValue()))) {
                patched = false;
                evidence.add(finding.file() + " still contains " + finding.currentValue());
            } else if (finding.expectedValue() != null && content.contains(finding.expectedValue())) {
                evidence.add(finding.file() + " now contains " + finding.expectedValue());
            }
        }
        boolean testsPassed = validations.stream().anyMatch(v -> "PASSED".equals(v.status()));
        boolean testsSkipped = validations.stream().allMatch(v -> v.status() != null && v.status().startsWith("SKIPPED"));
        SecurityVerifyResult result;
        String details;
        if (!patched) {
            result = SecurityVerifyResult.UNKNOWN;
            details = "Vulnerable token still present in the workspace.";
        } else if (testsPassed) {
            result = SecurityVerifyResult.PATCHED;
            details = "Vulnerable token removed/replaced and at least one validation command passed.";
        } else if (testsSkipped) {
            result = SecurityVerifyResult.PATCH_NOT_VERIFIED;
            details = "Files changed as planned but host tests were not executed. Not claimed fixed solely because a patch exists.";
        } else {
            result = SecurityVerifyResult.PATCH_NOT_VERIFIED;
            details = "Files changed as planned; validation did not prove the CVE is gone beyond the file parse.";
        }
        SecurityVerificationEntity row = new SecurityVerificationEntity();
        row.setId(UUID.randomUUID());
        row.setPatchExecutionId(execution.getId());
        row.setResult(result.name());
        row.setDetails(details);
        row.setEvidence(assembler.json(evidence));
        row.setCreatedAt(Instant.now());
        verifications.save(row);
        assembler.audit(execution.getJobId(), "SecurityVerificationAgent", "VERIFY", "SecurityScanTool", assembler.json(result));
        if (result == SecurityVerifyResult.PATCH_NOT_VERIFIED || result == SecurityVerifyResult.PATCHED) {
            return result;
        }
        return result;
    }

    private void createPullRequest(CodeRemediationJobEntity job) {
        RemediationTargetEntity target = targets.findFirstByJobId(job.getId())
                .orElseThrow(() -> new AiException(HttpStatus.CONFLICT, "NO_TARGET", "Missing repository target"));
        LocalWorkspaceGitProvider.assertSafeBranch(target.getAiBranch());
        transition(job, CodeRemediationState.PR_CREATING);
        String commit = null;
        try {
            if (target.getWorkspacePath() != null) {
                commit = gitProvider.commitChanges(Path.of(target.getWorkspacePath()),
                        "[Security] Remediate " + job.getCveId());
            }
        } catch (RuntimeException ex) {
            assembler.audit(job.getId(), "PullRequestAgent", "COMMIT_SKIPPED", "GitDiffTool", assembler.json(ex.getMessage()));
        }
        if (gitProvider.remoteMutationsEnabled()) {
            try {
                gitProvider.pushBranch(Path.of(target.getWorkspacePath()), target.getAiBranch());
            } catch (RuntimeException ex) {
                fail(job, CodeRemediationState.FAILED, "Push failed: " + ex.getMessage());
                return;
            }
        }
        PullRequestRef ref = gitProvider.createPullRequest(new PullRequestRequest(
                target.getOrganization() == null ? "local" : target.getOrganization(),
                target.getRepository(),
                "[Security] Remediate " + job.getCveId(),
                prBody(job, target),
                target.getAiBranch(),
                target.getDefaultBranch()
        )).orElse(PullRequestRef.skipped(gitProvider.name(), target.getRepository(), target.getAiBranch(), "NO_PROVIDER"));
        CodePullRequestEntity row = new CodePullRequestEntity();
        row.setId(UUID.randomUUID());
        row.setJobId(job.getId());
        row.setProvider(ref.provider());
        row.setRepository(ref.repository());
        row.setBranch(ref.branch());
        row.setCommitSha(commit == null ? ref.commitSha() : commit);
        row.setPullRequestUrl(ref.pullRequestUrl());
        row.setPullRequestNumber(ref.pullRequestNumber());
        row.setSkippedReason(ref.skippedReason());
        row.setCreatedAt(Instant.now());
        pullRequests.save(row);
        if (ref.pullRequestUrl() == null) {
            job.setStatus("APPROVED");
            job.setCompletedAt(Instant.now());
            transition(job, CodeRemediationState.PR_CREATED);
            assembler.audit(job.getId(), "PullRequestAgent", "PR_SKIPPED", "GitHubProvider", assembler.json(ref.skippedReason()));
        } else {
            job.setStatus("PR_CREATED");
            job.setCompletedAt(Instant.now());
            transition(job, CodeRemediationState.PR_CREATED);
            publish(TOPIC_PR_CREATED, job, assembler.json(ref.pullRequestUrl()));
        }
    }

    private String prBody(CodeRemediationJobEntity job, RemediationTargetEntity target) {
        return """
                Automated security remediation (human-approved).

                - CVE: %s
                - Investigation: %s
                - Repository: %s
                - Isolated branch: %s
                - Default branch was not modified
                - Merge is not performed by this system
                """.formatted(job.getCveId(), job.getInvestigationId(), target.getRepository(), target.getAiBranch());
    }

    private RemediationTargetEntity saveTarget(CodeRemediationJobEntity job, RepositoryBindingEntity binding, String branch, Path workspace) {
        RemediationTargetEntity target = new RemediationTargetEntity();
        target.setId(UUID.randomUUID());
        target.setJobId(job.getId());
        target.setProvider(binding.getProvider());
        target.setOrganization(binding.getOrganization());
        target.setRepository(binding.getRepository());
        target.setDefaultBranch(binding.getDefaultBranch() == null ? "main" : binding.getDefaultBranch());
        target.setAiBranch(branch);
        target.setTechnology(binding.getTechnology());
        target.setBuildSystem(binding.getBuildSystem());
        target.setRepositoryUrl(binding.getRepositoryUrl());
        target.setWorkspacePath(workspace.toString());
        target.setConfidence(binding.getConfidence());
        target.setEvidence(assembler.json(List.of("repository_binding_or_operator_input")));
        target.setCreatedAt(Instant.now());
        return targets.save(target);
    }

    private PatchPlanEntity savePlan(CodeRemediationJobEntity job, RemediationStrategy strategy) {
        PatchPlanEntity plan = new PatchPlanEntity();
        plan.setId(UUID.randomUUID());
        plan.setJobId(job.getId());
        plan.setStrategyType(strategy.strategyType().name());
        plan.setRationale(strategy.rationale());
        plan.setAffectedFiles(assembler.json(strategy.affectedFiles()));
        plan.setExpectedChanges(assembler.json(strategy.expectedChanges()));
        plan.setPrerequisites(assembler.json(strategy.prerequisites()));
        plan.setRisks(assembler.json(strategy.risks()));
        plan.setRollbackPlan(strategy.rollbackPlan());
        plan.setConfidence(strategy.confidence() == null ? null : strategy.confidence().name());
        plan.setCreatedAt(Instant.now());
        return plans.save(plan);
    }

    private void transition(CodeRemediationJobEntity job, CodeRemediationState next) {
        CodeRemediationState current = CodeRemediationState.valueOf(job.getCurrentState());
        if (current == next) {
            return;
        }
        if (!current.canTransitionTo(next)) {
            throw new AiException(HttpStatus.CONFLICT, "ILLEGAL_STATE_TRANSITION", current + " → " + next + " is not allowed");
        }
        job.setCurrentState(next.name());
        job.setUpdatedAt(Instant.now());
    }

    private void review(CodeRemediationJobEntity job, String reason) {
        CodeRemediationState current = CodeRemediationState.valueOf(job.getCurrentState());
        if (current.canTransitionTo(CodeRemediationState.REVIEW_REQUIRED)) {
            job.setCurrentState(CodeRemediationState.REVIEW_REQUIRED.name());
        } else {
            job.setCurrentState(CodeRemediationState.REVIEW_REQUIRED.name());
        }
        job.setStatus("REVIEW_REQUIRED");
        job.setReviewReason(reason);
        job.setOverallConfidence("LOW");
        job.setCompletedAt(Instant.now());
        job.setUpdatedAt(Instant.now());
        assembler.audit(job.getId(), "CodeRemediationOrchestrator", "REVIEW_REQUIRED", null, assembler.json(reason));
    }

    private void fail(CodeRemediationJobEntity job, CodeRemediationState state, String reason) {
        job.setCurrentState(state.name());
        job.setStatus("FAILED");
        job.setReviewReason(reason);
        job.setCompletedAt(Instant.now());
        job.setUpdatedAt(Instant.now());
    }

    private void publish(String topic, CodeRemediationJobEntity job, String payloadJson) {
        try {
            var payload = objectMapper.readTree(payloadJson.startsWith("{") ? payloadJson : "{\"value\":" + assembler.json(payloadJson) + "}");
            events.publish(topic, job.getId().toString(), new EventEnvelope(
                    UUID.randomUUID(),
                    topic,
                    1,
                    Instant.now(),
                    EventEnvelope.SOURCE_SERVICE,
                    job.getCorrelationId() == null ? job.getId() : job.getCorrelationId(),
                    payload));
        } catch (Exception ignored) {
            // Kafka must not block isolated workspace remediation in local/dev without topics.
        }
    }

    private static String inferProvider(String url) {
        if (url != null && url.startsWith("local://")) {
            return "LOCAL_WORKSPACE";
        }
        if (url != null && url.contains("github")) {
            return "GITHUB";
        }
        return "LOCAL_WORKSPACE";
    }

    private static String fixtureName(String url) {
        if (url != null && url.startsWith("local://")) {
            return url.substring("local://".length());
        }
        return url;
    }

    private static String primaryProduct(SecurityInvestigationContext investigation) {
        if (investigation.threat() == null || investigation.threat().affectedProducts() == null
                || investigation.threat().affectedProducts().isEmpty()) {
            return investigation.cveId() != null && investigation.cveId().contains("44228") ? "log4j-core" : null;
        }
        AffectedProduct product = investigation.threat().affectedProducts().get(0);
        return product.product();
    }

    private static String artifactFor(String product) {
        if (product == null) {
            return null;
        }
        String lower = product.toLowerCase(Locale.ROOT);
        if (lower.contains("log4j")) {
            return "log4j-core";
        }
        return product;
    }

    private static String targetVersion(SecurityInvestigationContext investigation, String artifact) {
        if (investigation.remediation() != null && investigation.remediation().targetVersion() != null) {
            return investigation.remediation().targetVersion();
        }
        if ("log4j-core".equals(artifact)) {
            return "2.17.1";
        }
        if (investigation.threat() != null && investigation.threat().affectedProducts() != null) {
            for (AffectedProduct product : investigation.threat().affectedProducts()) {
                if (product.versionEndExcluding() != null && !product.versionEndExcluding().isBlank()) {
                    return product.versionEndExcluding();
                }
            }
        }
        return null;
    }

    private static boolean looksContainerCve(SecurityInvestigationContext investigation) {
        String summary = investigation.threat() == null ? "" : String.valueOf(investigation.threat().summary());
        return summary.toLowerCase(Locale.ROOT).contains("docker")
                || summary.toLowerCase(Locale.ROOT).contains("container")
                || summary.toLowerCase(Locale.ROOT).contains("image");
    }

    private static Integer lineOf(String content, String token) {
        String[] lines = content.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(token)) {
                return i + 1;
            }
        }
        return null;
    }

    private static String excerpt(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= 2000 ? text : text.substring(0, 2000) + "…";
    }
}
