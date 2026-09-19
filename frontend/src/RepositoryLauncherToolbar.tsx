import type { RepositoryVisibility } from './api'
import {
  countActiveAdvancedRepositoryFilters,
  emptyRepositoryFilters,
  type ActivityAgeFilter,
  type BooleanFilter,
  type PresenceFilter,
  type RepositoryFilters,
} from './repositoryFilters'
import type { RepositorySort, RepositorySortField, SortDirection } from './repositorySorting'
import type { SavedRepositoryView } from './savedViews'

type RepositoryLauncherToolbarProps = {
  filters: RepositoryFilters
  onFiltersChange: (filters: RepositoryFilters) => void
  views: SavedRepositoryView[]
  activeViewId: string | null
  onShowAll: () => void
  onLoadView: (viewId: string) => void
  sort: RepositorySort
  onSortChange: (sort: RepositorySort) => void
  totalCount: number
  filteredCount: number
}

const sortOptions: Array<{ value: string; label: string }> = [
  { value: 'name:ASC', label: 'Name A–Z' },
  { value: 'name:DESC', label: 'Name Z–A' },
  { value: 'activity:DESC', label: 'Recently active' },
  { value: 'activity:ASC', label: 'Oldest activity' },
  { value: 'owner:ASC', label: 'Owner' },
  { value: 'primaryLanguage:ASC', label: 'Language' },
  { value: 'license:ASC', label: 'License status' },
  { value: 'actions:ASC', label: 'Actions status' },
  { value: 'release:ASC', label: 'Release status' },
]

export function RepositoryLauncherToolbar({
  filters,
  onFiltersChange,
  views,
  activeViewId,
  onShowAll,
  onLoadView,
  sort,
  onSortChange,
  totalCount,
  filteredCount,
}: RepositoryLauncherToolbarProps) {
  const advancedFilterCount = countActiveAdvancedRepositoryFilters(filters)
  const resultCountLabel = filteredCount === totalCount
    ? `${totalCount} repositories`
    : `${filteredCount} of ${totalCount} repositories`

  const update = <K extends keyof RepositoryFilters>(key: K, value: RepositoryFilters[K]) => {
    onFiltersChange({ ...filters, [key]: value })
  }

  const changeSort = (value: string) => {
    const [field, direction] = value.split(':') as [RepositorySortField, SortDirection]
    onSortChange({ field, direction })
  }

  return (
    <section className="repository-launcher" aria-label="Find repositories">
      <div className="launcher-primary">
        <label className="launcher-search" htmlFor="repository-search">
          <span className="sr-only">Search repositories</span>
          <input
            id="repository-search"
            type="search"
            placeholder="Search repositories…"
            autoComplete="off"
            value={filters.nameContains}
            onChange={(event) => update('nameContains', event.target.value)}
          />
        </label>

        <label className="launcher-select">
          <span className="sr-only">Saved view</span>
          <select
            aria-label="Saved view"
            value={activeViewId ?? ''}
            onChange={(event) => event.target.value ? onLoadView(event.target.value) : onShowAll()}
          >
            <option value="">All repositories</option>
            {views.map((view) => (
              <option key={view.id} value={view.id}>{view.name}</option>
            ))}
          </select>
        </label>

        <label className="launcher-select">
          <span className="sr-only">Sort repositories</span>
          <select
            aria-label="Sort repositories"
            value={`${sort.field}:${sort.direction}`}
            onChange={(event) => changeSort(event.target.value)}
          >
            {sortOptions.map((option) => (
              <option key={option.value} value={option.value}>{option.label}</option>
            ))}
          </select>
        </label>

        <details className="launcher-filters">
          <summary>
            Filters{advancedFilterCount > 0 ? ` · ${advancedFilterCount}` : ''}
          </summary>
          <div className="launcher-filter-panel">
            <div className="launcher-filter-heading">
              <strong>Advanced filters</strong>
              <button className="secondary-button" type="button" onClick={() => onFiltersChange({
                ...emptyRepositoryFilters,
                nameContains: filters.nameContains,
              })}>
                Clear filters
              </button>
            </div>

            <div className="filter-grid">
              <label>
                <span>Name prefix</span>
                <input value={filters.namePrefix} onChange={(event) => update('namePrefix', event.target.value)} />
              </label>
              <label>
                <span>Owner</span>
                <input value={filters.owner} onChange={(event) => update('owner', event.target.value)} />
              </label>
              <label>
                <span>Visibility</span>
                <select value={filters.visibility} onChange={(event) => update('visibility', event.target.value as 'ANY' | RepositoryVisibility)}>
                  <option value="ANY">Any</option>
                  <option value="PUBLIC">Public</option>
                  <option value="PRIVATE">Private</option>
                  <option value="INTERNAL">Internal</option>
                </select>
              </label>
              <label>
                <span>Archived</span>
                <select value={filters.archived} onChange={(event) => update('archived', event.target.value as BooleanFilter)}>
                  <option value="ANY">Any</option>
                  <option value="NO">Active only</option>
                  <option value="YES">Archived only</option>
                </select>
              </label>
              <label>
                <span>Fork</span>
                <select value={filters.fork} onChange={(event) => update('fork', event.target.value as BooleanFilter)}>
                  <option value="ANY">Any</option>
                  <option value="NO">Non-forks</option>
                  <option value="YES">Forks only</option>
                </select>
              </label>
              <label>
                <span>Topic</span>
                <input value={filters.topic} onChange={(event) => update('topic', event.target.value)} />
              </label>
              <label>
                <span>Topic match</span>
                <select value={filters.topicPresence} onChange={(event) => update('topicPresence', event.target.value as PresenceFilter)}>
                  <option value="ANY">Has topic</option>
                  <option value="PRESENT">Topic present</option>
                  <option value="MISSING">Topic absent</option>
                </select>
              </label>
              <label>
                <span>Language</span>
                <input value={filters.language} onChange={(event) => update('language', event.target.value)} />
              </label>
              <label>
                <span>Language match</span>
                <select value={filters.languagePresence} onChange={(event) => update('languagePresence', event.target.value as PresenceFilter)}>
                  <option value="ANY">Has language</option>
                  <option value="PRESENT">Language present</option>
                  <option value="MISSING">Language absent</option>
                </select>
              </label>
              <label>
                <span>License</span>
                <select value={filters.license} onChange={(event) => update('license', event.target.value as PresenceFilter)}>
                  <option value="ANY">Any</option>
                  <option value="PRESENT">Present</option>
                  <option value="MISSING">Missing</option>
                </select>
              </label>
              <label>
                <span>GitHub Actions</span>
                <select value={filters.actions} onChange={(event) => update('actions', event.target.value as PresenceFilter)}>
                  <option value="ANY">Any</option>
                  <option value="PRESENT">Present</option>
                  <option value="MISSING">Missing</option>
                </select>
              </label>
              <label>
                <span>Official release</span>
                <select value={filters.release} onChange={(event) => update('release', event.target.value as PresenceFilter)}>
                  <option value="ANY">Any</option>
                  <option value="PRESENT">Present</option>
                  <option value="MISSING">Missing</option>
                </select>
              </label>
              <label>
                <span>Activity</span>
                <select value={filters.activityAge} onChange={(event) => update('activityAge', event.target.value as ActivityAgeFilter)}>
                  <option value="ANY">Any age</option>
                  <option value="7_DAYS">Active in last 7 days</option>
                  <option value="30_DAYS">Active in last 30 days</option>
                  <option value="90_DAYS">Active in last 90 days</option>
                  <option value="365_DAYS">Active in last year</option>
                </select>
              </label>
            </div>
          </div>
        </details>
      </div>

      <div className="launcher-meta" aria-live="polite">{resultCountLabel}</div>
    </section>
  )
}
