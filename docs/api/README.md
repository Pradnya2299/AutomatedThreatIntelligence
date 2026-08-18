# HTTP API (Phase 1)

The dashboard talks **only** to **api-service** (default `http://localhost:8080`).

OpenAPI generation (springdoc) will be added when resource endpoints exist. Phase 1 exposes health only.

## Implemented now

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/` or `/api` | public | Explains which endpoints exist in Phase 2A |
| GET | `/api/health` | public | Liveness-style JSON: service name, status, timestamp |
| GET | `/api/me` | HTTP Basic | Current in-memory user (`admin` / `admin_change_me`, etc.) |
| GET | `/actuator/health` | public | Spring Boot actuator (includes db + redis) |
| GET | `/actuator/info` | public | Build info when available |

## Planned (later phases) — contract names only

- `GET /api/dashboard`
- `GET /api/vulnerabilities`, `GET /api/vulnerabilities/{id}`
- `GET /api/assets`, `GET /api/assets/{id}`
- `GET /api/findings`, `GET /api/findings/{id}`
- `GET /api/risk-assessments/{id}`
- `GET /api/remediation-plans`, `GET /api/remediation-plans/{id}`
- `POST /api/remediation-plans/{id}/approve`
- `POST /api/remediation-plans/{id}/reject`
- `POST /api/ingestion/cve`
- `GET /api/system/status`

Responses will use DTOs, not JPA entities. Error body (planned):

```json
{
  "code": "FINDING_NOT_FOUND",
  "message": "Finding not found",
  "correlationId": "uuid",
  "timestamp": "ISO-8601"
}
```

## Headers

Clients should send `X-Correlation-Id`. If absent, api-service generates one and returns it on the response.
