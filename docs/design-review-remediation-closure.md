# Design review remediation closure

Date: 2026-09-20

## Outcome

The design-review remediation plan is complete for RepoFleet's current supported runtime topology.

Implementation steps IP-001 through IP-013 and IP-015 are complete. IP-014 is intentionally deferred because RepoFleet currently supports one backend application instance; atomic multi-consumer queue claiming becomes mandatory before horizontal backend scaling is introduced.

## Finding closure

- **DR-001 – Data completeness and refresh outcome conflated:** closed. Repository data state, freshness and latest refresh outcome are now distinct.
- **DR-002 – Enrichment service central change concentration:** closed. Topics, languages, license, Actions and release enrichment are owned by focused facet components; the classification service is an orchestrator.
- **DR-003 – Queue claim multi-instance safety:** addressed for current topology. Single-backend support is explicit; multi-instance atomic claim remains a documented scaling gate.
- **DR-004 – Webhook idempotency check/process/insert race:** closed. Delivery IDs are claimed atomically before processing in the same transaction.
- **DR-005 – Parallel rule engines with divergent semantics:** closed. The duplicate compliance path was removed and the canonical evaluator retained.
- **DR-006 – Compliance group membership recalculated repeatedly:** closed. Membership is precomputed once per portfolio summary.
- **DR-007 – Frontend/backend API model drift:** closed. OpenAPI is used as the contract source with compile-time TypeScript assertions.
- **DR-008 – Monolithic frontend API module:** closed. API access is split into feature-oriented modules.

## Preserve constraints retained

The remediation preserved the intended design constraints:

- cache-first resilience,
- explicit unknown versus missing semantics,
- targeted refresh reuse,
- persistent refresh jobs,
- selective cache invalidation,
- compliance-domain separation,
- existing frontend state boundaries,
- behavior-focused regression tests.

## Verification

The final implementation step, IP-015, was verified by successful CI run #543 on PR #62.

A post-merge CI run on main is expected to remain the final repository-level regression check. No additional implementation work is required by the current design-review plan unless that run exposes a regression.

## Deferred scaling gate

Before RepoFleet supports more than one backend instance, implement IP-014:

1. atomic multi-consumer queue claiming,
2. concurrency tests proving one job is claimed exactly once,
3. exactly-once attempt increment semantics,
4. restart-recovery validation under multiple consumers,
5. deployment documentation permitting horizontal backend scaling.

See `docs/queue-concurrency.md`.
