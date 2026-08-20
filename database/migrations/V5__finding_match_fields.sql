-- Phase 2C: finding match classification (explanation remains JSONB).
ALTER TABLE findings
    ADD COLUMN match_type VARCHAR(32),
    ADD COLUMN match_confidence VARCHAR(16);

CREATE INDEX IF NOT EXISTS idx_findings_match_type ON findings (match_type);
CREATE INDEX IF NOT EXISTS idx_findings_confidence ON findings (match_confidence);

COMMENT ON COLUMN findings.match_type IS 'EXACT_VERSION_MATCH | VERSION_RANGE_MATCH | CPE_MATCH';
COMMENT ON COLUMN findings.match_confidence IS 'HIGH | MEDIUM | LOW. Engine persists HIGH only by default.';
