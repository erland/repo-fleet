import type {
  GitHubActionsStatus,
  LicenseStatus,
  ReleaseStatus,
  RepositorySummary,
} from './api'

type RepositoryInventoryProps = {
  repositories: RepositorySummary[]
  loading: boolean
  error: string | null
  emptyMessage?: string
  selectedRepositoryIds?: ReadonlySet<number>
  onToggleRepository?: (repositoryId: number) => void
}

function analysisUnavailable(state: string): boolean {
  return state === 'FAILED' || state === 'PARTIAL' || state === 'NOT_ANALYZED'
}

function licenseLabel(license: LicenseStatus): string {
  if (license.analysisState === 'FAILED' || license.presence === 'UNKNOWN') return 'Unknown'
  if (license.presence === 'MISSING') return 'Missing'
  if (license.name) return license.name
  return license.recognized === false ? 'Custom' : 'Present'
}

function actionsLabel(actions: GitHubActionsStatus): string {
  if (analysisUnavailable(actions.analysisState) || actions.workflowsPresent === null) return 'Unknown'
  if (!actions.workflowsPresent) return 'None'
  return `${actions.workflowCount ?? 0} workflow${actions.workflowCount === 1 ? '' : 's'}`
}

function releaseLabel(release: ReleaseStatus): string {
  if (analysisUnavailable(release.analysisState) || release.releasePresent === null) return 'Unknown'
  if (!release.releasePresent) return 'None'
  return release.latestReleaseTag ?? release.latestReleaseName ?? 'Published'
}

function activityLabel(repository: RepositorySummary): string {
  const value = repository.activity.pushedAt ?? repository.activity.updatedAt
  if (!value) return 'Unknown'

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Unknown'

  return date.toLocaleDateString('en-CA')
}

export function RepositoryInventory({
  repositories,
  loading,
  error,
  emptyMessage = 'No repositories are available.',
  selectedRepositoryIds = new Set<number>(),
  onToggleRepository = () => undefined,
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
          <p className="eyebrow">Portfolio inventory</p>
          <h2 id="repository-heading">Repositories</h2>
        </div>
        <span className="repository-count">{repositories.length} repositories</span>
      </div>

      <div className="table-scroll">
        <table className="repository-table">
          <thead>
            <tr>
              <th scope="col" className="selection-column">Select</th>
              <th scope="col">Repository</th>
              <th scope="col">Owner</th>
              <th scope="col">Visibility</th>
              <th scope="col">Topics</th>
              <th scope="col">Language</th>
              <th scope="col">License</th>
              <th scope="col">Actions</th>
              <th scope="col">Release</th>
              <th scope="col">Last activity</th>
            </tr>
          </thead>
          <tbody>
            {repositories.map((repository) => (
              <tr key={repository.id} className={selectedRepositoryIds.has(repository.id) ? 'repository-row-selected' : undefined}>
                <td className="selection-column">
                  <input
                    type="checkbox"
                    aria-label={`Select ${repository.fullName}`}
                    checked={selectedRepositoryIds.has(repository.id)}
                    onChange={() => onToggleRepository(repository.id)}
                  />
                </td>
                <td>
                  <a href={repository.url} target="_blank" rel="noreferrer" className="repository-link">
                    {repository.name}
                  </a>
                  {repository.archived && <span className="inline-badge">Archived</span>}
                  {repository.fork && <span className="inline-badge">Fork</span>}
                </td>
                <td>{repository.owner}</td>
                <td>{repository.visibility.toLowerCase()}</td>
                <td>
                  {repository.topics.length > 0 ? (
                    <div className="topic-list">
                      {repository.topics.map((topic) => <span className="topic" key={topic}>{topic}</span>)}
                    </div>
                  ) : '—'}
                </td>
                <td>{repository.primaryLanguage ?? '—'}</td>
                <td>{licenseLabel(repository.license)}</td>
                <td>{actionsLabel(repository.githubActions)}</td>
                <td>{releaseLabel(repository.release)}</td>
                <td>{activityLabel(repository)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
