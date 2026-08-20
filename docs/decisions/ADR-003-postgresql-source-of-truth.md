# ADR-003: PostgreSQL as source of truth

## Status

Accepted (Phase 1)

## Context

Findings, risk, inventory, and audit cannot live only in Redis or Kafka. pgvector is required for RAG. Independently scalable services often imply database-per-service, which makes “which assets have this CVE?” a distributed query.

## Decision

- **One PostgreSQL 16 database** with **pgvector**, **pgcrypto**, and **pg_trgm**.
- Flyway SQL in `database/migrations/` is canonical; api-service applies it.
- Redis is cache/locks/rate-limit (`cve:enrichment:*`, `ai:remediation:*`, `rag:query:*`, `correlation:*`, `lock:event:*`) and **never** the system of record.
- Logical table ownership is documented in service-boundaries.md; foreign keys still span services in this database.

## Consequences

- Correlation and dashboard queries stay simple SQL.
- Schema migrations must stay backward compatible (multiple services deploy independently).
- A future split (e.g. knowledge DB) can happen without changing the product contract.

## Alternatives considered

- Redis as primary store: rejected (volatility, poor relational query).
- MongoDB for raw CVE JSON only: extra system; JSONB is enough.
- Database-per-service immediately: rejected as over-engineering for Phase 1–3.
