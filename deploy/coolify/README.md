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
