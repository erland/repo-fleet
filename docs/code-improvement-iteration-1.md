# Code Improvement Iteration 1

## Goal

Improve maintainability without changing RepoFleet behaviour. Work in small, verifiable steps. A step is only DONE after CI is green.

## Findings

1. **Dead finder components remain after the repository launcher redesign.**
   - `RepositoryFiltersPanel.tsx` and `RepositorySortControls.tsx` are no longer used by `App.tsx`.
   - Their dedicated tests still live in `App.test.tsx`.
   - Keeping them increases maintenance surface and can confuse future changes because two alternative finder implementations coexist in the source tree.

2. **`App.tsx` owns too many responsibilities.**
   - Authentication bootstrap, repository loading, refresh orchestration, saved views, selection, details/compliance state and workspace rendering are all coordinated in one component.
   - This makes future UI changes harder to isolate and test.

3. **Frontend tests are overly centralized.**
   - `App.test.tsx` tests many independent components and contains shared repository fixtures.
   - Component-focused tests and reusable fixtures would make failures easier to diagnose and reduce coupling.

4. **The frontend still carries CSS for superseded finder controls.**
   - After dead components are removed, obsolete selectors can be identified and deleted safely in a separate step.

5. **Backend packages are functionally rich enough to merit a focused service-boundary review.**
   - Refresh, persistence, standards and GitHub integration each contain several collaborating services.
   - This should be reviewed finding-first before any backend refactor; no speculative restructuring should be done.

## Plan

| Step | Change | Status |
|---|---|---|
| 1 | Remove superseded finder/sort components and their dedicated tests | DONE |
| 2 | Remove CSS that is proven unused after Step 1 | DONE |
| 3 | Extract repository-workspace orchestration from `App.tsx` without behaviour changes | DONE |
| 4 | Split monolithic frontend tests and centralize reusable repository fixtures | IN PROGRESS |
| 5 | Perform focused backend service-boundary analysis and implement the highest-value low-risk finding | NOT STARTED |
| 6 | Final cleanup, documentation and full CI verification | NOT STARTED |

## Step 1 acceptance

- `RepositoryFiltersPanel.tsx` is removed.
- `RepositorySortControls.tsx` is removed.
- Their imports and dedicated tests are removed from `App.test.tsx`.
- The current `RepositoryLauncherToolbar` behaviour is unchanged.
- Existing layout guard still verifies that the legacy components are not used.
- Full CI passes.

## Step 1 verification

Completed and verified by successful CI run #468 on PR #48.

## Step 2 acceptance

- CSS used only by the removed legacy finder and sort components is deleted.
- Shared filter-grid styles used by the active launcher drawer remain intact.
- No active component loses required selectors.
- Full CI passes.

## Step 2 verification

Completed and verified by successful CI run #470 on PR #48.

## Step 3 acceptance

- Repository workspace composition is moved out of `App.tsx` into a dedicated `RepositoryWorkspace` component.
- Authentication, backend loading, refresh orchestration and state ownership remain in `App.tsx`.
- Repository launcher, details, selection, inventory, saved-view management and refresh panel are composed inside `RepositoryWorkspace`.
- Existing finder/layout tests protect the new component boundary.
- No user-visible behaviour changes.
- Full CI passes.

## Step 3 verification

Completed and verified by successful CI run #474 on PR #48.

## Step 4 acceptance

- Shared repository and inventory-status fixtures live in a reusable frontend test fixture module.
- Repository, detail, selection, portfolio-summary and saved-view component tests are moved out of `App.test.tsx`.
- `App.test.tsx` focuses on app shell, refresh, compliance and diagnostics behaviour.
- Existing coverage for repository discovery, details, accessibility and saved views is preserved.
- Full CI passes.

## Next step

After Step 4 is verified by CI: Step 5 – perform a focused backend service-boundary analysis and implement the highest-value low-risk finding.
