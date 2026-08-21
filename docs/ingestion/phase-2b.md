# Phase 2B — CVE ingestion and normalization

ingestion-service owns this pipeline. Correlation, risk, AI, and the dashboard are out of scope.

## Flow

```
POST /internal/ingestion/cve  (raw JSON)
        → validate CVE ID + description + size
        → INSERT cve_raw_records (immutable payload)
        → commit
        → publish cve.raw
        → consumer
        → INSERT event_processing_records (unique event_id + consumer)
        → UPSERT vulnerabilities + replace vulnerability_cpe
        → mark raw row NORMALIZED
        → commit
        → publish cve.normalized (IDs only)
```

## Uniqueness

| Record | Key |
|--------|-----|
| `cve_raw_records` | `UNIQUE (source, external_id)` — first write wins; `payload` is never updated |
| `payload_hash` | SHA-256 of request bytes, **not unique** (audit / change detection) |
| `vulnerabilities` | `UNIQUE (cve_id)` |
| Kafka `eventId` for `cve.raw` | Name-based UUID of `cve.raw\|{source}\|{cveId}` |

## CVSS canonical rule

Preserve v4, v3.1, v3.0, and v2 in `vulnerabilities.cvss_metrics`.

Canonical `cvss_score` / `cvss_vector` / `severity`: **v4 > v3.1 > v3.0 > v2**. No LLM.

## Idempotency

- Duplicate HTTP POST for the same source + CVE returns `200` / `DUPLICATE` with the original `eventId`. If the row is still `ACCEPTED`, `cve.raw` is republished (producer retry).
- Duplicate Kafka delivery: unique `(event_id, consumer)` on `event_processing_records`. Second delivery skips DB writes.
- Database constraints are the last line of defense, not Redis or an in-memory Set.

## Transactions vs Kafka

Vulnerability + CPE + event_processing (+ raw status) share one transaction.

Kafka publish runs **after commit**. There is **no outbox** in this phase, so a crash after commit and before publish can drop `cve.raw` / `cve.normalized`. Duplicate HTTP for `ACCEPTED` rows republishes `cve.raw`.

## Internal API

`POST /internal/ingestion/cve` — not for the React app.

`POST /internal/ingestion/cve/fixture/{name}` — classpath files under `cve-fixtures/` only (no filesystem paths).

Max body: 512 KiB.
