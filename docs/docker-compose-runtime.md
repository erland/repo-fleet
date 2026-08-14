# Docker Compose Runtime

RepoFleet Phase 1 can be started as a complete two-container service with Docker Compose.

No Java, Maven, Node.js or npm installation is required on the host. PostgreSQL is intentionally not part of Phase 1.

## Start with defaults

```bash
docker compose up --build -d
```

Default host endpoints:

- frontend: `http://localhost:8080`
- backend: `http://localhost:8081`

The browser should normally use the frontend URL. Nginx proxies `/api/*` internally to the backend service at `http://backend:8080`.

## Stop

```bash
docker compose down
```

## Configuration

Copy `.env.example` to `.env`:

```bash
cp .env.example .env
```

Then configure the GitHub App values if real repository discovery is required.

The service can start without GitHub credentials. In that mode the application remains available and the GitHub connection reports that it is not configured.

Important variables include:

- `REPOFLEET_FRONTEND_PORT`
- `REPOFLEET_BACKEND_PORT`
- `REPOFLEET_GITHUB_APP_ID`
- `REPOFLEET_GITHUB_INSTALLATION_ID`
- `REPOFLEET_GITHUB_PRIVATE_KEY`
- `REPOFLEET_GITHUB_PRIVATE_KEY_PATH`
- `REPOFLEET_GITHUB_TOKEN_REFRESH_MARGIN_SECONDS`
- `GITHUB_API_URL`
- `JAVA_OPTS`

Do not store real private keys in committed `.env` files.

## Startup ordering

Both services have health checks. The frontend declares:

```yaml
depends_on:
  backend:
    condition: service_healthy
```

This keeps the frontend from starting until the backend is accepting health requests.

## Networking

Compose creates one private bridge network named for the Compose project. The frontend reaches the backend through the service DNS name `backend`; no host networking is required.

The host backend port is exposed primarily for diagnostics and development. Normal browser traffic should enter through the frontend port.

## Validation

Run:

```bash
bash scripts/verify-step-23.sh
```

The validation:

1. validates the Compose model,
2. builds both images,
3. starts the stack with `docker compose up --wait`,
4. waits for healthy services,
5. checks frontend HTML,
6. checks frontend-to-backend `/api/status` proxying,
7. checks the backend endpoint directly,
8. tears the stack down.

The script uses alternate host ports by default to reduce conflicts with locally running development services.
