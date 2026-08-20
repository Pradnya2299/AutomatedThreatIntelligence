#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "${ROOT}"
docker compose up -d
echo "Infrastructure starting. Kafka UI: http://localhost:8088"
echo "Apply schema: cd backend && ./mvnw -pl api-service spring-boot:run"
echo "Then: ./scripts/seed-database.sh"
