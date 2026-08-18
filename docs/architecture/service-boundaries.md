# Service boundaries

Each backend module is a **separate Spring Boot application**. Responsibilities are exclusive unless noted. Collaboration is by Kafka events or by reading the shared PostgreSQL model (Phase 1). The frontend never talks to ingestion, correlation, risk, AI, or notification services directly.

## api-service

**Owns:** synchronous SecOps API, authentication/authorization, dashboard aggregations, human approval/rejection of remediation, triggering manual CVE ingestion, **Flyway schema application** (only migrator).

**Does:** REST for `/api/*`, Spring Security roles, audit of user actions, publish `cve.raw` (when an analyst submits a CVE) and `remediation.approved`.

**Does not:** normalize NVD feeds, run correlation, compute risk formulas, call OpenAI, send emails/Slack.

**Scale driver:** concurrent dashboard users.

## ingestion-service

**Owns:** CVE intake, validation, normalization, persistence of `vulnerabilities` / `vulnerability_cpe`, threat-intel enrichment (later), publish `cve.normalized` and `cve.enriched`.

**Does:** tolerate incomplete/malformed source payloads; store raw JSON; map into the internal vulnerability model.

**Does not:** decide which assets are affected; calculate risk; generate remediation text.

**Scale driver:** CVE publication bursts and reprocessing.

## correlation-service

**Owns:** matching enriched CVEs to `asset_software`, creating `findings` with an explainable match reason, publish `finding.created`.

**Does:** vendor/product/CPE/version-range comparison in a domain service (not string-equals-only). Consumes `cve.normalized`.

**Does not:** score business risk; talk to OpenAI; expose dashboard REST.

**Does not:** score business risk; talk to OpenAI.

**Scale driver:** assets × new CVEs.

## risk-service

**Owns:** deterministic risk engine, `risk_assessments`, publish `risk.calculated`.

**Does:** configurable weighted formula (documented in database design / later engine docs). Never delegates scoring to an LLM.

**Does not:** mutate assets; generate patch prose.

**Scale driver:** finding volume.

## ai-service

**Owns:** tool-based context assembly, RAG retrieval **through application tools**, structured OpenAI responses, `remediation_plans` draft creation, publish `remediation.requested` / `remediation.generated`.

**Does not:** connect the LLM to JDBC or arbitrary SQL; execute patches; approve work; bypass schema validation of model output.

**Scale driver:** LLM latency (scale workers independently from API).

## notification-service

**Owns:** `notifications` records and outbound channels (later: email, Slack). Consumes `notification.requested` and selected domain events.

**Does not:** change findings or risk.

## security-dashboard (frontend)

**Owns:** SOC UI. Calls api-service only.

**Does not:** access PostgreSQL, Redis, or Kafka.

## Logical data ownership (shared database)

See [logical-ownership.md](../database/logical-ownership.md). ingestion writes vulnerabilities; correlation writes findings; risk writes risk_assessments; AI writes remediation/knowledge; notification writes notifications; api-service reads/orchestrates and applies Flyway.

## Boundary violations to reject in review

- Business logic in controllers or Kafka listeners (listeners must delegate to services).
- JPA entities returned from REST.
- Redis used as durable store for findings or CVE records.
- Risk score produced by a prompt.
- Frontend importing another service’s OpenAPI for “convenience” in Phase 2+ without going through api-service.
