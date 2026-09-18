# Local Development

## Native development prerequisites

- Node.js 20.19+ or 22.12+
- npm
- Java 21
- Maven 3.9+

Docker/Compose can be used instead when no local Java/Node toolchain is desired.

## Backend

```bash
cd backend
mvn quarkus:dev
```

Default URL:

```text
http://localhost:8080
```

Useful endpoints:

```text
GET  /api/status
GET  /api/github/connection
GET  /api/repositories
GET  /api/inventory/status
POST /api/inventory/refresh
```

The current Phase 1 detail UI uses repository data already present in the inventory response; there is no separate repository-detail REST endpoint.

## Frontend

In another terminal:

```bash
cd frontend
npm ci
npm run dev
```

Open:

```text
http://localhost:5173
```

The Vite server proxies `/api/*` to `http://localhost:8080`.

## Tests and verification

Frontend:

```bash
cd frontend
npm ci
npm run typecheck
npm test
npm run build:bundle
```

Backend:

```bash
cd backend
mvn --batch-mode --no-transfer-progress verify
```

Phase 1 acceptance validation:

```bash
bash scripts/verify-step-21.sh
```

Complete product with Docker Compose:

```bash
bash scripts/verify-step-23.sh
```

Repository CI-quality-gate verification:

```bash
bash scripts/verify-step-24.sh
```

Release packaging without publishing:

```bash
bash scripts/verify-step-25.sh
```

Final documentation/completion consistency:

```bash
python3 scripts/validate-phase1-completion.py
```

## Live GitHub

Normal tests use deterministic fixtures and do not require GitHub credentials.

For live repository discovery, configure the GitHub App as documented in `docs/github-app-setup.md` and explicitly refresh the inventory.

## Phase 2 database foundation

Phase 2 requires PostgreSQL even though repository inventory is still in memory in Step 1.

The simplest local setup is the root Compose runtime:

```bash
cp .env.example .env
docker compose up --build
```

This starts PostgreSQL, backend and frontend. PostgreSQL data is stored in the named volume `repofleet-postgres-data`.

For native backend development, start PostgreSQL separately and configure:

```text
REPOFLEET_DB_URL=jdbc:postgresql://localhost:5432/repofleet
REPOFLEET_DB_USER=repofleet
REPOFLEET_DB_PASSWORD=repofleet
```

Flyway runs automatically at backend startup. Database readiness is exposed through Quarkus health at `/q/health/ready`.
