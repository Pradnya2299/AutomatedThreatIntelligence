# ADR-004: AI safety and human approval

## Status

Accepted (Phase 1)

## Context

The platform uses OpenAI for contextual remediation. Cybersecurity operations cannot allow unconstrained model actions: hallucinated risk, arbitrary SQL, or unattended production patching.

## Decision

1. **Risk engine is deterministic code**, not an LLM (see database-design.md formula).
2. **AI has no JDBC.** Context is gathered via allow-listed tools (`getVulnerability`, `searchKnowledgeBase`, …).
3. **Model output is JSON schema validated** before persistence.
4. Remediation plans enter **`PENDING_APPROVAL`**. Only `SECURITY_MANAGER` (or `ADMIN`) may approve/reject; actions are audited.
5. The hackathon **simulates** remediation after approval (`remediation.completed` with simulation metadata). No destructive production adapters in initial phases.
6. Redis may cache AI responses; the durable plan is PostgreSQL.

## Consequences

- Slightly more engineering than “call ChatGPT in a controller”.
- Analysts see explainable risk even if OpenAI is down.
- Approval UX is mandatory in the dashboard.

## Alternatives considered

- LLM-assigned risk scores: rejected (non-deterministic, unauditable).
- Auto-patch on CRITICAL: rejected for safety.
- Letting the model write SQL “with a read-only user”: still too broad; tools only.
