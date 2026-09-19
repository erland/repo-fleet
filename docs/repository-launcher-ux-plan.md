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
| 1 | Replace finder panels with a compact repository launcher toolbar | IN PROGRESS |
| 2 | Move advanced filters into a dedicated secondary disclosure/drawer | NOT STARTED |
| 3 | Replace mobile result cards/table with a compact repository list | NOT STARTED |
| 4 | Make repository rows open RepoFleet details; GitHub becomes secondary | NOT STARTED |
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

## Next step after Step 1

Step 2 – Move advanced filters into a dedicated secondary disclosure/drawer.
