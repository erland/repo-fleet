export type RefreshRunDiagnostic = {
  id: number
  triggerType: string
  startedAt: string
  completedAt: string | null
  durationMillis: number | null
  finalState: string
  discoveredCount: number
  processedCount: number
  successfulCount: number
  errorCount: number
  reusedCount: number
  scheduledCount: number
  failureSummary: string | null
}

export type TargetedRefreshFailure = {
  jobId: number
  githubRepositoryId: number
  triggerType: string
  attempts: number
  lastError: string | null
  completedAt: string | null
}

export type RefreshDiagnosticsSnapshot = {
  rateLimitRemaining: number | null
  rateLimitResetAt: string | null
  rateLimitPaused: boolean
  rateLimitResumeAt: string | null
  rateLimitPauseReason: string | null
  conditionalModifiedCount: number
  conditionalNotModifiedCount: number
  conditionalCachedFreshCount: number
  webhookTriggeredRefreshCount: number
  targetedFailedCount: number
  recentRuns: RefreshRunDiagnostic[]
  recentTargetedFailures: TargetedRefreshFailure[]
}

export async function fetchRefreshDiagnostics(): Promise<RefreshDiagnosticsSnapshot> {
  const response = await fetch('/api/diagnostics/refresh')
  if (!response.ok) {
    throw new Error(`Refresh diagnostics request failed with HTTP ${response.status}`)
  }
  return response.json() as Promise<RefreshDiagnosticsSnapshot>
}
