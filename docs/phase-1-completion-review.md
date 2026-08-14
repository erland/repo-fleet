# Phase 1 Completion Review

## Outcome

RepoFleet Phase 1 delivers a read-only repository portfolio inventory and maintenance-analysis workflow backed by a GitHub App installation.

The implemented interaction model is:

```text
refresh → filter/sort → inspect → select → save useful views
```

GitHub remains the source of truth. RepoFleet holds only the current inventory in backend memory and saved views in browser storage.

## Completed Phase 1 requirements

Phase 1 provides:

- GitHub App authentication and installation repository discovery,
- repository metadata including owner, visibility, archived/fork state and default branch,
- topics and detected languages with primary language,
- LICENSE presence and recognized/custom analysis,
- GitHub Actions workflow presence/count,
- published release presence and latest release metadata,
- activity timestamps,
- explicit inventory refresh with observable progress and partial/failure states,
- resilient GitHub API handling including bounded retry/token refresh behavior,
- combinable repository filters,
- sorting and result counts,
- explicit repository selection that survives filter changes,
- portfolio-level maintenance indicators,
- read-only repository detail view,
- named saved views in browser `localStorage`,
- accessibility/responsive UI pass,
- deterministic acceptance coverage,
- production frontend/backend Docker images,
- two-service Docker Compose runtime,
- pull-request CI quality gate,
- Git-tag-driven GHCR/GitHub Release packaging.

The 13 explicit Phase 1 acceptance scenarios are mapped in `docs/phase-1-acceptance-validation.md`.

## Final comparison with the functional specification

### Fully covered core completion criteria

The Phase 1 completion criteria in the development plan are covered:

- accessible installation repositories can be inventoried,
- topics/languages/LICENSE/Actions/releases are analyzed,
- name/prefix/topic/language and maintenance-state filters can be combined,
- results can be sorted,
- repositories can be selected,
- portfolio summaries and repository details are available,
- saved views can be reused locally,
- refresh is explicit and partial analysis is visible,
- the product has deterministic tests and deployable container packaging.

### Intentional deviations from the broader functional specification

The functional specification contains a few broader “should/include at least” ideas that were not required by the concrete Phase 1 implementation steps and remain intentionally deferred:

1. **Multiple-topic “has any of” filter**  
   The UI currently filters one exact topic at a time, with present/absent semantics.

2. **License recognized/type filters**  
   Recognition and license key/name are analyzed and visible, but filters currently focus on LICENSE present/missing.

3. **Arbitrary activity cutoff date**  
   Activity filtering uses predefined age windows rather than a user-selected calendar date.

4. **Grouped summary breakdowns**  
   Java count and maintenance indicators exist, but generic “group by every topic/primary language” summary views are not implemented.

5. **Dedicated repository-detail backend endpoint**  
   The conceptual API target mentioned `GET /api/repositories/{owner}/{name}` as an approximate option. Phase 1 instead renders detail from the current cached inventory payload, avoiding a redundant endpoint.

These deviations do not block the explicit Phase 1 completion criteria and are reasonable candidates for later UX/analytics refinement.

## Known limitations

### In-memory backend inventory

A backend restart clears the current inventory. Startup triggers a fresh discovery when GitHub is configured.

There is no scheduled/background refresh. The user explicitly refreshes inventory state.

### Saved views are browser-local

Saved views:

- are stored in `localStorage`,
- are not shared across browsers/devices/users,
- are not backed up by the server,
- store filters and sorting, not repository selection.

### One GitHub App installation per running backend

Runtime configuration targets one installation ID. Multi-installation/multi-user portfolio separation is not yet a product concept.

### API cost for large portfolios

Refresh enriches repositories through multiple GitHub REST calls. Rate-limit/transient handling is present, but portfolios much larger than the intended hundreds-of-repositories scale may justify caching, concurrency tuning or persistence.

### No historical analytics

RepoFleet records no time-series history. It shows the current GitHub state only.

### No write functionality

Phase 1 has no repository write permissions and performs no maintenance action. Selection exists specifically to prepare for later safe maintenance flows.

### Release workflow convention

Official application releases use strict `vMAJOR.MINOR.PATCH` tags. The source tag is the release version authority; the placeholder versions in development build metadata are not release-version sources.

## Candidate improvements for Phase 2

High-value candidates:

- repository standards/rules by logical group,
- compliance results explaining which rule each repository violates,
- multi-topic and richer license filters where they help rules/group definitions,
- shared/persistent saved views if multiple devices/users become important,
- richer portfolio grouping summaries,
- repository exceptions/waivers,
- maintenance observations derived from standards,
- explicit preview of future bulk maintenance actions.

Persistence should be introduced only when product data such as standards, exceptions, campaigns, shared views or history requires it.

## Technical debt before write functionality

Before enabling repository modifications, prioritize:

1. **Explicit authorization model**  
   Separate read-only analysis permissions from future write permissions and document exactly which maintenance actions require which GitHub App permissions.

2. **Action/audit model**  
   Define immutable records for requested changes, target repositories, generated commits/branches/PRs and outcomes before write operations exist.

3. **Idempotency and retry semantics for writes**  
   Reads can be retried freely; writes need operation IDs and safe retry behavior.

4. **Repository state preconditions**  
   Future changes should validate default branch/head state and detect repository changes between preview and execution.

5. **Persistent product data boundary**  
   If Phase 2 introduces standards/rules, add PostgreSQL/JPA/Flyway for RepoFleet-owned product data while keeping GitHub repository state conceptually separate.

6. **Dependency reproducibility**  
   The frontend still lacks a committed npm lockfile. Add one and move CI/image builds from `npm install` to `npm ci` before write-capable releases become operationally important.

7. **Release immutability policy**  
   Consider container digest recording/attestation in the release manifest once deployment governance matters.

## Phase 2 starting point

The recommended next product increment is **Standards, Rules & Maintenance Insights**.

A good first Phase 2 slice is:

1. define repository groups from the existing filter model,
2. define read-only standards for those groups,
3. compute compliance from the existing inventory,
4. explain failed requirements per repository,
5. only then design write-capable maintenance actions.

This preserves the Phase 1 principle: analysis before modification.
