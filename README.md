# RepoFleet

RepoFleet is a GitHub repository portfolio analytics and maintenance service.

Phase 1 is read-only and focuses on repository inventory and analytics. The project is structured as a monorepo with a React/TypeScript frontend and a Quarkus/Java backend.

## Project structure

```text
.
├── frontend/   React + TypeScript + Vite
├── backend/    Quarkus + Java
└── docs/       Functional specification, development plan and implementation status
```

## Prerequisites for local development

- Node.js 20.19+ or 22.12+
- npm
- Java 21
- Maven 3.9+

Docker-based packaging/runtime is planned later in Phase 1 according to the development plan.

## Run the backend

```bash
cd backend
mvn quarkus:dev
```

The backend listens on `http://localhost:8080` and currently exposes:

```text
GET /api/status
GET /api/repositories
GET /api/github/connection
```

Example response:

```json
{
  "service": "repo-fleet-backend",
  "status": "UP"
}
```

## Run the frontend

In a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`.

The Vite development server proxies `/api/*` requests to the Quarkus backend at `http://localhost:8080`, so the application shell verifies frontend-to-backend connectivity automatically.

## GitHub App configuration

Step 4 adds the server-side GitHub App authentication foundation. Repository discovery still uses deterministic sample data until Step 5.

Configure the backend with environment variables (see `.env.example`):

```text
REPOFLEET_GITHUB_APP_ID
REPOFLEET_GITHUB_INSTALLATION_ID
REPOFLEET_GITHUB_PRIVATE_KEY_PATH
```

The private key may alternatively be supplied as `REPOFLEET_GITHUB_PRIVATE_KEY` containing PEM text. A mounted PEM file is recommended for container deployments because the secret is then not embedded in the image or application configuration.

The diagnostic endpoint:

```text
GET /api/github/connection
```

returns `CONNECTED`, `NOT_CONFIGURED`, or `ERROR`. It verifies GitHub App authentication and installation-token acquisition but never returns the JWT, installation token, or private key. Installation tokens are cached server-side and refreshed before their GitHub expiry.

Phase 1 remains read-only; do not grant repository write permissions to the GitHub App.

## Tests

Frontend:

```bash
cd frontend
npm install
npm test
npm run build
```

Backend:

```bash
cd backend
mvn test
```

## Continuous integration

A lightweight GitHub Actions workflow is available at `.github/workflows/ci.yml`.

It runs on pull requests and pushes to `main` and validates the two application components independently:

- **Frontend:** installs dependencies, runs Vitest and creates the production Vite build.
- **Backend:** sets up Java 21 and runs the Maven `verify` lifecycle.

The jobs run in parallel so a failure can be attributed directly to frontend or backend. Docker image publishing and release packaging are intentionally left for the later Phase 1 packaging steps.

Because the project does not yet contain an npm lockfile, CI currently uses `npm install`. Once a lockfile is committed, CI should switch to `npm ci` for reproducible dependency installation and dependency caching.

## Java package convention

All project-owned Java code must use packages beginning with:

```text
info.isaksson.erland
```

RepoFleet backend code currently uses:

```text
info.isaksson.erland.repofleet
```

## Documentation

- `docs/functional-specification.md`
- `docs/development-plan-phase-1.md`
- `docs/implementation-status.md`

## Phase 1 progress

Steps 1–3 are complete. Step 4 adds the GitHub App authentication foundation while `GET /api/repositories` intentionally continues to use deterministic sample data until Step 5.

To verify Step 4 locally:

```bash
./scripts/verify-step-4.sh
```

## Repository discovery

Phase 1 Step 5 replaces the deterministic sample inventory with the repositories accessible to the configured GitHub App installation. The backend uses the installation token and follows GitHub pagination with up to 100 repositories per page. Maintenance enrichment fields (topics/languages, license, Actions and releases) intentionally remain `NOT_ANALYZED` until their dedicated later steps.

Run `scripts/verify-step-5.sh` to verify backend and frontend after configuring normal build dependencies. Runtime repository discovery additionally requires the GitHub App environment variables documented above.

## Repository inventory cache

Phase 1 now keeps the discovered repository inventory in backend memory.

Useful endpoints:

- `GET /api/repositories` – returns the current cached inventory.
- `GET /api/inventory/status` – returns refresh state and timestamps.
- `POST /api/inventory/refresh` – explicitly refreshes repository discovery from GitHub.

The service performs an initial refresh on backend startup. If a later refresh fails, the last successful inventory remains available.

## Repository classification enrichment

Each inventory refresh now enriches discovered repositories with:

- GitHub topics,
- detected programming languages,
- primary language based on GitHub's language byte counts.

Classification failures are isolated per repository. A failed topics/languages lookup for one repository does not make the complete portfolio refresh fail, and an API failure is represented separately from an empty topics/languages result.
