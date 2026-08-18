# Logical table ownership

One PostgreSQL database is shared (hackathon pragmatism; ADR-003). **Do not** add per-service databases or duplicate tables. Writers should respect this map even though FKs span services.

| Table | Logical owner (writer) | Typical readers |
|-------|------------------------|-----------------|
| organizations, users, roles, user_roles | api-service | all |
| assets, asset_software | api-service (inventory) | correlation, api |
| cve_raw_records, vulnerabilities, vulnerability_cpe | ingestion-service | api, correlation, risk, ai (via tools later) |
| findings | correlation-service | api, risk, ai |
| risk_assessments | risk-service | api, ai |
| remediation_plans, knowledge_documents, knowledge_chunks | ai-service (api-service updates approval columns) | api, notification |
| notifications | notification-service | api |
| audit_logs | all (append) | api |
| event_processing_records | the consuming service | ops |

api-service has **read/orchestration** access to everything and is the **only** service that applies Flyway.
