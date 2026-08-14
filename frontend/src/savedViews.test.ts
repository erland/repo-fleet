import { describe, expect, it } from 'vitest'
import { emptyRepositoryFilters } from './repositoryFilters'
import { defaultRepositorySort } from './repositorySorting'
import {
  SAVED_VIEWS_STORAGE_KEY,
  createSavedView,
  loadSavedViews,
  persistSavedViews,
  removeSavedView,
  type SavedViewsStorage,
} from './savedViews'

class MemoryStorage implements SavedViewsStorage {
  private values = new Map<string, string>()

  getItem(key: string): string | null {
    return this.values.get(key) ?? null
  }

  setItem(key: string, value: string): void {
    this.values.set(key, value)
  }
}

describe('saved views', () => {
  it('creates a named snapshot of filters and sorting', () => {
    const filters = { ...emptyRepositoryFilters, namePrefix: 'roman-' }
    const sort = { ...defaultRepositorySort, field: 'activity' as const, direction: 'DESC' as const }

    const view = createSavedView(' Recent novels ', filters, sort, 'view-1')

    expect(view.name).toBe('Recent novels')
    expect(view.filters.namePrefix).toBe('roman-')
    expect(view.sort).toEqual(sort)
  })

  it('persists and reloads saved views', () => {
    const storage = new MemoryStorage()
    const views = [createSavedView('Java', emptyRepositoryFilters, defaultRepositorySort, 'view-1')]

    expect(persistSavedViews(storage, views)).toBe(true)
    expect(storage.getItem(SAVED_VIEWS_STORAGE_KEY)).not.toBeNull()
    expect(loadSavedViews(storage)).toEqual(views)
  })

  it('handles missing or invalid browser storage content safely', () => {
    const storage = new MemoryStorage()
    expect(loadSavedViews(storage)).toEqual([])

    storage.setItem(SAVED_VIEWS_STORAGE_KEY, '{not valid json')
    expect(loadSavedViews(storage)).toEqual([])
  })

  it('handles storage failures without crashing', () => {
    const failing: SavedViewsStorage = {
      getItem: () => { throw new Error('blocked') },
      setItem: () => { throw new Error('blocked') },
    }

    expect(loadSavedViews(failing)).toEqual([])
    expect(persistSavedViews(failing, [])).toBe(false)
  })

  it('removes only the requested saved view', () => {
    const views = [
      createSavedView('One', emptyRepositoryFilters, defaultRepositorySort, '1'),
      createSavedView('Two', emptyRepositoryFilters, defaultRepositorySort, '2'),
    ]

    expect(removeSavedView(views, '1').map((view) => view.name)).toEqual(['Two'])
  })
})
