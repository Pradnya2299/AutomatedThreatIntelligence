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

# Reach published Postgres from a Flyway container on Linux, macOS, and Docker Desktop (Windows).
# Do not use --network host: it does not expose Windows localhost to Linux containers.
docker run --rm \
  --add-host=host.docker.internal:host-gateway \
  -v "${ROOT}/database/migrations:/flyway/sql:ro" \
  flyway/flyway:10 \
  -url="jdbc:postgresql://host.docker.internal:${POSTGRES_PORT:-5432}/${POSTGRES_DB:-threat_advisor}" \
  -user="${POSTGRES_USER:-threat_advisor}" \
  -password="${POSTGRES_PASSWORD:-threat_advisor_dev_change_me}" \
  -connectRetries=10 \
  migrate
