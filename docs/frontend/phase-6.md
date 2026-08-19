# Phase 6 — React SOC investigation dashboard

Phase 6 adds investigation screens to the existing **frontend/security-dashboard** (React, TypeScript, Vite, Tailwind). It presents Phase 5B investigation JSON from **api-service**. The UI does not recalculate risk, match CPEs, or invent CVEs/assets.

## Frontend architecture

```
Browser  →  Vite (:5173)  →  proxy /api  →  api-service (:8080)  →  ai-service investigations
```

Typed models live in `src/types/investigation.ts` and match `InvestigationResponse` on ai-service.

API access is centralized:

- `src/services/api/client.ts` — GET/POST, Basic auth, error mapping
- `src/services/api/investigations.ts` — `POST/GET /api/v1/investigations`

React Query loads detail. If `status` is `RUNNING`/`PENDING`, the page polls every 2s and stops on `COMPLETED` / `FAILED` / `REVIEW_REQUIRED`. POST is typically synchronous and already terminal.

## Component architecture

| Component | Role |
|---|---|
| `AgentPipeline` / `AgentCard` | Threat → Asset → Risk → Remediation from executions + trace |
| `DecisionTimeline` | Backend `decisionHistory` only |
| `EvidencePanel` | Grouped by source; DETERMINISTIC vs INTERPRETATION |
| `RiskCard` | Backend `riskAnalysis` + threat fields; no local formula |
| `AssetTable` | Correlation assets; version only if present in `matchReason` |
| `RemediationPanel` | Backend remediation; hidden when absent / REVIEW_REQUIRED |
| `HumanReviewBanner` | REVIEW_REQUIRED and FAILED |

## Screens

| Path | Page |
|---|---|
| `/investigations` | Start CTA + open-by-UUID. Session recents only (not a server catalog) |
| `/investigations/new` | CVE form → `POST /api/v1/investigations` |
| `/investigations/:investigationId` | Full investigation |

Phase 4 catalog screens remain under `/dashboard`, `/vulnerabilities`, `/findings`, `/assets`, `/remediation`.

There is **no** investigation list API. The UI does not fabricate a catalog. Session storage only remembers IDs the user opened.

## Environment variables

| Variable | Purpose |
|---|---|
| `VITE_API_BASE_URL` | Optional origin. Empty (default) uses relative `/api` via the Vite proxy |
| `VITE_API_USER` | Local Basic user (default `analyst`) |
| `VITE_API_PASSWORD` | Local Basic password (default `analyst_change_me`) |

Do not commit real secrets. Defaults match `.env.example` local-dev credentials.

`AI_DEMO_MODE=true` is a **backend** flag. The dashboard displays `[DEMO MODE]` text when the API returns it.

## Authentication

HTTP Basic, same as Phase 4. Vite proxy attaches credentials for `/api`. `client.ts` also sends `Authorization` for direct `VITE_API_BASE_URL` use.

## Local startup

```bash
docker compose up -d
# api-service (Flyway V8), correlation :8082, risk :8083, ai-service :8084 (AI_DEMO_MODE=true), api :8080

cd frontend/security-dashboard
npm install
npm run dev
```

Open http://localhost:5173 (redirects to `/investigations`).

## Demo instructions

1. Start investigation → `CVE-2021-44228`.
2. Confirm pipeline Threat → Asset → Risk → Remediation, decision history, evidence, assets, risk **97.5** from the engine, `[DEMO MODE]` remediation.
3. Start `CVE-2099-0000` → **HUMAN REVIEW REQUIRED**, no remediation card.
4. Facts strip: CVE / assets / risk are engines; AI orchestrates the recommendation.

## Polling

Only while status is not terminal. No WebSockets.

## Tests

```bash
cd frontend/security-dashboard
npm test
npm run build
```

## Known limitations

- No investigation list API; recents are session-only IDs the operator opened.
- `AffectedAssetMatch` has no dedicated version/CPE columns; the table shows version only when it appears in `matchReason`, otherwise **Not available**.
- Remediation DTO has no `affectedComponents` or `downtimeExpected`; those rows show **Not available**.
- POST `/api/v1/investigations` is typically synchronous; polling is only used if status is still RUNNING/PENDING.
