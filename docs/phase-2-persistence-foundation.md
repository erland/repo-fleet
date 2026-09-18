# Phase 2 Persistence Foundation

Phase 2 Step 1 introduces PostgreSQL as RepoFleet's persistent backend store without moving repository inventory data yet.

## Runtime configuration

The backend uses:

- `REPOFLEET_DB_URL`
- `REPOFLEET_DB_USER`
- `REPOFLEET_DB_PASSWORD`

Compose additionally uses `REPOFLEET_DB_NAME` to create the database.

Flyway migrations run automatically at application startup. The initial migration creates only RepoFleet system metadata; repository identity and enrichment tables belong to later Phase 2 steps.

## Health and startup

The backend includes Quarkus SmallRye Health. Compose health checks use `/q/health/ready`, which includes datasource readiness, so frontend startup waits for both PostgreSQL and backend readiness.

The existing `/api/status` endpoint remains available for Phase 1 compatibility.

## CI

The backend CI job starts PostgreSQL 17 as a GitHub Actions service and runs Maven verification against it. The full Docker Compose product test also starts PostgreSQL as part of the normal runtime.

## Production

Both the Debian Compose deployment and the Coolify Compose deployment include a persistent PostgreSQL service and named volume. Database credentials must be supplied through deployment environment variables/secrets.

Detailed backup and migration operating procedures are intentionally deferred to Phase 2 Step 33.
