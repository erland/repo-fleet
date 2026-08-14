import type { InventoryStatus } from './api'

type InventoryRefreshPanelProps = {
  status: InventoryStatus | null
  statusError: string | null
  refreshing: boolean
  onRefresh: () => void
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

export function InventoryRefreshPanel({
  status,
  statusError,
  refreshing,
  onRefresh,
}: InventoryRefreshPanelProps) {
  const running = refreshing || status?.state === 'RUNNING'
  const showPartial = status?.state === 'PARTIAL'
  const showFailed = status?.state === 'FAILED'

  return (
    <section className="refresh-panel" aria-labelledby="refresh-heading">
      <div className="refresh-summary">
        <div>
          <p className="eyebrow">Inventory freshness</p>
          <h2 id="refresh-heading">Repository refresh</h2>
          <p className="refresh-meta">
            Last successful refresh: <strong>{formatTimestamp(status?.lastSuccessfulRefreshAt)}</strong>
          </p>
        </div>
        <button className="refresh-button" type="button" onClick={onRefresh} disabled={running}>
          {running ? 'Refreshing…' : 'Refresh repositories'}
        </button>
      </div>

      {running && status && (
        <div className="refresh-progress" role="status" aria-live="polite">
          <div className="progress-row">
            <span>{progressLabel(status)}</span>
            {status.totalCount > 0 && (
              <span>{Math.round((status.processedCount / status.totalCount) * 100)}%</span>
            )}
          </div>
          <progress
            max={Math.max(status.totalCount, 1)}
            value={Math.min(status.processedCount, Math.max(status.totalCount, 1))}
          />
          {status.currentRepository && <p>Currently analyzing {status.currentRepository}</p>}
          <p className="refresh-note">Existing repository data remains available while refresh is running.</p>
        </div>
      )}

      {showPartial && (
        <div className="refresh-message refresh-message-warning" role="status">
          <strong>Refresh completed with partial failures.</strong>
          <span>
            {status.errorCount} {status.errorCount === 1 ? 'repository has' : 'repositories have'} incomplete or failed analysis.
          </span>
        </div>
      )}

      {showFailed && (
        <div className="refresh-message refresh-message-error" role="alert">
          <strong>Refresh failed.</strong>
          <span>{status.errorMessage ?? 'The repository inventory could not be fully refreshed.'}</span>
        </div>
      )}

      {status?.state === 'COMPLETED' && (
        <div className="refresh-message refresh-message-success" role="status">
          <strong>Refresh complete.</strong>
          <span>{status.repositoryCount} repositories are up to date.</span>
        </div>
      )}

      {statusError && (
        <div className="refresh-message refresh-message-error" role="alert">
          <strong>Refresh status unavailable.</strong>
          <span>{statusError}</span>
        </div>
      )}
    </section>
  )
}
