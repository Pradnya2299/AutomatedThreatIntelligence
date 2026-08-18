#!/usr/bin/env bash
set -euo pipefail

BOOTSTRAP="${KAFKA_BOOTSTRAP_SERVERS:-localhost:9092}"

echo "Creating Kafka topics against ${BOOTSTRAP}..."

create_topic() {
  local topic="$1"
  /opt/kafka/bin/kafka-topics.sh --bootstrap-server "${BOOTSTRAP}" \
    --create --if-not-exists \
    --topic "${topic}" \
    --partitions 1 \
    --replication-factor 1
}

create_topic "cve.raw"
create_topic "cve.normalized"
create_topic "cve.enriched"
create_topic "asset.updated"
create_topic "finding.created"
create_topic "risk.calculated"
create_topic "remediation.requested"
create_topic "remediation.generated"
create_topic "notification.requested"
create_topic "remediation.approved"
create_topic "remediation.completed"

# Dead-letter topics (retry exhaustion). Naming is documented in docs/kafka/.
create_topic "cve.raw.dlq"
create_topic "cve.normalized.dlq"
create_topic "cve.enriched.dlq"
create_topic "finding.created.dlq"
create_topic "risk.calculated.dlq"
create_topic "remediation.requested.dlq"
create_topic "remediation.generated.dlq"
create_topic "notification.requested.dlq"
create_topic "remediation.approved.dlq"

echo "Kafka topics:"
/opt/kafka/bin/kafka-topics.sh --bootstrap-server "${BOOTSTRAP}" --list
