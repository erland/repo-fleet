import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  fetchAuthSession,
  fetchInventoryStatus,
  fetchRepositories,
  fetchComplianceSummary,
  fetchRepositoryCompliance,
  fetchRefreshDiagnostics,
  saveRepositoryComplianceException,
  expireRepositoryComplianceException,
  removeRepositoryComplianceException,
  logout,
  startInventoryRefresh,
  startFullInventoryRefresh,
  type AuthSession,
  type InventoryStatus,
  type RepositorySummary,
  type CompliancePortfolioSummary,
  type RepositoryComplianceDetail,
  type RefreshDiagnosticsSnapshot,
} from './api'
import { InventoryRefreshPanel } from './InventoryRefreshPanel'
import { ComplianceOverviewPanel } from './ComplianceOverviewPanel'
import { PortfolioSummaryPanel } from './PortfolioSummaryPanel'
import { RepositoryDetailPanel } from './RepositoryDetailPanel'
import { RepositoryInventory } from './RepositoryInventory'
import { RepositorySelectionBar } from './RepositorySelectionBar'
import { RefreshDiagnosticsPanel } from './RefreshDiagnosticsPanel'
import { SavedViewsPanel } from './SavedViewsPanel'
import { RepositoryLauncherToolbar } from './RepositoryLauncherToolbar'
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
  const [complianceSummary, setComplianceSummary] = useState<CompliancePortfolioSummary | null>(null)
  const [complianceLoading, setComplianceLoading] = useState(false)
  const [complianceError, setComplianceError] = useState<string | null>(null)
  const [refreshDiagnostics, setRefreshDiagnostics] = useState<RefreshDiagnosticsSnapshot | null>(null)
  const [refreshDiagnosticsLoading, setRefreshDiagnosticsLoading] = useState(false)
  const [refreshDiagnosticsError, setRefreshDiagnosticsError] = useState<string | null>(null)
  const [filters, setFilters] = useState(emptyRepositoryFilters)
  const [sort, setSort] = useState(defaultRepositorySort)
  const [selectedRepositoryIds, setSelectedRepositoryIds] = useState<Set<number>>(new Set())
  const [detailRepositoryId, setDetailRepositoryId] = useState<number | null>(null)
  const [detailCompliance, setDetailCompliance] = useState<RepositoryComplianceDetail[]>([])
  const [detailComplianceLoading, setDetailComplianceLoading] = useState(false)
  const [detailComplianceError, setDetailComplianceError] = useState<string | null>(null)
  const [workspaceView, setWorkspaceView] = useState<'repositories' | 'insights'>('repositories')
  const [savedViews, setSavedViews] = useState<SavedRepositoryView[]>([])
  const [activeSavedViewId, setActiveSavedViewId] = useState<string | null>(null)
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

  const loadCompliance = useCallback(async () => {
    setComplianceLoading(true)
    try {
      const result = await fetchComplianceSummary()
      if (!mountedRef.current) return
      setComplianceSummary(result)
      setComplianceError(null)
    } catch {
      if (!mountedRef.current) return
      setComplianceError('Compliance summary could not be loaded from the backend.')
    } finally {
      if (mountedRef.current) setComplianceLoading(false)
    }
  }, [])

  const loadRefreshDiagnostics = useCallback(async () => {
    setRefreshDiagnosticsLoading(true)
    try {
      const result = await fetchRefreshDiagnostics()
      if (!mountedRef.current) return
      setRefreshDiagnostics(result)
      setRefreshDiagnosticsError(null)
    } catch {
      if (!mountedRef.current) return
      setRefreshDiagnosticsError('Refresh diagnostics could not be loaded from the backend.')
    } finally {
      if (mountedRef.current) setRefreshDiagnosticsLoading(false)
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
    void loadCompliance()
    void loadRefreshDiagnostics()

    return () => {
      mountedRef.current = false
    }
  }, [authSession, loadCompliance, loadRefreshDiagnostics, loadRepositories, loadStatus])

  useEffect(() => {
    if (inventoryStatus?.state !== 'RUNNING') return

    setRefreshing(true)
    const timer = window.setInterval(async () => {
      const [nextStatus] = await Promise.all([
        loadStatus(),
        loadRefreshDiagnostics(),
      ])
      if (!nextStatus) return

      await loadRepositories(false)
      if (nextStatus.state === 'RUNNING') return
      await Promise.all([
        loadCompliance(),
        loadRefreshDiagnostics(),
      ])

      window.clearInterval(timer)
      if (mountedRef.current) setRefreshing(false)
    }, REFRESH_POLL_INTERVAL_MS)

    return () => window.clearInterval(timer)
  }, [inventoryStatus?.state, loadCompliance, loadRefreshDiagnostics, loadRepositories, loadStatus])


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
    setActiveSavedViewId(viewId)
  }, [savedViews])

  const showAllRepositories = useCallback(() => {
    setFilters(emptyRepositoryFilters)
    setSort(defaultRepositorySort)
    setActiveSavedViewId(null)
  }, [])

  const changeFilters = useCallback((nextFilters: typeof filters) => {
    setFilters(nextFilters)
    setActiveSavedViewId(null)
  }, [])

  const changeSort = useCallback((nextSort: typeof sort) => {
    setSort(nextSort)
    setActiveSavedViewId(null)
  }, [])

  const deleteSavedView = useCallback((viewId: string) => {
    setSavedViews((current) => removeSavedView(current, viewId))
    setActiveSavedViewId((current) => current === viewId ? null : current)
  }, [])

  const reloadRepositoryCompliance = useCallback(async (repositoryId: number) => {
    setDetailComplianceLoading(true)
    try {
      const result = await fetchRepositoryCompliance(repositoryId)
      if (!mountedRef.current) return
      setDetailCompliance(result)
      setDetailComplianceError(null)
    } catch {
      if (!mountedRef.current) return
      setDetailComplianceError('Repository compliance detail could not be loaded.')
    } finally {
      if (mountedRef.current) setDetailComplianceLoading(false)
    }
  }, [])

  const openRepositoryDetails = useCallback((repositoryId: number) => {
    setDetailRepositoryId(repositoryId)
    setDetailCompliance([])
    setDetailComplianceError(null)
    void reloadRepositoryCompliance(repositoryId)
  }, [reloadRepositoryCompliance])

  const saveComplianceException = useCallback(async (
    repositoryId: number,
    ruleKey: string,
    reason: string,
    expiresAt: string | null,
  ) => {
    await saveRepositoryComplianceException(repositoryId, ruleKey, reason, expiresAt)
    await Promise.all([
      reloadRepositoryCompliance(repositoryId),
      loadCompliance(),
    ])
  }, [loadCompliance, reloadRepositoryCompliance])

  const expireComplianceException = useCallback(async (
    repositoryId: number,
    ruleKey: string,
  ) => {
    await expireRepositoryComplianceException(repositoryId, ruleKey)
    await Promise.all([
      reloadRepositoryCompliance(repositoryId),
      loadCompliance(),
    ])
  }, [loadCompliance, reloadRepositoryCompliance])

  const removeComplianceException = useCallback(async (
    repositoryId: number,
    ruleKey: string,
  ) => {
    await removeRepositoryComplianceException(repositoryId, ruleKey)
    await Promise.all([
      reloadRepositoryCompliance(repositoryId),
      loadCompliance(),
    ])
  }, [loadCompliance, reloadRepositoryCompliance])

  const closeRepositoryDetails = useCallback(() => {
    setDetailRepositoryId(null)
    setDetailCompliance([])
    setDetailComplianceError(null)
    setDetailComplianceLoading(false)
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

  const fullRefreshRepositories = useCallback(async () => {
    if (refreshing || inventoryStatus?.state === 'RUNNING') return

    setRefreshing(true)
    setStatusError(null)

    try {
      const started = await startFullInventoryRefresh()
      if (!mountedRef.current) return
      setInventoryStatus(started)

      if (started.state !== 'RUNNING') {
        setRefreshing(false)
        await loadRepositories(false)
      }
    } catch {
      if (!mountedRef.current) return
      setRefreshing(false)
      setStatusError('Full repository refresh could not be started.')
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
      <a className="skip-link" href="#main-content">Skip to main content</a>
      <main className="app-shell" id="main-content" tabIndex={-1}>
      <header className="app-header">
        <div>
          <p className="eyebrow">Repository portfolio management</p>
          <h1>RepoFleet</h1>
          <p className="intro">Find repositories quickly, then open one to inspect its maintenance and compliance details.</p>
        </div>
        {authSession.authenticated && authSession.user && (
          <div className="auth-user">
            {authSession.user.avatarUrl && <img src={authSession.user.avatarUrl} alt="" width="32" height="32" />}
            <span>{authSession.user.login}</span>
            <button type="button" onClick={() => void signOut()}>Sign out</button>
          </div>
        )}
      </header>

      <nav className="workspace-navigation" aria-label="RepoFleet sections">
        <button
          type="button"
          className={workspaceView === 'repositories' ? 'workspace-tab workspace-tab-active' : 'workspace-tab'}
          aria-current={workspaceView === 'repositories' ? 'page' : undefined}
          aria-controls="repositories-workspace"
          onClick={() => setWorkspaceView('repositories')}
        >
          Repositories
        </button>
        <button
          type="button"
          className={workspaceView === 'insights' ? 'workspace-tab workspace-tab-active' : 'workspace-tab'}
          aria-current={workspaceView === 'insights' ? 'page' : undefined}
          aria-controls="insights-workspace"
          onClick={() => setWorkspaceView('insights')}
        >
          Insights & diagnostics
        </button>
      </nav>

      {workspaceView === 'repositories' ? (
        <section id="repositories-workspace" aria-label="Repository workspace">
          <section className="repository-finder" aria-label="Repository finder">
            <RepositoryLauncherToolbar
              filters={filters}
              onFiltersChange={changeFilters}
              views={savedViews}
              activeViewId={activeSavedViewId}
              onShowAll={showAllRepositories}
              onLoadView={loadSavedView}
              sort={sort}
              onSortChange={changeSort}
              totalCount={repositories.length}
              filteredCount={filteredRepositories.length}
            />
          </section>

          <RepositoryDetailPanel
            repository={detailRepository}
            compliance={detailCompliance}
            complianceLoading={detailComplianceLoading}
            complianceError={detailComplianceError}
            onSaveException={saveComplianceException}
            onExpireException={expireComplianceException}
            onRemoveException={removeComplianceException}
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

          <SavedViewsPanel
            views={savedViews}
            activeViewId={activeSavedViewId}
            storageAvailable={savedViewsStorageAvailable}
            onShowAll={showAllRepositories}
            onSave={saveCurrentView}
            onLoad={loadSavedView}
            onDelete={deleteSavedView}
            managementOnly
          />

          <InventoryRefreshPanel
            status={inventoryStatus}
            statusError={statusError}
            refreshing={refreshing}
            diagnostics={refreshDiagnostics}
            onRefresh={refreshRepositories}
            onFullRefresh={fullRefreshRepositories}
          />
        </section>
      ) : (
        <section id="insights-workspace" className="insights-section" aria-labelledby="insights-heading">
          <div className="insights-heading">
            <p className="eyebrow">Secondary workspace</p>
            <h2 id="insights-heading">Insights & diagnostics</h2>
            <p>Portfolio signals, compliance and refresh diagnostics for deeper analysis.</p>
          </div>

          <PortfolioSummaryPanel
            summary={portfolioSummary}
            totalPortfolioCount={repositories.length}
          />

          <ComplianceOverviewPanel
            summary={complianceSummary}
            repositories={repositories}
            loading={complianceLoading}
            error={complianceError}
          />

          <RefreshDiagnosticsPanel
            diagnostics={refreshDiagnostics}
            loading={refreshDiagnosticsLoading}
            error={refreshDiagnosticsError}
          />
        </section>
      )}
      </main>
    </>
  )
}
