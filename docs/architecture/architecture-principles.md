# Architecture principles

These rules are binding for this repository. They exist to keep a cybersecurity product **safe, explainable, and operable**.

1. **Separate deployable services, one repo.** Independent scale and failure isolation without a microservice explosion of repositories.
2. **PostgreSQL is source of truth.** Redis is cache, locks, and rate limits only.
3. **Kafka for facts that other services must react to.** REST for user-wait paths.
4. **Thin adapters.** Controllers and consumers translate I/O; domain services decide.
5. **Repositories own SQL/JPA.** No queries in controllers or LLM tools beyond explicit service methods.
6. **External systems behind interfaces.** NVD, EDR, CMDB, OpenAI, Slack are adapters.
7. **Deterministic risk.** Same inputs → same scores. Formula is versioned and documented.
8. **Structured LLM output + schema validation.** Never trust raw completions.
9. **No arbitrary SQL for the model.** Tools such as `getVulnerability()` only.
10. **Idempotent consumers.** `eventId` + `event_processing_records` + DB uniqueness + optional Redis lock.
11. **Human approval before destructive remediation.** Phase 1–4 simulate execution.
12. **Audit sensitive actions.** Actor, action, resource, ids, correlationId, metadata.
13. **Trace with correlationId** from HTTP or first Kafka event through the pipeline.
14. **Flyway-owned schema.** `ddl-auto=validate` (or `none` until entities exist). Never auto-create in production.
15. **No secrets in Git.** `.env.example` documents keys; real values stay local or in a secret manager.
16. **DTOs at the HTTP boundary.** Entities stay internal.
17. **Prefer the simplest production-quality option** when the spec is ambiguous; record it as an ADR.
18. **Do not over-abstract.** No shared “enterprise framework” layer until duplication is real.
19. **Incomplete CVE data is normal.** Ingestion must not crash the pipeline on missing CVSS or CPE.
20. **Explainability is a feature.** Findings and risk store *why*, not only scores.
