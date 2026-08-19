# System architecture

## Purpose

Automated Threat Intelligence & Patch Advisor is an event-driven SecOps platform that turns newly published CVEs into **explainable findings, deterministic risk, and human-approved remediation plans**.

Phase 1 establishes service boundaries, contracts, schema, and local infrastructure. Runtime engines (full ingestion, correlation, risk, RAG, OpenAI) are intentionally deferred.

## Why this shape

A single CRUD Spring Boot app wrapping ChatGPT would fail three product requirements:

1. **Independent scale** — ingestion bursts must not starve the dashboard API.
2. **Safety** — risk must not be invented by an LLM; destructive actions need approval and audit.
3. **Traceability** — a CVE workflow must be reconstructable via `correlationId` across logs, Kafka, and the database.

The chosen style is an **event-driven modular architecture**: multiple Spring Boot applications in one Git repository, sharing a documented domain model, communicating asynchronously over Kafka and synchronously over REST only where a user is waiting.

## Logical view

```
                    ┌─────────────────────┐
  Analyst browser → │  security-dashboard │
                    └──────────┬──────────┘
                               │ REST
                    ┌──────────▼──────────┐
                    │     api-service     │  ← Flyway (schema owner)
                    └──────────┬──────────┘
                               │ publish / read
          ┌────────────────────┼────────────────────┐
          ▼                    ▼                    ▼
   PostgreSQL+pgvector        Redis              Kafka
   (source of truth)     (cache / locks)    (domain events)
          ▲                    ▲                    ▲
          │                    │                    │
 ingestion-service    correlation-service     risk-service
          │                    │                    │
          └──────────► ai-service ◄─────────────────┘
                             │
                    notification-service
```

## Runtime flow (target, later phases)

```
External CVE source
        │
        ▼
   cve.raw                 ingestion-service (normalize, persist)
        │
        ▼
   cve.normalized          ingestion-service (enrich, cache in Redis)
        │
        ▼
   cve.enriched            correlation-service → findings
        │
        ▼
   finding.created         risk-service → risk_assessments
        │
        ▼
   risk.calculated         ai-service (tools + RAG + structured LLM output)
        │
        ▼
   remediation.requested / remediation.generated
        │
        ▼
   Human approval via api-service (SECURITY_MANAGER)
        │
        ▼
   remediation.approved → simulated execution → remediation.completed
        │
        ▼
   notification.requested  notification-service
```

Phase 3 wires `risk.calculated` → ai-service RAG + structured remediation → `remediation.generated`. Phase 5A adds an explicit multi-agent investigation on REST/`security.investigation.requested` without replacing that path. Approval and execution remain later.

## Communication rules

| Path | Mechanism | Reason |
|------|-----------|--------|
| Dashboard ↔ backend | REST via api-service | User-facing, request/response, authn/z |
| Cross-service domain work | Kafka events | Decoupling, retry, fan-out, scale |
| Cache / locks | Redis | Hot reads, idempotency, rate limits |
| Durable state | PostgreSQL | Findings, risk, audit, knowledge |

api-service may **publish** commands/events (for example ingestion requests or approval). Other services **must not** be called by the frontend.

## Data ownership (Phase 1)

One PostgreSQL database is shared. Logical ownership:

| Data | Writer (intended) | Readers |
|------|-------------------|---------|
| vulnerabilities, raw payloads | ingestion-service | api, correlation, risk, ai (via tools later) |
| assets, asset_software | api-service (seed/CMDB later) | correlation, api |
| findings | correlation-service | api, risk, ai |
| risk_assessments | risk-service | api, ai |
| remediation_plans | ai-service / api-service (approval) | api, notification |
| security_investigations | ai-service | api (via ai-service) |
| knowledge_* | api-service / future admin | ai-service via tool APIs, not raw SQL from the LLM |
| notifications | notification-service | api |
| audit_logs, event_processing_records | all services | api (admin) |

Sharing a database in Phase 1 is a **deliberate simplification** so correlation can join assets and CVEs without a distributed join. See ADR-003. Services remain separately deployable.

## Observability

- `correlationId` on every HTTP request and Kafka envelope
- Actuator `/actuator/health` on every service
- Structured logging fields: timestamp, service, level, correlationId, eventId, operation, duration
- Kafka UI for local event inspection

## What Phase 1 includes vs excludes

**Includes:** repo structure, docs, ADRs, Maven skeletons, health endpoints, Flyway V1 schema, seed SQL, Compose (Postgres, Redis, Kafka KRaft, Kafka UI), React route shell, event contracts.

**Excludes:** live NVD/OSV clients, full correlation/risk engines, OpenAI/RAG runtime, remediation execution, production Kubernetes, complete REST resource APIs.

## Phase 1 infrastructure clients

Kafka auto-configuration is enabled on **ingestion-service** for `cve.raw` / `cve.normalized`. **api-service** still excludes Kafka auto-config. Redis remains cache-only.

## Recommended next phases

1. **Phase 2A:** Docker Compose, Flyway, demo seed (done).
2. **Phase 2B:** CVE ingest, normalize, persist, Kafka `cve.raw` / `cve.normalized` (done).
3. **Phase 2C:** CPE/vendor/product/version correlation, findings, `finding.created` (done).
4. **Phase 2D:** deterministic risk on `finding.created` → `risk.calculated` (done).
5. **Phase 3 (this branch):** RAG + structured remediation on `risk.calculated` (done). No dashboard.
6. **Phase 4 — API + dashboard:** REST DTOs, authn/z, screens bound to real data (done).
7. **Phase 5A — multi-agent investigation:** orchestrator + tools on existing engines (this branch).
8. **Later:** approval UX, E2E hardening, DLQ processors, metrics, images.
