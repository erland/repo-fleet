import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  fetchInventoryStatus,
  fetchRepositories,
  startInventoryRefresh,
  type InventoryStatus,
  type RepositorySummary,
} from './api'
import { InventoryRefreshPanel } from './InventoryRefreshPanel'
import { RepositoryFiltersPanel } from './RepositoryFiltersPanel'
import { RepositoryInventory } from './RepositoryInventory'
import { emptyRepositoryFilters, filterRepositories } from './repositoryFilters'

const REFRESH_POLL_INTERVAL_MS = 1000

export default function App() {
  const [repositories, setRepositories] = useState<RepositorySummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [inventoryStatus, setInventoryStatus] = useState<InventoryStatus | null>(null)
  const [statusError, setStatusError] = useState<string | null>(null)
  const [refreshing, setRefreshing] = useState(false)
  const [filters, setFilters] = useState(emptyRepositoryFilters)
  const mountedRef = useRef(true)

  const loadRepositories = useCallback(async (showInitialLoading = false) => {
    if (showInitialLoading) setLoading(true)

    try {
      const result = await fetchRepositories()
      if (!mountedRef.current) return
      setRepositories(result)
      setError(null)
    } catch {
      if (!mountedRef.current) return
      setError('Repository inventory could not be loaded from the backend.')
    } finally {
      if (mountedRef.current && showInitialLoading) setLoading(false)
    }
  }, [])

  const loadStatus = useCallback(async () => {
    try {
      const result = await fetchInventoryStatus()
      if (!mountedRef.current) return null
      setInventoryStatus(result)
      setStatusError(null)
      return result
    } catch {
      if (!mountedRef.current) return null
      setStatusError('Inventory refresh status could not be loaded from the backend.')
      return null
    }
  }, [])

  useEffect(() => {
    mountedRef.current = true
    void loadRepositories(true)
    void loadStatus()

    return () => {
      mountedRef.current = false
    }
  }, [loadRepositories, loadStatus])

  useEffect(() => {
    if (inventoryStatus?.state !== 'RUNNING') return

    setRefreshing(true)
    const timer = window.setInterval(async () => {
      const nextStatus = await loadStatus()
      if (!nextStatus || nextStatus.state === 'RUNNING') return

      window.clearInterval(timer)
      if (mountedRef.current) setRefreshing(false)
      await loadRepositories(false)
    }, REFRESH_POLL_INTERVAL_MS)

    return () => window.clearInterval(timer)
  }, [inventoryStatus?.state, loadRepositories, loadStatus])


  const filteredRepositories = useMemo(
    () => filterRepositories(repositories, filters),
    [repositories, filters],
  )

  const refreshRepositories = useCallback(async () => {
    if (refreshing || inventoryStatus?.state === 'RUNNING') return

    setRefreshing(true)
    setStatusError(null)

    try {
      const started = await startInventoryRefresh()
      if (!mountedRef.current) return
      setInventoryStatus(started)

      if (started.state !== 'RUNNING') {
        setRefreshing(false)
        await loadRepositories(false)
      }
    } catch {
      if (!mountedRef.current) return
      setRefreshing(false)
      setStatusError('Repository refresh could not be started.')
    }
  }, [inventoryStatus?.state, loadRepositories, refreshing])

  return (
    <main className="app-shell">
      <header className="app-header">
        <div>
          <p className="eyebrow">Repository portfolio management</p>
          <h1>RepoFleet</h1>
          <p className="intro">Analytics and maintenance for a large GitHub repository portfolio.</p>
        </div>
      </header>

      <InventoryRefreshPanel
        status={inventoryStatus}
        statusError={statusError}
        refreshing={refreshing}
        onRefresh={refreshRepositories}
      />

      <RepositoryFiltersPanel
        filters={filters}
        onChange={setFilters}
        totalCount={repositories.length}
        filteredCount={filteredRepositories.length}
      />

      <RepositoryInventory
        repositories={filteredRepositories}
        loading={loading}
        error={error}
        emptyMessage={repositories.length > 0 ? 'No repositories match the current filters.' : undefined}
      />
    </main>
  )
}
