-- Seed script for LOCAL development (bare Postgres started outside Docker):
--   psql -U postgres -d orchestrator -f db/init.sql
--
-- The Docker flows (docker compose / single-container image) instead use
-- db/init.sh, which seeds the same tables but takes the system-api host
-- from the SYSTEM_API_URL env var (container hostnames, not localhost).

CREATE TABLE IF NOT EXISTS sys_endpoints (
    id       BIGSERIAL PRIMARY KEY,
    name     VARCHAR(255) NOT NULL,
    endpoint VARCHAR(512) NOT NULL
);

-- The route looks these names up at startup and caches them, so this
-- must exist before camel-orchestrator boots.
INSERT INTO sys_endpoints (name, endpoint) VALUES
    ('validation_url', 'http://localhost:9001/validate'),
    ('token_url',      'http://localhost:9001/token'),
    ('process_url',    'http://localhost:9001/process');

CREATE TABLE IF NOT EXISTS audit_log (
    id             BIGSERIAL PRIMARY KEY,
    correlation_id VARCHAR(255),
    interface_name VARCHAR(255),
    response       TEXT
);
