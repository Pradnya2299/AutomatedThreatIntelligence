-- Additional lookup indexes. Vector IVFFlat is deferred until embeddings exist.

CREATE INDEX IF NOT EXISTS idx_assets_metadata_gin ON assets USING gin (metadata);
CREATE INDEX IF NOT EXISTS idx_vulnerabilities_raw_gin ON vulnerabilities USING gin (raw_source_payload);
CREATE INDEX IF NOT EXISTS idx_findings_explanation_gin ON findings USING gin (match_explanation);
