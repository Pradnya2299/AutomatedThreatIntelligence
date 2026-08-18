# risk-service

Deterministic finding risk engine (Phase 2D). Formula is documented in [docs/risk/phase-2d.md](../../docs/risk/phase-2d.md).

## Run

```bash
cd backend
./mvnw -pl risk-service spring-boot:run
```

Health: http://localhost:8083/api/health

Internal: `POST /internal/risk/run/{findingId}`
