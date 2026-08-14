# syntax=docker/dockerfile:1

FROM maven:3.9.11-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src ./src
RUN mvn --batch-mode --no-transfer-progress package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN apk add --no-cache curl \
    && addgroup -S repofleet \
    && adduser -S -G repofleet -u 10001 repofleet

COPY --from=build --chown=repofleet:repofleet /workspace/target/quarkus-app/lib/ ./lib/
COPY --from=build --chown=repofleet:repofleet /workspace/target/quarkus-app/*.jar ./
COPY --from=build --chown=repofleet:repofleet /workspace/target/quarkus-app/app/ ./app/
COPY --from=build --chown=repofleet:repofleet /workspace/target/quarkus-app/quarkus/ ./quarkus/

ENV QUARKUS_HTTP_HOST=0.0.0.0 \
    QUARKUS_HTTP_PORT=8080 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

EXPOSE 8080

USER repofleet

HEALTHCHECK --interval=10s --timeout=3s --start-period=15s --retries=5 \
  CMD curl --fail --silent --show-error http://127.0.0.1:${QUARKUS_HTTP_PORT}/api/status >/dev/null || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar quarkus-run.jar"]
