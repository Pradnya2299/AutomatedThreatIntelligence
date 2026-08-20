-- Phase 7: autonomous code remediation (isolated workspace + human-approved PRs).
-- Does not overload security_investigations.

CREATE TABLE remediation_repository_bindings (
    id UUID PRIMARY KEY,
    organization_id UUID,
    asset_id UUID REFERENCES assets (id),
    hostname VARCHAR(255),
    application_name VARCHAR(255),
    provider VARCHAR(32) NOT NULL,
    organization VARCHAR(255),
    repository VARCHAR(255) NOT NULL,
    repository_url VARCHAR(1024) NOT NULL,
    default_branch VARCHAR(128) NOT NULL DEFAULT 'main',
    technology VARCHAR(64),
    build_system VARCHAR(64),
    confidence VARCHAR(16) NOT NULL DEFAULT 'HIGH',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_remediation_bindings_hostname ON remediation_repository_bindings (hostname);
CREATE INDEX idx_remediation_bindings_asset ON remediation_repository_bindings (asset_id);

CREATE TABLE code_remediation_jobs (
    id UUID PRIMARY KEY,
    investigation_id UUID NOT NULL REFERENCES security_investigations (id),
    cve_id VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    current_state VARCHAR(32) NOT NULL,
    initiated_by VARCHAR(128),
    correlation_id UUID,
    overall_confidence VARCHAR(16),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    review_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_code_remediation_jobs_investigation ON code_remediation_jobs (investigation_id);

CREATE TABLE remediation_targets (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES code_remediation_jobs (id) ON DELETE CASCADE,
    provider VARCHAR(32) NOT NULL,
    organization VARCHAR(255),
    repository VARCHAR(255) NOT NULL,
    default_branch VARCHAR(128) NOT NULL,
    ai_branch VARCHAR(255),
    technology VARCHAR(64),
    build_system VARCHAR(64),
    repository_url VARCHAR(1024) NOT NULL,
    workspace_path VARCHAR(2048),
    confidence VARCHAR(16),
    evidence JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE patch_plans (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES code_remediation_jobs (id) ON DELETE CASCADE,
    strategy_type VARCHAR(64) NOT NULL,
    rationale TEXT,
    affected_files JSONB NOT NULL DEFAULT '[]'::jsonb,
    expected_changes JSONB NOT NULL DEFAULT '[]'::jsonb,
    prerequisites JSONB NOT NULL DEFAULT '[]'::jsonb,
    risks JSONB NOT NULL DEFAULT '[]'::jsonb,
    rollback_plan TEXT,
    confidence VARCHAR(16),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE patch_executions (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES code_remediation_jobs (id) ON DELETE CASCADE,
    patch_plan_id UUID REFERENCES patch_plans (id),
    attempt_number INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    unified_diff TEXT,
    files_changed INTEGER NOT NULL DEFAULT 0,
    lines_added INTEGER NOT NULL DEFAULT 0,
    lines_deleted INTEGER NOT NULL DEFAULT 0,
    safety_status VARCHAR(32),
    safety_violations JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE patch_changes (
    id UUID PRIMARY KEY,
    patch_execution_id UUID NOT NULL REFERENCES patch_executions (id) ON DELETE CASCADE,
    file_path VARCHAR(1024) NOT NULL,
    change_type VARCHAR(32) NOT NULL,
    before_excerpt TEXT,
    after_excerpt TEXT
);

CREATE TABLE validation_runs (
    id UUID PRIMARY KEY,
    patch_execution_id UUID NOT NULL REFERENCES patch_executions (id) ON DELETE CASCADE,
    command VARCHAR(1024) NOT NULL,
    exit_code INTEGER,
    duration_ms BIGINT,
    stdout_summary TEXT,
    stderr_summary TEXT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE security_verifications (
    id UUID PRIMARY KEY,
    patch_execution_id UUID NOT NULL REFERENCES patch_executions (id) ON DELETE CASCADE,
    result VARCHAR(32) NOT NULL,
    details TEXT,
    evidence JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE approval_requests (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES code_remediation_jobs (id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL,
    decided_by VARCHAR(128),
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    decided_at TIMESTAMPTZ
);

CREATE TABLE code_pull_requests (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES code_remediation_jobs (id) ON DELETE CASCADE,
    provider VARCHAR(32) NOT NULL,
    repository VARCHAR(255) NOT NULL,
    branch VARCHAR(255) NOT NULL,
    commit_sha VARCHAR(64),
    pull_request_url VARCHAR(2048),
    pull_request_number INTEGER,
    skipped_reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE code_remediation_audit (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES code_remediation_jobs (id) ON DELETE CASCADE,
    agent_name VARCHAR(128),
    action VARCHAR(64) NOT NULL,
    tool_name VARCHAR(128),
    detail JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_code_remediation_audit_job ON code_remediation_audit (job_id);
