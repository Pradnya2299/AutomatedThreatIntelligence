# HTTP API

The dashboard talks **only** to **api-service** (default `http://localhost:8080`).

## Implemented

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/` or `/api` | public | Service index |
| GET | `/api/health` | public | Liveness JSON |
| GET | `/api/me` | HTTP Basic | Current in-memory user |
| GET | `/api/dashboard/summary` | HTTP Basic | KPIs, risk distribution, top CVEs, activity |
| GET | `/api/vulnerabilities` | HTTP Basic | Paged CVE list (`q`, `severity`, `risk`, `page`, `size`) |
| GET | `/api/vulnerabilities/{cveId}` | HTTP Basic | CVE detail + affected assets |
| GET | `/api/findings` | HTTP Basic | Paged findings |
| GET | `/api/findings/{id}` | HTTP Basic | Match explanation, risk, AI plan |
| GET | `/api/assets` | HTTP Basic | Paged assets |
| GET | `/api/assets/{id}` | HTTP Basic | Software + findings |
| GET | `/api/remediation` | HTTP Basic | Paged AI plans |
| GET | `/api/remediation/{id}` | HTTP Basic | Full structured plan |
| POST | `/api/v1/investigations` | HTTP Basic | Run multi-agent CVE investigation (proxied to ai-service) |
| GET | `/api/v1/investigations/{id}` | HTTP Basic | Investigation state and agent results |
| GET | `/actuator/health` | public | Actuator |

List responses: `{ content, page, size, totalElements, totalPages }`.

DTOs only — JPA entities are not returned.

Error body:

```json
{
  "code": "FINDING_NOT_FOUND",
  "message": "Unable to load that finding.",
  "correlationId": "uuid",
  "timestamp": "ISO-8601"
}
```
