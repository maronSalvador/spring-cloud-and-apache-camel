# camel-orchestrator

Spring Boot + Apache Camel replacement for an IBM ACE flow that synchronously
orchestrates a client request across system APIs.

## Flow

```
client POST /process  (X-Correlation-Id header)
  1. validate payload via system API          -> audit log
  2. fetch bearer token via token API         -> audit log
  3. process payload via process API (XML)    -> transform XML -> JSON -> audit log
  <- transformed JSON
```

Behavior:

- **Fail-fast**: any downstream HTTP error or timeout terminates the flow and
  returns the configured default error (`orchestrator.default-error.*`),
  enriched with the client's correlation id.
- **No token caching**: the token API is called on every client request.
- **Audit transparency**: the `direct:audit` route logs
  `{correlationId, step, payload, timestamp}` and restores the flow body, so
  auditing never alters the data in flight.

## Build & run

Requires JDK 17+. The flow now runs as three services plus Postgres:

```
client -> camel-gateway :8090  (circuit breaker, retry on connect failures, timeouts)
       -> camel-orchestrator :8080  (the Camel flow)
       -> camel-system-api :9001    (POST /validate, /token, /process)
```

The system API and gateway live in the sibling projects `../camel-system-api`
and `../camel-gateway` (each is a standalone Gradle build with the same
`gradlew test` / `gradlew bootRun` workflow).

The orchestrator reads its downstream URLs from the `sys_endpoints` table.

### Full stack with docker compose

From the parent directory containing all three projects — builds each
project's image and starts Postgres + all three services:

```bash
cd ..
docker compose up --build
```

`db/init.sh` seeds `sys_endpoints` with the compose-internal hostnames
(`http://system-api:9001/*`). Call the gateway on
`http://localhost:8090/process`.

pgAdmin is included for inspecting `sys_endpoints` / `audit_log`:
`http://localhost:5050` — login `admin@orchestrator.dev` / `admin`
(configured in `pgadmin/servers.json`); the stack's Postgres is already
registered, password `orchestrator`.

For local (non-Docker) runs:

```bash
cd ../camel-system-api && gradle bootRun   # system APIs on :9001
cd ../camel-gateway    && gradle bootRun   # gateway on :8090
gradle bootRun                             # orchestrator on :8080
```

Override the database per environment with `DB_URL` / `DB_USER` / `DB_PASSWORD`.
For a bare (non-Docker) Postgres, seed it manually with
`psql -f db/init.sql` (localhost system-API URLs).

### Single-container option

The whole stack — PostgreSQL, orchestrator, system API and gateway — also
builds into **one container** (handy for demos; services then talk over
localhost, which matches the seeded endpoint URLs). From the parent
directory that contains all three projects:

```bash
docker build -t camel-orchestrator-stack .
docker run --name orchestrator-stack \
    -p 8090:8090 -p 8080:8080 -p 9001:9001 \
    camel-orchestrator-stack
```

First start seeds the database from `db/init.sh` automatically (its
`SYSTEM_API_URL` default points at localhost, which is correct when
everything shares one container).

## Try it

Through the gateway (resilience applied):

```bash
curl -X POST http://localhost:8090/process \
  -H 'Content-Type: application/json' \
  -H 'X-Correlation-Id: my-trace-001' \
  -d '{"order":"A-1"}'
```

Or directly against the orchestrator on `:8080`. Circuit breaker state:
`http://localhost:8090/actuator/circuitbreakers`.

A ready-made Postman collection covering all of the above (happy paths,
system APIs in isolation, error cases, circuit-breaker observation) is at
`postman/camel-orchestrator.postman_collection.json` — import it into
Postman and run.

## Key files

| Path | Purpose |
|---|---|
| `route/OrchestrationRoute.java` | The whole flow: entry point, steps, audit, error handling |
| `processor/XmlToJsonProcessor.java` | 1:1 XML→JSON (Jackson `XmlMapper`) — swap for JOLT/XSLT if reshaping needed |
| `processor/DefaultErrorResponseProcessor.java` | The single error exit |
| `config/OrchestratorProperties.java` | Typed config bound from `application.yml` |
