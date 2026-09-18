import { useEffect, useRef } from 'react'
import type { AnalysisState, RepositoryComplianceDetail, RepositorySummary } from './api'

type RepositoryDetailPanelProps = {
  repository: RepositorySummary | null
  compliance?: RepositoryComplianceDetail[]
  complianceLoading?: boolean
  complianceError?: string | null
  onClose: () => void
}

function analysisLabel(state: AnalysisState): string {
  switch (state) {
    case 'COMPLETE': return 'Complete'
    case 'PARTIAL': return 'Partial'
    case 'FAILED': return 'Failed'
    case 'NOT_ANALYZED': return 'Not analyzed'
  }
}

function booleanLabel(value: boolean): string {
  return value ? 'Yes' : 'No'
}

function formatDate(value: string | null): string {
  if (!value) return 'Unknown'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Unknown'
  return date.toLocaleString()
}

function licenseValue(repository: RepositorySummary): string {
  const license = repository.license
  if (license.analysisState !== 'COMPLETE' || license.presence === 'UNKNOWN') return 'Unknown'
  if (license.presence === 'MISSING') return 'Missing'
  return license.name ?? (license.recognized === false ? 'Custom or unrecognized' : 'Present')
}

function actionsValue(repository: RepositorySummary): string {
  const actions = repository.githubActions
  if (actions.analysisState !== 'COMPLETE' || actions.workflowsPresent === null) return 'Unknown'
  if (!actions.workflowsPresent) return 'No workflows'
  return `${actions.workflowCount ?? 0} workflow${actions.workflowCount === 1 ? '' : 's'}`
}

function releaseValue(repository: RepositorySummary): string {
  const release = repository.release
  if (release.analysisState !== 'COMPLETE' || release.releasePresent === null) return 'Unknown'
  if (!release.releasePresent) return 'No published release'
  return release.latestReleaseTag ?? release.latestReleaseName ?? 'Published release'
}

function DetailItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="detail-item">
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  )
}

export function RepositoryDetailPanel({
  repository,
  compliance = [],
  complianceLoading = false,
  complianceError = null,
  onClose,
}: RepositoryDetailPanelProps) {
  const panelRef = useRef<HTMLElement>(null)

  useEffect(() => {
    if (repository) {
      panelRef.current?.focus()
    }
  }, [repository])

  if (!repository) return null

  return (
    <aside
      ref={panelRef}
      className="detail-panel"
      aria-labelledby="repository-detail-heading"
      tabIndex={-1}
    >
      <div className="detail-heading">
        <div>
          <p className="eyebrow">Repository details</p>
          <h2 id="repository-detail-heading">{repository.fullName}</h2>
        </div>
        <button className="secondary-button" type="button" onClick={onClose}>Close details</button>
      </div>

      <div className="detail-actions">
        <a className="repository-link" href={repository.url} target="_blank" rel="noreferrer">
          Open on GitHub
        </a>
      </div>

      <dl className="detail-grid">
        <DetailItem label="Owner" value={repository.owner} />
        <DetailItem label="Visibility" value={repository.visibility.toLowerCase()} />
        <DetailItem label="Default branch" value={repository.defaultBranch} />
        <DetailItem label="Archived" value={booleanLabel(repository.archived)} />
        <DetailItem label="Fork" value={booleanLabel(repository.fork)} />
        <DetailItem label="Primary language" value={repository.primaryLanguage ?? 'Unknown'} />
        <DetailItem label="Languages" value={repository.languages.length ? repository.languages.join(', ') : 'None detected'} />
        <DetailItem label="Topics" value={repository.topics.length ? repository.topics.join(', ') : 'None'} />

        <DetailItem label="LICENSE" value={licenseValue(repository)} />
        <DetailItem label="LICENSE analysis" value={analysisLabel(repository.license.analysisState)} />
        <DetailItem label="License key" value={repository.license.key ?? '—'} />
        <DetailItem label="Recognized license" value={repository.license.recognized === null ? 'Unknown' : booleanLabel(repository.license.recognized)} />

        <DetailItem label="GitHub Actions" value={actionsValue(repository)} />
        <DetailItem label="Actions analysis" value={analysisLabel(repository.githubActions.analysisState)} />

        <DetailItem label="Official release" value={releaseValue(repository)} />
        <DetailItem label="Release analysis" value={analysisLabel(repository.release.analysisState)} />
        <DetailItem label="Release name" value={repository.release.latestReleaseName ?? '—'} />
        <DetailItem label="Release date" value={formatDate(repository.release.latestReleaseDate)} />
        <DetailItem
          label="Prerelease"
          value={repository.release.latestReleasePrerelease === null ? 'Unknown' : booleanLabel(repository.release.latestReleasePrerelease)}
        />

        <DetailItem label="Last push" value={formatDate(repository.activity.pushedAt)} />
        <DetailItem label="Last update" value={formatDate(repository.activity.updatedAt)} />
        <DetailItem label="Repository analysis" value={analysisLabel(repository.refreshStatus.state)} />
        <DetailItem label="Analysis message" value={repository.refreshStatus.message ?? '—'} />
      </dl>

      <section className="compliance-detail" aria-labelledby="repository-compliance-heading">
        <div className="compliance-detail-heading">
          <div>
            <p className="eyebrow">Standards</p>
            <h3 id="repository-compliance-heading">Compliance detail</h3>
          </div>
          <span>{compliance.length} applicable rule{compliance.length === 1 ? '' : 's'}</span>
        </div>

        {complianceLoading && <p role="status">Loading compliance detail…</p>}
        {complianceError && <p className="compliance-error" role="alert">{complianceError}</p>}

        {!complianceLoading && !complianceError && compliance.length === 0 && (
          <p className="compliance-detail-empty">No persisted compliance evaluations are available for this repository yet.</p>
        )}

        {compliance.length > 0 && (
          <div className="compliance-detail-list">
            {compliance.map((item) => (
              <article className="compliance-detail-item" key={item.ruleKey}>
                <div className="compliance-detail-title">
                  <div>
                    <h4>{item.ruleName}</h4>
                    <span>{item.severity}</span>
                  </div>
                  <strong>{item.result}</strong>
                </div>
                <dl>
                  <div>
                    <dt>Reason</dt>
                    <dd>{item.reason}</dd>
                  </div>
                  <div>
                    <dt>Observed value</dt>
                    <dd>{item.observedValue ?? '—'}</dd>
                  </div>
                  <div>
                    <dt>Last evaluated</dt>
                    <dd>{formatDate(item.evaluatedAt)}</dd>
                  </div>
                </dl>
              </article>
            ))}
          </div>
        )}
      </section>
    </aside>
  )
}
