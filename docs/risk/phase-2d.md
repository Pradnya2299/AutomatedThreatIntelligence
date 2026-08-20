# Phase 2D — Deterministic risk engine

risk-service consumes `finding.created`, scores the finding, upserts `risk_assessments`, and publishes `risk.calculated`.

No OpenAI, RAG, dashboard, or remediation.

## Formula (`v1`)

Each factor is 0–100.

```
riskScore = round(
      cvss               * 0.40
    + assetCriticality   * 0.25
    + internetExposure   * 0.15
    + exploitability     * 0.10
    + activeExploitation * 0.10
  , 2)
```

Column mapping on existing `risk_assessments` (no new tables):

| Factor | Column |
|--------|--------|
| cvss | `technical_risk` |
| assetCriticality | `asset_criticality_score` |
| internetExposure | `exposure_score` |
| exploitability | `exploitability_score` |
| activeExploitation | `business_impact_score` |
| riskScore | `final_risk_score` |

`reasons` JSON stores the explanation text and factor object.

## Factor mappings

| Input | Mapping |
|-------|---------|
| CVSS base 0.0–10.0 | `score * 10` (linear). Bands 0.0–3.9 / 4.0–6.9 / 7.0–8.9 / 9.0–10.0 are CVSS labels, not extra buckets. Null → 0. |
| `business_criticality` | LOW=25, MEDIUM=50, HIGH=75, CRITICAL=100 |
| `internet_exposure` boolean | FALSE=0 (NOT_EXPOSED), TRUE=100 (INTERNET). INTERNAL=50 unused (no enum in schema). |
| `exploit_available` boolean | FALSE=0 (NONE), TRUE=75 (AVAILABLE). PROVEN=100 unused. |
| `actively_exploited` boolean | FALSE=0, TRUE=100 |

## Severity

| Score | Level |
|-------|-------|
| 0–24.99 | LOW |
| 25–49.99 | MEDIUM |
| 50–74.99 | HIGH |
| 75–100 | CRITICAL |

## Idempotency

- `risk_assessments.finding_id` unique — recalculation updates the row.
- `event_processing_records (event_id, consumer)` with consumer `risk-service:finding.created`.

Kafka publish is **after commit**. No outbox.

## Internal API

`POST /internal/risk/run/{findingId}` — not for React.

## Local check

```bash
./scripts/migrate.sh
./scripts/seed-database.sh
# start kafka, postgres, correlation-service, risk-service
curl -sS -X POST http://localhost:8082/internal/correlation/run/CVE-2021-44228
# then risk-service will consume finding.created, or:
curl -sS -X POST http://localhost:8083/internal/risk/run/{findingId}
```
