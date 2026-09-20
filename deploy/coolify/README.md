# Coolify deployment

RepoFleet can be deployed in Coolify as a Docker Compose application using:

```text
deploy/coolify/compose.yaml
```

The Coolify profile uses the published frontend/backend GHCR images and a **separate shared PostgreSQL resource in Coolify**. It does not start its own PostgreSQL container.

The application services use project-specific names (`repo-fleet-frontend` and `repo-fleet-backend`) so they do not publish generic DNS names such as `frontend` or `backend` when Coolify connects multiple applications to a shared network.

## Target topology

```text
Internet
  |
  v
https://repo-fleet.apps.isaksson.info
  |
  v
Coolify / Traefik
  |
  v
repo-fleet-frontend:8080 (nginx)
  |
  +--> /api/* --> repo-fleet-backend:8080
                         |
                         +--> shared PostgreSQL:5432
```

Only the frontend is public. The backend and PostgreSQL remain internal.

### Backend instance count

The supported Coolify topology uses **one `repo-fleet-backend` instance**. Do not configure multiple backend replicas for RepoFleet yet. The targeted refresh queue is PostgreSQL-backed and durable, but its current claim operation assumes a single backend consumer.

Before horizontally scaling the backend, implement an atomic multi-consumer queue claim (for example `FOR UPDATE SKIP LOCKED` or an equivalent conditional update) and add concurrency tests proving that a refresh job cannot be claimed twice.


## 1. Create the application from Git

Create a Docker Compose application from:

```text
https://github.com/erland/repo-fleet
```

Use:

```text
/deploy/coolify/compose.yaml
```

as the Compose file.

Configure the public domain on `repo-fleet-frontend` only:

```text
https://repo-fleet.apps.isaksson.info
```

The frontend proxies `/api/*` to `repo-fleet-backend:8080`. Nginx receives that host explicitly through:

```text
REPOFLEET_BACKEND_HOST=repo-fleet-backend
```

so it does not fall back to a generic Docker DNS name such as `backend`.

## 2. Shared PostgreSQL

Use the existing shared PostgreSQL resource in Coolify. **Do not create PostgreSQL inside the RepoFleet Compose application.**

Create a dedicated database and login role for RepoFleet in the shared PostgreSQL instance:

```text
database: repofleet
owner:    repofleet
```

`deploy/coolify/bootstrap-db.sql` contains an example. Replace `CHANGE_ME` before running it and do not commit the real password.

Flyway remains responsible for the RepoFleet application schema and runs automatically when the backend starts.

Do not expose PostgreSQL port 5432 publicly.

## 3. Connect to the predefined network

Enable Coolify's **Connect To Predefined Network** setting for the RepoFleet application.

This is required because PostgreSQL is a separate Coolify resource. It allows `repo-fleet-backend` to resolve and reach the shared PostgreSQL resource by its internal Coolify hostname/alias while retaining the application-private `app` network between frontend and backend.

Set:

```text
REPOFLEET_DB_HOST
```

to the exact internal hostname/alias shown by the shared PostgreSQL resource.

Do not use the server's public IP and do not publish port 5432.

## 4. Required environment variables

Use `deploy/coolify/env.example` as a checklist.

At minimum configure:

```text
REPOFLEET_DB_HOST
REPOFLEET_DB_PASSWORD
REPOFLEET_GITHUB_APP_ID
REPOFLEET_GITHUB_INSTALLATION_ID
REPOFLEET_GITHUB_PRIVATE_KEY
REPOFLEET_GITHUB_WEBHOOK_SECRET
REPOFLEET_AUTH_CLIENT_ID
REPOFLEET_AUTH_CLIENT_SECRET
REPOFLEET_AUTH_SESSION_SECRET
REPOFLEET_AUTH_ALLOWED_USERS
```

The database variables default to:

```text
REPOFLEET_DB_NAME=repofleet
REPOFLEET_DB_USER=repofleet
```

The backend constructs:

```text
jdbc:postgresql://$REPOFLEET_DB_HOST:5432/$REPOFLEET_DB_NAME
```

`REPOFLEET_GITHUB_PRIVATE_KEY` may contain PEM text with literal newlines or escaped `\n`.

Useful optional values include:

```text
REPOFLEET_VERSION=latest
REPOFLEET_BACKEND_IMAGE=ghcr.io/erland/repo-fleet-backend
REPOFLEET_FRONTEND_IMAGE=ghcr.io/erland/repo-fleet-frontend
REPOFLEET_AUTH_CALLBACK_URL=https://repo-fleet.apps.isaksson.info/api/auth/github/callback
REPOFLEET_AUTH_SESSION_HOURS=12
REPOFLEET_AUTH_COOKIE_SECURE=true
REPOFLEET_GITHUB_TOKEN_REFRESH_MARGIN_SECONDS=300
GITHUB_API_URL=https://api.github.com
JAVA_OPTS=-XX:MaxRAMPercentage=75.0
```

Use a released or release-candidate value for `REPOFLEET_VERSION` when you want an immutable deployment rather than `latest`.

## 5. Shared-network naming

The frontend and backend service names are project-specific. The Coolify profile intentionally contains **no** service named:

```text
backend
frontend
postgres
```

and no RepoFleet-local PostgreSQL service.

The shared PostgreSQL hostname is supplied explicitly through `REPOFLEET_DB_HOST`. This avoids accidental DNS resolution to another application's `postgres` alias on Coolify's shared/predefined network.

## 6. GitHub OAuth callback

The GitHub OAuth App callback URL must match `REPOFLEET_AUTH_CALLBACK_URL`:

```text
https://repo-fleet.apps.isaksson.info/api/auth/github/callback
```

## 7. Backup and restore

RepoFleet's durable state lives in its **dedicated database inside the shared PostgreSQL resource**.

Configure scheduled PostgreSQL backups for the shared resource. Before deploying a version containing new Flyway migrations, also make sure a restorable backup of the `repofleet` database exists.

A logical backup can be created with:

```bash
pg_dump --format=custom --no-owner --no-privileges \
  --host="$REPOFLEET_DB_HOST" \
  --username="$REPOFLEET_DB_USER" \
  "$REPOFLEET_DB_NAME" > repofleet-backup.dump
```

Verify it with:

```bash
pg_restore --list repofleet-backup.dump >/dev/null
```

Restore into a clean `repofleet` database owned by the RepoFleet role, then start the backend and verify Flyway validation and application health.

## 8. Upgrade and rollback

Recommended production sequence:

1. pin `REPOFLEET_VERSION` to an immutable release/release-candidate;
2. verify a current database backup exists;
3. deploy the new application version;
4. verify backend startup and Flyway migration;
5. verify login, cached inventory, compliance and refresh diagnostics.

Flyway migrations are forward-only. If a new schema is incompatible with the previous application image, rollback may require restoring the matching pre-upgrade database backup as well as redeploying the previous image.

Because PostgreSQL is a separate Coolify resource, redeploying or recreating the RepoFleet application must not delete the database. Do not delete the dedicated `repofleet` database/role during application redeployments.

## 9. GitHub App webhook

Configure:

```text
Webhook URL: https://repo-fleet.apps.isaksson.info/api/github/webhook
Content type: application/json
Secret: same value as REPOFLEET_GITHUB_WEBHOOK_SECRET
Active: enabled
```

The backend verifies `X-Hub-Signature-256` and deduplicates deliveries by GitHub delivery ID. If `REPOFLEET_GITHUB_WEBHOOK_SECRET` is empty, signed webhook processing fails closed.

## 10. First deployment checklist

Before deploying verify:

- the shared PostgreSQL resource is running,
- database `repofleet` exists,
- role `repofleet` exists and owns the database,
- `REPOFLEET_DB_HOST` matches the PostgreSQL resource's internal Coolify hostname/alias,
- **Connect To Predefined Network** is enabled,
- the selected GHCR version exists for frontend and backend,
- all GitHub App/auth secrets are configured,
- the public domain is assigned only to `repo-fleet-frontend`.

Expected application services:

```text
repo-fleet-backend   healthy
repo-fleet-frontend  healthy
```

There should be no PostgreSQL service inside the RepoFleet Compose application.
