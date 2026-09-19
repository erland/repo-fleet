import type {
  InventoryStatus,
  RefreshDiagnosticsSnapshot,
  RepositoryComplianceDetail,
  RepositorySummary,
} from './api'
import { InventoryRefreshPanel } from './InventoryRefreshPanel'
import { RepositoryDetailPanel } from './RepositoryDetailPanel'
import { RepositoryInventory } from './RepositoryInventory'
import { RepositoryLauncherToolbar } from './RepositoryLauncherToolbar'
import { RepositorySelectionBar } from './RepositorySelectionBar'
import { SavedViewsPanel } from './SavedViewsPanel'
import type { RepositoryFilters } from './repositoryFilters'
import type { RepositorySort } from './repositorySorting'
import type { SavedRepositoryView } from './savedViews'

type RepositoryWorkspaceProps = {
  repositories: RepositorySummary[]
  visibleRepositories: RepositorySummary[]
  loading: boolean
  error: string | null
  inventoryStatus: InventoryStatus | null
  filters: RepositoryFilters
  onFiltersChange: (filters: RepositoryFilters) => void
  sort: RepositorySort
  onSortChange: (sort: RepositorySort) => void
  savedViews: SavedRepositoryView[]
  activeSavedViewId: string | null
  savedViewsStorageAvailable: boolean
  onShowAll: () => void
  onLoadSavedView: (viewId: string) => void
  onSaveCurrentView: (name: string) => void
  onDeleteSavedView: (viewId: string) => void
  detailRepository: RepositorySummary | null
  detailCompliance: RepositoryComplianceDetail[]
  detailComplianceLoading: boolean
  detailComplianceError: string | null
  onSaveException: (
    repositoryId: number,
    ruleKey: string,
    reason: string,
    expiresAt: string | null,
  ) => Promise<void>
  onExpireException: (repositoryId: number, ruleKey: string) => Promise<void>
  onRemoveException: (repositoryId: number, ruleKey: string) => Promise<void>
  onCloseDetails: () => void
  selectedRepositoryIds: Set<number>
  onToggleRepository: (repositoryId: number) => void
  onSelectVisible: () => void
  onDeselectVisible: () => void
  onClearSelection: () => void
  onOpenDetails: (repositoryId: number) => void
  statusError: string | null
  refreshing: boolean
  refreshDiagnostics: RefreshDiagnosticsSnapshot | null
  onRefresh: () => void
  onFullRefresh: () => void
}

export function RepositoryWorkspace({
  repositories,
  visibleRepositories,
  loading,
  error,
  inventoryStatus,
  filters,
  onFiltersChange,
  sort,
  onSortChange,
  savedViews,
  activeSavedViewId,
  savedViewsStorageAvailable,
  onShowAll,
  onLoadSavedView,
  onSaveCurrentView,
  onDeleteSavedView,
  detailRepository,
  detailCompliance,
  detailComplianceLoading,
  detailComplianceError,
  onSaveException,
  onExpireException,
  onRemoveException,
  onCloseDetails,
  selectedRepositoryIds,
  onToggleRepository,
  onSelectVisible,
  onDeselectVisible,
  onClearSelection,
  onOpenDetails,
  statusError,
  refreshing,
  refreshDiagnostics,
  onRefresh,
  onFullRefresh,
}: RepositoryWorkspaceProps) {
  return (
    <section id="repositories-workspace" aria-label="Repository workspace">
      <section className="repository-finder" aria-label="Repository finder">
        <RepositoryLauncherToolbar
          filters={filters}
          onFiltersChange={onFiltersChange}
          views={savedViews}
          activeViewId={activeSavedViewId}
          onShowAll={onShowAll}
          onLoadView={onLoadSavedView}
          sort={sort}
          onSortChange={onSortChange}
          totalCount={repositories.length}
          filteredCount={visibleRepositories.length}
        />
      </section>

      <RepositoryDetailPanel
        repository={detailRepository}
        compliance={detailCompliance}
        complianceLoading={detailComplianceLoading}
        complianceError={detailComplianceError}
        onSaveException={onSaveException}
        onExpireException={onExpireException}
        onRemoveException={onRemoveException}
        onClose={onCloseDetails}
      />

      <RepositorySelectionBar
        selection={selectedRepositoryIds}
        visibleRepositories={visibleRepositories}
        onSelectVisible={onSelectVisible}
        onDeselectVisible={onDeselectVisible}
        onClear={onClearSelection}
      />

      <RepositoryInventory
        repositories={visibleRepositories}
        loading={loading}
        error={error}
        emptyMessage={repositories.length > 0
          ? 'No repositories match the current filters.'
          : inventoryStatus?.state === 'RUNNING'
            ? 'Repository discovery is in progress…'
            : undefined}
        selectedRepositoryIds={selectedRepositoryIds}
        onToggleRepository={onToggleRepository}
        onOpenDetails={onOpenDetails}
      />

      <SavedViewsPanel
        views={savedViews}
        activeViewId={activeSavedViewId}
        storageAvailable={savedViewsStorageAvailable}
        onShowAll={onShowAll}
        onSave={onSaveCurrentView}
        onLoad={onLoadSavedView}
        onDelete={onDeleteSavedView}
        managementOnly
      />

      <InventoryRefreshPanel
        status={inventoryStatus}
        statusError={statusError}
        refreshing={refreshing}
        diagnostics={refreshDiagnostics}
        onRefresh={onRefresh}
        onFullRefresh={onFullRefresh}
      />
    </section>
  )
}
