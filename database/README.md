# Database artifacts

- `migrations/` — **canonical** Flyway history (copied into api-service at build; see ADR-005). V4 adds `cve_raw_records` for Phase 2B.
- `seed/demo_seed.sql` — fictional Northwind inventory and CVEs for demo scenarios A–E. Not applied by Flyway.
