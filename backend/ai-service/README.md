# ai-service

RAG + structured remediation advisor. See [docs/ai/phase-3.md](../../docs/ai/phase-3.md).

```bash
export AI_DEMO_MODE=true   # default; no OpenAI key required
./mvnw -pl ai-service spring-boot:run
```

Health: http://localhost:8084/api/health

- `POST /internal/ai/knowledge/ingest`
- `POST /internal/ai/remediation/{findingId}`
