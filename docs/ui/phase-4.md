# Phase 4 — SOC dashboard

React talks **only** to api-service. HTTP Basic is attached by the Vite `/api` proxy for local development (default `analyst` / `analyst_change_me`). api-service production security is unchanged.

## Routes

| Path | Page |
|------|------|
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

Approve/Review buttons are disabled (Phase 5).
