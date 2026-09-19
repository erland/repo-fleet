import { useEffect, useRef, useState } from 'react'
import type { AnalysisState, RepositoryComplianceDetail, RepositorySummary } from './api'

type RepositoryDetailPanelProps = {
  repository: RepositorySummary | null
  compliance?: RepositoryComplianceDetail[]
  complianceLoading?: boolean
  complianceError?: string | null
  onSaveException?: (
    repositoryId: number,
    ruleKey: string,
    reason: string,
    expiresAt: string | null,
  ) => Promise<void>
  onExpireException?: (repositoryId: number, ruleKey: string) => Promise<void>
  onRemoveException?: (repositoryId: number, ruleKey: string) => Promise<void>
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


function expiryDateValue(value: string | null | undefined): string {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return date.toISOString().slice(0, 10)
}

function ExceptionEditor({
  repositoryId,
  item,
  onSave,
  onExpire,
  onRemove,
}: {
  repositoryId: number
  item: RepositoryComplianceDetail
  onSave?: (
    repositoryId: number,
    ruleKey: string,
    reason: string,
    expiresAt: string | null,
  ) => Promise<void>
  onExpire?: (repositoryId: number, ruleKey: string) => Promise<void>
  onRemove?: (repositoryId: number, ruleKey: string) => Promise<void>
}) {
  const [editing, setEditing] = useState(false)
  const [reason, setReason] = useState(item.exceptionReason ?? '')
  const [expiry, setExpiry] = useState(expiryDateValue(item.exceptionExpiresAt))
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setReason(item.exceptionReason ?? '')
    setExpiry(expiryDateValue(item.exceptionExpiresAt))
    setEditing(false)
    setError(null)
  }, [item.exceptionExpiresAt, item.exceptionReason, item.acceptedDeviation])

  if (item.result !== 'FAIL' && !item.acceptedDeviation) return null

  const submit = async () => {
    if (!onSave || !reason.trim()) return
    setSaving(true)
    setError(null)
    try {
      await onSave(
        repositoryId,
        item.ruleKey,
        reason.trim(),
        expiry ? new Date(expiry + 'T23:59:59Z').toISOString() : null,
      )
      setEditing(false)
    } catch {
      setError('The accepted deviation could not be saved.')
    } finally {
      setSaving(false)
    }
  }

  const expire = async () => {
    if (!onExpire) return
    setSaving(true)
    setError(null)
    try {
      await onExpire(repositoryId, item.ruleKey)
      setEditing(false)
    } catch {
      setError('The accepted deviation could not be expired.')
    } finally {
      setSaving(false)
    }
  }

  const remove = async () => {
    if (!onRemove) return
    setSaving(true)
    setError(null)
    try {
      await onRemove(repositoryId, item.ruleKey)
      setEditing(false)
    } catch {
      setError('The accepted deviation could not be removed.')
    } finally {
      setSaving(false)
    }
  }

  if (!editing) {
    return (
      <div className="exception-actions">
        <button className="secondary-button" type="button" onClick={() => setEditing(true)}>
          {item.acceptedDeviation ? 'Edit exception' : 'Accept deviation'}
        </button>
        {item.acceptedDeviation && (
          <>
            <button className="secondary-button" type="button" disabled={saving} onClick={() => void expire()}>
              Expire exception
            </button>
            <button className="secondary-button" type="button" disabled={saving} onClick={() => void remove()}>
              Remove exception
            </button>
          </>
        )}
        {error && <p className="compliance-error" role="alert">{error}</p>}
      </div>
    )
  }

  return (
    <div className="exception-editor">
      <label>
        Reason
        <textarea value={reason} onChange={(event) => setReason(event.target.value)} rows={3} />
      </label>
      <label>
        Optional expiry
        <input type="date" value={expiry} onChange={(event) => setExpiry(event.target.value)} />
      </label>
      <div className="exception-editor-actions">
        <button
          className="refresh-button"
          type="button"
          disabled={saving || !reason.trim()}
          onClick={() => void submit()}
        >
          {saving ? 'Saving…' : 'Save exception'}
        </button>
        <button className="secondary-button" type="button" disabled={saving} onClick={() => setEditing(false)}>
          Cancel
        </button>
      </div>
      {error && <p className="compliance-error" role="alert">{error}</p>}
    </div>
  )
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
  onSaveException,
  onExpireException,
  onRemoveException,
  onClose,
}: RepositoryDetailPanelProps) {
  const panelRef = useRef<HTMLElement>(null)
  const previousFocusRef = useRef<HTMLElement | null>(null)

  useEffect(() => {
    if (!repository) return

    previousFocusRef.current = document.activeElement instanceof HTMLElement
      ? document.activeElement
      : null
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    panelRef.current?.focus()

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault()
        onClose()
        return
      }

      if (event.key !== 'Tab' || !panelRef.current) return

      const focusable = Array.from(panelRef.current.querySelectorAll<HTMLElement>(
        'a[href], button:not([disabled]), input:not([disabled]), textarea:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])',
      ))
      if (focusable.length === 0) {
        event.preventDefault()
        panelRef.current.focus()
        return
      }

      const first = focusable[0]
      const last = focusable[focusable.length - 1]
      const active = document.activeElement

      if (event.shiftKey && (active === first || active === panelRef.current)) {
        event.preventDefault()
        last.focus()
      } else if (!event.shiftKey && active === last) {
        event.preventDefault()
        first.focus()
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => {
      window.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = previousOverflow
      previousFocusRef.current?.focus()
    }
  }, [repository, onClose])

  if (!repository) return null

  return (
    <div
      className="detail-backdrop"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose()
      }}
    >
      <aside
        ref={panelRef}
        className="detail-panel"
        role="dialog"
        aria-modal="true"
        aria-labelledby="repository-detail-heading"
        aria-describedby="repository-detail-description"
        tabIndex={-1}
      >
      <div className="detail-heading">
        <div>
          <p className="eyebrow">Repository details</p>
          <h2 id="repository-detail-heading">{repository.fullName}</h2>
          <p id="repository-detail-description" className="detail-description">
            Repository metadata, maintenance analysis and compliance without leaving the result list.
          </p>
        </div>
        <button className="secondary-button" type="button" onClick={onClose} aria-label={`Close details for ${repository.fullName}`}>
          Close
        </button>
      </div>

      <div className="detail-actions">
        <a className="repository-link" href={repository.url} target="_blank" rel="noreferrer">
          Open on GitHub
        </a>
      </div>

      <section className="detail-section" aria-labelledby="repository-overview-heading">
        <h3 id="repository-overview-heading">Repository overview</h3>
        <dl className="detail-grid">
          <DetailItem label="Owner" value={repository.owner} />
          <DetailItem label="Visibility" value={repository.visibility.toLowerCase()} />
          <DetailItem label="Default branch" value={repository.defaultBranch} />
          <DetailItem label="Primary language" value={repository.primaryLanguage ?? 'Unknown'} />
          <DetailItem label="Languages" value={repository.languages.length ? repository.languages.join(', ') : 'None detected'} />
          <DetailItem label="Topics" value={repository.topics.length ? repository.topics.join(', ') : 'None'} />
          <DetailItem label="Archived" value={booleanLabel(repository.archived)} />
          <DetailItem label="Fork" value={booleanLabel(repository.fork)} />
          <DetailItem label="Last push" value={formatDate(repository.activity.pushedAt)} />
          <DetailItem label="Last update" value={formatDate(repository.activity.updatedAt)} />
        </dl>
      </section>

      <details className="detail-disclosure">
        <summary>Maintenance analysis</summary>
        <dl className="detail-grid">
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
          <DetailItem label="Repository analysis" value={analysisLabel(repository.refreshStatus.state)} />
          <DetailItem label="Analysis message" value={repository.refreshStatus.message ?? '—'} />
        </dl>
      </details>

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
                  <strong>{item.acceptedDeviation ? 'ACCEPTED DEVIATION' : item.result}</strong>
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
                  {item.acceptedDeviation && (
                    <div>
                      <dt>Exception</dt>
                      <dd>
                        {item.exceptionReason ?? 'Accepted deviation'}
                        {item.exceptionExpiresAt ? ' · expires ' + formatDate(item.exceptionExpiresAt) : ''}
                      </dd>
                    </div>
                  )}
                </dl>
                <ExceptionEditor
                  repositoryId={repository.id}
                  item={item}
                  onSave={onSaveException}
                  onExpire={onExpireException}
                  onRemove={onRemoveException}
                />
              </article>
            ))}
          </div>
        )}
      </section>
      </aside>
    </div>
  )
}
