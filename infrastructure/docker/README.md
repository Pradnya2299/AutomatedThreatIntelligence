# Docker notes

Compose file at the repository root (`docker-compose.yml`) starts infrastructure only:

- PostgreSQL 16 + pgvector
- Redis 7
- Apache Kafka 3.9 in KRaft mode (no ZooKeeper)
- Kafka UI
- One-shot topic initializer

Application images are intentionally omitted in Phase 1 so engineers can run Spring Boot and Vite on the host against this stack.
