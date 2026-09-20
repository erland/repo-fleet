# Code Improvement Iteration 2

## Goal

Continue improving RepoFleet maintainability and scalability without changing user-visible behaviour. Work in small, verifiable steps and require green CI before marking a step complete.

## Findings

1. **Compliance summary repeatedly scans the same result collections.**
   - `RepositoryComplianceSummaryService` repeatedly filters the full compliance-result list per repository and per rule.
   - This is easy to simplify by building lookup maps once and reusing them.
   - The current service tests already provide good semantic regression coverage.

2. **Group summary calculation reconstructs repositories repeatedly.**
   - Group membership is recalculated by reconstructing every active repository for every enabled group.
   - This should be addressed separately after Step 1 so the change remains easy to verify.

3. **Refresh start orchestration in `App.tsx` is duplicated.**
   - Incremental and full refresh callbacks share nearly identical lifecycle/error handling.
   - A later step can consolidate this without touching UI behaviour.

4. **Saved-view state management still lives directly in `App.tsx`.**
   - Persistence, active-view state and mutations are a coherent concern that could move to a dedicated hook after refresh orchestration is simplified.

5. **Direct static persistence access in some backend services reduces isolation.**
   - This deserves a focused review, but no broad repository abstraction should be introduced unless a concrete testing or maintenance benefit is demonstrated.

## Plan

| Step | Change | Status |
|---|---|---|
| 1 | Index compliance results once and reuse them in repository/rule summaries | DONE |
| 2 | Avoid repeated repository reconstruction during compliance group summaries | DONE |
| 3 | Consolidate duplicated refresh-start orchestration in `App.tsx` | IN PROGRESS |
| 4 | Extract saved-view state/persistence from `App.tsx` into a focused hook | NOT STARTED |
| 5 | Review backend persistence access and implement one justified low-risk boundary improvement | NOT STARTED |
| 6 | Final cleanup, documentation and full CI verification | NOT STARTED |

## Step 1 acceptance

- Actionable compliance results are indexed by repository id and rule key once.
- Rule results used for accepted-deviation counts are indexed by rule key once.
- Repository failure and rule summary behaviour is unchanged.
- Existing compliance summary tests remain green.
- Full CI passes.

## Step 1 verification

Completed and verified by successful CI run #484 on PR #49.

## Step 2 acceptance

- Each active repository is reconstructed at most once per compliance summary invocation.
- Enabled groups reuse the reconstructed repository set for membership evaluation.
- Group membership semantics and result counts are unchanged.
- Existing compliance summary tests remain green.
- Full CI passes.

## Step 2 verification

Completed and verified by successful CI run #486 on PR #49.

## Step 3 acceptance

- Incremental and full refresh share one orchestration function for guard, loading state, status update and error handling.
- Each refresh variant keeps its own API starter and user-facing failure message.
- Refresh polling behaviour is unchanged.
- Full frontend tests and type checks pass.
- Full CI passes.

## Next step

After Step 3 is verified by CI: Step 4 – extract saved-view state/persistence from `App.tsx` into a focused hook.
