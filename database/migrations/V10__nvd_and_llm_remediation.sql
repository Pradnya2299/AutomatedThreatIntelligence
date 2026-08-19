-- Phase 7A/7B: NIST NVD provenance on vulnerabilities + LLM audit on code remediation jobs.
-- Does not replace vulnerabilities or V9 remediation tables.

ALTER TABLE vulnerabilities
    ADD COLUMN IF NOT EXISTS source_identifier VARCHAR(128),
    ADD COLUMN IF NOT EXISTS ingested_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS intelligence_source VARCHAR(32),
    ADD COLUMN IF NOT EXISTS affected_versions TEXT[],
    ADD COLUMN IF NOT EXISTS fixed_versions TEXT[];

UPDATE vulnerabilities
SET intelligence_source = CASE
    WHEN lower(coalesce(source, '')) IN ('nvd', 'nist', 'nist-nvd') THEN 'NVD'
    ELSE 'SEED'
END
WHERE intelligence_source IS NULL;

CREATE TABLE nvd_sync_state (
    id                          VARCHAR(32) PRIMARY KEY,
    last_successful_sync_at     TIMESTAMPTZ,
    last_mod_start_at           TIMESTAMPTZ,
    last_mod_end_at             TIMESTAMPTZ,
    last_attempt_at             TIMESTAMPTZ,
    last_status                 VARCHAR(32),
    last_error                  TEXT,
    created_count               INTEGER NOT NULL DEFAULT 0,
    updated_count               INTEGER NOT NULL DEFAULT 0,
    unchanged_count             INTEGER NOT NULL DEFAULT 0,
    page_count                  INTEGER NOT NULL DEFAULT 0,
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO nvd_sync_state (id, last_status, updated_at)
VALUES ('default', 'NEVER_RUN', now())
ON CONFLICT (id) DO NOTHING;

COMMENT ON TABLE nvd_sync_state IS 'Logical owner: ingestion-service. Incremental NVD 2.0 sync checkpoint.';
COMMENT ON COLUMN vulnerabilities.intelligence_source IS 'NVD, NVD_CACHE, or SEED. Never a fabricated CVE body.';
COMMENT ON COLUMN vulnerabilities.source_identifier IS 'NVD CVE sourceIdentifier when ingested from NIST.';

ALTER TABLE code_remediation_jobs
    ADD COLUMN IF NOT EXISTS intelligence_mode VARCHAR(32),
    ADD COLUMN IF NOT EXISTS intelligence_source VARCHAR(32),
    ADD COLUMN IF NOT EXISTS model_name VARCHAR(128),
    ADD COLUMN IF NOT EXISTS prompt_version VARCHAR(32),
    ADD COLUMN IF NOT EXISTS code_analysis_json JSONB,
    ADD COLUMN IF NOT EXISTS llm_patch_plan_json JSONB,
    ADD COLUMN IF NOT EXISTS generated_patch_json JSONB;

COMMENT ON COLUMN code_remediation_jobs.intelligence_mode IS 'LLM_POWERED, DEMO_MODE, or DETERMINISTIC_FALLBACK.';
