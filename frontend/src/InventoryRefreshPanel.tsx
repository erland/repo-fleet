import type { InventoryStatus, RefreshDiagnosticsSnapshot } from './api'

type InventoryRefreshPanelProps = {
  status: InventoryStatus | null
  statusError: string | null
  refreshing: boolean
  diagnostics?: RefreshDiagnosticsSnapshot | null
  onRefresh: () => void
  onFullRefresh?: () => void
}

function formatTimestamp(value: string | null | undefined): string {
  if (!value) return 'Never'

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Unknown'

  return date.toLocaleString()
}

function progressLabel(status: InventoryStatus): string {
  if (status.totalCount <= 0) {
    return 'Preparing repository refresh…'
  }

  return `${status.processedCount} of ${status.totalCount} repositories processed`
}

function statusLabel(status: InventoryStatus | null, running: boolean, statusError: string | null): string {
  if (statusError) return 'Status unavailable'
  if (running) return 'Refreshing'
  if (status?.state === 'PARTIAL') return 'Needs attention'
  if (status?.state === 'FAILED') return 'Refresh failed'
  if (status?.state === 'COMPLETED') return 'Up to date'
  return 'Not refreshed'
}

export function InventoryRefreshPanel({
  status,
  statusError,
  refreshing,
  diagnostics,
  onRefresh,
  onFullRefresh,
}: InventoryRefreshPanelProps) {
  const running = refreshing || status?.state === 'RUNNING'
  const showPartial = status?.state === 'PARTIAL'
  const showFailed = status?.state === 'FAILED'
  const attention = running || showPartial || showFailed || Boolean(statusError)
  const progressPercent = status && status.totalCount > 0
    ? `${Math.round((status.processedCount / status.totalCount) * 100)}%`
    : null
  const partialMessage = status?.state === 'PARTIAL'
    ? `${status.errorCount} ${status.errorCount === 1 ? 'repository had' : 'repositories had'} a degraded or failed refresh. Previously cached metadata may still be available.`
    : null
  const currentStatusLabel = statusLabel(status, running, statusError)

  return (
    <section className="refresh-panel" aria-labelledby="refresh-heading">
      <details className="refresh-details" open={attention}>
        <summary className="refresh-status-summary">
          <div className="refresh-status-copy">
            <span
              className={`refresh-status-indicator refresh-status-${attention ? 'attention' : 'normal'}`}
              aria-hidden="true"
            />
            <div>
              <h2 id="refresh-heading">System status</h2>
              <p>
                <strong>{currentStatusLabel}</strong>
                <span> · Last successful refresh: {formatTimestamp(status?.lastSuccessfulRefreshAt)}</span>
              </p>
            </div>
          </div>
          <span className="refresh-status-action">Refresh controls</span>
        </summary>

        <div className="refresh-controls">
          <div className="refresh-actions">
            <button className="refresh-button" type="button" onClick={onRefresh} disabled={running}>
              {running ? 'Refreshing…' : 'Refresh repositories'}
            </button>
            {onFullRefresh && (
              <button className="refresh-button refresh-button-secondary" type="button" onClick={onFullRefresh} disabled={running}>
                Full refresh
              </button>
            )}
          </div>

          {running && status && (
            <div className="refresh-progress" role="status" aria-live="polite">
              <div className="progress-row">
                <span id="refresh-progress-label">{progressLabel(status)}</span>
                {progressPercent && <span>{progressPercent}</span>}
              </div>
              <progress
                aria-labelledby="refresh-progress-label"
                max={Math.max(status.totalCount, 1)}
                value={Math.min(status.processedCount, Math.max(status.totalCount, 1))}
              />
              {diagnostics?.rateLimitPaused && diagnostics.rateLimitResumeAt ? (
                <div className="refresh-message refresh-message-warning" role="status">
                  <strong>Temporarily paused by GitHub rate limiting.</strong>
                  <span>
                    Refresh will continue automatically around {formatTimestamp(diagnostics.rateLimitResumeAt)}.
                  </span>
                </div>
              ) : (
                status.currentRepository && <p>Currently analyzing {status.currentRepository}</p>
              )}
              <p className="refresh-note">Existing repository data remains available while refresh is running.</p>
            </div>
          )}

          {showPartial && (
            <div className="refresh-message refresh-message-warning" role="status">
              <strong>Refresh completed with warnings.</strong>
              <span>{partialMessage}</span>
            </div>
          )}

          {showFailed && (
            <div className="refresh-message refresh-message-error" role="alert">
              <strong>Refresh failed.</strong>
              <span>{status.errorMessage ?? 'The repository inventory could not be fully refreshed.'}</span>
            </div>
          )}

          {statusError && (
            <div className="refresh-message refresh-message-error" role="alert">
              <strong>Refresh status unavailable.</strong>
              <span>{statusError}</span>
            </div>
          )}
        </div>
      </details>
    </section>
  )
}
