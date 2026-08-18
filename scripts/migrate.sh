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

# Join the Compose network and talk to service name "postgres".
# This works on Docker Desktop (Windows/macOS) and Linux. Avoid --network host.
# MSYS_NO_PATHCONV prevents Git Bash from rewriting the Linux mount path.
MSYS_NO_PATHCONV=1 docker run --rm \
  --network threat-advisor \
  -v "${ROOT}/database/migrations:/flyway/sql:ro" \
  flyway/flyway:10 \
  -url="jdbc:postgresql://postgres:5432/${POSTGRES_DB:-threat_advisor}" \
  -user="${POSTGRES_USER:-threat_advisor}" \
  -password="${POSTGRES_PASSWORD:-threat_advisor_dev_change_me}" \
  -connectRetries=10 \
  migrate
