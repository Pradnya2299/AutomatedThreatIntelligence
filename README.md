# Automated Threat Intelligence & Patch Advisor

AI-driven vulnerability intelligence and remediation orchestration for SecOps.

This repository is a **multi-service, event-driven** platform (not a single Spring Boot monolith). **Phase 2B** adds CVE ingestion and normalization. Domain engines (correlation, risk, RAG, OpenAI) are **not** implemented yet.

## Current phase

**Phase 2B — CVE ingestion / normalization.** See [docs/ingestion/phase-2b.md](docs/ingestion/phase-2b.md).

## Repository layout

```
.
├── backend/                 # Java 21 / Spring Boot 3 services (Maven)
│   ├── api-service/
│   ├── ingestion-service/
│   ├── correlation-service/
│   ├── risk-service/
│   ├── ai-service/
│   └── notification-service/
├── frontend/security-dashboard/   # React + TypeScript + Vite
├── database/migrations/     # Canonical Flyway SQL
├── database/seed/           # Demo inventory and CVEs (scenarios A–E)
├── infrastructure/          # Postgres, Kafka, Redis, Docker notes
├── docs/                    # Architecture, API, Kafka, ADRs
├── scripts/
└── docker-compose.yml
```

## Prerequisites

- JDK 21
- Maven 3.9+ (or the Maven Wrapper in `backend/`)
- Node.js 20+
- Docker and Docker Compose
- Optional: copy `.env.example` to `.env` and adjust local passwords

## Run infrastructure

From the repository root:

```bash
cp .env.example .env   # first time only
chmod +x infrastructure/kafka/create-topics.sh scripts/*.sh
docker compose up -d
docker compose run --rm kafka-init
```

On **Git Bash for Windows**, do not pass `/opt/...` as a raw `docker exec` argument. MSYS rewrites it to `C:/Program Files/Git/opt/...`. List topics like this:

```bash
docker compose exec kafka bash -c '/opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:29092 --list'
```

Services:

| Component | URL / port |
|-----------|------------|
| PostgreSQL | `localhost:5432` database `threat_advisor` |
| Redis | `localhost:6379` |
| Kafka | `localhost:9092` |
| Kafka UI | http://localhost:8088 |

## Run Flyway (without starting Spring)

After Compose is healthy:

```bash
./scripts/migrate.sh
./scripts/seed-database.sh
./scripts/verify-infra.sh
```

Alternatively start api-service; it applies the same `database/migrations/` files via Flyway.

## Run backend services

From `backend/` (requires infrastructure):

```bash
./mvnw -pl api-service spring-boot:run
./mvnw -pl ingestion-service spring-boot:run
./mvnw -pl correlation-service spring-boot:run
./mvnw -pl risk-service spring-boot:run
./mvnw -pl ai-service spring-boot:run
./mvnw -pl notification-service spring-boot:run
```

Health checks (Phase 1):

| Service | Port | Health |
|---------|------|--------|
| api-service | 8080 | http://localhost:8080/api/health and `/actuator/health` |
| ingestion-service | 8081 | http://localhost:8081/api/health |
| correlation-service | 8082 | http://localhost:8082/api/health |
| risk-service | 8083 | http://localhost:8083/api/health |
| ai-service | 8084 | http://localhost:8084/api/health |
| notification-service | 8085 | http://localhost:8085/api/health |

## Run frontend

```bash
cd frontend/security-dashboard
npm install
npm run dev
```

Dashboard: http://localhost:5173 (proxies `/api` to api-service on 8080).

## Architecture snapshot

- **Kafka** carries asynchronous domain events (`cve.raw` → … → `remediation.completed`).
- **REST** is used by the dashboard against **api-service** only.
- **PostgreSQL** is the system of record; **pgvector** stores knowledge embeddings.
- **Redis** is cache, idempotency locks, and rate limiting — never source of truth.
- **Risk scores are deterministic** (no LLM). AI generates structured remediation proposals after tool-based context gathering, with **human approval** before any destructive action.
- Services are independently deployable; Phase 1 shares one Postgres database (documented in ADR-003).

## Documentation

- [System architecture](docs/architecture/system-architecture.md)
- [Service boundaries](docs/architecture/service-boundaries.md)
- [Architecture principles](docs/architecture/architecture-principles.md)
- [AI architecture](docs/architecture/ai-architecture.md)
- [RAG architecture](docs/architecture/rag-architecture.md)
- [Database design](docs/database/database-design.md)
- [ER model](docs/database/er-model.md)
- [Kafka architecture](docs/kafka/kafka-architecture.md)
- [Event contracts](docs/kafka/event-contracts.md)
- [ADRs](docs/decisions/)

## Next phase (recommended)

**Phase 2C** should implement **CPE correlation against seeded assets**, **findings**, and **deterministic risk** — still without OpenAI or RAG.

## License

Private hackathon / internal project unless otherwise specified.
