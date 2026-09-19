import type { RepositorySummary } from './api'

type RepositoryInventoryProps = {
  repositories: RepositorySummary[]
  loading: boolean
  error: string | null
  emptyMessage?: string
  selectedRepositoryIds?: ReadonlySet<number>
  onToggleRepository?: (repositoryId: number) => void
  onOpenDetails?: (repositoryId: number) => void
}

function analysisUnavailable(state: string): boolean {
  return state === 'FAILED' || state === 'PARTIAL' || state === 'NOT_ANALYZED'
}

function activityLabel(repository: RepositorySummary): string | null {
  const value = repository.activity.pushedAt ?? repository.activity.updatedAt
  if (!value) return null

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return null

  return date.toLocaleDateString('en-CA')
}

function maintenanceFlags(repository: RepositorySummary): string[] {
  const flags: string[] = []

  if (repository.license.analysisState === 'FAILED' || repository.license.presence === 'UNKNOWN') {
    flags.push('License unknown')
  } else if (repository.license.presence === 'MISSING') {
    flags.push('Missing license')
  }

  if (analysisUnavailable(repository.githubActions.analysisState) || repository.githubActions.workflowsPresent === null) {
    flags.push('Actions unknown')
  } else if (!repository.githubActions.workflowsPresent) {
    flags.push('No Actions')
  }

  if (analysisUnavailable(repository.release.analysisState) || repository.release.releasePresent === null) {
    flags.push('Release unknown')
  } else if (!repository.release.releasePresent) {
    flags.push('No release')
  }

  return flags
}

export function RepositoryInventory({
  repositories,
  loading,
  error,
  emptyMessage = 'No repositories are available.',
  selectedRepositoryIds = new Set<number>(),
  onToggleRepository = () => undefined,
  onOpenDetails = () => undefined,
}: RepositoryInventoryProps) {
  if (loading) {
    return (
      <section className="inventory-state" aria-live="polite" aria-busy="true">
        <h2>Repositories</h2>
        <p>Loading repository inventory…</p>
      </section>
    )
  }

  if (error) {
    return (
      <section className="inventory-state inventory-state-error" role="alert">
        <h2>Repositories</h2>
        <p>{error}</p>
      </section>
    )
  }

  if (repositories.length === 0) {
    return (
      <section className="inventory-state">
        <h2>Repositories</h2>
        <p>{emptyMessage}</p>
      </section>
    )
  }

  return (
    <section className="inventory-section" aria-labelledby="repository-heading">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Repository results</p>
          <h2 id="repository-heading">Repositories</h2>
        </div>
        <span className="repository-count">{repositories.length} repositories</span>
      </div>

      <div
        className="table-scroll"
        role="region"
        aria-label="Repository inventory table"
        tabIndex={0}
      >
        <table className="repository-table">
          <caption className="sr-only">
            Repository results with discovery information and maintenance flags
          </caption>
          <thead>
            <tr>
              <th scope="col" className="selection-column">Select</th>
              <th scope="col">Repository</th>
              <th scope="col">Topics</th>
              <th scope="col">Language</th>
              <th scope="col">Last activity</th>
              <th scope="col">Maintenance</th>
              <th scope="col">Details</th>
            </tr>
          </thead>
          <tbody>
            {repositories.map((repository) => {
              const flags = maintenanceFlags(repository)
              const activity = activityLabel(repository)
              const visibleTopics = repository.topics.slice(0, 2)
              const hiddenTopicCount = Math.max(0, repository.topics.length - visibleTopics.length)
              return (
                <tr
                  key={repository.id}
                  className={selectedRepositoryIds.has(repository.id) ? 'repository-row-selected repository-row-interactive' : 'repository-row-interactive'}
                  role="button"
                  tabIndex={0}
                  aria-label={`Open details for ${repository.fullName}`}
                  onClick={() => onOpenDetails(repository.id)}
                  onKeyDown={(event) => {
                    if (event.target !== event.currentTarget) return
                    if (event.key === 'Enter' || event.key === ' ') {
                      event.preventDefault()
                      onOpenDetails(repository.id)
                    }
                  }}
                >
                  <td className="selection-column" data-label="Select">
                    <input
                      type="checkbox"
                      aria-label={`Select ${repository.fullName}`}
                      checked={selectedRepositoryIds.has(repository.id)}
                      onClick={(event) => event.stopPropagation()}
                      onChange={() => onToggleRepository(repository.id)}
                    />
                  </td>
                  <td data-label="Repository">
                    <div className="repository-identity">
                      <button
                        className="repository-link repository-details-link"
                        type="button"
                        onClick={(event) => {
                          event.stopPropagation()
                          onOpenDetails(repository.id)
                        }}
                      >
                        {repository.fullName}
                      </button>
                      <div className="repository-badges repository-badges-desktop">
                        <span className="inline-badge">{repository.visibility.toLowerCase()}</span>
                        {repository.archived && <span className="inline-badge">Archived</span>}
                        {repository.fork && <span className="inline-badge">Fork</span>}
                      </div>
                      <div className="mobile-repository-signals" aria-label={`Repository indicators for ${repository.fullName}`}>
                        <span className="repository-signal">{repository.visibility.toLowerCase()}</span>
                        {repository.primaryLanguage && <span className="repository-signal">{repository.primaryLanguage}</span>}
                        {activity && <span className="repository-signal">{activity}</span>}
                        {repository.archived && <span className="repository-signal">archived</span>}
                        {repository.fork && <span className="repository-signal">fork</span>}
                        {visibleTopics.map((topic) => <span className="repository-signal repository-signal-topic" key={topic}>{topic}</span>)}
                        {hiddenTopicCount > 0 && <span className="repository-signal">+{hiddenTopicCount}</span>}
                        {flags.map((flag) => <span className="repository-signal repository-signal-warning" key={flag}>{flag}</span>)}
                      </div>
                      <a
                        className="mobile-github-link"
                        href={repository.url}
                        target="_blank"
                        rel="noreferrer"
                        onClick={(event) => event.stopPropagation()}
                      >
                        GitHub
                      </a>
                    </div>
                  </td>
                  <td data-label="Topics">
                    {repository.topics.length > 0 ? (
                      <div className="topic-list">
                        {repository.topics.map((topic) => <span className="topic" key={topic}>{topic}</span>)}
                      </div>
                    ) : '—'}
                  </td>
                  <td data-label="Language">{repository.primaryLanguage ?? '—'}</td>
                  <td data-label="Last activity">{activity ?? '—'}</td>
                  <td data-label="Maintenance">
                    {flags.length === 0 ? (
                      <span className="maintenance-clear">No maintenance flags</span>
                    ) : (
                      <div className="maintenance-flags">
                        {flags.map((flag) => <span className="maintenance-flag" key={flag}>{flag}</span>)}
                      </div>
                    )}
                  </td>
                  <td data-label="Details">
                    <a
                      className="table-action-button repository-github-link"
                      href={repository.url}
                      target="_blank"
                      rel="noreferrer"
                      onClick={(event) => event.stopPropagation()}
                    >
                      GitHub
                    </a>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </section>
  )
}
