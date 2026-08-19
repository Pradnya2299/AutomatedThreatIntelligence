#!/usr/bin/env bash
set -euo pipefail

BOOTSTRAP="${KAFKA_BOOTSTRAP_SERVERS:-localhost:9092}"

echo "Waiting for Kafka at ${BOOTSTRAP}..."
for i in $(seq 1 40); do
  if /opt/kafka/bin/kafka-topics.sh --bootstrap-server "${BOOTSTRAP}" --list >/tmp/kafka-topics.out 2>/tmp/kafka-topics.err; then
    echo "Kafka is reachable."
    break
  fi
  sleep 2
  if [[ "${i}" -eq 40 ]]; then
    echo "Kafka did not become reachable:"
    cat /tmp/kafka-topics.err
    exit 1
  fi
done

echo "Creating Kafka topics against ${BOOTSTRAP}..."

create_topic() {
  local topic="$1"
  local partitions="${2:-3}"
  /opt/kafka/bin/kafka-topics.sh --bootstrap-server "${BOOTSTRAP}" \
    --create --if-not-exists \
    --topic "${topic}" \
    --partitions "${partitions}" \
    --replication-factor 1
}

create_topic "cve.raw" 3
create_topic "cve.normalized" 3
create_topic "cve.enriched" 3
create_topic "asset.updated" 3
create_topic "finding.created" 3
create_topic "risk.calculated" 3
create_topic "remediation.requested" 3
create_topic "remediation.generated" 3
create_topic "notification.requested" 3
create_topic "remediation.approved" 1
create_topic "remediation.completed" 1
create_topic "security.investigation.requested" 3
create_topic "security.investigation.completed" 3

create_topic "cve.raw.dlq" 1
create_topic "cve.normalized.dlq" 1
create_topic "cve.enriched.dlq" 1
create_topic "finding.created.dlq" 1
create_topic "risk.calculated.dlq" 1
create_topic "remediation.requested.dlq" 1
create_topic "remediation.generated.dlq" 1
create_topic "notification.requested.dlq" 1
create_topic "remediation.approved.dlq" 1
create_topic "security.investigation.requested.dlq" 1
create_topic "security.investigation.completed.dlq" 1

echo "Kafka topics:"
/opt/kafka/bin/kafka-topics.sh --bootstrap-server "${BOOTSTRAP}" --list
