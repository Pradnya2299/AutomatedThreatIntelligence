# Phase 4 — SOC dashboard

React talks **only** to api-service. HTTP Basic is attached by the Vite `/api` proxy for local development (default `analyst` / `analyst_change_me`). api-service production security is unchanged.

## Routes

| Path | Page |
|------|------|
| `/investigations` | Investigation dashboard (Phase 6) |
| `/investigations/new` | Start CVE investigation |
| `/investigations/:id` | Agent pipeline, decisions, evidence, risk, remediation |
| `/dashboard` | KPIs, top CVEs, risk bars, activity |
| `/vulnerabilities` | Searchable CVE table |
| `/vulnerabilities/:cveId` | CVE detail, assets, AI plan |
| `/findings` | Finding table |
| `/findings/:id` | Match explanation, risk, AI advisor |
| `/assets` | Inventory |
| `/assets/:id` | Software + findings |
| `/remediation` | AI plans |
| `/remediation/:id` | Full plan |
| `/settings` | Local auth notes |

Approve/Review buttons are disabled (later approval phase). Investigation UX is Phase 6: [phase-6.md](../frontend/phase-6.md).
