# Phase 2C — Asset / vulnerability correlation

correlation-service owns this pipeline. Risk scoring, RAG, OpenAI, dashboards, and notifications are out of scope.

## Flow

```
cve.normalized
        → validate envelope
        → event_processing_records (unique event_id + consumer)
        → load vulnerability + vulnerability_cpe
        → SQL filter asset_software by normalized vendor + product
        → version compare / range evaluate
        → upsert findings (unique asset_id + vulnerability_id)
        → commit
        → publish finding.created
```

Internal-only trigger (not for React): `POST /internal/correlation/run/{cveId}`.

## Normalization

| Field | Rule |
|-------|------|
| vendor / product | trim, lowercase (`Locale.ROOT`), drop non-alphanumeric. `http_server` = `http-server` = `HTTP Server` |
| version | not identity-normalized; parsed by Maven `ComparableVersion` |

## CPE matching

- CPE 2.3: `cpe:2.3:part:vendor:product:version:...`
- CPE 2.2 URI is accepted when present.
- Vendor **or** product mismatch → **no finding**.
- CPE version `*` without range columns → **no finding** (wildcard is not “all versions”).
- CPE version `-` (NA) without ranges → `CPE_MATCH` / HIGH (product identity, typical for OS rows).
- Exact CPE version, no ranges → installed version must be equal → `EXACT_VERSION_MATCH`.
- Range columns (`version_start_including` / `_excluding`, `version_end_including` / `_excluding`) → installed version must lie in the interval → `VERSION_RANGE_MATCH`.

Limitations: escaped colons in CPE components are not decoded; edition / architecture / package manager are ignored when absent.

## Version comparison

Maven `ComparableVersion` (artifact `maven-artifact` 3.9.9) is used because it is the algorithm Maven itself uses for dotted numeric versions. It is **not** `String.compareTo`.

Required examples: `2.4.10 > 2.4.9`, `17.0.9 > 17.0.8`, `21.0.1 > 21.0.0`.

## Confidence (persisted findings)

| Level | Rule | Persist by default? |
|-------|------|---------------------|
| HIGH | vendor + product + exact version, in-range version, or NA CPE product match | yes |
| MEDIUM | vendor + product match but installed version missing / not comparable (`PARTIAL_MATCH`) | no (`correlation.persist-medium-confidence`) |
| LOW | reserved; engine does not emit LOW in this phase | no |

## Finding uniqueness / idempotency

- `UNIQUE (asset_id, vulnerability_id)` on `findings`.
- Existing rows are updated (match type, confidence, explanation, `updated_at`); `status` is left unchanged.
- Kafka duplicates: `event_processing_records` unique `(event_id, consumer)` with consumer `correlation-service:cve.normalized`.

## Transactions vs Kafka

Finding upsert + processing record share one transaction. `finding.created` is published **after commit**. There is **no outbox** in this phase, so a crash after commit and before publish can drop the Kafka event. Duplicate `cve.normalized` deliveries will not duplicate findings.

## `finding.created` payload

```json
{
  "findingId": "uuid",
  "vulnerabilityId": "uuid",
  "assetId": "uuid",
  "cveId": "CVE-2025-1234",
  "matchType": "EXACT_VERSION_MATCH",
  "matchConfidence": "HIGH",
  "matchExplanation": "Asset web-prod-01 is affected by ..."
}
```

## Query strategy

Candidates are loaded with PostgreSQL `regexp_replace(lower(vendor/product), '[^a-z0-9]', '', 'g')` plus `installation_status = INSTALLED` and `assets.status = ACTIVE`. The engine never loads the full inventory for a CVE.

## Security

CVE documents and inventory strings are untrusted data. The engine does not execute commands, SSH, or change hosts. Analysis only.
