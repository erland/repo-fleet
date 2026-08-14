# RepoFleet – Development Plan – Phase 1

## 1. Purpose

This development plan describes how to implement **Phase 1 – Repository Inventory & Analytics** from the functional specification.

The plan is intentionally divided into relatively small implementation steps. Each numbered development step should be suitable to execute as a separate prompt/conversation turn and should leave the project in a working state.

Phase 1 is read-only. The service shall connect to GitHub, build an inventory of accessible repositories, enrich that inventory with maintenance-oriented information, and provide filtering, selection, summary indicators, repository details, and saved views.

---

# 2. Technology Direction

## Frontend

Use:

- React
- TypeScript
- Vite
- React Router
- A lightweight state-management approach based primarily on React state/hooks unless stronger needs emerge
- A frontend test framework compatible with Vite/React
- Browser `localStorage` for Phase 1 user preferences and saved views

The frontend should be a standalone SPA consuming the backend REST API.

## Backend

Use:

- Java
- Quarkus
- Quarkus REST with JSON
- Quarkus REST Client for GitHub API integrations
- Maven
- Standard Quarkus testing support

The backend is responsible for:

- GitHub App authentication
- GitHub API access
- Repository discovery
- Repository metadata enrichment
- Aggregating GitHub data into frontend-friendly models
- In-memory caching/indexing
- Refresh orchestration
- API error handling

---

# 3. Database Decision for Phase 1

## Recommendation: no persistent database in Phase 1

Do **not** introduce PostgreSQL, JPA/Hibernate or Flyway during Phase 1 unless a concrete requirement appears that cannot reasonably be handled without persistence.

Reasons:

1. GitHub is the authoritative data source.
2. Phase 1 performs no repository mutations.
3. Repository inventory can be rebuilt from GitHub.
4. GitHub App installation access tokens are temporary and should not be treated as persisted application data.
5. Saved views are user-interface preferences and can initially be stored in browser `localStorage`.
6. Repository analysis can initially be cached in memory.
7. Avoiding a database keeps local development, deployment and testing simpler.

## Phase 1 storage model

### GitHub
Authoritative repository state.

### Backend memory
Temporary repository inventory and enrichment cache.

### Browser localStorage
Saved filters/views and selected UI preferences.

## Prepare for future persistence

Although Phase 1 does not use a database, avoid coupling business logic directly to the in-memory implementation. Use clear abstractions around repository inventory/cache and saved views.

Reconsider PostgreSQL + JPA/Hibernate + Flyway when adding:

- multiple application users,
- shared saved views,
- repository standards/rules,
- exceptions,
- maintenance campaigns,
- operation/audit history,
- scheduled analysis,
- historical analytics.

---

# 4. High-Level Phase 1 Architecture

```text
┌─────────────────────────────┐
│       React Frontend        │
│ Repository table            │
│ Filters / summaries         │
│ Repository details          │
│ Saved views                 │
└──────────────┬──────────────┘
               │ REST/JSON
               ▼
┌─────────────────────────────┐
│       Quarkus Backend       │
│ REST API                    │
│ Repository inventory        │
│ GitHub enrichment           │
│ In-memory cache             │
│ Refresh orchestration       │
└──────────────┬──────────────┘
               │ GitHub App
               ▼
┌─────────────────────────────┐
│          GitHub API         │
│ Repositories / Topics       │
│ Languages / License         │
│ Actions / Releases          │
└─────────────────────────────┘
```

The frontend must never receive or manage GitHub App secrets.

---

# 5. Repository Structure

Recommended monorepo structure:

```text
/
├── README.md
├── frontend/
│   ├── package.json
│   └── src/
├── backend/
│   ├── pom.xml
│   └── src/
├── docs/
│   ├── functional-specification.md
│   └── development-plan-phase-1.md
├── docker-compose.yml
├── .env.example
└── .github/
    └── workflows/
```

A monorepo is recommended because frontend and backend form one product, API changes can be coordinated easily, and CI can validate both together.

---

# 6. Core Domain Model for Phase 1

The backend should expose a frontend-oriented repository summary model approximately representing:

```text
RepositorySummary
- id
- owner
- name
- fullName
- url
- visibility
- archived
- fork
- defaultBranch
- topics[]
- languages[]
- primaryLanguage
- license
- githubActions
- release
- activity
- refreshStatus
```

Supporting concepts:

```text
LicenseStatus
- present
- recognized
- key/name where known

GitHubActionsStatus
- workflowsPresent
- workflowCount

ReleaseStatus
- releasePresent
- latestReleaseName
- latestReleaseTag
- latestReleaseDate

ActivityStatus
- pushedAt
- updatedAt

RefreshStatus
- complete
- partial
- failed
- suitable error details
```

Do not expose raw GitHub API response objects directly to the frontend.

---

# 7. GitHub App Integration Direction

Phase 1 should use a GitHub App rather than a personal access token as the primary integration mechanism.

The backend should:

1. Authenticate as the GitHub App.
2. Discover or use the configured app installation.
3. Generate installation access tokens as needed.
4. Use installation-scoped access to query repositories.
5. Keep GitHub credentials entirely server-side.

Phase 1 should request read-only permissions only. Verify exact minimal permissions against the GitHub endpoints during implementation.

---

# 8. Refresh and Caching Strategy

With hundreds of repositories, browser requests must not fan out into many live GitHub calls every time.

Use an in-memory inventory:

```text
GitHub → Refresh → Backend inventory/cache → REST API → Frontend
```

Support:

- initial inventory build,
- explicit refresh,
- repository-by-repository enrichment,
- refresh status/progress,
- partial results where useful,
- graceful individual repository failures.

A failed lookup for one repository must not invalidate the entire inventory. Persistent historical snapshots are out of scope.

---

# 8.1 Early CI Bootstrap Decision

A lightweight subset of Step 24 is intentionally implemented immediately after Step 1 so GitHub can validate builds and tests from the beginning of development.

The early workflow should:

- run frontend tests and production build,
- run backend tests/build,
- use separate frontend and backend jobs,
- run for pull requests and pushes to `main`,
- use read-only repository permissions,
- cancel obsolete runs for the same branch/PR.

The following remain part of their original later steps:

- Docker image validation,
- Docker Compose smoke tests,
- container publishing,
- release packaging.

This early CI bootstrap does not make Step 24 complete; it only moves its basic source quality gate forward.

# 9. Development Steps

## Step 1 – Bootstrap the Monorepo

### Goal
Create independently runnable frontend and backend applications.

### Implement
- Root project structure.
- React + TypeScript + Vite frontend.
- Quarkus Java backend.
- Root README and `.gitignore`.
- Backend health/test endpoint.
- Minimal frontend shell calling the backend.

### Tests
Basic smoke tests for frontend and backend.

### Definition of Done
- Both applications start locally.
- Frontend can call backend.
- Tests pass.
- README explains how to run both.

### Suggested prompt
> Implement Step 1 from `development-plan-phase-1.md`: bootstrap the monorepo with React/TypeScript/Vite frontend and Quarkus/Java backend. Complete the step including tests and documentation, and keep the project runnable.

---

## Step 2 – Establish Backend API Structure and Domain Models

### Goal
Create the internal/API model for repository analytics without GitHub integration yet.

### Implement
- Repository summary and supporting status models.
- Repository inventory service interface.
- Deterministic sample inventory implementation.
- `GET /api/repositories` backed by sample data.
- Keep GitHub-specific DTOs separate from public API models.

### Tests
Serialization, endpoint and representative complete/partial records.

### Definition of Done
The frontend/API client can retrieve realistic sample inventory JSON.

### Suggested prompt
> Implement Step 2 from `development-plan-phase-1.md`: establish the Phase 1 backend domain/API models, repository inventory abstraction, sample inventory service and `/api/repositories` endpoint. Add tests and update documentation.

---

## Step 3 – Build the Initial Repository Inventory UI

### Goal
Render repository inventory data from the backend.

### Implement
- Application layout.
- Repository page/table.
- Loading, empty and error states.
- Columns for name, owner, visibility, topics, primary language, license, Actions, release and activity.
- GitHub repository link.

### Tests
Populated result, loading, empty inventory and backend error.

### Definition of Done
The frontend renders Step 2 sample repositories in a useful portfolio table.

### Suggested prompt
> Implement Step 3 from `development-plan-phase-1.md`: build the initial React repository inventory UI against the existing sample backend API. Include loading, empty and error states plus frontend tests.

---

## Step 4 – Add GitHub App Authentication Foundation

### Goal
Allow the backend to authenticate securely as a GitHub App installation.

### Implement
- Configuration for GitHub API endpoint, App ID/private key and installation selection.
- GitHub App JWT generation.
- Installation access token acquisition.
- Token expiry/reacquisition and safe token reuse.
- Diagnostic/internal connectivity check without leaking credentials.

### Tests
JWT/token behavior, expiry/reacquisition and mocked GitHub failures.

### Definition of Done
The backend can obtain an installation token and verify GitHub connectivity.

### Implementation note

The Step 4 implementation supports a GitHub App private key either from a mounted PEM file or inline environment configuration, keeps all credentials server-side, caches installation tokens until shortly before expiry, and exposes only non-secret connection status through `/api/github/connection`.


### Suggested prompt
> Implement Step 4 from `development-plan-phase-1.md`: add secure GitHub App authentication to the Quarkus backend, including installation-token handling, configuration, mocked tests and documentation. Do not expose secrets to the frontend.

---

## Step 5 – Replace Sample Inventory with Real Repository Discovery

### Goal
Retrieve all repositories accessible to the GitHub App installation.

### Implement
- GitHub REST client for installation repositories.
- Pagination.
- Mapping to internal repository models.
- Populate fields available directly from repository discovery.
- Leave not-yet-enriched fields explicitly unknown.

### Tests
One/multiple pages, empty installation, private/public, archived and API failures.

### Definition of Done
`/api/repositories` returns real repositories accessible to the installation.

### Suggested prompt
> Implement Step 5 from `development-plan-phase-1.md`: replace sample repository discovery with real GitHub App installation repository discovery, including pagination, mapping and tests.

---

## Step 6 – Introduce the In-Memory Repository Inventory

### Goal
Avoid repeated live GitHub discovery calls.

### Implement
- In-memory inventory/cache.
- Current records and inventory timestamp.
- Refresh state.
- Initial refresh and explicit refresh operation.
- Preserve usable previous inventory when a later refresh fails where appropriate.

### Tests
Initial population, repeated reads, refresh and failed refresh.

### Definition of Done
Repository reads come from inventory rather than triggering full rediscovery.

### Suggested prompt
> Implement Step 6 from `development-plan-phase-1.md`: add the in-memory repository inventory/cache and refresh lifecycle, exposing refresh state through the backend API. Include tests.

---

## Step 7 – Add Topics and Language Enrichment

### Goal
Populate classification data needed by key filters.

### Implement
- Topics.
- Detected languages.
- Primary language.
- Explicit incomplete/failed enrichment state.
- Individual repository failures must not abort the whole refresh.

### Tests
Several/no topics, several/no languages and partial GitHub failure.

### Definition of Done
Inventory exposes reliable topic/language information suitable for filtering.

### Suggested prompt
> Implement Step 7 from `development-plan-phase-1.md`: enrich the repository inventory with GitHub topics and language information, including partial-failure handling and tests.

---

## Step 8 – Add LICENSE Analysis

### Goal
Identify repositories with and without license information.

### Implement
Distinguish:
- missing license,
- recognized license,
- custom/unrecognized license,
- analysis/API failure.

Do not treat an API failure as "missing license".

### Tests
Cover every state.

### Definition of Done
The inventory reliably distinguishes missing licenses from unknown/failed analysis.

### Suggested prompt
> Implement Step 8 from `development-plan-phase-1.md`: add LICENSE analysis to repository enrichment, distinguishing missing, recognized, custom/unrecognized and failed-analysis states. Add tests.

---

## Step 9 – Add GitHub Actions Analysis

### Goal
Identify repositories with or without GitHub Actions workflows.

### Implement
For each repository determine:
- workflows present/not present,
- workflow count,
- failed/unavailable analysis state.

Do not yet analyze workflow quality, latest execution or template/version compliance.

### Tests
No, one, several workflows and failed lookup.

### Definition of Done
Repositories can reliably be categorized by Actions presence.

### Suggested prompt
> Implement Step 9 from `development-plan-phase-1.md`: enrich repositories with GitHub Actions workflow presence/count using the GitHub API. Handle failures correctly and add tests.

---

## Step 10 – Add Official Release Analysis

### Goal
Identify repositories with and without official GitHub releases.

### Implement
Determine:
- at least one release,
- latest relevant release,
- tag/name/date.

Recommended semantics:
- drafts do not count as official,
- prereleases are exposed explicitly,
- normal published release satisfies "has official release".

### Tests
No release, published, draft-only, prerelease-only, multiple releases and failure.

### Definition of Done
The inventory can answer whether a repository has an official published release.

### Suggested prompt
> Implement Step 10 from `development-plan-phase-1.md`: add release analysis and clearly define published, draft and prerelease behavior. Include tests and expose latest-release metadata.

---

## Step 11 – Complete Refresh Orchestration and Progress Reporting

### Goal
Combine enrichment into a robust portfolio refresh pipeline.

### Implement
Pipeline:
1. discover repositories,
2. enrich topics/languages,
3. analyze licenses,
4. analyze Actions,
5. analyze releases,
6. update activity information,
7. retain per-repository error state.

Expose states such as idle, running, complete, partial and failed, plus total/processed/error counts where useful.

### Tests
Complete, partial and failed refresh scenarios.

### Definition of Done
Large portfolio refresh behaves predictably and progress is observable.

### Suggested prompt
> Implement Step 11 from `development-plan-phase-1.md`: combine repository discovery and all enrichment into a robust refresh pipeline with progress, partial failure handling and tests.

---

## Step 12 – Add Frontend Refresh Experience

### Goal
Make inventory refresh understandable and controllable.

### Implement
- Last successful refresh.
- Refresh button.
- In-progress/progress indicator.
- Partial failure warning.
- Keep usable existing data during refresh where practical.

### Tests
Idle, refreshing, success, partial and failed refresh.

### Definition of Done
The user can explicitly refresh and understand data freshness.

### Suggested prompt
> Implement Step 12 from `development-plan-phase-1.md`: add the complete repository refresh UX to the React frontend using the backend refresh API/status, with tests.

---

## Step 13 – Implement Repository Filtering

### Goal
Support the central Phase 1 use cases.

### Implement
Combinable filters for:
- name contains,
- name prefix,
- owner,
- visibility,
- active/archived,
- fork/non-fork,
- topic present/absent,
- language present/absent,
- license present/missing,
- Actions present/missing,
- official release present/missing,
- activity date/age.

Use predictable AND semantics across filter categories. Document ANY/ALL behavior for multi-value filters.

### Design decision
Perform filtering client-side in Phase 1 because the inventory is only hundreds of repositories.

### Tests
Focused filter unit tests plus common combinations.

### Definition of Done
All core functional-specification filtering examples can be reproduced.

### Suggested prompt
> Implement Step 13 from `development-plan-phase-1.md`: add combinable Phase 1 repository filtering to the React inventory, including all specified filter types, clear semantics and comprehensive tests.

---

## Step 14 – Add Sorting and Result Counts

### Goal
Make large filtered result sets easier to inspect.

### Implement
Sorting by useful fields including name, owner, activity, primary language, license, Actions and release state.

Show total repository count and filtered result count.

### Tests
Sorting and counts together with filters.

### Definition of Done
The inventory is practical to browse with roughly 200 repositories.

### Suggested prompt
> Implement Step 14 from `development-plan-phase-1.md`: add sorting and clear total/filtered result counts to the repository inventory, preserving filter state and adding tests.

---

## Step 15 – Add Repository Selection

### Goal
Prepare the interaction model required for future maintenance phases.

### Implement
- Select/deselect repository.
- Select all visible filtered repositories.
- Clear selection.
- Selected count.
- Stable repository identity.

Recommended behavior: selection persists across filter changes, with total selected count always visible.

### Tests
Carefully test selection/filter interaction.

### Definition of Done
The user can build an explicit selection from filtered results.

### Suggested prompt
> Implement Step 15 from `development-plan-phase-1.md`: add robust multi-repository selection to the inventory, including select-visible, persistent selection across filters, selected count and tests.

---

## Step 16 – Add Portfolio Summary Indicators

### Goal
Provide a quick maintenance overview before filtering.

### Implement
Indicators for at least:
- total repositories,
- active,
- archived,
- missing LICENSE,
- missing GitHub Actions,
- missing official release.

Also show useful topic and primary-language distributions.

### Tests
Test indicator calculations separately from presentation.

### Definition of Done
Major portfolio maintenance gaps are immediately visible.

### Suggested prompt
> Implement Step 16 from `development-plan-phase-1.md`: add portfolio summary indicators and topic/language distributions to the frontend using the repository inventory. Include calculation and UI tests.

---

## Step 17 – Add Repository Detail View

### Goal
Explain why a repository appears in a result.

### Implement
Detail route/view with:
- identity and GitHub link,
- visibility/archive/fork/default branch,
- topics,
- languages,
- license,
- Actions,
- release,
- activity,
- enrichment errors.

Keep it maintenance-oriented rather than cloning GitHub's repository UI.

### Tests
Complete record, missing optional data, partial errors and unknown route.

### Definition of Done
The user can inspect one repository's Phase 1 state without leaving the service.

### Suggested prompt
> Implement Step 17 from `development-plan-phase-1.md`: add the repository detail route/view with all Phase 1 analytics and partial-error handling. Include tests.

---

## Step 18 – Add Saved Views Using Browser Storage

### Goal
Reuse common filter combinations without introducing a database.

### Implement
- Save current filters as named view.
- List/apply/rename/delete views.
- Persist in `localStorage`.
- Version the saved-view format.
- Handle invalid/corrupt stored data.

Do not store repository analytics in localStorage.

### Tests
Serialization, persistence, corruption handling, apply/rename/delete.

### Definition of Done
Common portfolio queries survive browser restarts.

### Suggested prompt
> Implement Step 18 from `development-plan-phase-1.md`: add named saved filter views persisted in versioned browser localStorage, with management UI, resilience against invalid data and tests.

---

## Step 19 – Harden GitHub API and Rate-Limit Handling

### Goal
Make refresh reliable for everyday use across hundreds of repositories.

### Implement
Review all GitHub paths for:
- pagination,
- rate-limit information,
- transient failures,
- token expiry,
- authorization failures,
- unavailable/deleted repositories,
- repositories removed from the installation,
- sensible retries,
- avoidance of duplicate requests.

Expose meaningful errors without secrets.

### Tests
Failure-oriented integration tests with mocked GitHub responses.

### Definition of Done
Known GitHub/API failure modes degrade gracefully and do not corrupt inventory.

### Suggested prompt
> Implement Step 19 from `development-plan-phase-1.md`: harden all GitHub integration and refresh behavior for pagination, rate limits, token expiry, transient errors and repository-level failures. Add comprehensive mocked tests.

---

## Step 20 – Accessibility and Responsive UI Pass

### Goal
Make the inventory practical on desktop/tablet and keyboard-accessible.

### Implement
Review:
- responsive table behavior,
- filter layout,
- keyboard operation,
- focus handling,
- labels,
- loading/error/status communication,
- semantic markup.

Mobile does not need a full desktop table but important information and filters should remain usable.

### Tests
Add relevant accessibility/component tests where practical.

### Definition of Done
No obvious keyboard, labeling or responsive-layout blockers remain.

### Suggested prompt
> Implement Step 20 from `development-plan-phase-1.md`: perform the Phase 1 accessibility and responsive UI pass, fix identified issues and add appropriate tests.

---

## Step 21 – End-to-End Phase 1 Validation

### Goal
Verify implementation against the functional specification.

### Implement
End-to-end coverage for:
1. application loads,
2. repository inventory available,
3. refresh,
4. prefix filter,
5. topic filter,
6. Java filter,
7. missing LICENSE,
8. missing Actions,
9. missing release,
10. combined filters,
11. selection,
12. saved view,
13. repository details.

Use deterministic fixtures rather than live GitHub state.

### Definition of Done
All Phase 1 acceptance criteria have automated or documented validation.

### Suggested prompt
> Implement Step 21 from `development-plan-phase-1.md`: create the end-to-end Phase 1 validation suite against deterministic GitHub fixtures and verify every Phase 1 acceptance criterion.

---

## Step 22 – Dockerize Frontend and Backend

### Goal

Package both application components as production-ready Docker images.

### Implement

Create:

- backend Dockerfile,
- frontend Dockerfile,
- appropriate `.dockerignore` files,
- multi-stage builds where useful,
- container health checks,
- environment-based runtime configuration.

The frontend production image should serve the compiled React application and support SPA routing.

The backend image should run the packaged Quarkus application without requiring build tooling at runtime.

Do not bake GitHub App secrets or environment-specific configuration into images.

### Tests

Validate:

- both images build successfully,
- both containers start,
- frontend can reach backend,
- health checks become healthy,
- containers run without local Java/Node dependencies.

### Definition of Done

The complete Phase 1 application can be packaged as two independently runnable Docker images.

### Suggested prompt

> Implement Step 22 from `development-plan-phase-1.md`: Dockerize the React frontend and Quarkus backend as production-ready images, including health checks, secure runtime configuration and container smoke tests.

---

## Step 23 – Add Docker Compose Runtime

### Goal

Make the complete service runnable locally or on a simple Docker host with one command.

### Implement

Add `docker-compose.yml` that starts:

- frontend,
- backend.

Provide:

- networking between services,
- health checks/dependency handling,
- environment variable configuration,
- `.env.example`,
- sensible non-secret local defaults.

Do not add PostgreSQL in Phase 1.

### Tests

Validate a clean startup using only Docker/Compose and configuration values.

### Definition of Done

A developer or operator can start the complete application with Docker Compose without installing Java, Maven, Node.js or npm.

### Suggested prompt

> Implement Step 23 from `development-plan-phase-1.md`: add the complete Docker Compose runtime for frontend and backend, with environment configuration, health checks, `.env.example` and documentation.

---

## Step 24 – CI Quality Gate

### Goal

Ensure every repository change validates frontend, backend and container packaging.

### Implement

Add GitHub Actions CI for pull requests and appropriate branch events.

Validate:

### Frontend
- dependency installation,
- type checking,
- tests,
- production build.

### Backend
- Maven build,
- tests.

### Product
- end-to-end tests where practical,
- chosen formatting/linting checks.

### Docker
- frontend image build,
- backend image build,
- container or Docker Compose smoke test where practical.

Avoid:

- live GitHub integration tests on every commit,
- rebuilding the same artifacts unnecessarily,
- duplicate CI execution for the same commit when a pull request already covers it.

### Definition of Done

A pull request gets a clear pass/fail signal for source code, tests and production container packaging.

### Suggested prompt

> Implement Step 24 from `development-plan-phase-1.md`: add GitHub Actions CI for frontend, backend, tests and Docker packaging, avoiding duplicate or unnecessarily expensive runs.

---

## Step 25 – Package Versioned Releases with GitHub Actions

### Goal

Produce deployable releases automatically from Git tags/GitHub Releases.

### Implement

Add a release workflow that:

- runs for the chosen release/tag convention,
- rebuilds and tests the tagged source,
- builds production frontend and backend images,
- assigns immutable version tags,
- publishes container images to a container registry,
- optionally publishes a convenient moving alias,
- packages deployment assets required to run the release,
- creates or augments the GitHub Release with deployment artifacts/instructions as appropriate.

Recommended deployment assets:

- `docker-compose.yml`,
- `.env.example`,
- runtime/deployment documentation,
- any additional non-secret files required to deploy.

Use GitHub Container Registry as the default registry unless another registry is selected later.

The source tag, GitHub Release and container image versions must be traceable to each other.

### Versioning Principle

Do not spread manually maintained version numbers throughout source files.

Derive release versions primarily from Git tags / GitHub Release metadata and apply image tags/labels during packaging.

### Security

Use least-privilege GitHub workflow permissions.

Never package GitHub App private keys or runtime secrets into images or release archives.

### Definition of Done

Creating an official release/tag produces a traceable deployable package consisting of versioned frontend/backend container images and the files required to run them.

### Suggested prompt

> Implement Step 25 from `development-plan-phase-1.md`: add the GitHub Actions release pipeline that builds, tests, versions and publishes the frontend/backend Docker images and packages the Docker Compose deployment assets for each official release.

---

## Step 26 – Documentation and Phase 1 Completion Review

### Goal

Finish Phase 1 as a coherent, maintainable and deployable product increment.

### Implement

Review and update:

- root README,
- local development instructions,
- GitHub App setup instructions,
- configuration/environment variables,
- testing instructions,
- Docker build/run instructions,
- Docker Compose deployment,
- release/versioning process,
- refresh behavior,
- saved-view behavior,
- known limitations.

Perform a final comparison against the functional specification.

Create a short Phase 1 completion document listing:

- completed requirements,
- intentional deviations,
- known limitations,
- candidate improvements for Phase 2,
- technical debt worth addressing before write functionality is introduced.

### Definition of Done

A developer unfamiliar with the project can clone it, configure the GitHub App, run it locally or with Docker, run tests, and understand how official releases are packaged.

### Suggested prompt

> Implement Step 26 from `development-plan-phase-1.md`: complete documentation and perform a formal Phase 1 completion review against the functional specification, including Docker deployment and release packaging, fixing small gaps and documenting any remaining limitations.

---

# 10. Recommended Step Sequence

```text
1   Bootstrap
2   Backend domain/API
3   Initial frontend inventory
4   GitHub App authentication
5   Real repository discovery
6   In-memory inventory
7   Topics/languages
8   LICENSE analysis
9   Actions analysis
10  Release analysis
11  Refresh orchestration
12  Refresh UX
13  Filtering
14  Sorting/counts
15  Selection
16  Portfolio summaries
17  Repository details
18  Saved views
19  GitHub API hardening
20  Accessibility/responsive pass
21  End-to-end validation
22  Dockerize frontend/backend
23  Docker Compose runtime
24  CI quality gate
25  Versioned release packaging
26  Documentation/completion review
```

Each step should leave tests passing before proceeding.

---

# 11. Phase 1 API Surface – Conceptual Target

The exact endpoint design may evolve, but the target is approximately:

```text
GET  /api/repositories
GET  /api/repositories/{owner}/{name}
GET  /api/inventory/status
POST /api/inventory/refresh
GET  /api/github/connection
```

Avoid CRUD backend endpoints for concepts that are not persistent server-side resources. Saved views remain frontend-local in Phase 1.

---

# 12. Testing Strategy

## Backend unit tests
Focus on GitHub response mapping, analysis/classification, cache behavior and refresh orchestration.

## Backend integration tests
Focus on REST endpoints, mocked GitHub HTTP API, authentication/token workflow, pagination and partial failures.

## Frontend unit/component tests
Focus on filters, sorting, selection, summaries, saved views and loading/error states.

## End-to-end tests
Focus on user workflows. Normal automated tests should use deterministic GitHub fixtures. A manually invoked live-GitHub smoke/integration test may exist but should not be required in normal CI.

---

# 13. Important Phase 1 Design Constraints

## Read-only GitHub integration
Do not add repository write permissions during Phase 1.

## No database merely for caching
Do not introduce PostgreSQL simply to avoid fetching GitHub data. Re-evaluate based on measured behavior.

## No historical analytics yet
Do not store time series for repository, Actions, release or topic history.

## No maintenance rules yet
Do not implement standards, rules, exceptions or campaigns prematurely.

## No bulk writes
Selection is preparation for later phases and must not trigger GitHub modifications.

---

# 14. When to Introduce PostgreSQL/JPA/Flyway

Re-evaluate persistent storage at the start of Phase 2.

A likely future stack is:

- PostgreSQL
- Hibernate ORM / Jakarta Persistence
- Flyway

Good reasons to introduce persistence:

```text
Repository standards/rules
Saved views shared between devices/users
Repository exceptions
Maintenance campaigns
Audit history
Scheduled refresh configuration
Notifications
Multiple users/installations
Historical portfolio analytics
```

When persistence is introduced:

1. add it for explicit product data,
2. keep GitHub repository state conceptually separate,
3. use Flyway from the first persisted schema,
4. avoid treating the database as a second GitHub source of truth.

---

# 15. Container and Release Design Constraints

## Docker as the deployment contract

If the application works locally outside Docker but cannot run correctly from the production images, it is not considered deployable.

Docker images and Docker Compose should be maintained as first-class application artifacts.

## Runtime configuration

Environment-specific values must be supplied at runtime. Secrets must never be committed into the repository, Docker images or release archives.

## Reproducible packaging

The release pipeline should build images from the tagged source revision rather than repackaging arbitrary locally generated output.

## Registry and versioning

Container image tags should make it possible to identify the exact application release.

Prefer immutable release tags and optionally a moving convenience tag such as `latest`. Do not use `latest` as the only deployable identifier.

## Future database support

When PostgreSQL is introduced in a later phase, add it as another service in the container runtime rather than fundamentally changing the deployment model.

---

# 16. Phase 1 Completion Criteria

Phase 1 is complete when the user can:

- connect through a GitHub App,
- see all accessible repositories,
- see topics and languages,
- distinguish repositories with/without LICENSE,
- distinguish repositories with/without GitHub Actions,
- distinguish repositories with/without an official release,
- filter by name/prefix/topic/language and combine filters,
- sort results,
- select repositories,
- see portfolio summaries,
- open repository details,
- save reusable views locally,
- explicitly refresh GitHub data,
- understand partial analysis failures,
- use the product effectively for a portfolio of hundreds of repositories.

No persistent database and no GitHub write permissions are required to satisfy Phase 1.

---

# 17. Result of Phase 1

At the end of Phase 1, the product should answer questions such as:

> Which `roman-*` repositories do not have GitHub Actions?

> Which repositories with topic `board-game` have no official release?

> Which repositories contain Java code?

> Which repositories are missing LICENSE?

> Which repositories with a particular prefix do or do not have a particular topic?

Phase 2 can then build standards, rules and compliance insight on top of this inventory without redesigning the core GitHub integration.
