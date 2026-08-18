# RAG architecture

## Why RAG

Remediation advice must reflect **this organization’s** patch windows, emergency-change rules, and OS standards — not generic CVE blog text. Retrieval-augmented generation attaches those documents to the AI context **without** giving the model database-wide access.

## Storage

PostgreSQL + **pgvector**:

- `knowledge_documents` — title, type, source, raw body, metadata
- `knowledge_chunks` — text, ordinal, metadata, `embedding vector`
- Document types (seed targets): vulnerability management, patch management, emergency change, Linux/Windows security standards, application security, asset criticality, incident response

Embeddings are stored next to chunks so retrieval is transactional with the rest of the system of record. Redis may cache identical AI JSON responses; it is never the source of truth.

Phase 3 implements classpath ingest, demo or OpenAI embeddings, and cosine-distance search via pgvector (`<=>`).

## Pipeline (target, not Phase 1 runtime)

```
Knowledge document
        → chunk (stable size/overlap, later phase)
        → embedding (OpenAI text-embedding-3-small or equivalent)
        → pgvector insert
        → searchKnowledgeBase(query, filters)
        → ranked chunks in AI context
        → structured remediation JSON
```

The LLM never embeds or searches by issuing SQL. `searchKnowledgeBase` is a Java tool.

## Retrieval policy

- Filter by document type when the orchestrator knows the asset OS (Linux vs Windows standards).
- Return chunk text + source + document type so the UI can show “retrieved policy/context”.
- If retrieval is empty, the plan must still validate; confidence should drop; UI should show no policy grounding.

## Phase 1

Schema and seed **document text** are included. Chunk embeddings may be null until Phase 4. A vector index is created for when embeddings land.
