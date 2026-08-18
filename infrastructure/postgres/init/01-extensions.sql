-- Enable PostgreSQL extensions required by Threat Advisor.
-- Runs once on empty data volume via docker-entrypoint-initdb.d.

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
