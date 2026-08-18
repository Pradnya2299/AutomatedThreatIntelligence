# Flyway

## Canonical source

**`database/migrations/` is the only authoritative schema history.**

`api-service` copies those files onto the classpath at Maven `generate-resources` (`target/classes/db/migration`). There are **no** checked-in copies under `api-service/src/main/resources/db/migration`. Generate-resources deletes stale `*.sql` copies first so leftover Phase 1 filenames cannot collide on version numbers.

## Who applies migrations

**api-service** runs Flyway on startup (`spring.flyway.enabled=true`). Reasons:

- One writer for schema history (`flyway_schema_history`)
- Dashboard/API is always started in local demos
- Other services must not race to mutate DDL

Other services keep `spring.flyway.enabled=false` and `ddl-auto=validate` (no entities in Phase 2A, so validate is a no-op until mappings exist).

## What Flyway does not apply

Demo seed lives in `database/seed/demo_seed.sql` and is loaded with `./scripts/seed-database.sh` (or by the infrastructure Testcontainers test). Seed is **not** a Flyway version so production-shaped deploys can skip demo rows.

## Hibernate

`spring.jpa.hibernate.ddl-auto=validate` on every service. Never `create` / `create-drop` / `update`.
