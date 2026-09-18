# Phase 2 acceptance matrix

This document is the deterministic CI acceptance gate for Phase 2 core behavior.
All tests run without real GitHub App credentials or live GitHub API access.

| Acceptance area | CI coverage |
| --- | --- |
| Persistence across restart/reload | `Phase2CoreAcceptanceTest.phase2CoreBehaviorSurvivesPersistenceAndRemainsIncremental` reloads the active inventory solely from persisted repository identity and enrichment snapshot state. |
| Incremental refresh | `Phase2CoreAcceptanceTest` verifies a fresh `APPARENTLY_UNCHANGED` repository produces `REUSE_CACHED` with zero scheduled enrichment. |
| Conditional cache reuse | `Phase2ConditionalCacheAcceptanceTest` verifies a fresh category cache returns `CACHED_FRESH` and performs zero GitHub API calls. Existing `GitHubConditionalRequestExecutorTest` also covers ETag modified/304 behavior. |
| Repository groups | `Phase2CoreAcceptanceTest` creates a prefix-based service group and verifies group-scoped rule applicability. |
| Standards/rules | `Phase2CoreAcceptanceTest` creates selected-group and global rules and evaluates them through the real assignment/evaluation services. |
| UNKNOWN semantics | `Phase2CoreAcceptanceTest` verifies `README_REQUIRED` evaluates to `UNKNOWN`, not `FAIL`. Existing rule-evaluator tests cover additional UNKNOWN cases. |
| Accepted deviations / exceptions | `Phase2CoreAcceptanceTest` persists and reloads an active RepoFleet exception. `RepositoryComplianceSummaryServiceTest` verifies an accepted FAIL is separated from actionable failures while UNKNOWN remains distinct. |
| Webhook idempotency | `GitHubWebhookResourceTest.acceptsSignedDeliveryExactlyOnce` sends the same valid signed delivery twice and verifies exactly one persisted delivery. |
| Webhook lifecycle/invalidation | `GitHubWebhookEventProcessorTest` covers metadata updates, create/delete, targeted invalidation and installation membership changes. |
| Targeted refresh | `Phase2CoreAcceptanceTest` verifies persistent active-job deduplication. `TargetedRepositoryRefreshServiceTest` verifies a single repository is enriched/persisted/replaced without fleet discovery. |
| Persistent queue retry/recovery | `RepositoryRefreshQueueServiceTest` covers retry/backoff, terminal failure, deduplication and restart recovery. |
| Phase 1 regression | `Phase1GitHubFixtureAcceptanceTest` verifies the deterministic Phase 1 inventory/enrichment behavior still works. |

## Acceptance rule

Step 32 is complete only when the normal backend CI test suite containing all tests above is green.
No live GitHub secret, installation token or external GitHub request is required for this acceptance gate.
