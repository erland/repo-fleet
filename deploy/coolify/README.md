# Coolify deployment

RepoFleet can be deployed in Coolify as a Docker Compose application using:

```text
deploy/coolify/compose.yaml
```

The Compose file uses the frontend and backend images already published to GHCR by the release and release-candidate workflows. It intentionally uses project-specific service names (`repo-fleet-frontend` and `repo-fleet-backend`) so the services do not publish generic DNS names such as `frontend` or `backend` when Coolify connects multiple applications to a shared network.

## Coolify setup

Create a new Docker Compose application from the GitHub repository and set the Compose location to:

```text
/deploy/coolify/compose.yaml
```

Configure the public domain on `repo-fleet-frontend`. The frontend container listens on internal port `8080`.

For the standard apps.isaksson.info deployment, use:

```text
https://repo-fleet.apps.isaksson.info
```

The frontend proxies `/api/*` to `repo-fleet-backend:8080` on the application-private Compose network.

## Required environment variables

Set these as Coolify environment variables/secrets:

```text
REPOFLEET_GITHUB_APP_ID
REPOFLEET_GITHUB_INSTALLATION_ID
REPOFLEET_GITHUB_PRIVATE_KEY
REPOFLEET_GITHUB_WEBHOOK_SECRET
REPOFLEET_AUTH_CLIENT_ID
REPOFLEET_AUTH_CLIENT_SECRET
REPOFLEET_AUTH_SESSION_SECRET
REPOFLEET_AUTH_ALLOWED_USERS
REPOFLEET_DB_PASSWORD
```

`REPOFLEET_GITHUB_PRIVATE_KEY` may contain PEM text with literal newlines or escaped `\n`, matching the backend's existing configuration support.

The following variables are optional:

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

Use a released or release-candidate value for `REPOFLEET_VERSION` when you want an immutable deployment instead of `latest`.

## PostgreSQL persistence

Phase 2 adds the project-local `repo-fleet-postgres` service and persistent volume `repo-fleet-postgres-data`. Set `REPOFLEET_DB_PASSWORD` as a Coolify secret. `REPOFLEET_DB_NAME` and `REPOFLEET_DB_USER` are optional and default to `repofleet`.

The backend waits for PostgreSQL readiness and Flyway migrations run automatically before the application becomes ready.

## Shared Coolify network

RepoFleet uses project-specific service names for PostgreSQL, frontend and backend, so it does not publish generic DNS aliases such as `postgres` or `backend` when Coolify connects applications to a shared network. If that option is enabled for operational consistency or future shared dependencies, the project-specific service names prevent the DNS alias collision that can occur when several applications all expose a service named `backend`.

## GitHub OAuth callback

The GitHub OAuth App callback URL must match `REPOFLEET_AUTH_CALLBACK_URL`. For the standard domain this is:

```text
https://repo-fleet.apps.isaksson.info/api/auth/github/callback
```


## Phase 2 backup and restore

RepoFleet's PostgreSQL volume is production state. Before deploying a version that contains new Flyway
migrations, create and verify a logical backup.

From the Coolify server, identify the Compose project/container names for RepoFleet and run `pg_dump`
against the `repo-fleet-postgres` service/container. Example when executing inside the PostgreSQL
container:

```bash
pg_dump --format=custom --no-owner --no-privileges \
  --username="$REPOFLEET_DB_USER" "$REPOFLEET_DB_NAME" \
  > /tmp/repofleet-backup.dump
```

Copy the dump to protected persistent/off-host storage and verify it with:

```bash
pg_restore --list repofleet-backup.dump >/dev/null
```

For restore, stop `repo-fleet-backend`, recreate the application database, restore with `pg_restore`,
then start the backend and confirm that Flyway validation and the backend health check succeed.

## Upgrade and rollback

Recommended production sequence:

1. pin `REPOFLEET_VERSION` to an immutable release/release-candidate version;
2. take and verify a database backup before a migration-bearing release;
3. deploy and wait for PostgreSQL/backend health;
4. inspect backend logs for Flyway failures;
5. verify login, inventory, compliance and refresh diagnostics.

Flyway migrations are forward-only. If a new schema is not compatible with the previous application image,
a rollback requires both the previous application version and restoration of the matching pre-upgrade
database backup.

Do not delete `repo-fleet-postgres-data` during redeployments. Do not use a Docker/Coolify cleanup action
that removes persistent volumes unless the intent is to destroy RepoFleet's production data.

## Disk and secrets

Monitor the host filesystem and the RepoFleet PostgreSQL volume. Keep enough free space for the live
database plus backups.

Treat `REPOFLEET_DB_PASSWORD`, GitHub App credentials, session secret and webhook secret as production
secrets. Store backups outside the Git repository and restrict access because they contain RepoFleet
application state and repository metadata.


## GitHub App webhook

Phase 2 uses GitHub App webhooks for repository lifecycle changes and targeted cache invalidation.

Configure the GitHub App webhook with:

```text
Webhook URL: https://repo-fleet.apps.isaksson.info/api/github/webhook
Content type: application/json
Secret: same value as REPOFLEET_GITHUB_WEBHOOK_SECRET
Active: enabled
```

The backend verifies `X-Hub-Signature-256` and deduplicates deliveries by GitHub delivery ID. If `REPOFLEET_GITHUB_WEBHOOK_SECRET` is empty, signed webhook processing fails closed.
