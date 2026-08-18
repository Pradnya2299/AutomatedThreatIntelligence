# AI architecture

## Role of AI in this product

The AI layer **explains and recommends**. It does **not** decide risk scores and does **not** apply production patches. That split exists because:

- Risk must be defensible to auditors and engineers.
- LLM output is probabilistic; patching production is not.
- Tool-calling keeps the model inside a capability envelope.

## Phase 1 status

This document is the target design. **No OpenAI client, no tool runtime, and no RAG indexer are implemented in Phase 1.**

## Orchestration (target)

On `risk.calculated` (or analyst-triggered generate):

1. ai-service loads finding id from the event payload (not from a free-form prompt).
2. An orchestrator invokes **allow-listed tools** (Java methods), each implemented as an application service:
   - `getVulnerability()`
   - `getAffectedAssets()`
   - `getAssetDetails()`
   - `getRiskAssessment()`
   - `searchKnowledgeBase()` (RAG; see rag-architecture.md)
   - `getPatchInformation()`
   - `getCorporatePolicy()`
   - `generateRemediationPlan()` (assembles the LLM request; does not execute changes)
3. Tool results are packed into a structured context object.
4. OpenAI is asked for **JSON matching a schema** (priority, summary, reason, affectedAssetCount, recommendedAction, patchVersion, temporaryMitigation, verificationSteps, rollbackPlan, confidence).
5. Output is schema-validated. Invalid JSON is retried once, then failed with an audit event — never stored as a trusted plan.
6. A `remediation_plans` row is written as `PENDING_APPROVAL`.
7. `remediation.generated` is published. Approval happens in api-service.

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
