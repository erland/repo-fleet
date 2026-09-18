import type { RefreshDiagnosticsSnapshot } from './api'

type RefreshDiagnosticsPanelProps = {
  diagnostics: RefreshDiagnosticsSnapshot | null
  loading: boolean
  error: string | null
}

function formatDuration(ms: number | null): string {
  if (ms == null) return 'Running'
  if (ms < 1000) return `${ms} ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(1)} s`
  return `${(ms / 60000).toFixed(1)} min`
}

function formatDate(value: string | null): string {
  if (!value) return 'Unknown'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? 'Unknown' : date.toLocaleString()
}

export function RefreshDiagnosticsPanel({
  diagnostics,
  loading,
  error,
}: RefreshDiagnosticsPanelProps) {
  return (
    <section className="diagnostics-panel" aria-labelledby="refresh-diagnostics-heading">
      <div className="diagnostics-heading">
        <div>
          <p className="eyebrow">Operations</p>
          <h2 id="refresh-diagnostics-heading">Refresh diagnostics</h2>
        </div>
        {loading && <span role="status">Loading…</span>}
      </div>

      {error && <p className="compliance-error" role="alert">{error}</p>}

      {!error && diagnostics && (
        <>
          <div className="diagnostics-cards">
            <article>
              <span>Rate limit remaining</span>
              <strong>{diagnostics.rateLimitRemaining ?? 'Unknown'}</strong>
              <small>Reset: {formatDate(diagnostics.rateLimitResetAt)}</small>
            </article>
            <article>
              <span>Conditional 304s</span>
              <strong>{diagnostics.conditionalNotModifiedCount}</strong>
              <small>{diagnostics.conditionalCachedFreshCount} fresh-cache skips</small>
            </article>
            <article>
              <span>Modified responses</span>
              <strong>{diagnostics.conditionalModifiedCount}</strong>
              <small>Conditional requests returning new data</small>
            </article>
            <article>
              <span>Webhook refreshes</span>
              <strong>{diagnostics.webhookTriggeredRefreshCount}</strong>
              <small>{diagnostics.targetedFailedCount} targeted failures</small>
            </article>
          </div>

          <div className="diagnostics-section">
            <h3>Recent refresh runs</h3>
            {diagnostics.recentRuns.length === 0 ? (
              <p>No refresh history is available yet.</p>
            ) : (
              <div className="diagnostics-table-wrap" role="region" aria-label="Recent refresh runs" tabIndex={0}>
                <table className="diagnostics-table">
                  <thead>
                    <tr>
                      <th>Trigger</th>
                      <th>State</th>
                      <th>Duration</th>
                      <th>Reused</th>
                      <th>Refreshed</th>
                      <th>Errors</th>
                    </tr>
                  </thead>
                  <tbody>
                    {diagnostics.recentRuns.map((run) => (
                      <tr key={run.id}>
                        <td>{run.triggerType}</td>
                        <td>{run.finalState}</td>
                        <td>{formatDuration(run.durationMillis)}</td>
                        <td>{run.reusedCount}</td>
                        <td>{run.scheduledCount}</td>
                        <td>{run.errorCount}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {diagnostics.recentTargetedFailures.length > 0 && (
            <div className="diagnostics-section">
              <h3>Recent targeted failures</h3>
              <ul className="diagnostics-failures">
                {diagnostics.recentTargetedFailures.map((failure) => (
                  <li key={failure.jobId}>
                    <strong>Repository {failure.githubRepositoryId}</strong>
                    <span>{failure.triggerType} · {failure.attempts} attempt{failure.attempts === 1 ? '' : 's'}</span>
                    <small>{failure.lastError ?? 'Unknown error'} · {formatDate(failure.completedAt)}</small>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </>
      )}
    </section>
  )
}
