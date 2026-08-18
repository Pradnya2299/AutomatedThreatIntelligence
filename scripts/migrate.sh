#!/usr/bin/env bash
# Apply canonical Flyway migrations to local Postgres using the Flyway Docker image.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
if [[ -f "${ROOT}/.env" ]]; then
  set -a
  # shellcheck source=/dev/null
  source "${ROOT}/.env"
  set +a
fi

# Host networking so this works the same way as Spring Boot on the developer machine.
docker run --rm \
  --network host \
  -v "${ROOT}/database/migrations:/flyway/sql:ro" \
  flyway/flyway:10 \
  -url="jdbc:postgresql://127.0.0.1:${POSTGRES_PORT:-5432}/${POSTGRES_DB:-threat_advisor}" \
  -user="${POSTGRES_USER:-threat_advisor}" \
  -password="${POSTGRES_PASSWORD:-threat_advisor_dev_change_me}" \
  -connectRetries=10 \
  migrate
