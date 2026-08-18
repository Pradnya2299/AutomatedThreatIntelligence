-- V1: core domain schema for Automated Threat Intelligence & Patch Advisor.
-- Canonical copy lives in database/migrations and api-service classpath db/migration.

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE organizations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(64) NOT NULL UNIQUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE roles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations (id),
    username        VARCHAR(128) NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(255) NOT NULL,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE user_roles (
    user_id         UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id         UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE assets (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id       UUID NOT NULL REFERENCES organizations (id),
    hostname              VARCHAR(255) NOT NULL,
    ip_address            INET,
    operating_system      VARCHAR(128),
    os_version            VARCHAR(64),
    architecture          VARCHAR(32),
    environment           VARCHAR(32) NOT NULL,
    owner                 VARCHAR(255),
    department            VARCHAR(128),
    business_criticality  VARCHAR(32) NOT NULL,
    internet_exposure     BOOLEAN NOT NULL DEFAULT FALSE,
    status                VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    location_region       VARCHAR(128),
    metadata              JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_assets_org_hostname UNIQUE (organization_id, hostname)
);

CREATE INDEX idx_assets_hostname ON assets (hostname);
CREATE INDEX idx_assets_environment ON assets (environment);
CREATE INDEX idx_assets_internet_exposure ON assets (internet_exposure);
CREATE INDEX idx_assets_criticality ON assets (business_criticality);

CREATE TABLE asset_software (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id            UUID NOT NULL REFERENCES assets (id) ON DELETE CASCADE,
    vendor              VARCHAR(255) NOT NULL,
    product             VARCHAR(255) NOT NULL,
    version             VARCHAR(128),
    cpe                 VARCHAR(512),
    installation_status VARCHAR(32) NOT NULL DEFAULT 'INSTALLED',
    first_seen          TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_asset_software_vendor_product_version
    ON asset_software (vendor, product, version);
CREATE INDEX idx_asset_software_cpe ON asset_software (cpe);

CREATE TABLE vulnerabilities (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cve_id                    VARCHAR(32) NOT NULL UNIQUE,
    description               TEXT,
    published_at              TIMESTAMPTZ,
    modified_at               TIMESTAMPTZ,
    cvss_score                NUMERIC(3, 1),
    cvss_vector               VARCHAR(128),
    severity                  VARCHAR(32),
    cwe                       VARCHAR(32),
    affected_vendors          TEXT[],
    affected_products         TEXT[],
    exploit_available         BOOLEAN,
    actively_exploited        BOOLEAN,
    source                    VARCHAR(64),
    source_url                TEXT,
    raw_source_payload        JSONB,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_vulnerabilities_cve_id ON vulnerabilities (cve_id);
CREATE INDEX idx_vulnerabilities_severity ON vulnerabilities (severity);
CREATE INDEX idx_vulnerabilities_published_at ON vulnerabilities (published_at);
CREATE INDEX idx_vulnerabilities_actively_exploited ON vulnerabilities (actively_exploited);

CREATE TABLE vulnerability_cpe (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vulnerability_id        UUID NOT NULL REFERENCES vulnerabilities (id) ON DELETE CASCADE,
    cpe                     VARCHAR(512),
    vendor                  VARCHAR(255),
    product                 VARCHAR(255),
    version_start_including VARCHAR(128),
    version_start_excluding VARCHAR(128),
    version_end_including   VARCHAR(128),
    version_end_excluding   VARCHAR(128),
    operating_system        VARCHAR(128),
    architecture            VARCHAR(32)
);

CREATE INDEX idx_vulnerability_cpe_vendor_product
    ON vulnerability_cpe (vendor, product);
CREATE INDEX idx_vulnerability_cpe_cpe ON vulnerability_cpe (cpe);

CREATE TABLE findings (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID NOT NULL REFERENCES organizations (id),
    asset_id            UUID NOT NULL REFERENCES assets (id),
    vulnerability_id    UUID NOT NULL REFERENCES vulnerabilities (id),
    status              VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    match_explanation   JSONB NOT NULL DEFAULT '{}'::jsonb,
    detected_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_findings_asset_vuln UNIQUE (asset_id, vulnerability_id)
);

CREATE INDEX idx_findings_status ON findings (status);
CREATE INDEX idx_findings_vulnerability ON findings (vulnerability_id);

CREATE TABLE risk_assessments (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    finding_id              UUID NOT NULL UNIQUE REFERENCES findings (id) ON DELETE CASCADE,
    technical_risk          NUMERIC(5, 2) NOT NULL,
    exploitability_score    NUMERIC(5, 2) NOT NULL,
    exposure_score          NUMERIC(5, 2) NOT NULL,
    business_impact_score   NUMERIC(5, 2) NOT NULL,
    asset_criticality_score NUMERIC(5, 2) NOT NULL,
    final_risk_score        NUMERIC(5, 2) NOT NULL,
    risk_level              VARCHAR(16) NOT NULL,
    formula_version         VARCHAR(32) NOT NULL DEFAULT 'v1',
    reasons                 JSONB NOT NULL DEFAULT '[]'::jsonb,
    calculated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_risk_assessments_final_score ON risk_assessments (final_risk_score);
CREATE INDEX idx_risk_assessments_level ON risk_assessments (risk_level);

CREATE TABLE remediation_plans (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    finding_id              UUID NOT NULL REFERENCES findings (id),
    status                  VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    priority                VARCHAR(16),
    summary                 TEXT,
    reason                  TEXT,
    recommended_action      TEXT,
    patch_version           VARCHAR(128),
    temporary_mitigation    TEXT,
    verification_steps      JSONB NOT NULL DEFAULT '[]'::jsonb,
    rollback_plan           TEXT,
    confidence              NUMERIC(4, 3),
    retrieved_context       JSONB NOT NULL DEFAULT '[]'::jsonb,
    model_raw_output        JSONB,
    approved_by             UUID REFERENCES users (id),
    approved_at             TIMESTAMPTZ,
    rejection_reason        TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_remediation_plans_status ON remediation_plans (status);
CREATE INDEX idx_remediation_plans_finding ON remediation_plans (finding_id);

CREATE TABLE knowledge_documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID REFERENCES organizations (id),
    title           VARCHAR(512) NOT NULL,
    document_type   VARCHAR(64) NOT NULL,
    source          VARCHAR(512),
    body            TEXT NOT NULL,
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE knowledge_chunks (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id     UUID NOT NULL REFERENCES knowledge_documents (id) ON DELETE CASCADE,
    chunk_index     INTEGER NOT NULL,
    content         TEXT NOT NULL,
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    embedding       vector(1536),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_knowledge_chunks_document ON knowledge_chunks (document_id);

CREATE TABLE notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID REFERENCES organizations (id),
    channel         VARCHAR(32) NOT NULL,
    recipient       VARCHAR(255),
    title           VARCHAR(512) NOT NULL,
    body            TEXT,
    status          VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    resource_type   VARCHAR(64),
    resource_id     UUID,
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at         TIMESTAMPTZ
);

CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor           VARCHAR(255) NOT NULL,
    action          VARCHAR(128) NOT NULL,
    resource_type   VARCHAR(64) NOT NULL,
    resource_id     VARCHAR(64),
    correlation_id  UUID,
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_action ON audit_logs (action);
CREATE INDEX idx_audit_logs_correlation ON audit_logs (correlation_id);

CREATE TABLE event_processing_records (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        UUID NOT NULL UNIQUE,
    event_type      VARCHAR(128) NOT NULL,
    consumer        VARCHAR(128) NOT NULL,
    status          VARCHAR(32) NOT NULL,
    processed_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX idx_event_processing_consumer ON event_processing_records (consumer, event_type);
