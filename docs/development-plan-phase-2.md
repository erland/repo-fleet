# Development Plan – Phase 2

## Phase 2 goal

Phase 2 turns RepoFleet from a read-only inventory browser into a persistent portfolio analysis service that can answer:

> What should my repositories look like, where do they deviate, and why?

Phase 2 remains **read-only against GitHub**. RepoFleet may persist its own configuration, cache, refresh state, rules, exceptions, and analysis results, but it must not modify repositories, branches, workflows, releases, topics, files, or settings on GitHub.

Each numbered step below is intentionally sized to fit into one focused implementation prompt, including tests and documentation where relevant.

## Architectural principles

- PostgreSQL becomes the persistent backend store.
- GitHub App installation tokens remain the authority for repository data access.
- GitHub user authentication remains separate from repository-data access.
- Cached repository data must be usable immediately after startup.
- Refreshes should become incremental and rate-limit aware.
- Conditional requests should be preferred where GitHub supports them.
- Full consistency refresh remains available.
- Webhooks may invalidate or refresh cached data later in the phase.
- Rules and compliance results must always be explainable.
- Existing Phase 1 behavior should remain compatible unless a step explicitly changes it.
- Phase 2 introduces no GitHub write operations.

## Step 1 – Phase 2 persistence foundation

Introduce PostgreSQL without moving inventory data yet.

Deliverables:
- Quarkus PostgreSQL/JDBC and persistence dependencies.
- Flyway migrations.
- local Docker Compose PostgreSQL service.
- production Compose PostgreSQL configuration.
- datasource environment variables.
- health/status verification of database connectivity.
- CI database setup for backend integration tests.
- local and production documentation.

Acceptance:
- backend starts with an empty database.
- migrations run automatically.
- existing Phase 1 functionality still works.

## Step 2 – Persistent repository identity model

Create the persistent repository entity/table.

Suggested fields:
- GitHub repository ID.
- owner/login.
- name and full name.
- visibility.
- archived/fork flags.
- default branch.
- GitHub `updated_at`.
- GitHub `pushed_at`.
- first-seen and last-seen timestamps.
- active/inactive state.

Acceptance:
- identities can be inserted, updated and uniquely matched by GitHub repository ID.

## Step 3 – Persist discovered repository inventory

Synchronize GitHub discovery into PostgreSQL.

Behavior:
- insert new repositories.
- update known repositories.
- mark repositories no longer returned by the installation according to an explicit retention policy.
- update `last_seen_at`.

Acceptance:
- a restart no longer loses the list of known repositories.

## Step 4 – Serve cached repositories immediately on startup

Make `/api/repositories` use persisted repository data before GitHub refresh completes.

Requirements:
- startup must not block on GitHub.
- background refresh may still start automatically.
- cached inventory remains visible if GitHub is temporarily unavailable.

Acceptance:
- first repository response is independent of refresh duration.

## Step 5 – Persistent enrichment snapshot model

Persist current enrichment data:
- topics.
- languages.
- license.
- Actions/workflows.
- latest published release.
- activity.
- enrichment status.
- last successful refresh.
- last relevant error.

Acceptance:
- current repository summaries can be reconstructed from PostgreSQL.

## Step 6 – Persist progressive enrichment results

Persist each completed repository enrichment immediately.

Requirements:
- successful data replaces cached values.
- transient failures do not erase valid cached data.
- progressive frontend behavior remains.
- completed work survives restart.

Acceptance:
- a process restart does not discard already completed enrichment work.

## Step 7 – Refresh history and observability

Persist refresh runs.

Suggested data:
- refresh ID.
- trigger type.
- start/end.
- discovered/processed/success/error counts.
- final state.
- GitHub rate-limit observations.
- failed repository summary.

Acceptance:
- operator can see when refresh ran, duration and result.

## Step 8 – Repository change fingerprinting

Add cheap repository-level change detection using persisted metadata such as:
- repository ID.
- `updated_at`.
- `pushed_at`.
- default branch.
- archived/visibility.

Classify repositories as:
- new.
- likely changed.
- apparently unchanged.

Acceptance:
- refresh planning can reuse cached data where safe.

## Step 9 – Incremental refresh planner

Introduce an explicit refresh plan per repository:
- reuse cached enrichment.
- refresh selected categories.
- full enrichment.
- full enrichment for new repository.

Expose counters such as:
- reused.
- new.
- changed.
- scheduled.

Acceptance:
- a normal refresh no longer enriches every repository automatically.

## Step 10 – GitHub conditional request infrastructure

Add generic ETag support.

Persist:
- resource/category.
- ETag.
- last successful fetch.

Support:
- `If-None-Match`.
- `304 Not Modified`.
- new ETag after `200`.
- existing retry/rate-limit handling.

Acceptance:
- `304` reuses cached data and is not treated as failure.

## Step 11 – Conditional topics and languages refresh

Use conditional requests or equivalent safe freshness checks for topics and languages.

Acceptance:
- unchanged data avoids unnecessary payload downloads.
- transient failures preserve cached values.

## Step 12 – Conditional license, workflows and release refresh

Extend incremental/conditional behavior to:
- license.
- workflows.
- latest release.

Do not force ETag where another GitHub signal is more appropriate.

Acceptance:
- normal refresh uses materially fewer GitHub requests than Phase 1.

## Step 13 – Refresh policy and cache freshness

Introduce configurable freshness rules:
- identity freshness.
- enrichment freshness by category.
- full consistency interval.
- stale-data indicator.

Acceptance:
- RepoFleet can distinguish fresh, stale and refreshing data.

## Step 14 – Controlled enrichment concurrency

Introduce bounded concurrency only after incremental refresh works.

Requirements:
- conservative configurable worker count.
- preserve repository failure isolation.
- respect rate-limit and retry signals.
- no unbounded parallelism.

Acceptance:
- refresh improves without materially increasing rate-limit failures.

## Step 15 – Repository standards domain model

Persist RepoFleet-owned standards/rules.

Initial rule ideas:
- LICENSE required.
- Actions workflow required.
- published release required.
- required topic.
- maximum inactivity age.
- README presence when reliably available.

Each rule includes:
- stable ID.
- name/description.
- severity: Required / Recommended / Informational.
- enabled state.
- parameters.
- scope.

Acceptance:
- rules can be stored and retrieved without evaluation.

## Step 16 – Repository groups

Persist repository groups based on declarative selectors such as:
- name prefix.
- owner.
- visibility.
- archived/fork.
- topic.
- language.

Acceptance:
- backend can resolve membership deterministically.

## Step 17 – Rule-to-group assignments

Allow rules to apply to:
- all repositories.
- selected repository groups.

Define deterministic precedence and duplicate behavior.

Acceptance:
- backend can explain which rules apply to a repository and why.

## Step 18 – Rule evaluation engine

Evaluate repository/rule pairs as:
- PASS.
- FAIL.
- UNKNOWN.
- NOT_APPLICABLE when justified.

Each result must include:
- human-readable reason.
- observed supporting value.

Unknown inventory data must never silently become failure.

Acceptance:
- evaluation is deterministic and unit tested.

## Step 19 – Persist compliance results

Persist:
- repository.
- rule.
- result.
- reason.
- evaluated timestamp.
- source inventory/enrichment version or timestamp.

Re-evaluate only when relevant source data or rule configuration changes.

Acceptance:
- compliance remains available after restart.

## Step 20 – Compliance summary API

Add read-only APIs for:
- portfolio summary.
- result/severity counts.
- group summaries.
- repositories with most required-rule failures.
- rule-level result counts.

Acceptance:
- frontend need not recompute compliance.

## Step 21 – Compliance overview UI

Add frontend overview with:
- Required / Recommended / Informational.
- pass/fail/unknown counts.
- group filtering.
- rule filtering.
- stale/refreshing indicators.

Acceptance:
- highest-priority deviations are easy to identify.

## Step 22 – Repository compliance detail

Extend repository detail with:
- applicable rules.
- result.
- reason.
- observed value.
- severity.
- last evaluated time.

Acceptance:
- every compliance flag is explainable.

## Step 23 – Rule detail and affected repositories

Add rule-centric drill-down:
- rule definition.
- scope/groups.
- result counts.
- affected repositories.
- reason per failed/unknown repository.

Acceptance:
- user can answer which repositories violate a standard.

## Step 24 – Repository exceptions / accepted deviations

Persist exceptions with:
- repository.
- rule.
- reason.
- optional expiry.
- created/updated metadata.
- active/expired state.

Acceptance:
- intentional deviations can be separated from actionable failures.

## Step 25 – Exception management UI

Add UI to create, edit, expire and remove RepoFleet-owned exceptions.

This writes only to RepoFleet's own database.

Acceptance:
- exception changes immediately affect compliance summaries.

## Step 26 – GitHub webhook endpoint foundation

Add a secured GitHub App webhook endpoint.

Requirements:
- signature verification.
- reject invalid payloads.
- delivery-ID idempotency.
- record supported/unsupported event types.
- no GitHub writes.

Acceptance:
- a signed test delivery is accepted exactly once.

## Step 27 – Repository lifecycle webhook handling

Handle useful repository lifecycle events:
- created.
- edited.
- renamed/transferred where applicable.
- archived/unarchived.
- deleted.

Update persisted repository identity and mark relevant enrichment dirty.

Acceptance:
- common metadata changes appear without a full refresh.

## Step 28 – Push/release/workflow webhook invalidation

Use useful events to mark specific cached categories dirty.

Examples:
- push → activity/default-branch-related data.
- release → release enrichment.
- workflow events where useful.
- installation repository changes → discovery synchronization.

Acceptance:
- webhook events reduce polling requirements.

## Step 29 – Targeted background refresh queue

Introduce a small persistent job mechanism for:
- webhook-triggered refresh.
- stale cache.
- manual single-repository refresh.

Requirements:
- deduplication.
- bounded workers.
- retry policy.
- state survives restart.
- PostgreSQL preferred before adding an external queue product.

Acceptance:
- one repository can refresh without scanning the full fleet.

## Step 30 – Scheduled consistency refresh

Add a low-frequency scheduled consistency refresh.

Purpose:
- catch missed webhooks.
- detect installation changes.
- validate stale records.
- maintain eventual consistency.

It must use the incremental planner and conditional requests.

Acceptance:
- scheduled refresh is cheap when little changed.

## Step 31 – Rate-limit and refresh diagnostics UI

Expose:
- known GitHub rate-limit remaining/reset.
- recent refresh durations.
- conditional cache hits / 304 counts.
- reused vs refreshed repositories.
- webhook-triggered refresh counts.
- recent failures.

Acceptance:
- API pressure and refresh performance are visible without server logs.

## Step 32 – Phase 2 end-to-end acceptance suite

Add deterministic CI acceptance tests for:
- persistence across restart.
- incremental refresh.
- conditional cache reuse.
- groups.
- rules.
- UNKNOWN semantics.
- exceptions.
- webhook idempotency.
- targeted refresh.

Acceptance:
- Phase 2 core behavior runs without real GitHub secrets.

## Step 33 – Production migration and backup documentation

Update Debian/production documentation for PostgreSQL-backed RepoFleet:
- initialization.
- migrations.
- backup.
- restore.
- upgrade.
- rollback considerations.
- secrets.
- disk usage.
- troubleshooting.

Acceptance:
- a fresh Debian 13 installation can deploy Phase 2 from documentation alone.

## Step 34 – Phase 2 completion review

Formally verify:
- Phase 1 behavior remains.
- cached inventory survives restart.
- normal refresh is incremental.
- GitHub API usage is bounded/observable.
- groups and rules work.
- compliance is explainable.
- exceptions work.
- webhooks and targeted refresh work.
- no GitHub write operations exist.
- deployment/docs are complete.

Document:
- deferred gaps.
- technical debt.
- prerequisites for Phase 3 write operations.

Phase 2 is DONE only after CI, production deployment and completion review are verified.

## Implementation blocks

The 34 steps form five natural blocks:

1. **Persistence and incremental inventory** — Steps 1–14.
2. **Standards and repository groups** — Steps 15–17.
3. **Compliance and exceptions** — Steps 18–25.
4. **Webhooks and targeted refresh** — Steps 26–31.
5. **Acceptance, operations and completion** — Steps 32–34.

Each numbered step is intended to be implemented and verified in one focused prompt before proceeding to the next.
