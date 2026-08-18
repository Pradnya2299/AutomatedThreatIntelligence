# ADR-005: Canonical Flyway location

## Status

Accepted (Phase 2A)

## Context

Phase 1 duplicated SQL in `database/migrations/` and `api-service/src/main/resources/db/migration`, which would diverge.

## Decision

- Canonical files: `database/migrations/`
- api-service is the sole Flyway runner
- Maven copies migrations into the api-service classpath at build time
- Seed SQL stays outside Flyway

## Consequences

- Developers edit one directory
- `spring-boot:run` from `backend/api-service` still migrates after `mvn compile` / `spring-boot:run` (which runs generate-resources)
- IDEs that skip Maven resource copying must run Maven before expecting classpath migrations
