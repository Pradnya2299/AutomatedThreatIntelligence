-- Phase 5B: agentic investigation metadata on the existing investigations table.
ALTER TABLE security_investigations
    ADD COLUMN IF NOT EXISTS current_state VARCHAR(32) NOT NULL DEFAULT 'INITIALIZED',
    ADD COLUMN IF NOT EXISTS overall_confidence VARCHAR(16),
    ADD COLUMN IF NOT EXISTS iteration_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS decision_history JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS execution_trace JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS completed_agents JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS pending_agents JSONB NOT NULL DEFAULT '[]'::jsonb;

COMMENT ON COLUMN security_investigations.current_state IS
    'Granular investigation state (INITIALIZED … COMPLETED / REVIEW_REQUIRED / FAILED).';
COMMENT ON COLUMN security_investigations.decision_history IS
    'Typed orchestrator decisions (action, reason, evidence, result).';
COMMENT ON COLUMN security_investigations.execution_trace IS
    'Per-step agent/tool execution audit trail.';
