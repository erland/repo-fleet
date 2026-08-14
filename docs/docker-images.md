# Docker Images

RepoFleet Phase 1 is packaged as two independently runnable images.

## Backend

Build:

```bash
docker build -t repofleet-backend ./backend
```

Run without GitHub integration:

```bash
docker run --rm -p 8080:8080 repofleet-backend
```

The backend listens on `0.0.0.0:8080` and exposes its container health check through `GET /api/status`.

GitHub App configuration is supplied only at runtime. For example:

```bash
docker run --rm -p 8080:8080 \
  -e REPOFLEET_GITHUB_APP_ID=... \
  -e REPOFLEET_GITHUB_INSTALLATION_ID=... \
  -e REPOFLEET_GITHUB_PRIVATE_KEY_PATH=/run/secrets/github-app.pem \
  -v "$PWD/secrets:/run/secrets:ro" \
  repofleet-backend
```

Supported runtime configuration includes the existing GitHub variables plus:

- `GITHUB_API_URL`
- `REPOFLEET_GITHUB_TOKEN_REFRESH_MARGIN_SECONDS`
- `QUARKUS_HTTP_PORT`
- `JAVA_OPTS`

Secrets are never copied into the image.

## Frontend

Build:

```bash
docker build -t repofleet-frontend ./frontend
```

Run against a backend reachable from the frontend container:

```bash
docker run --rm -p 8081:8080 \
  -e BACKEND_URL=http://host.docker.internal:8080 \
  repofleet-frontend
```

`BACKEND_URL` is substituted into the Nginx configuration when the container starts. The React bundle therefore does not need to be rebuilt for each backend address.

The frontend:

- listens on port `8080`,
- serves the Vite production build,
- proxies `/api/` to `BACKEND_URL`,
- serves `index.html` as the fallback for client-side SPA routes,
- exposes `/healthz` for the Docker health check.

## Security/runtime properties

Both images are multi-stage builds and contain no Maven/Node build tooling in their runtime stages. The application processes run as non-root users.

The Docker build contexts exclude build outputs, local environment files and common private-key formats through component-specific `.dockerignore` files.

## Smoke validation

Step 22 intentionally does not introduce Docker Compose; that belongs to Step 23.

With Docker available, run:

```bash
./scripts/verify-step-22.sh
```

The script:

1. builds both images,
2. creates an isolated Docker network,
3. starts backend and frontend containers,
4. waits for both health checks,
5. verifies the frontend HTML,
6. verifies that the frontend `/api/status` proxy reaches the backend,
7. removes its temporary containers/network.

### Nginx non-root runtime

The frontend image runs Nginx as the built-in `nginx` user. The image explicitly makes both `/etc/nginx/conf.d` and `/run` writable by that user because the official Nginx entrypoint generates the runtime proxy configuration and Nginx creates its PID file when the container starts.
