# Phase 5A — Multi-agent security orchestration

Phase 5A adds a **deterministic CVE investigation workflow** on ai-service. Five agents run in a fixed order. They call existing Java services through **tools**. The LLM is not used for CPE matching, version comparison, asset correlation, risk scoring, or database lookups.

Phase 1–4 pipelines stay in place: Kafka `cve.normalized` → findings → `risk.calculated` → RAG remediation still works. This phase adds an explicit investigation that can be triggered from REST.

## Architecture

```
                    ┌──────────────────────┐
                    │  ORCHESTRATOR AGENT  │
                    └───────────┬──────────┘
                                │
              ┌─────────────────┼─────────────────┐
              ▼                 ▼                 ▼
       THREAT INTEL       ASSET INVESTIGATION   RISK ANALYST
          AGENT                 AGENT              AGENT
              │                 │                 │
              └─────────────────┼─────────────────┘
                                ▼
                       REMEDIATION AGENT
                                │
                         RAG + OpenAI/demo
                                │
                                ▼
                       Structured Plan
```

`SecurityOrchestrator` is a Spring service (not LangChain/LangGraph). Sequence:

`startInvestigation` → `runThreatAnalysis` → `runAssetInvestigation` → `runRiskAnalysis` → `runRemediation` → `buildFinalDecision`

The orchestrator can become more autonomous later without changing tool boundaries.

## Agent responsibilities

| Agent | Answers | Tools | LLM |
|---|---|---|---|
| ThreatIntelligenceAgent | What is this CVE? | `CveLookupTool`, `VulnerabilityContextTool` | No. Summary is deterministic from the CVE row. |
| AssetInvestigationAgent | Are we exposed? | `CorrelationTool` (HTTP to correlation-service), `AffectedAssetLookupTool` (findings + assets) | No |
| RiskAnalystAgent | How urgent is it for us? | `RiskCalculationTool` (HTTP to risk-service, then `risk_assessments`) | No. Explanation text comes from the engine. |
| RemediationAgent | What should we do? | `RemediationGenerationTool` wrapping Phase 3 `RemediationGenerationService` | Yes, via existing RAG + demo/OpenAI path only |

## Deterministic vs AI

**Must stay Java:**

- CVE/CPE lookup
- Correlation / version matching
- Finding rows
- Risk formula (`formula_version=v1`)
- “Not affected” only after a successful correlation run with zero matches

**LLM may:**

- Produce the structured remediation plan (Phase 3), grounded in retrieved knowledge chunks
- Not invent CVSS, affected hostnames, or a different risk score

If `exploit_available` / `actively_exploited` are null, threat intel reports `UNKNOWN_FROM_SOURCE`. It does not claim active exploitation.

## Shared context

`SecurityInvestigationContext` is an immutable typed object (builder/withers). It carries investigation id, CVE id, vulnerability entity, threat/asset/risk/remediation results, agent executions, errors, evidence, timestamps, and the final recommendation.

Agent execution status: `PENDING` | `RUNNING` | `COMPLETED` | `FAILED` | `SKIPPED`.

Investigation status: `PENDING` | `RUNNING` | `COMPLETED` | `FAILED` | `REVIEW_REQUIRED`.

## Error handling

| Failure | Orchestrator |
|---|---|
| Threat intel (unknown CVE or lookup error) | Stop. Status `REVIEW_REQUIRED`. Later agents do not run. |
| Correlation / asset lookup error | Stop. Status `FAILED`. **Never** write `affected=false`. Exposure is unknown. |
| No matches after a successful correlation | Skip risk and remediation. Status `COMPLETED` with `exposed=false`. |
| Risk engine error | Skip remediation. Preserve threat + assets. Status `FAILED`. |
| Remediation / RAG error | Preserve threat, assets, and risk. Mark remediation `FAILED`. Status `FAILED`. |

## REST

Public BFF (api-service, HTTP Basic), same contract on ai-service for local debugging (`permitAll` like other internal AI routes):

```http
POST /api/v1/investigations
{"cveId":"CVE-2021-44228"}

GET /api/v1/investigations/{investigationId}
```

POST runs the workflow **synchronously** (hackathon). The response includes `investigationId`, `status`, agent outputs, executions, and `recommendation`. api-service proxies to `${AI_SERVICE_URL}` (default `http://localhost:8084`) so the React app still talks only to port 8080.

## Kafka

Existing `risk.calculated` and `remediation.generated` contracts are unchanged.

New topics (same envelope as [event-contracts.md](../kafka/event-contracts.md)):

- `security.investigation.requested` — optional async trigger (`payload.cveId`). Consumer `ai-service:security.investigation.requested`. REST does **not** publish this (avoids double-run).
- `security.investigation.completed` — published when the orchestrator finishes (success or controlled failure).

## Database

Flyway **V7** `security_investigations` (JSONB snapshots of typed agent results). Findings/risk/remediation tables cannot represent per-agent execution state. Logical owner: **ai-service**. api-service still applies migrations.

## Observability

Logs (no API keys, no prompts):

```
[INVESTIGATION] id=…
[AGENT] ThreatIntelligenceAgent STARTED
[AGENT] ThreatIntelligenceAgent COMPLETED
```

MDC: `investigationId`, `cveId`, `agent`, existing `correlationId`.

## Testing

Unit tests (no OpenAI key, tools mocked):

- `OrchestratorTest` — order, context passing, failure strategy, skip when not exposed
- `ThreatIntelligenceAgentTest`
- `AssetInvestigationAgentTest`
- `RiskAnalystAgentTest`
- `RemediationAgentTest`

Phase 3 `RemediationPipelineTest` still covers RAG persistence with `AI_DEMO_MODE=true`.

## Local run

1. Compose + Flyway V7 + seed (see root README).
2. Start correlation-service (8082), risk-service (8083), ai-service (8084, `AI_DEMO_MODE=true`), api-service (8080).
3. Trigger:

```bash
curl -sS -u analyst:analyst_change_me -H 'Content-Type: application/json' \
  -d '{"cveId":"CVE-2021-44228"}' \
  http://localhost:8080/api/v1/investigations
```

Direct ai-service (no Basic auth): `http://localhost:8084/api/v1/investigations`.

## Future extension points (not in 5A)

- Autonomous planner / conditional agent fan-out
- Jira, GitHub, ServiceNow, SIEM, cloud inventory
- React investigation screen
- Human approval execution (still a later phase)
- LangChain/LangGraph — not required; Spring orchestration is enough
