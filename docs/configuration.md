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
