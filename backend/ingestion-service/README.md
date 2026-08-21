# ingestion-service

CVE intake, validation, normalization, persistence of `vulnerabilities` / `vulnerability_cpe`, and Kafka `cve.raw` / `cve.normalized`. See [docs/ingestion/phase-2b.md](../../docs/ingestion/phase-2b.md).

## Run

Infrastructure must be up, and **api-service (or Flyway) must have applied migrations through V4**.

```bash
cd backend
./mvnw -pl ingestion-service spring-boot:run
```

Health: http://localhost:8081/api/health

## Ingest a fixture

```bash
curl -sS -X POST http://localhost:8081/internal/ingestion/cve/fixture/cve-critical
curl -sS -X POST http://localhost:8081/internal/ingestion/cve \
  -H 'Content-Type: application/json' \
  --data-binary @src/main/resources/cve-fixtures/cve-high.json
```

Do not send this API from the React dashboard.

## Package layout

Controller → `CveIngestionService` / `CveNormalizationService`. Kafka listener only delegates to the normalization service.
