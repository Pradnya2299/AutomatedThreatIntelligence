# Kafka architecture

## Why Kafka

CVE workflows are **asynchronous**: enrichment, correlation, and AI can take longer than an HTTP timeout, and multiple consumers (risk, notifications, audit) need the same facts. Kafka provides ordered-per-key delivery, replay, and independent consumer scaling.

REST remains for dashboard reads and approval POSTs.

## Broker topology (local)

Phase 1 uses **Apache Kafka 3.9 in KRaft mode** (combined controller + broker). ZooKeeper is not used locally to reduce moving parts. See ADR-001. `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false`; topics are created by `infrastructure/kafka/create-topics.sh`.

Kafka UI: http://localhost:8088

## Topics

| Topic | Producer (intended) | Consumer (intended) |
|-------|---------------------|---------------------|
| cve.raw | api-service, future connectors | ingestion-service |
| cve.normalized | ingestion-service | correlation-service |
| cve.enriched | ingestion-service | correlation-service (later; Phase 2C uses cve.normalized) |
| asset.updated | api-service | correlation-service |
| finding.created | correlation-service | risk-service |
| risk.calculated | risk-service | ai-service, notification-service |
| remediation.requested | ai-service or api-service | ai-service |
| remediation.generated | ai-service | notification-service, api-service (read model) |
| security.investigation.requested | api-service or operators (optional) | ai-service |
| security.investigation.completed | ai-service | notification-service, api-service (later) |
| security.remediation.requested | api-service / ai-service | ai-service (lifecycle) |
| security.remediation.planned | ai-service | audit consumers |
| security.patch.generated | ai-service | audit consumers |
| security.patch.validated | ai-service | audit consumers |
| security.remediation.approval.requested | ai-service | dashboard / notification |
| security.remediation.approved | ai-service | GitHub/PR path |
| security.remediation.rejected | ai-service | audit |
| security.pullrequest.created | ai-service | notification |
| notification.requested | any | notification-service |
| remediation.approved | api-service | notification-service, future executor |
| remediation.completed | future executor / simulation | notification-service, api-service |

Dead-letter topics `*.dlq` receive exhausted retries. Phase 1 creates them; processors come later.

## Envelope

Every message uses the common envelope in [event-contracts.md](event-contracts.md). `correlationId` is copied from the inbound event or HTTP header `X-Correlation-Id`.

## Keys

Suggested record keys (Phase 2+): `cveId` for CVE topics, `findingId` for finding/risk/remediation, `assetId` for `asset.updated`. Keys keep per-CVE ordering.

## Idempotency

Consumers must:

1. Optionally take Redis lock `lock:event:{eventId}`
2. Insert `event_processing_records` (`event_id` unique) before side effects, or use the unique constraint as the conflict signal
3. Rely on domain uniqueness (CVE ID, finding pair)

## Retry

Spring Kafka default retry is not enough for poison messages. Target: bounded retries with backoff, then DLQ. Phase 1 configuration placeholders only.

## What is not in Phase 1

No production consumers, no Schema Registry. JSON envelopes are the contract. Avro/Schema Registry can be an ADR later if multiple languages appear (they will not in this repo).
