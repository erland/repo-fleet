import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  fetchAuthSession,
  fetchInventoryStatus,
  fetchRepositories,
  logout,
  startInventoryRefresh,
  type AuthSession,
  type InventoryStatus,
  type RepositorySummary,
} from './api'
import { InventoryRefreshPanel } from './InventoryRefreshPanel'
import { PortfolioSummaryPanel } from './PortfolioSummaryPanel'
import { RepositoryDetailPanel } from './RepositoryDetailPanel'
import { RepositoryFiltersPanel } from './RepositoryFiltersPanel'
import { RepositoryInventory } from './RepositoryInventory'
import { RepositorySelectionBar } from './RepositorySelectionBar'
import { RepositorySortControls } from './RepositorySortControls'
import { SavedViewsPanel } from './SavedViewsPanel'
import { emptyRepositoryFilters, filterRepositories } from './repositoryFilters'
import { defaultRepositorySort, sortRepositories } from './repositorySorting'
import { clearRepositorySelection, deselectVisibleRepositories, selectVisibleRepositories, toggleRepositorySelection } from './repositorySelection'
import { summarizePortfolio } from './portfolioSummary'
import { createSavedView, loadSavedViews, persistSavedViews, removeSavedView, type SavedRepositoryView } from './savedViews'

const REFRESH_POLL_INTERVAL_MS = 1000

export default function App() {
  const [authSession, setAuthSession] = useState<AuthSession | null>(null)
  const [authError, setAuthError] = useState<string | null>(null)
  const [repositories, setRepositories] = useState<RepositorySummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [inventoryStatus, setInventoryStatus] = useState<InventoryStatus | null>(null)
  const [statusError, setStatusError] = useState<string | null>(null)
  const [refreshing, setRefreshing] = useState(false)
  const [filters, setFilters] = useState(emptyRepositoryFilters)
  const [sort, setSort] = useState(defaultRepositorySort)
  const [selectedRepositoryIds, setSelectedRepositoryIds] = useState<Set<number>>(new Set())
  const [detailRepositoryId, setDetailRepositoryId] = useState<number | null>(null)
  const [savedViews, setSavedViews] = useState<SavedRepositoryView[]>([])
  const [savedViewsInitialized, setSavedViewsInitialized] = useState(false)
  const [savedViewsStorageAvailable, setSavedViewsStorageAvailable] = useState(true)
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
    void fetchAuthSession()
      .then((session) => {
        if (!mountedRef.current) return
        setAuthSession(session)
        setAuthError(null)
      })
      .catch(() => {
        if (!mountedRef.current) return
        setAuthError('Authentication status could not be loaded.')
      })

    return () => { mountedRef.current = false }
  }, [])

  useEffect(() => {
    try {
      setSavedViews(loadSavedViews(window.localStorage))
    } catch {
      setSavedViewsStorageAvailable(false)
    } finally {
      setSavedViewsInitialized(true)
    }
  }, [])

  useEffect(() => {
    if (!savedViewsInitialized) return

    const persisted = persistSavedViews(
      savedViewsStorageAvailable ? window.localStorage : null,
      savedViews,
    )
    if (savedViewsStorageAvailable && !persisted) {
      setSavedViewsStorageAvailable(false)
    }
  }, [savedViews, savedViewsInitialized, savedViewsStorageAvailable])

  useEffect(() => {
    if (!authSession || (authSession.authEnabled && !authSession.authenticated)) return
    mountedRef.current = true
    void loadRepositories(true)
    void loadStatus()

    return () => {
      mountedRef.current = false
    }
  }, [authSession, loadRepositories, loadStatus])

  useEffect(() => {
    if (inventoryStatus?.state !== 'RUNNING') return

    setRefreshing(true)
    const timer = window.setInterval(async () => {
      const nextStatus = await loadStatus()
      if (!nextStatus) return

      await loadRepositories(false)
      if (nextStatus.state === 'RUNNING') return

      window.clearInterval(timer)
      if (mountedRef.current) setRefreshing(false)
    }, REFRESH_POLL_INTERVAL_MS)

    return () => window.clearInterval(timer)
  }, [inventoryStatus?.state, loadRepositories, loadStatus])


  const filteredRepositories = useMemo(
    () => filterRepositories(repositories, filters),
    [repositories, filters],
  )

  const sortedRepositories = useMemo(
    () => sortRepositories(filteredRepositories, sort),
    [filteredRepositories, sort],
  )


  const detailRepository = useMemo(
    () => repositories.find((repository) => repository.id === detailRepositoryId) ?? null,
    [repositories, detailRepositoryId],
  )

  const portfolioSummary = useMemo(
    () => summarizePortfolio(filteredRepositories),
    [filteredRepositories],
  )



  const saveCurrentView = useCallback((name: string) => {
    setSavedViews((current) => [
      ...current,
      createSavedView(
        name,
        filters,
        sort,
        globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${current.length}`,
      ),
    ])
  }, [filters, sort])

  const loadSavedView = useCallback((viewId: string) => {
    const view = savedViews.find((candidate) => candidate.id === viewId)
    if (!view) return

    setFilters({ ...view.filters })
    setSort({ ...view.sort })
  }, [savedViews])

  const deleteSavedView = useCallback((viewId: string) => {
    setSavedViews((current) => removeSavedView(current, viewId))
  }, [])

  const openRepositoryDetails = useCallback((repositoryId: number) => {
    setDetailRepositoryId(repositoryId)
  }, [])

  const closeRepositoryDetails = useCallback(() => {
    setDetailRepositoryId(null)
  }, [])

  const toggleRepository = useCallback((repositoryId: number) => {
    setSelectedRepositoryIds((current) => toggleRepositorySelection(current, repositoryId))
  }, [])

  const selectVisible = useCallback(() => {
    setSelectedRepositoryIds((current) => selectVisibleRepositories(current, sortedRepositories))
  }, [sortedRepositories])

  const deselectVisible = useCallback(() => {
    setSelectedRepositoryIds((current) => deselectVisibleRepositories(current, sortedRepositories))
  }, [sortedRepositories])

  const clearSelection = useCallback(() => {
    setSelectedRepositoryIds(clearRepositorySelection())
  }, [])

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

  const signOut = useCallback(async () => {
    try {
      await logout()
    } finally {
      window.location.assign('/')
    }
  }, [])

  const loginError = typeof window !== 'undefined'
    ? new URLSearchParams(window.location.search).get('auth_error')
    : null

  if (authError) {
    return (
      <main className="auth-shell">
        <section className="auth-card" role="alert">
          <p className="eyebrow">Repository portfolio management</p>
          <h1>RepoFleet</h1>
          <p>{authError}</p>
          <button type="button" onClick={() => window.location.reload()}>Try again</button>
        </section>
      </main>
    )
  }

  if (!authSession) {
    return (
      <main className="auth-shell">
        <section className="auth-card">
          <p className="eyebrow">Repository portfolio management</p>
          <h1>RepoFleet</h1>
          <p>Checking authentication…</p>
        </section>
      </main>
    )
  }

  if (authSession.authEnabled && !authSession.authenticated) {
    return (
      <main className="auth-shell">
        <section className="auth-card">
          <p className="eyebrow">Repository portfolio management</p>
          <h1>RepoFleet</h1>
          <p>Sign in with an authorized GitHub account to access the repository portfolio.</p>
          {loginError === 'not_allowed' && <p className="auth-error" role="alert">This GitHub account is not allowed to use RepoFleet.</p>}
          {loginError && loginError !== 'not_allowed' && <p className="auth-error" role="alert">GitHub sign-in did not complete. Please try again.</p>}
          <a className="primary-action" href="/api/auth/login">Sign in with GitHub</a>
        </section>
      </main>
    )
  }

  return (
    <>
      <a className="skip-link" href="#main-content">Skip to repository inventory</a>
      <main className="app-shell" id="main-content" tabIndex={-1}>
      <header className="app-header">
        <div>
          <p className="eyebrow">Repository portfolio management</p>
          <h1>RepoFleet</h1>
          <p className="intro">Analytics and maintenance for a large GitHub repository portfolio.</p>
        </div>
        {authSession.authenticated && authSession.user && (
          <div className="auth-user">
            {authSession.user.avatarUrl && <img src={authSession.user.avatarUrl} alt="" width="32" height="32" />}
            <span>{authSession.user.login}</span>
            <button type="button" onClick={() => void signOut()}>Sign out</button>
          </div>
        )}
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

      <SavedViewsPanel
        views={savedViews}
        storageAvailable={savedViewsStorageAvailable}
        onSave={saveCurrentView}
        onLoad={loadSavedView}
        onDelete={deleteSavedView}
      />

      <PortfolioSummaryPanel
        summary={portfolioSummary}
        totalPortfolioCount={repositories.length}
      />

      <RepositorySortControls
        sort={sort}
        onChange={setSort}
        totalCount={repositories.length}
        filteredCount={filteredRepositories.length}
      />

      <RepositoryDetailPanel
        repository={detailRepository}
        onClose={closeRepositoryDetails}
      />

      <RepositorySelectionBar
        selection={selectedRepositoryIds}
        visibleRepositories={sortedRepositories}
        onSelectVisible={selectVisible}
        onDeselectVisible={deselectVisible}
        onClear={clearSelection}
      />

      <RepositoryInventory
        repositories={sortedRepositories}
        loading={loading}
        error={error}
        emptyMessage={repositories.length > 0
          ? 'No repositories match the current filters.'
          : inventoryStatus?.state === 'RUNNING'
            ? 'Repository discovery is in progress…'
            : undefined}
        selectedRepositoryIds={selectedRepositoryIds}
        onToggleRepository={toggleRepository}
        onOpenDetails={openRepositoryDetails}
      />
      </main>
    </>
  )
}
