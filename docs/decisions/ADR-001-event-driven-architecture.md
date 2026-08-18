# ADR-001: Event-driven modular architecture

## Status

Accepted (Phase 1)

## Context

The product must ingest CVEs, correlate assets, score risk, generate AI remediation, and notify analysts. A single Spring Boot monolith would couple dashboard latency to ingestion and AI. Fully isolated microservices with per-service databases would complicate joins (CVE × software) for a hackathon-scale team.

## Decision

Use **multiple Spring Boot applications in one repository**, collaborating via **Kafka domain events** and a **shared PostgreSQL** (see ADR-003). REST is limited to api-service for the dashboard.

Local Kafka runs in **KRaft** mode (no ZooKeeper) to keep Compose small. Replication factor 1 is local-only.

## Consequences

- Services can scale and fail independently.
- Developers run only the service they are changing plus Compose infra.
- Distributed tracing depends on `correlationId` discipline.
- Dual writes (DB then Kafka) need outbox or careful idempotency in later phases; Phase 1 does not implement outbox (simplest path; document the gap).

## Alternatives considered

- Modular monolith, one process: rejected by the product spec.
- Per-service databases from day one: rejected as over-engineering for correlation queries.
- HTTP-only orchestration between services: rejected; too chatty and fragile for long AI/correlation jobs.
