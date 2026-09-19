import { renderToString } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import App from './App'
import { InventoryRefreshPanel } from './InventoryRefreshPanel'
import { ComplianceOverviewPanel } from './ComplianceOverviewPanel'
import { PortfolioSummaryPanel } from './PortfolioSummaryPanel'
import { RepositoryDetailPanel } from './RepositoryDetailPanel'
import { RepositoryFiltersPanel } from './RepositoryFiltersPanel'
import { RepositoryInventory } from './RepositoryInventory'
import { RepositorySelectionBar } from './RepositorySelectionBar'
import { RefreshDiagnosticsPanel } from './RefreshDiagnosticsPanel'
import { RepositorySortControls } from './RepositorySortControls'
import { SavedViewsPanel } from './SavedViewsPanel'
import type { CompliancePortfolioSummary, InventoryStatus, RepositorySummary } from './api'
import { emptyRepositoryFilters } from './repositoryFilters'
import { defaultRepositorySort } from './repositorySorting'
import { summarizePortfolio } from './portfolioSummary'
import { createSavedView } from './savedViews'

const repository: RepositorySummary = {
  id: 1001,
  owner: 'erland',
  name: 'roman-nollpunkten',
  fullName: 'erland/roman-nollpunkten',
  url: 'https://github.com/erland/roman-nollpunkten',
  visibility: 'PRIVATE',
  archived: false,
  fork: false,
  defaultBranch: 'main',
  topics: ['novel', 'publishing'],
  languages: ['Python', 'Markdown'],
  primaryLanguage: 'Python',
  license: { analysisState: 'COMPLETE', presence: 'PRESENT', recognized: true, key: 'mit', name: 'MIT License' },
  githubActions: { analysisState: 'COMPLETE', workflowsPresent: true, workflowCount: 3 },
  release: {
    analysisState: 'COMPLETE',
    releasePresent: true,
    latestReleaseName: 'v1.2.0',
    latestReleaseTag: 'v1.2.0',
    latestReleaseDate: '2026-08-10T17:30:00Z',
    latestReleasePrerelease: false,
  },
  activity: { pushedAt: '2026-08-12T14:15:00Z', updatedAt: '2026-08-12T14:16:30Z' },
  refreshStatus: { state: 'COMPLETE', message: null },
}

describe('App', () => {
  it('renders the RepoFleet application shell', () => {
    const html = renderToString(<App />)

    expect(html).toContain('RepoFleet')
    expect(html).toContain('Repository portfolio management')
    expect(html).toContain('Checking authentication')
  })
})

describe('RepositoryFiltersPanel', () => {
  it('keeps simple search visible and advanced filters collapsed by default', () => {
    const html = renderToString(
      <RepositoryFiltersPanel
        filters={{ ...emptyRepositoryFilters, owner: 'erland', license: 'MISSING' }}
        onChange={() => undefined}
        totalCount={20}
        filteredCount={4}
      />,
    )

    expect(html).toContain('Search repositories')
    expect(html).toContain('Repository name or owner/name')
    expect(html).toContain('<details')
    expect(html).toContain('Advanced filters · 2 active')
    expect(html).toContain('Clear all')
    expect(html).toContain('4 of 20 repositories match current filters')
  })
})

describe('RepositoryInventory', () => {
  it('renders a populated repository table', () => {
    const html = renderToString(<RepositoryInventory repositories={[repository]} loading={false} error={null} />)

    expect(html).toContain('erland/roman-nollpunkten')
    expect(html).toContain('novel')
    expect(html).toContain('Python')
    expect(html).toContain('No maintenance flags')
    expect(html).toContain('View details')
  })

  it('renders the loading state', () => {
    const html = renderToString(<RepositoryInventory repositories={[]} loading error={null} />)
    expect(html).toContain('Loading repository inventory')
  })

  it('renders the empty state', () => {
    const html = renderToString(<RepositoryInventory repositories={[]} loading={false} error={null} />)
    expect(html).toContain('No repositories are available')
  })

  it('renders the backend error state', () => {
    const html = renderToString(
      <RepositoryInventory repositories={[]} loading={false} error="Repository inventory could not be loaded from the backend." />,
    )
    expect(html).toContain('Repository inventory could not be loaded')
  })

  it('keeps incomplete analysis distinct from missing capabilities', () => {
    const incomplete = {
      ...repository,
      id: 1002,
      name: 'legacy-java-tool',
      license: { analysisState: 'FAILED', presence: 'UNKNOWN', recognized: null, key: null, name: null },
      githubActions: { analysisState: 'PARTIAL', workflowsPresent: null, workflowCount: null },
      release: {
        analysisState: 'NOT_ANALYZED',
        releasePresent: null,
        latestReleaseName: null,
        latestReleaseTag: null,
        latestReleaseDate: null,
        latestReleasePrerelease: null,
      },
    } satisfies RepositorySummary

    const html = renderToString(<RepositoryInventory repositories={[incomplete]} loading={false} error={null} />)
    expect(html).toContain('License unknown')
    expect(html).toContain('Actions unknown')
    expect(html).toContain('Release unknown')
  })
})

const inventoryStatus = (overrides: Partial<InventoryStatus> = {}): InventoryStatus => ({
  state: 'COMPLETED',
  lastAttemptAt: '2026-08-14T10:00:00Z',
  lastSuccessfulRefreshAt: '2026-08-14T10:01:00Z',
  completedAt: '2026-08-14T10:01:00Z',
  errorMessage: null,
  repositoryCount: 2,
  totalCount: 2,
  processedCount: 2,
  successfulCount: 2,
  errorCount: 0,
  currentRepository: null,
  running: false,
  ...overrides,
})

describe('InventoryRefreshPanel', () => {
  it('renders the idle state and refresh control', () => {
    const html = renderToString(
      <InventoryRefreshPanel status={null} statusError={null} refreshing={false} onRefresh={() => undefined} />,
    )

    expect(html).toContain('System status')
    expect(html).toContain('Not refreshed')
    expect(html).toContain('Last successful refresh:')
    expect(html).toContain('Never')
    expect(html).toContain('Refresh repositories')
  })

  it('renders a separate full refresh control when provided', () => {
    const html = renderToString(
      <InventoryRefreshPanel
        status={inventoryStatus()}
        statusError={null}
        refreshing={false}
        onRefresh={() => undefined}
        onFullRefresh={() => undefined}
      />,
    )

    expect(html).toContain('Full refresh')
  })

  it('renders refresh progress without hiding existing-data guidance', () => {
    const html = renderToString(
      <InventoryRefreshPanel
        status={inventoryStatus({
          state: 'RUNNING',
          processedCount: 3,
          totalCount: 10,
          successfulCount: 3,
          currentRepository: 'erland/repo-fleet',
          running: true,
        })}
        statusError={null}
        refreshing
        onRefresh={() => undefined}
      />,
    )

    expect(html).toContain('Refreshing')
    expect(html).toContain('3 of 10 repositories processed')
    expect(html).toContain('30%')
    expect(html).toContain('<details class="refresh-details" open=""')
    expect(html).toContain('erland/repo-fleet')
    expect(html).toContain('Existing repository data remains available')
    expect(html).toContain('Refreshing')
  })

  it('shows a temporary GitHub rate-limit pause and resume time', () => {
    const html = renderToString(
      <InventoryRefreshPanel
        status={inventoryStatus({ state: 'RUNNING', totalCount: 10, processedCount: 4, running: true })}
        statusError={null}
        refreshing
        diagnostics={{
          rateLimitRemaining: 0,
          rateLimitResetAt: '2026-09-19T06:00:00Z',
          rateLimitPaused: true,
          rateLimitResumeAt: '2026-09-19T06:00:01Z',
          rateLimitPauseReason: 'GitHub API rate limit reached',
          conditionalModifiedCount: 0,
          conditionalNotModifiedCount: 0,
          conditionalCachedFreshCount: 0,
          webhookTriggeredRefreshCount: 0,
          targetedFailedCount: 0,
          recentRuns: [],
          recentTargetedFailures: [],
        }}
        onRefresh={() => undefined}
      />,
    )

    expect(html).toContain('Temporarily paused by GitHub rate limiting')
    expect(html).toContain('Refresh will continue automatically')
  })

  it('renders the success state', () => {
    const html = renderToString(
      <InventoryRefreshPanel
        status={inventoryStatus()}
        statusError={null}
        refreshing={false}
        onRefresh={() => undefined}
      />,
    )

    expect(html).toContain('System status')
    expect(html).toContain('Up to date')
    expect(html).toContain('Last successful refresh')
  })

  it('renders the partial failure warning', () => {
    const html = renderToString(
      <InventoryRefreshPanel
        status={inventoryStatus({ state: 'PARTIAL', successfulCount: 1, errorCount: 1 })}
        statusError={null}
        refreshing={false}
        onRefresh={() => undefined}
      />,
    )

    expect(html).toContain('Needs attention')
    expect(html).toContain('Refresh completed with partial failures')
    expect(html).toContain('1 repository has incomplete or failed analysis')
    expect(html).toContain('<details class="refresh-details" open=""')
  })

  it('renders the failed refresh state', () => {
    const html = renderToString(
      <InventoryRefreshPanel
        status={inventoryStatus({
          state: 'FAILED',
          errorMessage: 'GitHub unavailable',
          successfulCount: 0,
          errorCount: 2,
        })}
        statusError={null}
        refreshing={false}
        onRefresh={() => undefined}
      />,
    )

    expect(html).toContain('Refresh failed')
    expect(html).toContain('GitHub unavailable')
    expect(html).toContain('<details class="refresh-details" open=""')
  })
})

describe('RepositoryFiltersPanel', () => {
  it('renders all core Phase 1 filter controls and result count', () => {
    const html = renderToString(
      <RepositoryFiltersPanel
        filters={emptyRepositoryFilters}
        onChange={() => undefined}
        totalCount={200}
        filteredCount={12}
      />,
    )

    expect(html).toContain('Search repositories')
    expect(html).toContain('Advanced filters')
    expect(html).toContain('Name prefix')
    expect(html).toContain('Owner')
    expect(html).toContain('Visibility')
    expect(html).toContain('Topic')
    expect(html).toContain('Language')
    expect(html).toContain('License')
    expect(html).toContain('GitHub Actions')
    expect(html).toContain('Official release')
    expect(html).toContain('Activity')
    expect(html).toContain('12 of 200 repositories match current filters')
  })
})

describe('RepositorySortControls', () => {
  it('renders useful sort fields and a filtered/total result count', () => {
    const html = renderToString(
      <RepositorySortControls
        sort={defaultRepositorySort}
        onChange={() => undefined}
        totalCount={200}
        filteredCount={12}
      />,
    )

    expect(html).toContain('Sort by')
    expect(html).toContain('Last activity')
    expect(html).toContain('Primary language')
    expect(html).toContain('License state')
    expect(html).toContain('Actions state')
    expect(html).toContain('Release state')
    expect(html).toContain('12 of 200 repositories')
  })

  it('renders a compact count when no filter reduces the result set', () => {
    const html = renderToString(
      <RepositorySortControls
        sort={defaultRepositorySort}
        onChange={() => undefined}
        totalCount={200}
        filteredCount={200}
      />,
    )

    expect(html).toContain('200 repositories')
  })
})

describe('RepositorySelectionBar', () => {
  it('shows total selected and visible selected counts', () => {
    const html = renderToString(
      <RepositorySelectionBar
        selection={new Set([1001, 9999])}
        visibleRepositories={[repository]}
        onSelectVisible={() => undefined}
        onDeselectVisible={() => undefined}
        onClear={() => undefined}
      />,
    )

    expect(html).toContain('2 selected')
    expect(html).toContain('1 of 1 visible selected')
    expect(html).toContain('Deselect visible')
    expect(html).toContain('Clear selection')
  })

  it('renders repository row selection using stable repository id', () => {
    const html = renderToString(
      <RepositoryInventory
        repositories={[repository]}
        loading={false}
        error={null}
        selectedRepositoryIds={new Set([repository.id])}
        onToggleRepository={() => undefined}
      />,
    )

    expect(html).toContain('Select erland/roman-nollpunkten')
    expect(html).toContain('checked')
    expect(html).toContain('repository-row-selected')
  })
})

describe('RepositorySelectionBar visibility', () => {
  it('keeps repository selection controls out of the discovery flow until needed', () => {
    const html = renderToString(
      <RepositorySelectionBar
        selection={new Set()}
        visibleRepositories={[repository]}
        onSelectVisible={() => undefined}
        onDeselectVisible={() => undefined}
        onClear={() => undefined}
      />,
    )

    expect(html).toBe('')
  })
})

describe('PortfolioSummaryPanel', () => {
  it('renders core portfolio signals for the filtered scope', () => {
    const summary = summarizePortfolio([repository])

    const html = renderToString(
      <PortfolioSummaryPanel
        summary={summary}
        totalPortfolioCount={200}
      />,
    )

    expect(html).toContain('Portfolio signals')
    expect(html).toContain('Missing LICENSE')
    expect(html).toContain('Missing Actions')
    expect(html).toContain('Missing release')
    expect(html).toContain('Java repositories')
    expect(html).toContain('Archived')
    expect(html).toContain('Forks')
    expect(html).toContain('Summary for 1 filtered repositories out of 200')
  })

  it('shows unknown maintenance analysis separately from missing', () => {
    const incomplete = {
      ...repository,
      license: { analysisState: 'FAILED', presence: 'UNKNOWN', recognized: null, key: null, name: null },
    } satisfies RepositorySummary

    const html = renderToString(
      <PortfolioSummaryPanel
        summary={summarizePortfolio([incomplete])}
        totalPortfolioCount={1}
      />,
    )

    expect(html).toContain('1 unknown')
  })
})

describe('RepositoryDetailPanel', () => {
  it('renders the full read-only repository metadata view', () => {
    const html = renderToString(
      <RepositoryDetailPanel
        repository={repository}
        compliance={[{
          ruleKey: 'license-required',
          ruleName: 'License required',
          ruleType: 'LICENSE_REQUIRED',
          severity: 'REQUIRED',
          result: 'FAIL',
          reason: 'Repository does not contain a license.',
          observedValue: 'MISSING',
          evaluatedAt: '2026-09-18T12:00:00Z',
        }]}
        complianceLoading={false}
        complianceError={null}
        onClose={() => undefined}
      />,
    )

    expect(html).toContain('Repository details')
    expect(html).toContain('erland/roman-nollpunkten')
    expect(html).toContain('role="dialog"')
    expect(html).toContain('aria-modal="true"')
    expect(html).toContain('Repository overview')
    expect(html).toContain('Maintenance analysis')
    expect(html).toContain('Default branch')
    expect(html).toContain('publishing')
    expect(html).toContain('MIT License')
    expect(html).toContain('3 workflows')
    expect(html).toContain('v1.2.0')
    expect(html).toContain('Repository analysis')
    expect(html).toContain('Open on GitHub')
    expect(html).toContain('Close details')
    expect(html).toContain('Compliance detail')
    expect(html).toContain('License required')
    expect(html).toContain('Repository does not contain a license.')
    expect(html).toContain('MISSING')
    expect(html).toContain('Last evaluated')
    expect(html).toContain('Accept deviation')
  })

  it('renders edit, expire and remove controls for an accepted deviation', () => {
    const html = renderToString(
      <RepositoryDetailPanel
        repository={repository}
        compliance={[{
          ruleKey: 'license-required',
          ruleName: 'License required',
          ruleType: 'LICENSE_REQUIRED',
          severity: 'REQUIRED',
          result: 'FAIL',
          reason: 'Repository does not contain a license.',
          observedValue: 'MISSING',
          evaluatedAt: '2026-09-18T12:00:00Z',
          acceptedDeviation: true,
          exceptionReason: 'Legacy repository accepted temporarily.',
          exceptionExpiresAt: '2026-12-31T23:59:59Z',
        }]}
        onClose={() => undefined}
      />,
    )

    expect(html).toContain('ACCEPTED DEVIATION')
    expect(html).toContain('Legacy repository accepted temporarily.')
    expect(html).toContain('Edit exception')
    expect(html).toContain('Expire exception')
    expect(html).toContain('Remove exception')
  })

  it('renders nothing when no repository is selected for details', () => {
    expect(renderToString(
      <RepositoryDetailPanel repository={null} onClose={() => undefined} />,
    )).toBe('')
  })

  it('keeps unknown analysis explicit in the detail view', () => {
    const incomplete = {
      ...repository,
      license: { analysisState: 'FAILED', presence: 'UNKNOWN', recognized: null, key: null, name: null },
      githubActions: { analysisState: 'FAILED', workflowsPresent: null, workflowCount: null },
      release: {
        analysisState: 'FAILED',
        releasePresent: null,
        latestReleaseName: null,
        latestReleaseTag: null,
        latestReleaseDate: null,
        latestReleasePrerelease: null,
      },
    } satisfies RepositorySummary

    const html = renderToString(
      <RepositoryDetailPanel repository={incomplete} onClose={() => undefined} />,
    )

    expect((html.match(/Unknown/g) ?? []).length).toBeGreaterThanOrEqual(4)
    expect(html).toContain('Failed')
  })

  it('offers a details action for each repository row', () => {
    const html = renderToString(
      <RepositoryInventory
        repositories={[repository]}
        loading={false}
        error={null}
        onOpenDetails={() => undefined}
      />,
    )

    expect(html).toContain('View details')
  })
})

describe('SavedViewsPanel', () => {
  it('renders saved views and browser-storage guidance', () => {
    const view = createSavedView(
      'Java missing LICENSE',
      { ...emptyRepositoryFilters, language: 'Java', license: 'MISSING' },
      defaultRepositorySort,
      'view-1',
    )

    const html = renderToString(
      <SavedViewsPanel
        views={[view]}
        activeViewId="view-1"
        storageAvailable
        onShowAll={() => undefined}
        onSave={() => undefined}
        onLoad={() => undefined}
        onDelete={() => undefined}
      />,
    )

    expect(html).toContain('Quick categories')
    expect(html).toContain('All repositories')
    expect(html).toContain('Java missing LICENSE')
    expect(html).toContain('1 saved view')
    expect(html).toContain('Manage views')
    expect(html).toContain('Save current view')
    expect(html).toContain('Delete')
    expect(html).toContain('aria-pressed="true"')
    expect(html).toContain('Repository selection is not included')
  })

  it('shows a graceful warning when browser storage is unavailable', () => {
    const html = renderToString(
      <SavedViewsPanel
        views={[]}
        activeViewId={null}
        storageAvailable={false}
        onShowAll={() => undefined}
        onSave={() => undefined}
        onLoad={() => undefined}
        onDelete={() => undefined}
      />,
    )

    expect(html).toContain('Browser storage is unavailable')
    expect(html).toContain('No saved views yet')
  })
})

describe('Accessibility and responsive markup', () => {
  it('renders the authentication bootstrap in a semantic main region', () => {
    const html = renderToString(<App />)

    expect(html).toContain('<main')
    expect(html).toContain('Checking authentication')
  })

  it('labels the repository table as a keyboard-scrollable region with a caption', () => {
    const html = renderToString(
      <RepositoryInventory repositories={[repository]} loading={false} error={null} />,
    )

    expect(html).toContain('role="region"')
    expect(html).toContain('aria-label="Repository inventory table"')
    expect(html).toContain('tabindex="0"')
    expect(html).toContain('Repository results with discovery information and maintenance flags')
    expect(html).toContain('data-label="Repository"')
    expect(html).toContain('data-label="Maintenance"')
    expect(html).toContain('data-label="Details"')
  })

  it('makes repository details programmatically focusable when opened', () => {
    const html = renderToString(
      <RepositoryDetailPanel repository={repository} onClose={() => undefined} />,
    )

    expect(html).toContain('tabindex="-1"')
    expect(html).toContain('role="dialog"')
    expect(html).toContain('aria-modal="true"')
    expect(html).toContain('aria-labelledby="repository-detail-heading"')
    expect(html).toContain('aria-describedby="repository-detail-description"')
    expect(html).toContain('aria-label="Close details for erland/roman-nollpunkten"')
  })

  it('gives saved-view actions descriptive accessible names', () => {
    const view = createSavedView(
      'Java missing LICENSE',
      emptyRepositoryFilters,
      defaultRepositorySort,
      'view-1',
    )

    const html = renderToString(
      <SavedViewsPanel
        views={[view]}
        activeViewId="view-1"
        storageAvailable
        onShowAll={() => undefined}
        onSave={() => undefined}
        onLoad={() => undefined}
        onDelete={() => undefined}
      />,
    )

    expect(html).toContain('aria-label="Repository views"')
    expect(html).toContain('aria-pressed="true"')
    expect(html).toContain('aria-label="Delete saved view Java missing LICENSE"')
    expect(html).toContain('aria-describedby="saved-views-description"')
  })

  it('associates refresh progress with its visible text label', () => {
    const html = renderToString(
      <InventoryRefreshPanel
        status={inventoryStatus({
          state: 'RUNNING',
          totalCount: 10,
          processedCount: 2,
          currentRepository: 'erland/repo-fleet',
          running: true,
        })}
        statusError={null}
        refreshing
        onRefresh={() => undefined}
      />,
    )

    expect(html).toContain('id="refresh-progress-label"')
    expect(html).toContain('aria-labelledby="refresh-progress-label"')
  })
})


describe('Workspace accessibility', () => {
  it('connects workspace navigation to labelled content regions', () => {
    const source = renderToString(<App />)

    expect(source).toContain('Skip to main content')
  })
})

describe('ComplianceOverviewPanel', () => {
  it('renders severity counts, filters, freshness and priority deviations', () => {
    const summary: CompliancePortfolioSummary = {
      repositoryCount: 2,
      evaluatedRuleCount: 4,
      resultCounts: { PASS: 2, FAIL: 1, UNKNOWN: 1, NOT_APPLICABLE: 0 },
      severityResultCounts: {
        REQUIRED: { PASS: 1, FAIL: 1, UNKNOWN: 0, NOT_APPLICABLE: 0 },
        RECOMMENDED: { PASS: 1, FAIL: 0, UNKNOWN: 1, NOT_APPLICABLE: 0 },
        INFORMATIONAL: { PASS: 0, FAIL: 0, UNKNOWN: 0, NOT_APPLICABLE: 0 },
      },
      groups: [{
        groupKey: 'services',
        groupName: 'Services',
        repositoryCount: 1,
        resultCounts: { PASS: 1, FAIL: 1, UNKNOWN: 0, NOT_APPLICABLE: 0 },
      }],
      repositoriesWithMostRequiredFailures: [{
        githubRepositoryId: 1001,
        fullName: 'erland/roman-nollpunkten',
        requiredFailureCount: 1,
      }],
      rules: [{
        ruleKey: 'license-required',
        ruleName: 'License required',
        severity: 'REQUIRED',
        resultCounts: { PASS: 1, FAIL: 1, UNKNOWN: 0, NOT_APPLICABLE: 0 },
      }],
    }

    const html = renderToString(
      <ComplianceOverviewPanel
        summary={summary}
        repositories={[{ ...repository, refreshStatus: { state: 'COMPLETE', message: null, freshness: 'STALE' } }]}
        loading={false}
        error={null}
      />,
    )

    expect(html).toContain('Compliance overview')
    expect(html).toContain('Required')
    expect(html).toContain('Recommended')
    expect(html).toContain('Informational')
    expect(html).toContain('All groups')
    expect(html).toContain('All rules')
    expect(html).toMatch(/1(?:<!-- -->)? stale/)
    expect(html).toContain('Highest-priority deviations')
    expect(html).toMatch(/1(?:<!-- -->)? required failure/)
  })
})


describe('RefreshDiagnosticsPanel', () => {
  it('renders API pressure, reuse and recent failures', () => {
    const html = renderToString(
      <RefreshDiagnosticsPanel
        loading={false}
        error={null}
        diagnostics={{
          rateLimitRemaining: 4321,
          rateLimitResetAt: '2026-09-18T18:00:00Z',
          rateLimitPaused: false,
          rateLimitResumeAt: null,
          rateLimitPauseReason: null,
          conditionalModifiedCount: 5,
          conditionalNotModifiedCount: 12,
          conditionalCachedFreshCount: 30,
          webhookTriggeredRefreshCount: 7,
          targetedFailedCount: 1,
          recentRuns: [{
            id: 1,
            triggerType: 'SCHEDULED_CONSISTENCY',
            startedAt: '2026-09-18T16:00:00Z',
            completedAt: '2026-09-18T16:02:00Z',
            durationMillis: 120000,
            finalState: 'COMPLETED',
            discoveredCount: 200,
            processedCount: 200,
            successfulCount: 200,
            errorCount: 0,
            reusedCount: 180,
            scheduledCount: 20,
            failureSummary: null,
          }],
          recentTargetedFailures: [{
            jobId: 9,
            githubRepositoryId: 1001,
            triggerType: 'WEBHOOK_RELEASES',
            attempts: 3,
            lastError: 'GitHub unavailable',
            completedAt: '2026-09-18T16:05:00Z',
          }],
        }}
      />,
    )

    expect(html).toContain('Refresh diagnostics')
    expect(html).toContain('4321')
    expect(html).toContain('12')
    expect(html).toContain('30')
    expect(html).toContain('fresh-cache skips')
    expect(html).toContain('SCHEDULED_CONSISTENCY')
    expect(html).toContain('180')
    expect(html).toContain('GitHub unavailable')
  })
})
