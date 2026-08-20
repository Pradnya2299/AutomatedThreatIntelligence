#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck disable=SC1091
if [[ -f "${ROOT}/.env" ]]; then
  set -a
  # shellcheck source=/dev/null
  source "${ROOT}/.env"
  set +a
fi

PGUSER="${POSTGRES_USER:-threat_advisor}"
PGPASSWORD="${POSTGRES_PASSWORD:-threat_advisor_dev_change_me}"
PGDATABASE="${POSTGRES_DB:-threat_advisor}"
PGHOST="${POSTGRES_HOST:-localhost}"
PGPORT="${POSTGRES_PORT:-5432}"
export PGPASSWORD

echo "== PostgreSQL =="
psql -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d "${PGDATABASE}" -c "SELECT extname FROM pg_extension WHERE extname = 'vector';"
psql -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d "${PGDATABASE}" -c "SELECT COUNT(*) AS assets FROM assets;"

echo "== Redis =="
redis-cli -h "${REDIS_HOST:-localhost}" -p "${REDIS_PORT:-6379}" ping

echo "== Kafka topics =="
list_topics() {
  # Quote the Kafka path so Git Bash (MSYS) does not rewrite /opt/... to C:/Program Files/Git/opt/...
  MSYS_NO_PATHCONV=1 docker compose -f "${ROOT}/docker-compose.yml" exec -T kafka \
    bash -c '/opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:29092 --list'
}
if docker info >/dev/null 2>&1; then
  list_topics
elif command -v sg >/dev/null; then
  sg docker -c "docker compose -f '${ROOT}/docker-compose.yml' exec -T kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:29092 --list"
else
  echo "Could not list Kafka topics (need Docker socket access)." >&2
  exit 1
fi

echo "== Kafka UI =="
curl -fsS "http://localhost:${KAFKA_UI_PORT:-8088}" >/dev/null && echo "kafka-ui reachable"

echo "Infrastructure checks passed."
