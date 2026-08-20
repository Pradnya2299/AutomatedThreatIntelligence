-- Phase 2B: immutable raw CVE intake + extra normalized columns.
-- Canonical uniqueness for raw rows: (source, external_id). payload_hash is audit-only (not unique)
-- so a later payload revision from another source can still be stored as a different (source, id) pair.
-- Same source + same CVE ID is first-write-wins; the payload column is never updated.

CREATE TABLE cve_raw_records (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source              VARCHAR(64) NOT NULL,
    external_id         VARCHAR(32) NOT NULL,
    received_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    payload             JSONB NOT NULL,
    payload_hash        VARCHAR(64) NOT NULL,
    processing_status   VARCHAR(32) NOT NULL,
    error_message       TEXT,
    event_id            UUID NOT NULL,
    correlation_id      UUID NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_cve_raw_source_external UNIQUE (source, external_id),
    CONSTRAINT ck_cve_raw_status CHECK (processing_status IN ('ACCEPTED', 'NORMALIZED', 'FAILED'))
);

CREATE INDEX idx_cve_raw_event_id ON cve_raw_records (event_id);
CREATE INDEX idx_cve_raw_payload_hash ON cve_raw_records (payload_hash);
CREATE INDEX idx_cve_raw_status ON cve_raw_records (processing_status);

COMMENT ON TABLE cve_raw_records IS 'Logical owner: ingestion-service. Immutable original CVE documents.';
COMMENT ON COLUMN cve_raw_records.payload IS 'Original request JSON. Never updated after insert.';
COMMENT ON COLUMN cve_raw_records.payload_hash IS 'SHA-256 hex of the raw request bytes. Not a uniqueness key.';
COMMENT ON COLUMN cve_raw_records.event_id IS 'Stable Kafka eventId for cve.raw (name-based UUID of source+CVE).';

ALTER TABLE vulnerabilities
    ADD COLUMN metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN cvss_metrics JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN cwes TEXT[],
    ADD COLUMN reference_urls TEXT[];

COMMENT ON COLUMN vulnerabilities.cvss_metrics IS 'All CVSS versions preserved. Canonical score/severity use v4 > v3.1 > v3.0 > v2.';
COMMENT ON COLUMN vulnerabilities.metadata IS 'Unmapped CVE fields (other languages, leftover NVD nodes).';

CREATE UNIQUE INDEX uq_vulnerability_cpe_vuln_cpe
    ON vulnerability_cpe (vulnerability_id, COALESCE(cpe, ''));
