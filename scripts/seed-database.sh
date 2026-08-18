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

PGHOST="${POSTGRES_HOST:-localhost}"
PGPORT="${POSTGRES_PORT:-5432}"
PGUSER="${POSTGRES_USER:-threat_advisor}"
PGPASSWORD="${POSTGRES_PASSWORD:-threat_advisor_dev_change_me}"
PGDATABASE="${POSTGRES_DB:-threat_advisor}"
export PGPASSWORD

echo "Seeding ${PGDATABASE} on ${PGHOST}:${PGPORT}..."
SEED="${ROOT}/database/seed/demo_seed.sql"
if command -v psql >/dev/null 2>&1; then
  psql -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d "${PGDATABASE}" -f "${SEED}"
else
  echo "psql not found on PATH; seeding through the Postgres container."
  docker exec -i threat-advisor-postgres \
    psql -U "${PGUSER}" -d "${PGDATABASE}" < "${SEED}"
fi
echo "Seed complete."
