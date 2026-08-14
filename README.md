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
