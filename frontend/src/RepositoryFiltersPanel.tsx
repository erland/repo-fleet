import type { RepositoryVisibility } from './api'
import {
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
  const update = <K extends keyof RepositoryFilters>(key: K, value: RepositoryFilters[K]) => {
    onChange({ ...filters, [key]: value })
  }

  return (
    <section className="filter-panel" aria-labelledby="filter-heading">
      <div className="filter-heading-row">
        <div>
          <p className="eyebrow">Inventory filters</p>
          <h2 id="filter-heading">Filter repositories</h2>
          <p className="filter-help">Filters use AND semantics across categories. Topic and language values are exact, case-insensitive matches.</p>
        </div>
        <button className="secondary-button" type="button" onClick={() => onChange(emptyRepositoryFilters)}>
          Clear filters
        </button>
      </div>

      <div className="filter-grid">
        <label>
          <span>Name contains</span>
          <input value={filters.nameContains} onChange={(event) => update('nameContains', event.target.value)} />
        </label>
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

      <p className="filter-count" aria-live="polite">{filteredCount} of {totalCount} repositories match</p>
    </section>
  )
}
