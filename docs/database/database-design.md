# Database design

## Role of PostgreSQL

PostgreSQL is the **system of record** for organizations, assets, vulnerabilities, findings, risk, remediation, knowledge, users, notifications, audit, and consumer idempotency. Hibernate must not create tables in production (`ddl-auto=validate`). Schema changes go through **Flyway**. Canonical SQL is **`database/migrations/`**. api-service copies those files onto its classpath at build and is the only process that runs Flyway. See [flyway.md](flyway.md) and [logical-ownership.md](logical-ownership.md).

## Identifier strategy

Primary keys are UUID (`gen_random_uuid()`). Natural keys (CVE ID, hostname per org, event id) have unique constraints to make retries safe.

## Core tables (V1)

| Table | Purpose |
|-------|---------|
| organizations | Tenant / company |
| users, roles, user_roles | Authn/z |
| assets | Inventory (CMDB-ready) |
| asset_software | Installed products / CPE / versions |
| vulnerabilities | Normalized CVE + threat flags + raw payload |
| vulnerability_cpe | CPE / vendor / product / version ranges |
| findings | Asset × vulnerability match + explanation |
| risk_assessments | Deterministic scores + reason list |
| remediation_plans | AI (later) + approval workflow |
| knowledge_documents / knowledge_chunks | RAG corpus + embeddings |
| notifications | Outbound notification records |
| audit_logs | Security-relevant actions |
| event_processing_records | Consumer idempotency |

## Indexes (V1)

- vulnerabilities: unique `cve_id`; indexes on severity, published_at, exploit flags
- assets: hostname; environment; internet exposure
- asset_software: (vendor, product, version); cpe
- findings: status; (asset_id, vulnerability_id) unique
- risk_assessments: final_risk_score, risk_level
- remediation_plans: status

## Uniqueness for idempotency

- `vulnerabilities.cve_id`
- `assets (organization_id, hostname)`
- `findings (asset_id, vulnerability_id)`
- `event_processing_records (event_id, consumer)`
- `knowledge_chunks (document_id, chunk_index)`
- `asset_software (asset_id, vendor, product, version)` (V3)

## pgvector

`knowledge_chunks.embedding` is `vector(1536)` for **OpenAI `text-embedding-3-small`**. Phase 2A does not populate embeddings.
- `findings (asset_id, vulnerability_id)`
- `event_processing_records.event_id` unique
- `risk_assessments` one current row per finding (`finding_id` unique in V1)

## Flyway vs services

**api-service** runs Flyway on startup (`spring.flyway.enabled=true`). Other services set Flyway off. Hibernate is `ddl-auto=validate` everywhere — never `create` / `update`.

`knowledge_chunks.embedding` is `vector(1536)` for planned model **text-embedding-3-small**. Embeddings are not populated in Phase 2A.

## Risk formula (Phase 2D, `formula_version=v1`)

See [phase-2d.md](../risk/phase-2d.md). Implemented in risk-service (no LLM):

```
riskScore = round(
    0.40 * cvss               # CVSS base * 10
  + 0.25 * assetCriticality   # LOW=25 MEDIUM=50 HIGH=75 CRITICAL=100
  + 0.15 * internetExposure   # boolean: false=0 true=100
  + 0.10 * exploitability     # exploit_available: false=0 true=75
  + 0.10 * activeExploitation # actively_exploited: false=0 true=100
, 2)
```

Levels: `LOW` 0–24.99, `MEDIUM` 25–49.99, `HIGH` 50–74.99, `CRITICAL` 75–100.

Stored on `risk_assessments` (one row per `finding_id`). Reasons JSON holds the generated explanation plus factor breakdown.

## JSON columns

`raw_source_payload`, `metadata`, `match_explanation`, `risk_reasons`, `audit.metadata` use JSONB for incomplete external data without schema churn.

## pgvector

`knowledge_chunks.embedding vector(1536)` matches OpenAI `text-embedding-3-small`. IVFFlat/HNSW can be added when volume warrants; V1 uses a vector cosine index where supported.
