# Phase 7 — Autonomous code remediation (human-approved PRs)

Phase 7 extends a **completed** Phase 5B investigation with an **opt-in** code remediation workflow. It does **not** rewrite `SecurityOrchestrator`, `InvestigationPlanner`, correlation, risk, or the recommendation `RemediationAgent`.

The product claim is:

> Supports multiple CVE remediation strategies and automatically remediates vulnerabilities when sufficient evidence, a safe remediation strategy, and successful validation are available. Otherwise it escalates to human review.

It does **not** claim every CVE can be auto-fixed.

## Reuse vs new

| Existing (unchanged) | New |
|---|---|
| `SecurityOrchestrator` / `InvestigationPlanner` | `CodeRemediationService` |
| Threat / Asset / Risk / Remediation agents | Discovery, analysis, strategy, patch, validation, verify, approval, PR |
| `security_investigations` | V9 tables (`code_remediation_jobs`, …) |
| `EventEnvelope` | Additional topic names |
| Phase 6 dashboard | `AutonomousRemediationPanel` |
| `GitProvider` interface | `LocalWorkspaceGitProvider`, `GitHubGitProvider` |

## Flow

```
COMPLETED investigation
        │
        v
POST /api/v1/investigations/{id}/remediation
        │
        v
Repository discovery (binding or explicit operator input — never guessed)
        │
        v
Isolated workspace + ai-security/<cve>-<id> branch
        │
        v
Code analysis → strategy → patch plan → patch (workspace only)
        │
        v
Safety guardrails → validation → security verification
        │
        v
AWAITING_APPROVAL  (human gate)
        │
   APPROVE / REJECT / REQUEST_CHANGES
        │
        v
PR only after APPROVE. Never merge. Never touch main/master/production/release*.
```

When `GITHUB_ENABLED=false` (default), analysis/planning/patch/validation still run in a temp workspace. Approve records a skipped PR (`GITHUB_DISABLED`) and does not push.

## Strategies

`DEPENDENCY_UPGRADE`, `DEPENDENCY_OVERRIDE`, `TRANSITIVE_DEPENDENCY_OVERRIDE`, `BASE_IMAGE_UPGRADE`, `DOCKERFILE_CHANGE`, `KUBERNETES_CONFIGURATION_CHANGE`, `APPLICATION_CONFIGURATION_CHANGE`, `SOURCE_CODE_CHANGE`, `FRAMEWORK_UPGRADE`, `MULTI_FILE_CHANGE`, `MANUAL_REVIEW`.

Classifier is evidence-driven (files on disk + CVE product). Missing files → `MANUAL_REVIEW` / `REVIEW_REQUIRED`. No patch, no PR.

## Patch safety

Rejects: uncertain repo, secrets in diff, protected files (`.env`, keys), binaries, unexpected files, `AI_MAX_CHANGED_FILES` / `AI_MAX_CHANGED_LINES`, non-allowed extensions, protected branch names.

Bounded retries: `AI_MAX_PATCH_ATTEMPTS` (default 3).

## REST (api-service → ai-service)

- `POST /api/v1/investigations/{id}/remediation`
- `GET /api/v1/investigations/{id}/remediation`
- `GET /api/v1/remediations/{id}`
- `GET /api/v1/remediations/{id}/patch`
- `GET /api/v1/remediations/{id}/diff`
- `GET /api/v1/remediations/{id}/validations`
- `GET /api/v1/remediations/{id}/pull-request`
- `POST /api/v1/remediations/{id}/approve`
- `POST /api/v1/remediations/{id}/reject`
- `POST /api/v1/remediations/{id}/request-changes`

No shell-execution endpoints.

## Kafka

`security.remediation.requested|planned`, `security.patch.generated|validated`, `security.remediation.approval.requested|approved|rejected`, `security.pullrequest.created` (+ `.dlq`). Same `EventEnvelope`.

## Demo

After V9 (`./scripts/migrate.sh` or start api-service) and seed:

1. Complete investigation `CVE-2021-44228`.
2. Dashboard → Autonomous remediation → Start.
3. Expect Maven `log4j-core` `2.14.1` → `2.17.1` in the real diff.
4. Security verification is `PATCH_NOT_VERIFIED` when host tests are skipped (`AI_SKIP_HOST_BUILDS=true`).
5. Approve does **not** push if GitHub is disabled.
6. `local://empty-service` → `REVIEW_REQUIRED`, no PR.

## Environment

See `.env.example`: `GITHUB_ENABLED`, `GITHUB_TOKEN`, `AI_MAX_CHANGED_FILES`, `AI_MAX_PATCH_ATTEMPTS`, `AI_SKIP_HOST_BUILDS`.
