import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  fetchAuthSession,
  logout,
  type AuthSession,
} from './api'
import { ComplianceOverviewPanel } from './ComplianceOverviewPanel'
import { PortfolioSummaryPanel } from './PortfolioSummaryPanel'
import { RefreshDiagnosticsPanel } from './RefreshDiagnosticsPanel'
import { RepositoryWorkspace } from './RepositoryWorkspace'
import { emptyRepositoryFilters, filterRepositories } from './repositoryFilters'
import { defaultRepositorySort, sortRepositories } from './repositorySorting'
import { clearRepositorySelection, deselectVisibleRepositories, selectVisibleRepositories, toggleRepositorySelection } from './repositorySelection'
import { summarizePortfolio } from './portfolioSummary'
import { useSavedRepositoryViews } from './useSavedRepositoryViews'
import { useRepositoryComplianceDetail } from './useRepositoryComplianceDetail'
import { useRepositoryPortfolioData } from './useRepositoryPortfolioData'

export default function App() {
  const [authSession, setAuthSession] = useState<AuthSession | null>(null)
  const [authError, setAuthError] = useState<string | null>(null)
  const [filters, setFilters] = useState(emptyRepositoryFilters)
  const [sort, setSort] = useState(defaultRepositorySort)
  const [selectedRepositoryIds, setSelectedRepositoryIds] = useState<Set<number>>(new Set())
  const [workspaceView, setWorkspaceView] = useState<'repositories' | 'insights'>('repositories')
  const portfolioDataEnabled = Boolean(
    authSession && (!authSession.authEnabled || authSession.authenticated),
  )
  const {
    repositories,
    loading,
    error,
    inventoryStatus,
    statusError,
    refreshing,
    complianceSummary,
    complianceLoading,
    complianceError,
    refreshDiagnostics,
    refreshDiagnosticsLoading,
    refreshDiagnosticsError,
    reloadCompliance: loadCompliance,
    refreshRepositories,
    fullRefreshRepositories,
  } = useRepositoryPortfolioData(portfolioDataEnabled)

  const {
    views: savedViews,
    activeViewId: activeSavedViewId,
    storageAvailable: savedViewsStorageAvailable,
    saveView,
    activateView,
    clearActiveView,
    deleteView,
  } = useSavedRepositoryViews()
  const mountedRef = useRef(true)

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

  const filteredRepositories = useMemo(
    () => filterRepositories(repositories, filters),
    [repositories, filters],
  )

  const sortedRepositories = useMemo(
    () => sortRepositories(filteredRepositories, sort),
    [filteredRepositories, sort],
  )


  const portfolioSummary = useMemo(
    () => summarizePortfolio(filteredRepositories),
    [filteredRepositories],
  )



  const saveCurrentView = useCallback((name: string) => {
    saveView(name, filters, sort)
  }, [filters, saveView, sort])

  const loadSavedView = useCallback((viewId: string) => {
    const view = activateView(viewId)
    if (!view) return

    setFilters({ ...view.filters })
    setSort({ ...view.sort })
  }, [activateView])

  const showAllRepositories = useCallback(() => {
    setFilters(emptyRepositoryFilters)
    setSort(defaultRepositorySort)
    clearActiveView()
  }, [clearActiveView])

  const changeFilters = useCallback((nextFilters: typeof filters) => {
    setFilters(nextFilters)
    clearActiveView()
  }, [clearActiveView])

  const changeSort = useCallback((nextSort: typeof sort) => {
    setSort(nextSort)
    clearActiveView()
  }, [clearActiveView])

  const deleteSavedView = useCallback((viewId: string) => {
    deleteView(viewId)
  }, [deleteView])

  const {
    repository: detailRepository,
    compliance: detailCompliance,
    loading: detailComplianceLoading,
    error: detailComplianceError,
    open: openRepositoryDetails,
    saveException: saveComplianceException,
    expireException: expireComplianceException,
    removeException: removeComplianceException,
    close: closeRepositoryDetails,
  } = useRepositoryComplianceDetail({
    repositories,
    onComplianceChanged: loadCompliance,
  })

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
        <RepositoryWorkspace
          repositories={repositories}
          visibleRepositories={sortedRepositories}
          loading={loading}
          error={error}
          inventoryStatus={inventoryStatus}
          filters={filters}
          onFiltersChange={changeFilters}
          sort={sort}
          onSortChange={changeSort}
          savedViews={savedViews}
          activeSavedViewId={activeSavedViewId}
          savedViewsStorageAvailable={savedViewsStorageAvailable}
          onShowAll={showAllRepositories}
          onLoadSavedView={loadSavedView}
          onSaveCurrentView={saveCurrentView}
          onDeleteSavedView={deleteSavedView}
          detailRepository={detailRepository}
          detailCompliance={detailCompliance}
          detailComplianceLoading={detailComplianceLoading}
          detailComplianceError={detailComplianceError}
          onSaveException={saveComplianceException}
          onExpireException={expireComplianceException}
          onRemoveException={removeComplianceException}
          onCloseDetails={closeRepositoryDetails}
          selectedRepositoryIds={selectedRepositoryIds}
          onToggleRepository={toggleRepository}
          onSelectVisible={selectVisible}
          onDeselectVisible={deselectVisible}
          onClearSelection={clearSelection}
          onOpenDetails={openRepositoryDetails}
          statusError={statusError}
          refreshing={refreshing}
          refreshDiagnostics={refreshDiagnostics}
          onRefresh={refreshRepositories}
          onFullRefresh={fullRefreshRepositories}
        />
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
