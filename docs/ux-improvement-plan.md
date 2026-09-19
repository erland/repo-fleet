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
| 4 | Simplify the repository result list | IN PROGRESS |
| 5 | Compress refresh into system status | NOT STARTED |
| 6 | Move portfolio/compliance/diagnostics into secondary navigation | NOT STARTED |
| 7 | Refine the repository detail flow | NOT STARTED |
| 8 | Responsive and accessibility-focused final pass | NOT STARTED |

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

## Next step after Step 4

Step 5 – Compress refresh into system status.
