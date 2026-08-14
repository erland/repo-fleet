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

## LICENSE analysis

Repository enrichment now distinguishes:

- no license-like file in the repository root,
- a license recognized by GitHub,
- a license file that exists but is custom/unrecognized,
- a LICENSE analysis failure.

An API failure is never reported as a missing LICENSE.

## GitHub Actions analysis

Repository enrichment now checks the GitHub Actions workflow inventory for each repository and records:

- whether workflows are present,
- the workflow count,
- a failed/unknown state when the Actions API cannot be analyzed.

A failed Actions API call is not interpreted as “no workflows”. The GitHub App installation needs repository **Actions: read** permission for private repositories.

## GitHub Release analysis

Repository enrichment now analyses published GitHub Releases separately from tags.

- Draft releases are ignored.
- A published prerelease counts as a release but is marked as a prerelease.
- The latest published release is selected by `published_at` (falling back to `created_at`).
- An API failure remains an unknown/failed analysis and is never reported as “no release”.

## Refresh orchestration and progress

A portfolio refresh now has observable lifecycle/progress data through `GET /api/inventory/status`.

The status includes:

- lifecycle state (`NOT_STARTED`, `RUNNING`, `COMPLETED`, `PARTIAL`, `FAILED`),
- start/completion and last-success timestamps,
- total and processed repository counts,
- successful and error counts,
- the repository currently being enriched while a refresh is running.

`POST /api/inventory/refresh` starts an asynchronous refresh when the service is running normally, allowing the frontend to poll status concurrently. A second refresh request while one is already running does not start a duplicate run.

Repository-level partial/failed enrichment is retained in the inventory. A mixed result produces a `PARTIAL` portfolio refresh instead of discarding successfully enriched repositories.

## Frontend refresh experience

The inventory page now exposes repository data freshness directly to the user:

- last successful refresh,
- explicit refresh button,
- progress while a refresh is running,
- current repository being analyzed,
- complete, partial and failed refresh outcomes.

Existing repository data remains visible while refresh status is polled. When a refresh finishes, the inventory is reloaded without replacing the table with an initial-loading screen.

## Repository filtering

Phase 1 filtering is performed client-side because the expected inventory is only hundreds of repositories.

Supported filters are combinable and use **AND semantics across filter categories**:

- name contains / name prefix,
- owner and visibility,
- active/archived and fork/non-fork,
- topic present/absent,
- language present/absent,
- LICENSE present/missing,
- GitHub Actions present/missing,
- official release present/missing,
- recent activity windows.

Topic and language filters currently accept a single exact, case-insensitive value. When a topic/language value is entered with the neutral match mode, it means “present”. Unknown/failed analyses are never treated as “missing”.

## Repository sorting and counts

Filtered inventory results can now be sorted client-side by:

- name,
- owner,
- last activity,
- primary language,
- LICENSE state,
- GitHub Actions state,
- official release state.

Sorting is deterministic and does not mutate the underlying repository inventory. Maintenance-state sorting orders known present values before known missing values, with unknown/failed analysis after both. Direction can be reversed.

The UI shows both the total repository count and the filtered result count so large portfolios remain easy to inspect.

## Repository selection

The inventory now supports explicit multi-repository selection using stable GitHub repository IDs.

- Individual repositories can be selected or deselected.
- All currently visible filtered repositories can be selected in one action.
- Visible repositories can be deselected without losing selections hidden by filters.
- The complete selection can be cleared explicitly.
- Total selected count remains visible even when filters hide selected repositories.

This interaction model intentionally prepares RepoFleet for later guided and pull-request-based maintenance phases without performing any repository mutations in Phase 1.

## Portfolio summary indicators

The Phase 1 inventory now includes summary indicators for the current filtered result set:

- repository count,
- repositories missing LICENSE,
- repositories missing GitHub Actions,
- repositories missing an official GitHub Release,
- repositories containing Java,
- archived repositories,
- forks.

Unknown or failed analysis is tracked separately and is not counted as a known missing capability. This keeps portfolio maintenance signals trustworthy when GitHub API enrichment is partial.

## Repository detail view

Each inventory row now exposes a read-only detail view containing the repository's currently known Phase 1 metadata:

- owner, visibility, archive/fork status and default branch,
- topics and detected languages,
- LICENSE analysis,
- GitHub Actions analysis and workflow count,
- official release metadata,
- activity timestamps,
- repository enrichment status and message.

Unknown or failed analysis remains explicit instead of being interpreted as a missing capability. The detail view links to the repository on GitHub but performs no mutations.

## Saved views

Named inventory views can now be stored in the browser using `localStorage`.

A saved view captures the current:

- repository filters,
- sort field,
- sort direction.

Saved views can be loaded or deleted later in the same browser. Repository selection is deliberately not part of a saved view, keeping selection an explicit temporary action. Storage failures are handled gracefully; the application remains usable even when browser storage is blocked or unavailable.

## GitHub API resilience

Repository refresh now applies bounded resilience rules around GitHub API calls:

- installation tokens are invalidated and reacquired after a `401`,
- transient network failures, rate limits and retryable server errors receive bounded retries,
- ordinary authorization errors and unavailable resources are not retried repeatedly,
- errors exposed to inventory status are sanitized and never include authorization tokens,
- overlapping repository-discovery pages are de-duplicated by stable GitHub repository ID,
- release discovery continues to later pages when a full page contains only drafts,
- repositories that disappear during enrichment are marked unavailable without issuing the remaining metadata requests,
- a successful later discovery removes repositories that are no longer part of the GitHub App installation.

The previous in-memory inventory is still retained when repository discovery itself fails, so transient GitHub failures cannot replace usable portfolio data with an empty result.

## Accessibility and responsive behavior

The Phase 1 UI includes an accessibility and responsive-layout pass:

- a keyboard skip link moves directly to the main inventory,
- interactive controls have visible `:focus-visible` treatment,
- the repository table is labeled as a keyboard-scrollable region on larger screens,
- repository details receive focus when opened,
- loading, refresh, selection and result status remain exposed through appropriate live/status semantics,
- saved-view actions have descriptive accessible names,
- filters and controls collapse cleanly on tablet/mobile,
- below 720 px the repository table becomes labeled card-style rows so important repository information remains usable without horizontal table navigation,
- forced-colors and reduced-motion preferences receive basic support.

Native HTML controls and explicit/wrapped labels remain the primary interaction model so keyboard use does not depend on custom widgets.

## Phase 1 acceptance validation

Phase 1 now has a deterministic acceptance-validation suite covering the complete read-only workflow from GitHub discovery and refresh through repository filtering, sorting, selection, saved views and repository details.

The validation uses mocked GitHub API clients and fixed frontend fixtures, so CI results do not depend on the current contents of the real GitHub installation.

See `docs/phase-1-acceptance-validation.md` for the criterion-by-criterion validation matrix and run `./scripts/verify-step-21.sh` to execute the full Phase 1 validation.

## Docker images

The frontend and backend can now be built as independent production images:

```bash
docker build -t repofleet-backend ./backend
docker build -t repofleet-frontend ./frontend
```

The frontend runtime uses `BACKEND_URL` to proxy `/api` requests to the backend, while GitHub App configuration remains backend-only runtime configuration. Both images include container health checks and run without local Node, Maven or Java build tooling.

See `docs/docker-images.md` and use `./scripts/verify-step-22.sh` for the isolated two-container smoke validation. Docker Compose is intentionally deferred to Step 23.
