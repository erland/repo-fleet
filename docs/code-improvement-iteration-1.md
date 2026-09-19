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
| 2 | Remove CSS that is proven unused after Step 1 | IN PROGRESS |
| 3 | Extract repository-workspace orchestration from `App.tsx` without behaviour changes | NOT STARTED |
| 4 | Split monolithic frontend tests and centralize reusable repository fixtures | NOT STARTED |
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

## Next step

After Step 2 is verified by CI: Step 3 – extract repository-workspace orchestration from `App.tsx` without behaviour changes.
