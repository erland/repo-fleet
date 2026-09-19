import type { RepositoryVisibility } from './api'
import {
  countActiveAdvancedRepositoryFilters,
  emptyRepositoryFilters,
  type ActivityAgeFilter,
  type BooleanFilter,
  type PresenceFilter,
  type RepositoryFilters,
} from './repositoryFilters'

type RepositoryFiltersPanelProps = {
  filters: RepositoryFilters
  onChange: (filters: RepositoryFilters) => void
  totalCount: number
  filteredCount: number
}

export function RepositoryFiltersPanel({
  filters,
  onChange,
  totalCount,
  filteredCount,
}: RepositoryFiltersPanelProps) {
  const resultCountLabel = `${filteredCount} of ${totalCount} repositories match current filters`
  const advancedFilterCount = countActiveAdvancedRepositoryFilters(filters)
  const advancedSummary = advancedFilterCount === 0
    ? 'Advanced filters'
    : `Advanced filters · ${advancedFilterCount} active`

  const update = <K extends keyof RepositoryFilters>(key: K, value: RepositoryFilters[K]) => {
    onChange({ ...filters, [key]: value })
  }

  return (
    <section className="filter-panel" aria-labelledby="filter-heading">
      <div className="simple-search-row">
        <label className="repository-search-field" htmlFor="repository-search">
          <span id="filter-heading">Search repositories</span>
          <input
            id="repository-search"
            type="search"
            placeholder="Repository name or owner/name"
            autoComplete="off"
            value={filters.nameContains}
            onChange={(event) => update('nameContains', event.target.value)}
          />
        </label>
        <button className="secondary-button" type="button" onClick={() => onChange(emptyRepositoryFilters)}>
          Clear all
        </button>
      </div>

      <p className="filter-count" aria-live="polite">{resultCountLabel}</p>

      <details className="advanced-filters">
        <summary>
          <span>{advancedSummary}</span>
          <small>Prefix, owner, metadata and maintenance status</small>
        </summary>

        <p className="filter-help">
          Advanced filters use AND semantics across categories. Topic and language values are exact, case-insensitive matches.
        </p>

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
      </details>
    </section>
  )
}
