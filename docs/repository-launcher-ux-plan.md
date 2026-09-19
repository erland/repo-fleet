# RepoFleet – Repository launcher UX iteration 2

## Goal

Make repository discovery feel like a launcher rather than a management dashboard.

Primary flow:

1. search or choose a saved view,
2. scan several repositories at once,
3. open RepoFleet repository details,
4. use deeper analysis only when needed.

Design principle:

> If information is not needed to identify a repository or decide which repository to open, it should not take permanent space in the result view.

## Steps

| Step | Change | Status |
| ---: | --- | --- |
| 1 | Replace finder panels with a compact repository launcher toolbar | DONE |
| 2 | Move advanced filters into a dedicated secondary disclosure/drawer | DONE |
| 3 | Replace mobile result cards/table with a compact repository list | DONE |
| 4 | Make repository rows open RepoFleet details; GitHub becomes secondary | IN PROGRESS |
| 5 | Compact repository details and add relative recent activity times | NOT STARTED |
| 6 | Final mobile density and usability validation | NOT STARTED |

## Step 1 acceptance

- Search, Saved View, Sort and Filters are grouped in one compact launcher.
- Saved View is a dropdown with "All repositories" as the neutral option.
- Sorting is a single dropdown while retaining compatibility with all existing saved sort combinations.
- Advanced filters are collapsed by default and only consume space when explicitly opened.
- Saved-view management remains available but appears after repository results rather than before them.
- The old standalone finder, saved-view navigation and sorting panels are no longer part of the primary repository flow.
- Existing saved views remain compatible with browser storage.
- Frontend tests pass.

## Step 1 verification

Completed and verified by successful CI run #439 on PR #47.

## Step 2 acceptance

- The launcher keeps only a compact Filters button in the primary flow.
- Advanced filters render in an overlay drawer rather than expanding the page.
- The drawer blocks background scrolling while open.
- Escape, backdrop click and an explicit Close action dismiss the drawer.
- Focus moves to the drawer when opened and returns to the Filters button when closed.
- Active advanced-filter count remains visible on the launcher button.
- Existing filter semantics remain unchanged.
- Frontend tests pass.

## Step 2 verification

Completed and verified by successful CI run #444 on PR #47.

## Step 3 acceptance

- Desktop keeps the information-dense table layout.
- Mobile no longer renders each table cell as a labelled card row.
- Each mobile repository result is a compact row with repository name and only present indicators/values.
- Visibility, language, activity, archived/fork state, up to two topics and maintenance warnings can appear as compact badges.
- Missing optional metadata consumes no mobile space.
- Additional topics collapse to a compact "+N" indicator.
- The repository results heading is compact on mobile.
- Existing repository selection and detail actions remain available.
- Frontend tests pass.

## Step 3 verification

Completed and verified by successful CI run #449 on PR #47.

## Step 4 acceptance

- Clicking a repository row opens RepoFleet repository details.
- Repository name is an explicit internal details action rather than an external GitHub link.
- Enter/Space on a focused repository row opens details.
- Selection checkbox remains independent and does not open details.
- GitHub remains available as a secondary link and does not trigger the RepoFleet detail action.
- Mobile and desktop use the same primary interaction model.
- Frontend tests pass.

## Next step after Step 4

Step 5 – Compact repository details and add relative recent activity times.
