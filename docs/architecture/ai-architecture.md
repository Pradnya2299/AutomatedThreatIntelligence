# AI architecture

## Role of AI in this product

The AI layer **explains and recommends**. It does **not** decide risk scores and does **not** apply production patches. That split exists because:

- Risk must be defensible to auditors and engineers.
- LLM output is probabilistic; patching production is not.
- Tool-calling keeps the model inside a capability envelope.

## Phase 5A / 5B status

A **SecurityOrchestrator** in ai-service runs a bounded decision loop. The default path is still Threat Intelligence → Asset Investigation → Risk Analyst → Remediation. Phase 5B inspects evidence and confidence before each step. Tools call existing correlation-service, risk-service, repositories, and Phase 3 `RemediationGenerationService`. See [phase-5.md](../ai/phase-5.md) and [phase-5b.md](../ai/phase-5b.md).

## Phase 1 status

This document started as the target design. Phases 3–5B implemented RAG, structured remediation, and bounded investigation orchestration. The isolation rules below still apply.

## Orchestration (Phase 3 event path + Phase 5 investigation)

On `risk.calculated` (unchanged): ai-service still generates a plan for that finding.

On `POST /api/v1/investigations` or `security.investigation.requested`:

1. Orchestrator creates `SecurityInvestigationContext` and a `security_investigations` row.
2. `InvestigationPlanner` selects a typed `AgentAction` from current evidence (fallback order is Threat → Asset → Risk → Remediation).
3. Agents invoke **allow-listed tools** (Java):
   - `CveLookupTool` / `CpeLookupTool` / `VulnerabilityContextTool`
   - `CorrelationTool` / `AffectedAssetLookupTool` / `AssetDetailsTool`
   - `RiskCalculationTool`
   - `KnowledgeSearchTool` / `PolicyRetrievalTool` / `RemediationGenerationTool`
4. Tool results stay in typed DTOs. The LLM never calculates risk or invents assets.
5. `remediation.generated` may still be published by the Phase 3 path. `security.investigation.completed` is published when the investigation finishes.

A `remediation_plans` row remains `GENERATED` until a later approval phase.

## Isolation rules

| Allowed | Forbidden |
|---------|-----------|
| Tools that call Java services / repositories | LLM constructing SQL |
| Read APIs / DB via those services | Direct `DataSource` in the model adapter |
| Cached completions in Redis (`ai:remediation:{findingId}`) as hints | Treating cache as the plan of record |
| Simulated remediation after approval | Unattended production mutation in the hackathon |

## Prompts and policies

System prompts must instruct the model to **ground recommendations in retrieved policy chunks** when present, and to say when knowledge is missing. Confidence is a model field but **is not** a risk score.

## Failure behavior

Timeouts on OpenAI; no API keys in logs; retry with backoff; DLQ for poison `risk.calculated` events after exhaustion. Analysts can regenerate from the UI later.
