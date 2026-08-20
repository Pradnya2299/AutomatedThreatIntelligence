# Database artifacts

- `migrations/` — **canonical** Flyway history (copied into api-service at build; see ADR-005). V5 adds finding match_type / match_confidence for Phase 2C.
- `seed/demo_seed.sql` — fictional Northwind inventory, CVEs, and demo findings/risk/remediation rows for the catalog tabs. Not applied by Flyway.
