# Phase 7A / 7B — NVD intelligence + LLM code remediation

Phase 7A makes **NIST NVD 2.0** the external CVE source (JSON API only). Phase 7B routes code analysis, patch planning, and patch generation through **structured OpenAI calls** when `AI_DEMO_MODE=false`. Deterministic engines, PatchSafetyGuard, validation, verification, and human approval are unchanged.

See [phase-7.md](phase-7.md) for the original isolated-workspace approval workflow.

## NVD

- Client: `ingestion-service` `NvdApiClient` (`GET ?cveId=` and incremental `lastModStartDate` / `lastModEndDate` with pagination).
- Normalize with existing `CveDocumentParser` after `NvdDocumentAdapter` unwraps `vulnerabilities[]`.
- Upsert `vulnerabilities` by CVE ID (`CREATED` / `UPDATED` / `UNCHANGED`). Raw NVD JSON is stored on `raw_source_payload`.
- Kafka: existing `cve.raw` (via `CveIngestionService`) and `cve.normalized`.
- Investigation: ai-service `RefreshingCveLookupTool` POSTs `/internal/ingestion/nvd/cves/{cveId}` when local data is missing or SEED/stale. Cached row on NVD failure is marked `NVD_CACHE`. No cache → investigation `REVIEW_REQUIRED` (existing CVE_NOT_FOUND path).
- Sync: `NVD_SYNC_ENABLED=false` by default. Scheduler uses an **initial delay** equal to the interval (no call on startup). Checkpoint: `nvd_sync_state`.

## LLM

Agents: `CodeAnalysisAgent` → `PatchPlanningAgent` → `PatchGenerationAgent`. They only produce JSON. `PatchApplicationService` writes the workspace after hash + `PatchSafetyGuard`. Validation and security verification remain deterministic. Bounded repair uses `AI_MAX_PATCH_ATTEMPTS`.

| `AI_DEMO_MODE` | Mode badge | Implementation |
|---|---|---|
| `true` (default) | DEMO MODE | `DemoCodeRemediationLlm` — structured, not OpenAI |
| `false` | LLM POWERED | `OpenAiCodeRemediationLlm` structured chat |
| OpenAI down | DETERMINISTIC_FALLBACK | Existing file parsers/patchers |

## Local

Same as Phase 7, plus **ingestion-service :8081** if you want live NVD lookup. Apply Flyway **V10**. Keep `NVD_SYNC_ENABLED=false` unless you intend to poll NVD.
