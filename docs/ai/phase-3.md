# Phase 3 — Contextual remediation advisor (RAG + OpenAI)

ai-service consumes `risk.calculated` and writes `remediation_plans` as **GENERATED**. It does not decide vulnerability or risk. Those values come from correlation-service and risk-service.

## Demo vs live OpenAI

| `AI_DEMO_MODE` | Behavior |
|---|---|
| `true` (default) | Deterministic mock embeddings + mock plan labeled `[DEMO MODE]` / `model_name=demo-deterministic`. Not claimed to be OpenAI. |
| `false` | Official `openai-java` SDK. Requires `OPENAI_API_KEY`. Chat model: `OPENAI_MODEL` (default `gpt-4o-mini`). Embeddings: `OPENAI_EMBEDDING_MODEL` (default `text-embedding-3-small`). |

Never hardcode keys. Config: `openai.api-key=${OPENAI_API_KEY}` and `ai.api-key=${OPENAI_API_KEY}`.

## Chunking

~2000 characters (~500 tokens at ~4 chars/token) with 200-character overlap. Re-ingest deletes and replaces chunks for the same document `source` (`classpath:knowledge/{file}`).

## Embeddings

Isolated behind `EmbeddingService`. Live vectors are 1536-d (`vector(1536)`). Demo mode uses a hashed bag-of-words unit vector of the same size.

## Vector search

Query embedding → `ORDER BY embedding <=> query LIMIT topK` (`AI_RAG_TOP_K`, default 5). PostgreSQL + pgvector only.

## Prompt

`PromptBuilder` builds the system prompt and a structured user context (vulnerability, asset, risk, retrieved chunks). Retrieval query is a compact string from CVE, product, version, asset type/criticality, and risk.

## Redis

Optional cache `ai:remediation:{findingId}:{riskId}` TTL 1h. PostgreSQL is the source of truth. Cache failures are ignored.

## Idempotency

Consumer `ai-service:risk.calculated` on `event_processing_records`. Unique index on `remediation_plans (finding_id, risk_assessment_id)`. Existing **GENERATED** plans skip a new OpenAI/demo call.

## Local demo

```bash
./scripts/migrate.sh   # includes V6
./scripts/seed-database.sh
export AI_DEMO_MODE=true
# from backend/
./mvnw -pl ai-service spring-boot:run
curl -sS -X POST http://localhost:8084/internal/ai/knowledge/ingest
# after correlation + risk have created a finding:
curl -sS -X POST http://localhost:8084/internal/ai/remediation/{findingId}
```

Kafka path: produce/consume `risk.calculated` → persist plan → `remediation.generated`.
