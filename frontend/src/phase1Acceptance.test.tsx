import { renderToString } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import App from './App'
import { InventoryRefreshPanel } from './InventoryRefreshPanel'
import { RepositoryDetailPanel } from './RepositoryDetailPanel'
import { RepositoryInventory } from './RepositoryInventory'
import type { InventoryStatus } from './api'
import { phase1Repositories, PHASE_1_NOW } from './phase1AcceptanceFixtures'
import { emptyRepositoryFilters, filterRepositories } from './repositoryFilters'
import {
  selectVisibleRepositories,
  toggleRepositorySelection,
} from './repositorySelection'
import { sortRepositories } from './repositorySorting'
import {
  createSavedView,
  loadSavedViews,
  persistSavedViews,
  type SavedViewsStorage,
} from './savedViews'

class MemoryStorage implements SavedViewsStorage {
  private values = new Map<string, string>()
  getItem(key: string): string | null { return this.values.get(key) ?? null }
  setItem(key: string, value: string): void { this.values.set(key, value) }
}

const completedRefresh: InventoryStatus = {
  state: 'COMPLETED',
  lastAttemptAt: '2026-08-14T11:59:00Z',
  lastSuccessfulRefreshAt: '2026-08-14T12:00:00Z',
  completedAt: '2026-08-14T12:00:00Z',
  errorMessage: null,
  repositoryCount: phase1Repositories.length,
  totalCount: phase1Repositories.length,
  processedCount: phase1Repositories.length,
  successfulCount: phase1Repositories.length,
  errorCount: 0,
  currentRepository: null,
  running: false,
}

describe('Phase 1 end-to-end acceptance validation', () => {
  it('1. application loads and exposes the repository inventory shell', () => {
    const appHtml = renderToString(<App />)
    const inventoryHtml = renderToString(
      <RepositoryInventory repositories={phase1Repositories} loading={false} error={null} />,
    )

    expect(appHtml).toContain('RepoFleet')
    expect(appHtml).toContain('Repository portfolio management')
    expect(inventoryHtml).toContain('roman-alpha')
    expect(inventoryHtml).toContain('tool-java')
  })

  it('2-3. repository inventory is available and refresh success is understandable', () => {
    const html = renderToString(
      <InventoryRefreshPanel
        status={completedRefresh}
        statusError={null}
        refreshing={false}
        onRefresh={() => undefined}
      />,
    )

    expect(phase1Repositories).toHaveLength(4)
    expect(html).toContain('Refresh complete')
    expect(html).toContain('4 repositories are up to date')
  })

  it('4. reproduces the name-prefix use case', () => {
    const result = filterRepositories(
      phase1Repositories,
      { ...emptyRepositoryFilters, namePrefix: 'roman-' },
      PHASE_1_NOW,
    )
    expect(result.map((repository) => repository.name)).toEqual(['roman-alpha', 'roman-beta'])
  })

  it('5. reproduces the topic filter use case', () => {
    const result = filterRepositories(
      phase1Repositories,
      { ...emptyRepositoryFilters, topic: 'novel', topicPresence: 'PRESENT' },
      PHASE_1_NOW,
    )
    expect(result.map((repository) => repository.name)).toEqual(['roman-alpha', 'roman-beta'])
  })

  it('6. identifies repositories containing Java', () => {
    const result = filterRepositories(
      phase1Repositories,
      { ...emptyRepositoryFilters, language: 'Java', languagePresence: 'PRESENT' },
      PHASE_1_NOW,
    )
    expect(result.map((repository) => repository.name)).toEqual(['roman-beta', 'tool-java'])
  })

  it('7-9. identifies known missing LICENSE, Actions and official release independently', () => {
    expect(filterRepositories(
      phase1Repositories,
      { ...emptyRepositoryFilters, license: 'MISSING' },
      PHASE_1_NOW,
    ).map((repository) => repository.name)).toEqual(['roman-beta', 'legacy-archive'])

    expect(filterRepositories(
      phase1Repositories,
      { ...emptyRepositoryFilters, actions: 'MISSING' },
      PHASE_1_NOW,
    ).map((repository) => repository.name)).toEqual(['roman-beta', 'legacy-archive'])

    expect(filterRepositories(
      phase1Repositories,
      { ...emptyRepositoryFilters, release: 'MISSING' },
      PHASE_1_NOW,
    ).map((repository) => repository.name)).toEqual(['roman-beta', 'tool-java'])
  })

  it('10. combines filters with AND semantics and sorts the result deterministically', () => {
    const filtered = filterRepositories(
      phase1Repositories,
      {
        ...emptyRepositoryFilters,
        namePrefix: 'roman-',
        topic: 'novel',
        topicPresence: 'PRESENT',
        language: 'Java',
        languagePresence: 'PRESENT',
        license: 'MISSING',
        actions: 'MISSING',
        release: 'MISSING',
      },
      PHASE_1_NOW,
    )

    const result = sortRepositories(filtered, { field: 'name', direction: 'ASC' })
    expect(result.map((repository) => repository.name)).toEqual(['roman-beta'])
  })

  it('11. builds a persistent explicit selection from filtered results', () => {
    const visible = filterRepositories(
      phase1Repositories,
      { ...emptyRepositoryFilters, namePrefix: 'roman-' },
      PHASE_1_NOW,
    )

    let selection = selectVisibleRepositories(new Set<number>(), visible)
    selection = toggleRepositorySelection(selection, phase1Repositories[2].id)

    expect([...selection].sort((a, b) => a - b)).toEqual([101, 102, 103])

    const narrowerVisible = filterRepositories(
      phase1Repositories,
      { ...emptyRepositoryFilters, nameContains: 'beta' },
      PHASE_1_NOW,
    )
    expect(selection.has(narrowerVisible[0].id)).toBe(true)
    expect(selection.has(101)).toBe(true)
  })

  it('12. saves and reloads a named filter/sort view deterministically', () => {
    const storage = new MemoryStorage()
    const view = createSavedView(
      'Java repos missing LICENSE',
      { ...emptyRepositoryFilters, language: 'Java', languagePresence: 'PRESENT', license: 'MISSING' },
      { field: 'activity', direction: 'DESC' },
      'phase-1-view',
    )

    expect(persistSavedViews(storage, [view])).toBe(true)
    const loaded = loadSavedViews(storage)

    expect(loaded).toEqual([view])
    const result = sortRepositories(
      filterRepositories(phase1Repositories, loaded[0].filters, PHASE_1_NOW),
      loaded[0].sort,
    )
    expect(result.map((repository) => repository.name)).toEqual(['roman-beta'])
  })

  it('13. renders repository details from the same deterministic inventory item', () => {
    const repository = phase1Repositories[1]
    const html = renderToString(
      <RepositoryDetailPanel repository={repository} onClose={() => undefined} />,
    )

    expect(html).toContain('erland/roman-beta')
    expect(html).toContain('Default branch')
    expect(html).toContain('Java')
    expect(html).toContain('Missing')
    expect(html).toContain('No workflows')
    expect(html).toContain('No published release')
  })
})
