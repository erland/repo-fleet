import { useCallback, useEffect, useState } from 'react'
import type { RepositoryFilters } from './repositoryFilters'
import type { RepositorySort } from './repositorySorting'
import {
  createSavedView,
  loadSavedViews,
  persistSavedViews,
  removeSavedView,
  type SavedRepositoryView,
} from './savedViews'

export function useSavedRepositoryViews() {
  const [views, setViews] = useState<SavedRepositoryView[]>([])
  const [activeViewId, setActiveViewId] = useState<string | null>(null)
  const [initialized, setInitialized] = useState(false)
  const [storageAvailable, setStorageAvailable] = useState(true)

  useEffect(() => {
    try {
      setViews(loadSavedViews(window.localStorage))
    } catch {
      setStorageAvailable(false)
    } finally {
      setInitialized(true)
    }
  }, [])

  useEffect(() => {
    if (!initialized) return

    const persisted = persistSavedViews(
      storageAvailable ? window.localStorage : null,
      views,
    )
    if (storageAvailable && !persisted) {
      setStorageAvailable(false)
    }
  }, [initialized, storageAvailable, views])

  const saveView = useCallback((
    name: string,
    filters: RepositoryFilters,
    sort: RepositorySort,
  ) => {
    setViews((current) => [
      ...current,
      createSavedView(
        name,
        filters,
        sort,
        globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${current.length}`,
      ),
    ])
  }, [])

  const activateView = useCallback((viewId: string) => {
    const view = views.find((candidate) => candidate.id === viewId)
    if (!view) return null

    setActiveViewId(viewId)
    return view
  }, [views])

  const clearActiveView = useCallback(() => {
    setActiveViewId(null)
  }, [])

  const deleteView = useCallback((viewId: string) => {
    setViews((current) => removeSavedView(current, viewId))
    setActiveViewId((current) => current === viewId ? null : current)
  }, [])

  return {
    views,
    activeViewId,
    storageAvailable,
    saveView,
    activateView,
    clearActiveView,
    deleteView,
  }
}
