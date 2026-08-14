import { renderToString } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import App from './App'
import { InventoryRefreshPanel } from './InventoryRefreshPanel'
import { RepositoryFiltersPanel } from './RepositoryFiltersPanel'
import { RepositoryInventory } from './RepositoryInventory'
import type { InventoryStatus, RepositorySummary } from './api'
import { emptyRepositoryFilters } from './repositoryFilters'

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
    expect(html).toContain('Loading repository inventory')
  })
})

describe('RepositoryInventory', () => {
  it('renders a populated repository table', () => {
    const html = renderToString(<RepositoryInventory repositories={[repository]} loading={false} error={null} />)

    expect(html).toContain('roman-nollpunkten')
    expect(html).toContain('novel')
    expect(html).toContain('MIT License')
    expect(html).toContain('3 workflows')
    expect(html).toContain('v1.2.0')
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
    expect((html.match(/Unknown/g) ?? []).length).toBeGreaterThanOrEqual(3)
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

    expect(html).toContain('Last successful refresh')
    expect(html).toContain('Never')
    expect(html).toContain('Refresh repositories')
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

    expect(html).toContain('3 of 10 repositories processed')
    expect(html).toContain('30%')
    expect(html).toContain('erland/repo-fleet')
    expect(html).toContain('Existing repository data remains available')
    expect(html).toContain('Refreshing')
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

    expect(html).toContain('Refresh complete')
    expect(html).toContain('2 repositories are up to date')
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

    expect(html).toContain('Refresh completed with partial failures')
    expect(html).toContain('1 repository has incomplete or failed analysis')
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

    expect(html).toContain('Name contains')
    expect(html).toContain('Name prefix')
    expect(html).toContain('Owner')
    expect(html).toContain('Visibility')
    expect(html).toContain('Topic')
    expect(html).toContain('Language')
    expect(html).toContain('License')
    expect(html).toContain('GitHub Actions')
    expect(html).toContain('Official release')
    expect(html).toContain('Activity')
    expect(html).toContain('12 of 200 repositories match')
  })
})

