-- Phase 3: RAG/remediation columns. Reuses knowledge_* and remediation_plans.
ALTER TABLE knowledge_documents
    ADD CONSTRAINT uq_knowledge_documents_source UNIQUE (source);

ALTER TABLE remediation_plans
    ADD COLUMN IF NOT EXISTS risk_assessment_id UUID REFERENCES risk_assessments (id),
    ADD COLUMN IF NOT EXISTS affected_components JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS prerequisites JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS implementation_steps JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS downtime_expected BOOLEAN,
    ADD COLUMN IF NOT EXISTS reference_urls JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS model_name VARCHAR(128),
    ADD COLUMN IF NOT EXISTS prompt_version VARCHAR(32);

CREATE UNIQUE INDEX IF NOT EXISTS uq_remediation_finding_risk
    ON remediation_plans (finding_id, risk_assessment_id)
    WHERE risk_assessment_id IS NOT NULL;

ALTER TABLE remediation_plans DROP CONSTRAINT IF EXISTS ck_remediation_status;
ALTER TABLE remediation_plans ADD CONSTRAINT ck_remediation_status
    CHECK (status IN (
        'DRAFT',
        'GENERATED',
        'REVIEW_REQUIRED',
        'PENDING_APPROVAL',
        'APPROVED',
        'REJECTED',
        'COMPLETED',
        'FAILED'
    ));

COMMENT ON COLUMN knowledge_chunks.embedding IS
    'pgvector embedding; dimension 1536 for OpenAI text-embedding-3-small (or demo hash vectors).';
COMMENT ON COLUMN remediation_plans.status IS
    'Phase 3 writes GENERATED or FAILED. Approval/execution is later.';
