# Single-container build: PostgreSQL + camel-orchestrator + camel-system-api
# + camel-gateway, all talking over localhost.
#
# Build (from ~/claude-projects, the directory containing all three projects):
#   docker build -t camel-orchestrator-stack .
# Run:
#   docker run --name orchestrator-stack -p 8090:8090 -p 8080:8080 -p 9001:9001 \
#       camel-orchestrator-stack
# Then: POST http://localhost:8090/process
#
# NOTE: one container running multiple services is a deliberate trade-off
# (convenience for demos/dev). For production, split into separate containers
# per service and keep the postgres-only compose workflow.

########## Stage 1: build all three Spring Boot jars ##########
FROM gradle:8.2-jdk17 AS build
WORKDIR /workspace

COPY camel-orchestrator/ camel-orchestrator/
COPY camel-system-api/ camel-system-api/
COPY camel-gateway/ camel-gateway/

# sh gradlew: copies from Windows carry no exec bit. --no-daemon: the
# daemon would linger in the build layer. Tests are run separately, not
# during image build.
RUN cd camel-system-api && sh gradlew bootJar -x test --no-daemon -q \
    && cd ../camel-gateway && sh gradlew bootJar -x test --no-daemon -q \
    && cd ../camel-orchestrator && sh gradlew bootJar -x test --no-daemon -q

########## Stage 2: postgres base + a JRE + the three jars ##########
FROM postgres:16-bookworm

# Bookworm's default JRE is 17 — matches the projects' toolchain
RUN apt-get update \
    && apt-get install -y --no-install-recommends openjdk-17-jre-headless \
    && rm -rf /var/lib/apt/lists/*

ENV POSTGRES_DB=orchestrator \
    POSTGRES_USER=orchestrator \
    POSTGRES_PASSWORD=orchestrator

# seeds sys_endpoints (-> http://localhost:9001/* via the script's default)
# and creates audit_log
COPY camel-orchestrator/db/init.sh /docker-entrypoint-initdb.d/init.sh
COPY docker/entrypoint.sh /usr/local/bin/stack-entrypoint.sh
COPY --from=build \
    /workspace/camel-system-api/build/libs/camel-system-api-0.0.1-SNAPSHOT.jar \
    /workspace/camel-gateway/build/libs/camel-gateway-0.0.1-SNAPSHOT.jar \
    /workspace/camel-orchestrator/build/libs/camel-orchestrator-0.0.1-SNAPSHOT.jar \
    /app/

EXPOSE 8090 8080 9001 5432

ENTRYPOINT ["stack-entrypoint.sh"]
