import { renderToString } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import { PortfolioSummaryPanel } from './PortfolioSummaryPanel'
import { RepositoryDetailPanel } from './RepositoryDetailPanel'
import { RepositoryInventory } from './RepositoryInventory'
import { RepositorySelectionBar } from './RepositorySelectionBar'
import { SavedViewsPanel } from './SavedViewsPanel'
import type { RepositorySummary } from './api'
import { emptyRepositoryFilters } from './repositoryFilters'
import { defaultRepositorySort } from './repositorySorting'
import { summarizePortfolio } from './portfolioSummary'
import { createSavedView } from './savedViews'
import { repositoryFixture as repository } from './testFixtures'

describe('RepositoryInventory', () => {
  it('renders a populated repository table', () => {
    const html = renderToString(<RepositoryInventory repositories={[repository]} loading={false} error={null} />)

    expect(html).toContain('erland/roman-nollpunkten')
    expect(html).toContain('novel')
    expect(html).toContain('Python')
    expect(html).toContain('repository-signal')
    expect(html).toContain('Open details for erland/roman-nollpunkten')
    expect(html).toContain('repository-row-interactive')
    expect(html).toContain('GitHub')
  })

  it('renders loading, empty and error states', () => {
    expect(renderToString(<RepositoryInventory repositories={[]} loading error={null} />))
      .toContain('Loading repository inventory')
    expect(renderToString(<RepositoryInventory repositories={[]} loading={false} error={null} />))
      .toContain('No repositories are available')
    expect(renderToString(
      <RepositoryInventory repositories={[]} loading={false} error="Repository inventory could not be loaded from the backend." />,
    )).toContain('Repository inventory could not be loaded')
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

  it('shows stale metadata without converting known capabilities into missing flags', () => {
    const stale = {
      ...repository,
      refreshStatus: {
        state: 'COMPLETE',
        message: 'Cached metadata retained after transient failure',
        freshness: 'STALE',
        latestOutcome: 'DEGRADED',
      },
    } satisfies RepositorySummary

    const html = renderToString(
      <RepositoryInventory repositories={[stale]} loading={false} error={null} />,
    )

    expect(html).toContain('Stale metadata')
    expect(html).not.toContain('License unknown')
    expect(html).not.toContain('Actions unknown')
    expect(html).not.toContain('Release unknown')
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
    expect(html).toContain('mobile-repository-signals')
    expect(html).toContain('repository-signal')
    expect(html).toContain('mobile-github-link')
    expect(html).toContain('repository-row-interactive')
    expect(html).toContain('aria-label="Open details for erland/roman-nollpunkten"')
  })

  it('makes each repository row open RepoFleet details and keeps GitHub secondary', () => {
    const html = renderToString(
      <RepositoryInventory repositories={[repository]} loading={false} error={null} onOpenDetails={() => undefined} />,
    )

    expect(html).toContain('Open details for erland/roman-nollpunkten')
    expect(html).toContain('repository-details-link')
    expect(html).toContain('repository-github-link')
  })
})

describe('RepositorySelectionBar', () => {
  it('shows selection state and stays hidden when nothing is selected', () => {
    const selected = renderToString(
      <RepositorySelectionBar
        selection={new Set([1001, 9999])}
        visibleRepositories={[repository]}
        onSelectVisible={() => undefined}
        onDeselectVisible={() => undefined}
        onClear={() => undefined}
      />,
    )
    expect(selected).toContain('2 selected')
    expect(selected).toContain('1 of 1 visible selected')
    expect(selected).toContain('Deselect visible')
    expect(selected).toContain('Clear selection')

    const empty = renderToString(
      <RepositorySelectionBar
        selection={new Set()}
        visibleRepositories={[repository]}
        onSelectVisible={() => undefined}
        onDeselectVisible={() => undefined}
        onClear={() => undefined}
      />,
    )
    expect(empty).toBe('')
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

describe('PortfolioSummaryPanel', () => {
  it('renders core portfolio signals and unknown maintenance separately', () => {
    const html = renderToString(
      <PortfolioSummaryPanel summary={summarizePortfolio([repository])} totalPortfolioCount={200} />,
    )
    expect(html).toContain('Portfolio signals')
    expect(html).toContain('Missing LICENSE')
    expect(html).toContain('Missing Actions')
    expect(html).toContain('Missing release')
    expect(html).toContain('Java repositories')
    expect(html).toContain('Archived')
    expect(html).toContain('Forks')
    expect(html).toContain('Summary for 1 filtered repositories out of 200')

    const incomplete = {
      ...repository,
      license: { analysisState: 'FAILED', presence: 'UNKNOWN', recognized: null, key: null, name: null },
    } satisfies RepositorySummary
    expect(renderToString(
      <PortfolioSummaryPanel summary={summarizePortfolio([incomplete])} totalPortfolioCount={1} />,
    )).toContain('1 unknown')
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

    for (const value of [
      'Repository details','erland/roman-nollpunkten','Repository overview','Repository metadata',
      'Maintenance analysis','Default branch','Last push','Last update','publishing','MIT License',
      '3 workflows','v1.2.0','Repository analysis','Open on GitHub','Close details','Compliance detail',
      'License required','Repository does not contain a license.','MISSING','Last evaluated','Accept deviation',
    ]) expect(html).toContain(value)
    expect(html).toContain('role="dialog"')
    expect(html).toContain('aria-modal="true"')
  })

  it('shows refresh outcome separately from usable repository analysis', () => {
    const degraded = {
      ...repository,
      refreshStatus: {
        state: 'COMPLETE',
        message: 'Cached metadata retained after transient failure',
        freshness: 'STALE',
        latestOutcome: 'DEGRADED',
      },
    } satisfies RepositorySummary

    const html = renderToString(
      <RepositoryDetailPanel repository={degraded} onClose={() => undefined} />,
    )

    expect(html).toContain('Repository analysis')
    expect(html).toContain('Complete')
    expect(html).toContain('Data freshness')
    expect(html).toContain('STALE')
    expect(html).toContain('Latest refresh')
    expect(html).toContain('DEGRADED')
  })

  it('shows recent repository activity as relative time', () => {
    const now = Date.now()
    const recent = {
      ...repository,
      activity: {
        pushedAt: new Date(now - 3 * 60 * 60 * 1000).toISOString(),
        updatedAt: new Date(now - 4 * 24 * 60 * 60 * 1000).toISOString(),
      },
    } satisfies RepositorySummary

    const html = renderToString(<RepositoryDetailPanel repository={recent} onClose={() => undefined} />)
    expect(html).toContain('3 hours ago')
    expect(html).toContain('4 days ago')
  })

  it('renders accepted deviation controls and unknown analysis explicitly', () => {
    const accepted = renderToString(
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
    expect(accepted).toContain('ACCEPTED DEVIATION')
    expect(accepted).toContain('Edit exception')
    expect(accepted).toContain('Expire exception')
    expect(accepted).toContain('Remove exception')

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
    const unknown = renderToString(<RepositoryDetailPanel repository={incomplete} onClose={() => undefined} />)
    expect((unknown.match(/Unknown/g) ?? []).length).toBeGreaterThanOrEqual(4)
    expect(unknown).toContain('Failed')
  })

  it('renders nothing without a selected repository and stays programmatically focusable when opened', () => {
    expect(renderToString(<RepositoryDetailPanel repository={null} onClose={() => undefined} />)).toBe('')
    const html = renderToString(<RepositoryDetailPanel repository={repository} onClose={() => undefined} />)
    expect(html).toContain('tabindex="-1"')
    expect(html).toContain('aria-labelledby="repository-detail-heading"')
    expect(html).toContain('aria-describedby="repository-detail-description"')
    expect(html).toContain('aria-label="Close details for erland/roman-nollpunkten"')
  })
})

describe('SavedViewsPanel', () => {
  it('renders saved views, accessibility metadata and browser-storage guidance', () => {
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

    for (const value of [
      'Quick categories','All repositories','Java missing LICENSE','1 saved view','Manage views',
      'Save current view','Delete','aria-pressed="true"','Repository selection is not included',
      'aria-label="Repository views"','aria-label="Delete saved view Java missing LICENSE"',
      'aria-describedby="saved-views-description"',
    ]) expect(html).toContain(value)
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
