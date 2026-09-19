# RepoFleet – UX improvement plan

## Goal

Shift RepoFleet from a dashboard-first experience to a repository-discovery experience where the primary flow is:

1. search for a repository or choose a saved view,
2. review matching repositories,
3. open a repository for details.

Portfolio analytics, compliance, refresh controls and diagnostics remain available but should not compete with repository discovery.

## Steps

| Step | Change | Status |
| ---: | --- | --- |
| 1 | Establish repository finder as the primary information architecture | DONE |
| 2 | Separate simple search from advanced filters | DONE |
| 3 | Make saved views first-class navigation | DONE |
| 4 | Simplify the repository result list | DONE |
| 5 | Compress refresh into system status | DONE |
| 6 | Move portfolio/compliance/diagnostics into secondary navigation | DONE |
| 7 | Refine the repository detail flow | DONE |
| 8 | Responsive and accessibility-focused final pass | IN PROGRESS |

## Step 1 acceptance

- Repository discovery appears before portfolio analytics and operational panels.
- Search/filter controls, saved views and sorting remain functional.
- Repository results appear immediately after the finder workflow.
- Existing detail, compliance, refresh and diagnostics functionality remains available.
- Frontend tests pass.

## Step 1 verification

Completed and verified by successful CI run #398 on PR #46.

## Step 2 acceptance

- Simple repository search is always visible.
- Search matches repository name and full `owner/name`.
- Advanced filters remain available behind progressive disclosure.
- The collapsed advanced-filter control indicates how many advanced filters are active.
- Clear all resets both simple search and advanced filters.
- Existing saved views remain compatible with the filter model.
- Frontend tests pass.

## Step 2 verification

Completed and verified by successful CI run #405 on PR #46.

## Step 3 acceptance

- Saved views are directly selectable as quick repository categories.
- "All repositories" is always available as the neutral view.
- The active saved view is visibly and programmatically indicated.
- Manual changes to search/filter/sort leave the saved-view selection state.
- Create/delete actions are available behind secondary "Manage views" disclosure.
- Existing browser-storage persistence remains unchanged.
- Frontend tests pass.

## Step 3 verification

Completed and verified by successful CI run #410 on PR #46.

## Step 4 acceptance

- Repository results prioritize full repository name, topics, primary language and recent activity.
- Owner and visibility are not separate high-cost columns; visibility remains available as a compact badge.
- Detailed license, Actions and release values move out of the discovery list and remain available in repository details.
- Only maintenance exceptions/unknown states appear as compact flags in the list.
- Repository selection controls stay out of the main flow until a selection exists.
- Mobile result cards contain substantially fewer labelled rows.
- Frontend tests pass.

## Step 4 verification

Completed and verified by successful CI run #416 on PR #46.

## Step 5 acceptance

- Refresh is represented by a compact system-status row instead of a full dashboard panel.
- Normal/completed state is collapsed by default.
- Running, partial, failed and unavailable states expand automatically.
- Progress, current repository, rate-limit pauses and failure messages remain visible when relevant.
- Standard and full refresh actions remain available behind the status disclosure.
- Last successful refresh remains visible in the compact summary.
- Frontend tests pass.

## Step 5 verification

Completed and verified by successful CI run #421 on PR #46.

## Step 6 acceptance

- Repository discovery remains the default workspace.
- Portfolio summary, compliance overview and refresh diagnostics are removed from the default repository flow.
- A secondary "Insights & diagnostics" workspace exposes those analytical views without removing functionality.
- Compact system status remains available in the repository workspace.
- Navigation is keyboard-accessible and responsive.
- Frontend tests pass.

## Step 6 verification

Completed and verified by successful CI run #425 on PR #46.

## Step 7 acceptance

- Repository details open as a focused side drawer without replacing the repository result context.
- The drawer receives focus and behaves as a labelled modal dialog.
- Escape and backdrop interaction close the drawer.
- Core repository metadata is immediately visible in a compact overview.
- Detailed maintenance analysis uses progressive disclosure.
- Compliance detail remains available in the same repository context.
- The drawer becomes full-width on small screens.
- Frontend tests pass.

## Step 7 verification

Completed and verified by successful CI run #429 on PR #46.

## Step 8 acceptance

- Repository detail drawer traps keyboard focus while open and restores the previous focus target when closed.
- Escape remains available for closing, and background scrolling is prevented while the drawer is open.
- Workspace navigation exposes explicit relationships to its content regions.
- Skip-link wording reflects the whole application rather than only repository inventory.
- Primary mobile controls meet larger touch-target sizing.
- Narrow-screen repository cards reduce redundant labels and keep the primary action full-width.
- Very narrow screens use single-column saved-view shortcuts and tighter page padding.
- Existing forced-colors and reduced-motion behavior remains intact.
- Frontend tests pass.

## Completion after Step 8

When Step 8 is verified by CI, the UX improvement series is complete.
