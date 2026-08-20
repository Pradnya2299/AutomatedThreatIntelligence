# Event contracts

## Common envelope

All Kafka payloads are JSON objects with this outer shape:

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "risk.calculated",
  "eventVersion": 1,
  "timestamp": "2026-08-18T12:00:00Z",
  "source": "risk-service",
  "correlationId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "payload": {}
}
```

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| eventId | UUID string | yes | Idempotency key |
| eventType | string | yes | Equals topic name for V1 |
| eventVersion | int | yes | Start at 1; additive payload changes preferred |
| timestamp | ISO-8601 UTC | yes | Producer time |
| source | string | yes | Service artifact name |
| correlationId | UUID string | yes | End-to-end CVE workflow trace |
| payload | object | yes | Event-specific; may be empty for heartbeat tests |

Consumers **must ignore unknown payload fields** (forward compatible).

## Payload sketches (V1, implemented later)

### cve.raw

Published after a raw row is committed. `eventId` is stable per `(source, cveId)`.

```json
{
  "rawRecordId": "uuid",
  "cveId": "CVE-2024-90001",
  "source": "manual",
  "payloadHash": "sha256-hex",
  "receivedAt": "ISO-8601",
  "raw": { }
}
```

The original document is also stored in `cve_raw_records.payload` (immutable).

### cve.normalized

IDs only so downstream services load the canonical row (avoids duplicating large NVD documents on the bus).

```json
{
  "vulnerabilityId": "uuid",
  "cveId": "CVE-2024-90001",
  "severity": "CRITICAL",
  "rawRecordEventId": "uuid"
}
```

### cve.enriched

```json
{
  "vulnerabilityId": "uuid",
  "cveId": "CVE-2024-12345",
  "exploitAvailable": true,
  "activelyExploited": false
}
```

### asset.updated

```json
{
  "assetId": "uuid",
  "organizationId": "uuid",
  "changeType": "CREATED|UPDATED|SOFTWARE_CHANGED"
}
```

### finding.created

Published by correlation-service after a finding is inserted or updated. IDs plus match metadata for Phase 2D risk — not the full `findings` row.

```json
{
  "findingId": "uuid",
  "vulnerabilityId": "uuid",
  "assetId": "uuid",
  "cveId": "CVE-2024-12345",
  "matchType": "EXACT_VERSION_MATCH",
  "matchConfidence": "HIGH",
  "matchExplanation": "Asset web-prod-01 is affected by CVE-2024-12345 because it runs Apache HTTP Server 2.4.49, which matches the vulnerable CPE cpe:2.3:a:apache:http_server:2.4.49:*:*:*:*:*:*:*."
}
```

### risk.calculated

Published by risk-service after upserting `risk_assessments`.

```json
{
  "riskAssessmentId": "uuid",
  "findingId": "uuid",
  "vulnerabilityId": "uuid",
  "assetId": "uuid",
  "cveId": "CVE-2021-44228",
  "riskScore": 97.50,
  "riskLevel": "CRITICAL",
  "factors": {
    "cvss": 100,
    "assetCriticality": 100,
    "internetExposure": 100,
    "exploitability": 75,
    "activeExploitation": 100
  },
  "explanation": "Risk is CRITICAL (97.50) because ..."
}
```

### remediation.generated

Published by ai-service after a validated GENERATED plan is committed.

```json
{
  "remediationPlanId": "uuid",
  "findingId": "uuid",
  "riskAssessmentId": "uuid",
  "cveId": "CVE-2021-44228",
  "assetId": "uuid",
  "priority": "IMMEDIATE",
  "summary": "...",
  "recommendedAction": "...",
  "status": "GENERATED"
}
```

### security.investigation.requested

Optional async trigger. REST investigations do not publish this event (avoids double execution).

```json
{
  "cveId": "CVE-2021-44228"
}
```

### security.investigation.completed

Published by ai-service after the orchestrator finishes (including controlled failures).

```json
{
  "investigationId": "uuid",
  "cveId": "CVE-2021-44228",
  "status": "COMPLETED",
  "affected": true,
  "riskScore": 91.50,
  "riskLevel": "CRITICAL",
  "remediationPlanId": "uuid"
}
```

### notification.requested

```json
{
  "channel": "IN_APP|EMAIL|SLACK",
  "template": "RISK_CRITICAL",
  "resourceType": "FINDING",
  "resourceId": "uuid"
}
```

## Serialization

UTF-8 JSON. No Java-specific types. Dates are ISO-8601 strings. A shared Java record library may be added in Phase 2 if duplication appears; Phase 1 documents the contract only (principle: no premature shared module).
