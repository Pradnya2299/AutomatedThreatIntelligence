-- Enable PostgreSQL extensions required by Threat Advisor.
-- Runs once on an empty data volume (docker-entrypoint-initdb.d) as the superuser.
-- Application Flyway also uses CREATE EXTENSION IF NOT EXISTS as a safety net.

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Allow the application role to create extensions on a fresh database if needed.
GRANT CREATE ON DATABASE threat_advisor TO CURRENT_USER;
