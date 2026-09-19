# Configuration Reference

RepoFleet is configured through environment variables. Environment-specific values and secrets are not built into Docker images.

## Backend

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `REPOFLEET_GITHUB_APP_ID` | For live GitHub | – | GitHub App ID used when signing app JWTs. |
| `REPOFLEET_GITHUB_INSTALLATION_ID` | For live GitHub | – | Installation whose accessible repositories form the inventory. |
| `REPOFLEET_GITHUB_PRIVATE_KEY_PATH` | One private-key source for live GitHub | – | Path to a GitHub App private key that is actually readable inside the backend runtime. Preferred when the deployment mounts secrets. |
| `REPOFLEET_GITHUB_PRIVATE_KEY` | Alternative private-key source | – | PEM text supplied directly as an environment variable. |
| `REPOFLEET_GITHUB_TOKEN_REFRESH_MARGIN_SECONDS` | No | `300` | Refresh installation token before expiry. |
| `GITHUB_API_URL` | No | `https://api.github.com` | REST API base URL; useful for GitHub Enterprise/testing. |
| `QUARKUS_HTTP_HOST` | No | Docker image sets `0.0.0.0` | Backend bind host. |
| `QUARKUS_HTTP_PORT` | No | `8080` | Backend container port. |
| `JAVA_OPTS` | No | `-XX:MaxRAMPercentage=75.0` in image/Compose | JVM runtime tuning. |

The backend can start without GitHub App settings. In that mode the service endpoints remain available and the GitHub connection reports `NOT_CONFIGURED`.

## Frontend container

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `BACKEND_URL` | No | `http://backend:8080` | Nginx upstream for `/api/*`. |

`BACKEND_URL` is substituted when the frontend container starts, so the frontend bundle does not need to be rebuilt for each environment.

## Docker Compose host/runtime

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `COMPOSE_PROJECT_NAME` | No | `repo-fleet` | Compose project/network/container prefix. |
| `REPOFLEET_FRONTEND_PORT` | No | `8080` | Frontend host port. |
| `REPOFLEET_BACKEND_PORT` | No | `8081` | Backend diagnostic host port. |
| `REPOFLEET_FRONTEND_IMAGE` | No | `repo-fleet-frontend:local` | Local Compose frontend image. |
| `REPOFLEET_BACKEND_IMAGE` | No | `repo-fleet-backend:local` | Local Compose backend image. |

Official release deployment archives replace the local image defaults with versioned GHCR image references.

## Secret handling

Do not commit:

- `.env` containing real credentials,
- GitHub App PEM/private-key files,
- installation access tokens.

The root `.gitignore` excludes common environment/private-key files. Release packaging includes only `.env.example`, never `.env`.

## Docker Compose private key note

The stock Compose file intentionally does not assume a host secret-file location. Therefore a path such as `/run/secrets/github-app.pem` only works after the operator explicitly mounts that file into the backend container.

For a minimal local Compose setup, `REPOFLEET_GITHUB_PRIVATE_KEY` is the direct option. For a hosted/managed deployment, prefer a platform secret mount and `REPOFLEET_GITHUB_PRIVATE_KEY_PATH`.


## GitHub user authentication

| Variable | Production | Purpose |
|---|---|---|
| `REPOFLEET_AUTH_ENABLED` | `true` | Protect RepoFleet API with GitHub login. |
| `REPOFLEET_AUTH_CLIENT_ID` | required | GitHub App Client ID, distinct from App ID. |
| `REPOFLEET_AUTH_CLIENT_SECRET` | secret | GitHub App Client Secret used only by the backend OAuth exchange. |
| `REPOFLEET_AUTH_SESSION_SECRET` | secret | HMAC key for RepoFleet session cookies; minimum 32 characters. |
| `REPOFLEET_AUTH_CALLBACK_URL` | production URL | Exact registered GitHub App callback URL. |
| `REPOFLEET_AUTH_ALLOWED_USERS` | required | Comma-separated GitHub logins allowed to enter RepoFleet. |
| `REPOFLEET_AUTH_SESSION_HOURS` | `12` | RepoFleet session lifetime. |
| `REPOFLEET_AUTH_COOKIE_SECURE` | `true` | Require HTTPS for the session cookie. |

The GitHub user access token is used only to retrieve the authenticated identity and is not persisted as the RepoFleet session.

## Phase 2 database configuration

RepoFleet now requires a PostgreSQL datasource. Configure the backend with:

| Variable | Default | Purpose |
|---|---|---|
| `REPOFLEET_DB_URL` | `jdbc:postgresql://localhost:5432/repofleet` | JDBC connection URL |
| `REPOFLEET_DB_USER` | `repofleet` | Database user |
| `REPOFLEET_DB_PASSWORD` | `repofleet` for local development | Database password |
| `REPOFLEET_DB_NAME` | `repofleet` in Compose | Database created by the PostgreSQL container |

Production deployments should always override the database password with a deployment secret. Flyway migrations run automatically at startup.


## Phase 2 webhook and refresh configuration

| Variable | Default | Purpose |
|---|---|---|
| `REPOFLEET_GITHUB_WEBHOOK_SECRET` | none | HMAC secret used to verify GitHub App webhook deliveries. Required when webhooks are enabled. |
| `REPOFLEET_REFRESH_ENRICHMENT_WORKERS` | `2` | Bounded worker count for full inventory enrichment. |
| `REPOFLEET_REFRESH_TARGETED_QUEUE_ENABLED` | `true` | Enables the persistent targeted refresh worker/poller. |
| `REPOFLEET_REFRESH_TARGETED_WORKERS` | `2` | Bounded worker count for targeted refresh jobs. |
| `REPOFLEET_REFRESH_USAGE_CHECK_INTERVAL_MINUTES` | `60` | Minimum interval between repository discovery/fingerprint checks triggered by authenticated use. |
| `REPOFLEET_REFRESH_CONSISTENCY_SCHEDULER_ENABLED` | `false` | Optional background consistency scheduler; disabled by default because normal consistency checks are usage-triggered. |
| `REPOFLEET_REFRESH_CONSISTENCY_INTERVAL_HOURS` | `24` | Interval between scheduled consistency refreshes. |
| `REPOFLEET_REFRESH_IDENTITY_FRESHNESS_MINUTES` | `15` | Identity freshness window. |
| `REPOFLEET_REFRESH_ENRICHMENT_FRESHNESS_MINUTES` | `60` | Overall enrichment freshness window. |
| `REPOFLEET_REFRESH_TOPICS_FRESHNESS_MINUTES` | `60` | Topic cache freshness. |
| `REPOFLEET_REFRESH_LANGUAGES_FRESHNESS_MINUTES` | `60` | Language cache freshness. |
| `REPOFLEET_REFRESH_LICENSE_FRESHNESS_MINUTES` | `240` | Root/license cache freshness. |
| `REPOFLEET_REFRESH_WORKFLOWS_FRESHNESS_MINUTES` | `60` | Workflow cache freshness. |
| `REPOFLEET_REFRESH_RELEASES_FRESHNESS_MINUTES` | `60` | Release cache freshness. |
| `REPOFLEET_REFRESH_FULL_CONSISTENCY_HOURS` | `24` | Maximum age before full consistency enrichment is forced. |

Production must set a non-empty `REPOFLEET_GITHUB_WEBHOOK_SECRET` when GitHub App webhooks are enabled.


### Usage-triggered consistency checks

RepoFleet loads persisted repository data from PostgreSQL at backend startup without contacting GitHub. The first authenticated API use starts a repository discovery/fingerprint check when the latest refresh attempt is older than `REPOFLEET_REFRESH_USAGE_CHECK_INTERVAL_MINUTES`.

Repositories with a complete persisted enrichment snapshot and an unchanged repository fingerprint are reused without full enrichment, regardless of snapshot age. New repositories, repositories whose fingerprint changed, and repositories with incomplete enrichment are scheduled for enrichment. A changed fingerprint invalidates conditional-resource freshness so the subsequent enrichment checks GitHub instead of trusting an otherwise-fresh category TTL.


For apparently unchanged repositories, each usage-triggered consistency run performs lightweight conditional verification of topics and releases only. Their existing 60-minute freshness settings and ETags prevent these resources from being fetched more often than configured. Languages, license/root contents, and workflows are not re-enriched unless the repository fingerprint changes or the stored enrichment is incomplete.
