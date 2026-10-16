#!/bin/bash
# Starts PostgreSQL (via the official image's entrypoint, which handles
# initdb + /docker-entrypoint-initdb.d on first run), waits for it, then
# brings up the three services. Everything shares the container's network
# namespace, so the seeded localhost URLs all resolve as-is.
set -e

docker-entrypoint.sh postgres &
PG_PID=$!

echo "[stack] waiting for PostgreSQL..."
until pg_isready -q -U "${POSTGRES_USER:-orchestrator}" -d "${POSTGRES_DB:-orchestrator}"; do
    sleep 1
done
echo "[stack] PostgreSQL is ready"

stop_all() {
    echo "[stack] shutting down"
    kill $(jobs -p) 2>/dev/null || true
    wait
}
trap stop_all SIGTERM SIGINT

java -jar /app/camel-system-api-0.0.1-SNAPSHOT.jar &
java -jar /app/camel-orchestrator-0.0.1-SNAPSHOT.jar &
java -jar /app/camel-gateway-0.0.1-SNAPSHOT.jar &

wait -n "$PG_PID" $(jobs -p)
exit $?
