# RepoFleet – Implementation Status

This document tracks implementation progress against `development-plan-phase-1.md` and `development-plan-phase-2.md`.

Update this file whenever a development step is started, completed, blocked, or intentionally deferred.

## Status Values

- `NOT STARTED` – implementation has not begun.
- `IN PROGRESS` – currently being implemented.
- `DONE` – implemented and verified according to the step's Definition of Done.
- `BLOCKED` – cannot currently proceed; add the reason in Notes.
- `DEFERRED` – intentionally postponed; add the reason in Notes.

## Phase 1 Progress

| Step | Development step | Status | Notes |
|---:|---|---|---|
| 1 | Bootstrap the Monorepo | DONE | Verified by green GitHub Actions frontend and backend build. |
| 2 | Establish Backend API Structure and Domain Models | DONE | Verified by green GitHub Actions frontend and backend build. |
| 3 | Build the Initial Repository Inventory UI | DONE | Verified by green GitHub Actions frontend and backend build. |
| 4 | Add GitHub App Authentication Foundation | DONE | Verified by GitHub Actions. |
| 5 | Replace Sample Inventory with Real Repository Discovery | DONE | Verified by GitHub Actions. |
| 6 | Introduce the In-Memory Repository Inventory | DONE | Verified by GitHub Actions. |
| 7 | Add Topics and Language Enrichment | DONE | Verified by GitHub Actions. |
| 8 | Add LICENSE Analysis | DONE | Verified by GitHub Actions. |
| 9 | Add GitHub Actions Analysis | DONE | Verified by GitHub Actions. |
| 10 | Add Official Release Analysis | DONE | Verified by GitHub Actions. |
| 11 | Complete Refresh Orchestration and Progress Reporting | DONE | Verified by GitHub Actions. |
| 12 | Add Frontend Refresh Experience | DONE | Verified by GitHub Actions. |
| 13 | Implement Repository Filtering | DONE | Verified by GitHub Actions. |
| 14 | Add Sorting and Result Counts | DONE | Verified by GitHub Actions. |
| 15 | Add Repository Selection | DONE | Verified by GitHub Actions. |
| 16 | Add Portfolio Summary Indicators | DONE | Verified by GitHub Actions. |
| 17 | Add Repository Detail View | DONE | Verified by GitHub Actions. |
| 18 | Add Saved Views Using Browser Storage | DONE | Verified by GitHub Actions. |
| 19 | Harden GitHub API and Rate-Limit Handling | DONE | Verified by GitHub Actions. |
| 20 | Accessibility and Responsive UI Pass | DONE | Verified by GitHub Actions. |
| 21 | End-to-End Phase 1 Validation | DONE | Verified by GitHub Actions. |
| 22 | Dockerize Frontend and Backend | DONE | Verified by GitHub Actions, including Docker image builds, non-root container startup, health checks and frontend-to-backend proxy smoke test. |
| 23 | Add Docker Compose Runtime | DONE | Verified by GitHub Actions, including clean Docker Compose build/startup, health checks and frontend-to-backend connectivity. |
| 24 | CI Quality Gate | DONE | Verified by GitHub Actions with the final Quality Gate passing. |
| 25 | Package Versioned Releases with GitHub Actions | DONE | Verified by GitHub Actions release workflow: tagged source validation, versioned GHCR images and deployment/GitHub Release packaging. |
| 26 | Documentation and Phase 1 Completion Review | DONE | Verified by GitHub Actions Quality Gate. Phase 1 documentation/completion review is complete. |

## CI Evolution

The early CI bootstrap has been superseded by the completed Step 24 Quality Gate. Pull requests now validate repository policy, frontend, backend and production Compose packaging before the final `Quality Gate` result.

## Phase 2 Progress

| Step | Development step | Status | Notes |
|---:|---|---|---|
| 1 | Phase 2 persistence foundation | DONE | Verified by green CI and Coolify validation on PR #38. PostgreSQL/Flyway foundation is in place; inventory remains in memory. |
| 2 | Persistent repository identity model | DONE | Verified by green CI and Coolify validation on PR #38. Repository identity is stored uniquely by GitHub repository ID. |
| 3 | Persist discovered repository inventory | DONE | Verified by green CI #104 and Coolify validation #24 on PR #38. Successful discovery synchronizes repository identity and active state. |
| 4 | Serve cached repositories immediately on startup | DONE | Verified by green CI #108 and Coolify validation #28 on PR #38. Active repository identities are loaded before asynchronous GitHub refresh. |
| 5 | Persistent enrichment snapshot model | DONE | Verified by green CI #114 and Coolify validation #34 on PR #38. Full RepositorySummary values can be reconstructed from PostgreSQL snapshots. |
| 6 | Persist progressive enrichment results | DONE | Verified by green CI #119 and Coolify validation #39 on PR #38. Each completed enrichment is persisted and transient failures preserve valid cached metadata. |
| 7 | Refresh history and observability | DONE | Verified by green CI #127 and Coolify validation #47 on PR #38. Refresh runs are persisted and exposed through the inventory history API. |
| 8 | Repository change fingerprinting | DONE | Verified by green CI #132 and Coolify validation #52 on PR #38. Discovery results are classified against persisted GitHub metadata. |
| 9 | Incremental refresh planner | DONE | Verified by green CI #140 and Coolify validation #60 on PR #38. Unchanged repositories with complete snapshots reuse cached enrichment. |
| 10 | GitHub conditional request infrastructure | DONE | Verified by green CI #145 and Coolify validation #65 on PR #38. Persistent ETag state and generic 200/304 handling are available. |
| 11 | Conditional topics and languages refresh | DONE | Verified by green CI #150 and Coolify validation #70 on PR #38. Topics/languages use ETag requests and preserve cached values. |
| 12 | Conditional license, workflows and release refresh | DONE | Verified by green CI #155 and Coolify validation #75 on PR #38. License, workflows and releases now preserve cached data on 304/transient failures. |
| 13 | Refresh policy and cache freshness | DONE | Verified by green CI #172 and Coolify validation #92 on PR #38. RepoFleet distinguishes FRESH, STALE and REFRESHING data with configurable freshness policy. |
| 14 | Controlled enrichment concurrency | DONE | Verified by green CI #178 and Coolify validation #98 on PR #38. Enrichment uses bounded configurable concurrency with repository-level failure isolation. |
| 15 | Repository standards domain model | DONE | Verified by green CI #188 and Coolify validation #108 on PR #38. Standards/rules can be persisted and retrieved without evaluation. |
| 16 | Repository groups | DONE | Verified by green CI #196 and Coolify validation #116 on PR #38. Repository groups can be persisted and resolved deterministically. |
| 17 | Rule-to-group assignments | DONE | Verified by green CI #204 and Coolify validation #124 on PR #38. Applicable rules are resolved deterministically with explicit scope and group explanations. |
| 18 | Rule evaluation engine | DONE | Verified by green CI #219 and Coolify validation #139 on PR #38. Applicable rules evaluate deterministically as PASS/FAIL/UNKNOWN/NOT_APPLICABLE. |
| 19 | Persist compliance results | DONE | Verified by green CI #227 and Coolify validation #147 on PR #38. Compliance results persist across restarts and are reused until source/rule timestamps change. |
| 20 | Compliance summary API | DONE | Verified by green CI #234 and Coolify validation #154 on PR #38. Frontend can consume persisted portfolio, severity, group, repository and rule compliance summaries. |
| 21 | Compliance overview UI | DONE | Verified by green CI #241 and Coolify validation #161 on PR #38. Compliance overview exposes severity/result counts, filters, priority deviations and freshness. |
| 22 | Repository compliance detail | DONE | Verified by green CI #249 and Coolify validation #169 on PR #38. Repository detail now explains each persisted compliance flag. |
| 23 | Rule detail and affected repositories | DONE | Verified by green CI #258 and Coolify validation #178 on PR #38. Rule drill-down shows scope/groups, counts and affected repositories with reasons. |
| 24 | Repository exceptions / accepted deviations | DONE | Verified by green CI #275 and Coolify validation #195 on PR #38. Active accepted deviations are persisted, expiry-aware and separated from actionable failures. |
| 25 | Exception management UI | IN PROGRESS | Adding create/edit/expire/remove controls and write APIs with immediate compliance-summary refresh. |
| 26 | GitHub webhook endpoint foundation | NOT STARTED | |
| 27 | Repository lifecycle webhook handling | NOT STARTED | |
| 28 | Push/release/workflow webhook invalidation | NOT STARTED | |
| 29 | Targeted background refresh queue | NOT STARTED | |
| 30 | Scheduled consistency refresh | NOT STARTED | |
| 31 | Rate-limit and refresh diagnostics UI | NOT STARTED | |
| 32 | Phase 2 end-to-end acceptance suite | NOT STARTED | |
| 33 | Production migration and backup documentation | NOT STARTED | |
| 34 | Phase 2 completion review | NOT STARTED | |

## Current Position

Current step: **Phase 2 Step 25 – Exception management UI (IN PROGRESS).**

Phase 1 completed steps: **26 / 26**  
Phase 2 completed steps: **24 / 34**

## Update Convention

After each implementation prompt:

1. Update the relevant row above.
2. Update `Current Position`.
3. Record important deviations or decisions in `Notes`.
4. Only mark a step `DONE` after its Definition of Done has been verified.

This status file should be included in every updated project ZIP so progress survives between conversations.

## Post-Phase 1 Operational Additions

After Phase 1 completion, production operations were extended with:

- `docs/debian-13-installation.md` for a complete Debian 13 deployment under `/opt/repo-fleet`,
- host-level Nginx + Let's Encrypt guidance using `certbot --nginx` for `repo-fleet.isaksson.info`,
- `deploy/docker-compose.server.yml` with only the frontend bound to configurable loopback port `127.0.0.1:${REPOFLEET_FRONTEND_PORT:-8082}`,
- `deploy/deploy.sh` with exact version deployment and automatic health-check rollback attempt,
- `.github/workflows/deploy.yml`, a manually triggered `workflow_dispatch` deployment of official versions or RCs via SSH and short-lived GHCR authentication,
- GitHub App web-flow user authentication with allowlisted GitHub logins and signed HttpOnly RepoFleet sessions.
- non-blocking automatic inventory startup and progressive repository snapshots so `/api/repositories` can respond immediately while GitHub enrichment continues.

## Post-Phase-1 RC deployment

Production deployment now supports immutable `MAJOR.MINOR.PATCH-rc.N` images published from the default branch without a formal GitHub Release. Official releases remain Git-tag driven.
