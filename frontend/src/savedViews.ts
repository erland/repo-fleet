import type { RepositoryFilters } from './repositoryFilters'
import type { RepositorySort } from './repositorySorting'

export const SAVED_VIEWS_STORAGE_KEY = 'repofleet.savedViews.v1'

export type SavedRepositoryView = {
  id: string
  name: string
  filters: RepositoryFilters
  sort: RepositorySort
}

export interface SavedViewsStorage {
  getItem(key: string): string | null
  setItem(key: string, value: string): void
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function isSavedRepositoryView(value: unknown): value is SavedRepositoryView {
  if (!isObject(value)) return false
  return typeof value.id === 'string'
    && typeof value.name === 'string'
    && isObject(value.filters)
    && isObject(value.sort)
}

export function loadSavedViews(storage: SavedViewsStorage | null): SavedRepositoryView[] {
  if (!storage) return []

  try {
    const raw = storage.getItem(SAVED_VIEWS_STORAGE_KEY)
    if (!raw) return []

    const parsed: unknown = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []

    return parsed.filter(isSavedRepositoryView)
  } catch {
    return []
  }
}

export function persistSavedViews(
  storage: SavedViewsStorage | null,
  views: SavedRepositoryView[],
): boolean {
  if (!storage) return false

  try {
    storage.setItem(SAVED_VIEWS_STORAGE_KEY, JSON.stringify(views))
    return true
  } catch {
    return false
  }
}

export function createSavedView(
  name: string,
  filters: RepositoryFilters,
  sort: RepositorySort,
  id: string,
): SavedRepositoryView {
  return {
    id,
    name: name.trim(),
    filters: { ...filters },
    sort: { ...sort },
  }
}

export function removeSavedView(
  views: SavedRepositoryView[],
  viewId: string,
): SavedRepositoryView[] {
  return views.filter((view) => view.id !== viewId)
}
