import {
  defaultRepositorySort,
  type RepositorySort,
  type RepositorySortField,
  type SortDirection,
} from './repositorySorting'

type RepositorySortControlsProps = {
  sort: RepositorySort
  onChange: (sort: RepositorySort) => void
  totalCount: number
  filteredCount: number
}

export function RepositorySortControls({
  sort,
  onChange,
  totalCount,
  filteredCount,
}: RepositorySortControlsProps) {
  const resultCountLabel = filteredCount === totalCount
    ? `${totalCount} repositories`
    : `${filteredCount} of ${totalCount} repositories`

  return (
    <section className="sort-panel" aria-label="Repository sorting">
      <div className="sort-controls">
        <label>
          <span>Sort by</span>
          <select
            value={sort.field}
            onChange={(event) => onChange({ ...sort, field: event.target.value as RepositorySortField })}
          >
            <option value="name">Name</option>
            <option value="owner">Owner</option>
            <option value="activity">Last activity</option>
            <option value="primaryLanguage">Primary language</option>
            <option value="license">License state</option>
            <option value="actions">Actions state</option>
            <option value="release">Release state</option>
          </select>
        </label>

        <label>
          <span>Direction</span>
          <select
            value={sort.direction}
            onChange={(event) => onChange({ ...sort, direction: event.target.value as SortDirection })}
          >
            <option value="ASC">Ascending</option>
            <option value="DESC">Descending</option>
          </select>
        </label>

        <button className="secondary-button" type="button" onClick={() => onChange(defaultRepositorySort)}>
          Reset sorting
        </button>
      </div>

      <p className="sort-count" aria-live="polite">{resultCountLabel}</p>
    </section>
  )
}
