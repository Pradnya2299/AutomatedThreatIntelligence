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

```json
{
  "source": "nvd|manual|osv",
  "sourceUrl": "https://...",
  "receivedAt": "ISO-8601",
  "raw": { }
}
```

### cve.normalized

```json
{
  "vulnerabilityId": "uuid",
  "cveId": "CVE-2024-12345"
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

```json
{
  "findingId": "uuid",
  "assetId": "uuid",
  "vulnerabilityId": "uuid",
  "cveId": "CVE-2024-12345"
}
```

### risk.calculated

```json
{
  "riskAssessmentId": "uuid",
  "findingId": "uuid",
  "finalRiskScore": 94,
  "riskLevel": "CRITICAL"
}
```

### remediation.requested / generated / approved / completed

```json
{
  "remediationPlanId": "uuid",
  "findingId": "uuid",
  "status": "PENDING_APPROVAL|APPROVED|REJECTED|COMPLETED|FAILED"
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
