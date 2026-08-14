# RepoFleet – Functional Specification

## 1. Purpose

The service shall provide a unified overview of a large GitHub repository portfolio and make it easier to identify repositories that require attention, standardization, or maintenance.

The primary goal is to answer questions that are difficult or cumbersome to answer in GitHub's standard interface, for example:

- Which repositories with a certain name prefix have a specific topic?
- Which repositories are missing GitHub Actions?
- Which repositories with a given topic have no official release?
- Which repositories are missing a LICENSE file?
- Which repositories contain Java code?
- Which repositories deviate from a desired standard?
- Which selected repositories should receive the same maintenance action?

The service should primarily support portfolio-level analysis and maintenance rather than replace GitHub as the source of truth.

---

## 2. Product Principles

The service should follow these functional principles:

1. **Portfolio first**  
   The main unit of work is a collection of repositories, not a single repository.

2. **Analysis before modification**  
   Users should first be able to understand the current state before making changes.

3. **Filter, select, act**  
   A common workflow should be:
   - find matching repositories,
   - inspect their state,
   - select relevant repositories,
   - perform or prepare a maintenance action.

4. **Safe by default**  
   Potentially destructive or broad changes should require explicit selection and preview.

5. **GitHub remains authoritative**  
   The service should reflect GitHub state rather than create a parallel repository management system.

6. **Progressive capability**  
   Read-only analytics should be useful on its own. Maintenance features can then be added in later phases.

---

# Phase 1 – Repository Inventory & Analytics

## 3. Goal

Phase 1 shall provide a complete, searchable and filterable overview of repositories and their most important maintenance-related properties.

This phase is read-only.

---

## 4. Repository Overview

The user shall be able to view all repositories available to the service in a single repository inventory.

For each repository, the overview should be able to show at least:

- Repository name
- Repository owner
- Visibility
- Archived status
- Fork status
- Default branch
- Topics
- Detected programming languages
- License status
- GitHub Actions status
- Release status
- Latest repository activity
- Repository URL

The interface should make it easy to scan many repositories without opening them individually.

---

## 5. Filtering and Search

The user shall be able to filter the repository inventory using multiple criteria simultaneously.

Supported filters should include at least:

### Repository identity
- Name contains
- Name starts with a prefix
- Owner
- Visibility
- Archived / active
- Fork / non-fork

### Topics
- Has topic
- Does not have topic
- Has any of several topics

### Programming language
- Contains a specific language
- Does not contain a specific language

### License
- Has a LICENSE file
- Does not have a LICENSE file
- Has a recognized license
- License type, where available

### GitHub Actions
- Has GitHub Actions workflows
- Does not have GitHub Actions workflows

### Releases
- Has at least one official release
- Has no official release

### Activity
- Recently updated
- Not updated since a selected date or period

Filters should be combinable so that questions such as the following are possible:

> Show all repositories starting with `roman-` that do not have GitHub Actions.

> Show all repositories with topic `board-game` that do not have an official release.

> Show all repositories containing Java code and missing a LICENSE file.

---

## 6. Repository Detail View

The user shall be able to open a repository from the inventory and view a concise maintenance-oriented summary.

The detail view should include:

- Basic repository information
- Topics
- Languages
- License status
- GitHub Actions status
- Release status
- Recent activity
- Any detected maintenance observations

The purpose of this view is not to replace GitHub's repository page, but to explain why the repository appears in a filtered result.

---

## 7. Repository Selection

The user shall be able to select one or more repositories from the current result set.

Selection should support:

- Individual selection
- Select all visible results
- Clear selection
- Keep the selection while adjusting the visible result where practical

Phase 1 does not perform changes, but selection should already exist so that later maintenance features can reuse the same workflow.

---

## 8. Summary Indicators

The service should provide high-level portfolio summaries, for example:

- Total number of repositories
- Active repositories
- Archived repositories
- Repositories missing LICENSE
- Repositories without GitHub Actions
- Repositories without releases
- Repositories grouped by topic
- Repositories grouped by primary language

These indicators should allow the user to quickly identify areas that need further investigation.

---

## 9. Saved Views

The user should be able to save useful combinations of filters as named views.

Examples:

- Roman repositories
- Board games missing releases
- Java repositories
- Repositories missing LICENSE
- Repositories without GitHub Actions

A saved view should allow the user to return to the same portfolio question later without rebuilding the filters.

---

# Phase 2 – Standards, Rules & Maintenance Insights

## 10. Goal

Phase 2 shall allow the user to define what "good" looks like for different groups of repositories and identify deviations automatically.

The phase remains primarily analytical.

---

## 11. Repository Groups

The user shall be able to define logical groups of repositories based on filters.

Examples:

- Repositories whose names start with `roman-`
- Repositories with topic `board-game`
- All Java repositories
- All active private repositories
- All repositories with a specific topic

A repository may belong to multiple groups.

---

## 12. Repository Standards

The user shall be able to define expected characteristics for a repository group.

Examples of standards:

- Must have a LICENSE file
- Must have a certain topic
- Must have GitHub Actions
- Must have at least one official release
- Must not be archived
- Should contain a README
- Should have recent activity
- Should use one or more expected topics

The service should distinguish where appropriate between:

- Required
- Recommended
- Informational

---

## 13. Compliance Overview

For each repository group, the service shall provide an overview of how well repositories match the defined standards.

Example:

| Requirement | Compliant | Missing |
|---|---:|---:|
| LICENSE | 17 | 1 |
| Topic `novel` | 14 | 4 |
| GitHub Actions | 12 | 6 |
| Official release | 16 | 2 |

The user shall be able to select a failed requirement and see the affected repositories.

---

## 14. Repository Maintenance Status

Each repository should be able to receive an overall maintenance status such as:

- Healthy
- Attention recommended
- Action required
- Excluded
- Archived

The status should be explainable through the rules or observations that caused it.

The user should never have to guess why a repository has been flagged.

---

## 15. Exceptions

The user shall be able to mark intentional exceptions.

Examples:

- This repository intentionally has no release.
- This archived repository does not need GitHub Actions.
- This repository should not receive the standard topic.
- This repository intentionally uses a different license model.

Exceptions should prevent known and accepted deviations from creating permanent noise in the maintenance overview.

---

# Phase 3 – Guided Maintenance Actions

## 16. Goal

Phase 3 shall allow selected maintenance actions to be performed across multiple repositories while keeping the user in control.

The core interaction should be:

**Find → Select → Preview → Apply**

---

## 17. Topic Maintenance

The user shall be able to manage repository topics for selected repositories.

Supported actions should include:

- Add a topic
- Remove a topic
- Add several topics
- Preview repositories that will be changed
- Identify repositories where no change is necessary

The service should clearly separate:

- Repositories that will change
- Repositories already compliant
- Repositories that cannot be updated
- Repositories excluded by the user

---

## 18. LICENSE Maintenance

The user shall be able to identify selected repositories that are missing a LICENSE file and prepare a common license update.

The user should be able to:

- Choose repositories
- Choose the intended license content or template
- Preview affected repositories
- Identify existing license conflicts
- Exclude specific repositories
- Apply the change to the final selection

Existing license files should never be silently overwritten.

---

## 19. Common File Maintenance

The service should support adding or updating selected common files across repositories.

Potential examples include:

- LICENSE
- CODEOWNERS
- CONTRIBUTING
- SECURITY
- Common repository documentation
- Shared configuration files

Each operation should clearly show:

- What file is affected
- Which repositories are affected
- Whether the file already exists
- Whether the operation creates, updates, or skips the file

---

## 20. Change Preview

Before a maintenance action is applied, the service shall provide a clear preview.

The preview should show:

- Number of selected repositories
- Number of repositories that require change
- Number already compliant
- Number with potential conflicts
- Number excluded
- Summary of the intended change

For file changes, the user should be able to inspect the proposed content before applying it.

---

## 21. Maintenance Result

After an operation, the service shall show the outcome per repository.

Possible outcomes include:

- Completed
- No change required
- Skipped
- Failed
- Requires manual review

The user should be able to retry failed operations without repeating successful ones.

---

# Phase 4 – Pull Request Based Maintenance

## 22. Goal

Phase 4 shall make repository content changes safer by supporting maintenance through branches and pull requests rather than direct modification of the default branch.

---

## 23. Pull Request Maintenance Workflow

For supported content changes, the user should be able to choose a pull-request-based workflow.

Typical flow:

1. Select repositories
2. Select maintenance action
3. Preview changes
4. Create maintenance changes
5. Create pull requests
6. Review progress in the service
7. Merge through normal GitHub workflows

---

## 24. Pull Request Overview

The service shall provide an overview of pull requests created by maintenance operations.

For each repository it should show:

- Repository
- Maintenance operation
- Pull request status
- Merge status
- Failed or blocked state
- Link to the pull request

This allows a large maintenance campaign to be followed from one place.

---

## 25. Conflict Handling

The service should identify repositories where an automated maintenance change is unsafe or ambiguous.

Examples:

- Existing LICENSE differs from intended license
- Target file contains repository-specific customization
- Maintenance branch already exists
- Related maintenance pull request is already open

Such repositories should be separated for manual review rather than forcing the operation.

---

# Phase 5 – Reusable Maintenance Campaigns

## 26. Goal

Phase 5 shall allow the user to repeat common maintenance activities across a repository portfolio in a controlled and traceable way.

---

## 27. Maintenance Campaigns

The user shall be able to create a named maintenance campaign.

Examples:

- Add standard LICENSE to board game repositories
- Roll out latest publishing workflows
- Add topic `novel`
- Update repository governance files

A campaign should contain:

- Name
- Description
- Target repository group or filter
- Intended maintenance action
- Current status
- Included repositories
- Excluded repositories
- Results

---

## 28. Campaign Progress

The user shall be able to see campaign progress at portfolio level.

Example statuses:

- Not started
- Planned
- In progress
- Pull request open
- Completed
- Failed
- Manually excluded

This makes it possible to roll out a change gradually rather than modifying every repository at once.

---

## 29. Campaign History

The service shall maintain a history of maintenance campaigns performed through the service.

The user should be able to answer questions such as:

- Which repositories received this change?
- Which repositories were excluded?
- Which repositories failed?
- Which pull requests were created?
- Has this repository already received this maintenance update?

The history should focus on service-initiated maintenance rather than attempt to replicate the full Git history.

---

# Phase 6 – Advanced Portfolio Analytics

## 30. Goal

Phase 6 shall provide deeper insight into repository health, consistency and long-term maintenance needs.

---

## 31. Repository Freshness

The service should be able to identify potentially stale repositories.

Possible indicators include:

- No recent commits
- No recent release
- Old maintenance standard
- Archived candidate
- Long-standing failed maintenance requirement

The service should present these as observations, not automatically archive repositories.

---

## 32. Portfolio Consistency

The service should identify inconsistencies across similar repositories.

Examples:

- Most `roman-*` repositories have GitHub Actions but some do not.
- Most board game repositories use a particular topic but a few are missing it.
- Similar repositories use different license conventions.
- Some repositories use an older maintenance pattern than their peers.

This should help discover problems even before an explicit rule has been defined.

---

## 33. Comparative Views

The user should be able to compare groups of repositories.

Examples:

- Roman repositories vs board game repositories
- Archived vs active repositories
- Java vs TypeScript repositories
- Repositories with Actions vs repositories without Actions

The purpose is operational insight rather than developer productivity scoring.

---

# Phase 7 – Automation & Continuous Maintenance

## 34. Goal

Phase 7 shall make recurring repository hygiene easier while retaining user control.

---

## 35. Scheduled Analysis

The service should be able to refresh repository analytics periodically and identify newly introduced deviations.

Examples:

- Repository loses a required topic
- New repository is created without LICENSE
- New repository matches a defined group but lacks required standards
- Repository no longer has an official release
- A repository becomes stale according to a defined rule

---

## 36. Maintenance Notifications

The user should be able to receive notifications when action is needed.

Notifications should focus on actionable changes, for example:

- 3 new repositories are missing LICENSE
- 2 repositories now violate the publishing standard
- 5 repositories match a newly defined maintenance rule
- A maintenance campaign has unresolved failures

---

## 37. Suggested Actions

The service may suggest maintenance actions based on detected deviations.

Examples:

> 6 repositories in the `roman-*` group are missing the standard GitHub Actions setup.

Suggested action:

> Prepare maintenance campaign for these 6 repositories.

Suggested actions should require user approval before making repository changes.

---

# 38. Cross-Phase Functional Requirements

## 38.1 Auditability

The user should be able to understand:

- What the service detected
- Why a repository was flagged
- What action was proposed
- What action was performed
- Which repositories were affected
- What succeeded or failed

---

## 38.2 Bulk Operations

Bulk operations should always operate on an explicit repository selection.

The service should clearly show the number of affected repositories before execution.

---

## 38.3 Dry Run / Preview

Any write operation affecting multiple repositories should provide a preview before execution.

The preview should be sufficiently clear that the user can determine whether the scope is correct.

---

## 38.4 Exclusions

The user should be able to exclude individual repositories from:

- Rules
- Saved groups
- Maintenance campaigns
- Bulk operations

---

## 38.5 Large Repository Portfolios

The interface should remain usable for portfolios containing hundreds of repositories.

Users should not be expected to inspect or open repositories one at a time.

---

## 38.6 Private and Public Repositories

The service should support both private and public repositories available to the connected GitHub account or organization.

Visibility should be clearly indicated.

---

## 38.7 Archived Repositories

Archived repositories should remain visible but should be easily filterable and excludable from active maintenance work.

---

# 39. Initial Phase 1 Acceptance Criteria

Phase 1 can be considered functionally complete when the user can:

1. Connect the service to GitHub.
2. View all repositories available through that connection.
3. Filter repositories by name or prefix.
4. Filter repositories by topic.
5. Filter repositories by programming language.
6. Identify repositories with or without a LICENSE file.
7. Identify repositories with or without GitHub Actions.
8. Identify repositories with or without an official release.
9. Combine several filters in the same query.
10. Select repositories from a filtered result.
11. Save a useful filtered view.
12. Open a repository and understand why it matched the current criteria.
13. View high-level portfolio counts for key maintenance indicators.

This first phase should already provide significant value without requiring the service to modify any repository.

---

# 40. Suggested Phase Order

The recommended implementation order from a product perspective is:

1. **Phase 1 – Repository Inventory & Analytics**
2. **Phase 2 – Standards, Rules & Maintenance Insights**
3. **Phase 3 – Guided Maintenance Actions**
4. **Phase 4 – Pull Request Based Maintenance**
5. **Phase 5 – Reusable Maintenance Campaigns**
6. **Phase 6 – Advanced Portfolio Analytics**
7. **Phase 7 – Automation & Continuous Maintenance**

The separation is intentional:

- Phase 1 solves the immediate visibility problem.
- Phase 2 turns observations into repeatable standards.
- Phase 3 introduces controlled write operations.
- Phase 4 makes content changes safer.
- Phase 5 supports larger rollout programs.
- Phase 6 improves long-term portfolio insight.
- Phase 7 reduces recurring manual maintenance.

---

# 41. Out of Scope for the Functional Specification

The following topics should be defined in the subsequent Development Plan rather than this document:

- Application architecture
- Frontend framework
- Backend technology
- Hosting platform
- Database choice
- GitHub API implementation details
- GitHub App permission details
- Authentication implementation
- Webhooks
- Polling and synchronization strategy
- Rate-limit handling
- Caching strategy
- Data model implementation
- Background processing
- Deployment
- CI/CD for the service itself
- Testing technology
- Observability and logging technology
- Security implementation details

These are technical design choices rather than functional requirements.
