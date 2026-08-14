# Phase 1 Acceptance Validation

This document maps the Phase 1 acceptance criteria to deterministic automated validation.

The suite deliberately does **not** depend on live GitHub state. Backend GitHub responses and frontend repository data use fixed fixtures so the same behavior is tested in CI every time.

| # | Acceptance criterion | Automated validation |
|---|---|---|
| 1 | Application loads | `frontend/src/phase1Acceptance.test.tsx` renders the application shell. |
| 2 | Repository inventory available | Frontend fixture inventory is rendered; backend resource tests verify `/api/repositories`. |
| 3 | Refresh | `Phase1GitHubFixtureAcceptanceTest` performs discovery + enrichment + in-memory refresh from mocked GitHub clients; refresh UI success is also rendered. |
| 4 | Prefix filter | Phase 1 frontend acceptance suite verifies `roman-` prefix behavior. |
| 5 | Topic filter | Acceptance suite verifies exact case-insensitive topic filtering. |
| 6 | Java filter | Acceptance suite identifies repositories containing Java, not only repositories where Java is primary. |
| 7 | Missing LICENSE | Acceptance suite verifies known missing LICENSE results. |
| 8 | Missing Actions | Acceptance suite verifies known missing GitHub Actions results. |
| 9 | Missing release | Acceptance suite verifies known missing official release results. |
| 10 | Combined filters | Acceptance suite combines prefix/topic/language/LICENSE/Actions/release using AND semantics and then sorts deterministically. |
| 11 | Selection | Acceptance suite selects visible results, adds another repository, then narrows filters and verifies hidden selections persist. |
| 12 | Saved view | Acceptance suite persists and reloads a named filter/sort view through deterministic in-memory browser storage. |
| 13 | Repository details | Acceptance suite renders the detail view from the same deterministic fixture and verifies maintenance metadata. |

## Deterministic GitHub fixture

`backend/src/test/java/info/isaksson/erland/repofleet/repository/inventory/Phase1GitHubFixtureAcceptanceTest.java` exercises the actual Phase 1 backend path:

1. GitHub App installation repository discovery,
2. GitHub metadata calls for topics/languages/LICENSE/Actions/releases,
3. repository mapping,
4. enrichment,
5. in-memory refresh orchestration,
6. final inventory/status assertions.

The GitHub clients are mocked at their API boundary. No GitHub credentials or network access are required.

## Frontend acceptance fixture

`frontend/src/phase1AcceptanceFixtures.ts` contains a fixed four-repository portfolio covering:

- two `roman-` repositories,
- topic differences,
- Java present/absent,
- LICENSE present/missing,
- Actions present/missing,
- release present/missing,
- archived/non-archived states.

`frontend/src/phase1Acceptance.test.tsx` uses this portfolio to reproduce the central functional-specification use cases end to end across the real Phase 1 filter, sorting, selection, saved-view and detail-view logic.

## Running the validation

Run:

```bash
./scripts/verify-step-21.sh
```

The script runs the complete frontend test/build and backend Maven verification suites. GitHub Actions remains the authoritative verification environment for marking Step 21 complete.
