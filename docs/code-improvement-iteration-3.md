# Code Improvement Iteration 3

## Goal

Perform one final, deliberately small maintainability pass. Prefer testability and clear responsibility boundaries; stop early if remaining findings are mostly stylistic or would require broad architecture changes.

## Findings

1. **Saved-view hook owns meaningful logic but has no focused tests.**
   - The current frontend test stack has Vitest and React/ReactDOM, but no DOM hook-testing library.
   - Rather than add new test infrastructure, extract the hook's storage/mutation decisions into pure helpers and test those directly.

2. **Repository compliance detail handling still contributes significant state/callback weight in `App.tsx`.**
   - This may form a coherent hook, but should only be extracted if Step 1 remains green and the boundary is still clearly useful.

3. **Initial data loading / refresh polling remain concentrated in `App.tsx`.**
   - This is a larger lifecycle concern. Review it separately and stop if extraction would mainly reshuffle complexity.

4. **Compliance summary service remains sizeable but now has clearer service boundaries.**
   - Further splitting may be stylistic rather than materially useful; only proceed if a concrete responsibility boundary emerges.

## Plan

| Step | Change | Status |
|---|---|---|
| 1 | Make saved-view hook logic directly testable with pure helpers and focused tests | IN PROGRESS |
| 2 | Review/extract repository compliance detail state from `App.tsx` if clearly beneficial | NOT STARTED |
| 3 | Review app data-loading/polling lifecycle; implement only a justified low-risk extraction | NOT STARTED |
| 4 | Final dead-code/duplication review and stop/go decision | NOT STARTED |
| 5 | Final cleanup, documentation and full CI verification | NOT STARTED |

## Step 1 acceptance

- Saved-view storage initialization/persistence decisions can be tested without DOM-specific test infrastructure.
- Saved-view add/activate/delete behaviour has focused tests.
- `useSavedRepositoryViews` remains thin and preserves existing behaviour.
- No new test dependencies are introduced.
- Full CI passes.

## Next step

After Step 1 is verified by CI: Step 2 – review repository compliance detail state in `App.tsx` and extract it only if the boundary is clearly beneficial.
