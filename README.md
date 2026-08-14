# RepoFleet

RepoFleet is a GitHub repository portfolio analytics and maintenance service.

**Phase 1 is read-only and complete in implementation:** it inventories repositories accessible to a GitHub App installation, analyzes maintenance-relevant metadata, and lets the user refresh, filter, sort, inspect, select and save reusable portfolio views.

## What Phase 1 does

RepoFleet answers questions such as:

- Which `roman-*` repositories do not have GitHub Actions?
- Which repositories with a topic have no published release?
- Which repositories contain Java?
- Which repositories are missing LICENSE?
- Which repositories match several maintenance conditions at once?

Analyzed repository data includes:

- owner/name and visibility,
- archived/fork/default branch,
- topics,
- languages and primary language,
- LICENSE state,
- GitHub Actions workflow state,
- official release state,
- recent activity.

The backend inventory is in-memory; GitHub remains the source of truth. Saved views are stored locally in the browser.

## Project structure

```text
.
├── backend/                 Quarkus + Java 21
├── frontend/                React + TypeScript + Vite
├── docs/                    Product, development, deployment and completion docs
├── scripts/                 Validation/build/release helpers
├── .github/workflows/       CI and official release workflows
├── docker-compose.yml       Local/simple-host runtime
└── .env.example             Non-secret runtime configuration example
```

Project-owned Java packages use:

```text
info.isaksson.erland.repofleet
```

## Fastest way to run

With Docker and Docker Compose installed:

```bash
cp .env.example .env
docker compose up --build -d
```

Open:

```text
http://localhost:8080
```

Backend diagnostics are exposed at `http://localhost:8081` by default.

The service can start without GitHub credentials, but live repository discovery requires a configured GitHub App.

Full Compose instructions: `docs/docker-compose-runtime.md`.

## Configure the GitHub App

Phase 1 needs only read access:

- Metadata: read-only
- Contents: read-only
- Actions: read-only

Configure the App ID and installation ID, then provide the private key either as a mounted/readable file through `REPOFLEET_GITHUB_PRIVATE_KEY_PATH` or as PEM text through `REPOFLEET_GITHUB_PRIVATE_KEY`.

For the stock Docker Compose runtime, the PEM environment-variable form is the simplest because the Compose file does not assume a host key-file mount.

See:

- `docs/github-app-setup.md`
- `docs/configuration.md`

## Native development

Backend:

```bash
cd backend
mvn quarkus:dev
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`; Vite proxies `/api/*` to the backend.

Detailed instructions: `docs/local-development.md`.

## Tests and quality gate

Frontend:

```bash
cd frontend
npm install
npm run typecheck
npm test
npm run build:bundle
```

Backend:

```bash
cd backend
mvn --batch-mode --no-transfer-progress verify
```

The GitHub Actions CI workflow validates repository policy, frontend, backend and the complete production Docker Compose runtime. A final stable check named **Quality Gate** provides the pull-request pass/fail signal.

See `docs/ci-quality-gate.md`.

## Refresh behavior

The backend performs repository discovery/enrichment into an in-memory snapshot.

Endpoints:

```text
GET  /api/status
GET  /api/github/connection
GET  /api/repositories
GET  /api/inventory/status
POST /api/inventory/refresh
```

A failed discovery keeps the previous successful inventory. Per-repository metadata failures are isolated and represented as partial/failed analysis rather than silently becoming “missing” data.

The frontend polls refresh status while a refresh is running and keeps the existing repository table visible.

## Saved views

Named views are stored in browser `localStorage`.

A saved view contains:

- filters,
- sort field,
- sort direction.

Repository selection is intentionally not persisted in a saved view.

Saved views are local to the current browser/device.

## Docker images

Independent production images can be built with:

```bash
docker build -t repo-fleet-backend ./backend
docker build -t repo-fleet-frontend ./frontend
```

The frontend container proxies `/api/*` to a runtime-configurable `BACKEND_URL`. Both images run non-root and include health checks.

See `docs/docker-images.md`.

## Official releases

Official releases use Git tags:

```text
vMAJOR.MINOR.PATCH
```

For example:

```bash
git tag v1.0.0
git push origin v1.0.0
```

The tag is the application release-version source of truth.

The release workflow:

1. revalidates the tagged source,
2. publishes versioned frontend/backend images to GHCR,
3. adds commit-SHA trace tags and `latest`,
4. packages version-specific deployment assets,
5. creates or augments the matching GitHub Release.

No GitHub App private keys/runtime secrets are packaged.

See `docs/release-publishing.md`.


## Debian 13 production deployment

A complete production installation guide for `repo-fleet.isaksson.info` is available in:

- `docs/debian-13-installation.md`

It covers a fresh Debian 13 server, `/opt/repo-fleet`, Docker/Compose, GitHub App setup, Nginx, Let's Encrypt/Certbot, Basic Auth, SSH deployment credentials and the manually triggered production deployment workflow.

Production-specific deployment assets are in `deploy/`, and `.github/workflows/deploy.yml` deploys a selected official `vMAJOR.MINOR.PATCH` GitHub Release to `/opt/repo-fleet`.

## Phase 1 completion

The formal review is in:

- `docs/phase-1-completion-review.md`
- `docs/phase-1-acceptance-validation.md`
- `docs/implementation-status.md`

The completion review lists the delivered requirements, intentional deviations from the broader functional specification, known limitations, Phase 2 candidates and technical debt to address before introducing GitHub write functionality.

## Documentation index

- `docs/functional-specification.md` – product scope across phases
- `docs/development-plan-phase-1.md` – implementation plan
- `docs/implementation-status.md` – verified step status
- `docs/local-development.md` – native development/testing
- `docs/github-app-setup.md` – GitHub App permissions/install/configuration
- `docs/configuration.md` – environment-variable reference
- `docs/docker-images.md` – individual production images
- `docs/docker-compose-runtime.md` – complete Docker runtime
- `docs/ci-quality-gate.md` – pull-request validation
- `docs/release-publishing.md` – versioned GHCR/GitHub Releases
- `docs/phase-1-acceptance-validation.md` – deterministic acceptance coverage
- `docs/phase-1-completion-review.md` – final Phase 1 assessment
