-- Phase 5A: multi-agent CVE investigation state.
-- Existing findings/risk/remediation tables cannot represent per-agent execution.

CREATE TABLE security_investigations (
    id                      UUID PRIMARY KEY,
    cve_id                  VARCHAR(32) NOT NULL,
    status                  VARCHAR(32) NOT NULL,
    correlation_id          UUID,
    threat_result           JSONB NOT NULL DEFAULT '{}'::jsonb,
    asset_result            JSONB NOT NULL DEFAULT '{}'::jsonb,
    risk_result             JSONB NOT NULL DEFAULT '{}'::jsonb,
    remediation_result      JSONB NOT NULL DEFAULT '{}'::jsonb,
    executions              JSONB NOT NULL DEFAULT '[]'::jsonb,
    errors                  JSONB NOT NULL DEFAULT '[]'::jsonb,
    evidence                JSONB NOT NULL DEFAULT '[]'::jsonb,
    recommendation          JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at            TIMESTAMPTZ,
    CONSTRAINT ck_security_investigations_status CHECK (status IN (
        'PENDING',
        'RUNNING',
        'COMPLETED',
        'FAILED',
        'REVIEW_REQUIRED'
    ))
);

CREATE INDEX idx_security_investigations_cve ON security_investigations (cve_id);
CREATE INDEX idx_security_investigations_status ON security_investigations (status);
CREATE INDEX idx_security_investigations_created ON security_investigations (created_at DESC);

COMMENT ON TABLE security_investigations IS
    'Phase 5A orchestrator state for a CVE investigation. Agent results are typed JSON snapshots, not LLM scratchpads.';
