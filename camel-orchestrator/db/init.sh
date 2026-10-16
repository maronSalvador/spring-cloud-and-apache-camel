#!/bin/bash
# Executed by the postgres image entrypoint on first start.
# .sh (not .sql) so the endpoint URLs can come from the environment:
# - single-container stack: default http://localhost:9001 (same network namespace)
# - docker compose stack:   set SYSTEM_API_URL=http://system-api:9001
set -e
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE TABLE sys_endpoints (
        id       BIGSERIAL PRIMARY KEY,
        name     VARCHAR(255) NOT NULL,
        endpoint VARCHAR(512) NOT NULL
    );

    -- The route looks these names up at startup. Point them at the
    -- camel-system-api service.
    INSERT INTO sys_endpoints (name, endpoint) VALUES
        ('validation_url', '${SYSTEM_API_URL:-http://localhost:9001}/validate'),
        ('token_url',      '${SYSTEM_API_URL:-http://localhost:9001}/token'),
        ('process_url',    '${SYSTEM_API_URL:-http://localhost:9001}/process');

    CREATE TABLE audit_log (
        id             BIGSERIAL PRIMARY KEY,
        correlation_id VARCHAR(255),
        interface_name VARCHAR(255),
        response       TEXT
    );
EOSQL
