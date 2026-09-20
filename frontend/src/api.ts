export type ServiceStatus = {
  service: string
  status: string
}

export type AnalysisState = 'NOT_ANALYZED' | 'COMPLETE' | 'PARTIAL' | 'FAILED'
export type RepositoryVisibility = 'PUBLIC' | 'PRIVATE' | 'INTERNAL'
export type LicensePresence = 'PRESENT' | 'MISSING' | 'UNKNOWN'

export type LicenseStatus = {
  analysisState: AnalysisState
  presence: LicensePresence
  recognized: boolean | null
  key: string | null
  name: string | null
}

export type GitHubActionsStatus = {
  analysisState: AnalysisState
  workflowsPresent: boolean | null
  workflowCount: number | null
}

export type ReleaseStatus = {
  analysisState: AnalysisState
  releasePresent: boolean | null
  latestReleaseName: string | null
  latestReleaseTag: string | null
  latestReleaseDate: string | null
  latestReleasePrerelease: boolean | null
}

export type ActivityStatus = {
  pushedAt: string | null
  updatedAt: string | null
}

export type CacheFreshness = 'FRESH' | 'STALE' | 'REFRESHING'

export type RepositoryRefreshStatus = {
  state: AnalysisState
  message: string | null
  freshness?: CacheFreshness | null
}

export type RepositorySummary = {
  id: number
  owner: string
  name: string
  fullName: string
  url: string
  visibility: RepositoryVisibility
  archived: boolean
  fork: boolean
  defaultBranch: string
  topics: string[]
  languages: string[]
  primaryLanguage: string | null
  license: LicenseStatus
  githubActions: GitHubActionsStatus
  release: ReleaseStatus
  activity: ActivityStatus
  refreshStatus: RepositoryRefreshStatus
}

export async function fetchServiceStatus(): Promise<ServiceStatus> {
  const response = await fetch('/api/status')

  if (!response.ok) {
    throw new Error(`Backend status request failed with HTTP ${response.status}`)
  }

  return response.json() as Promise<ServiceStatus>
}

export async function fetchRepositories(): Promise<RepositorySummary[]> {
  const response = await fetch('/api/repositories')

  if (!response.ok) {
    throw new Error(`Repository request failed with HTTP ${response.status}`)
  }

  return response.json() as Promise<RepositorySummary[]>
}

export type InventoryRefreshState = 'NOT_STARTED' | 'RUNNING' | 'COMPLETED' | 'PARTIAL' | 'FAILED'

export type InventoryStatus = {
  state: InventoryRefreshState
  lastAttemptAt: string | null
  lastSuccessfulRefreshAt: string | null
  completedAt: string | null
  errorMessage: string | null
  repositoryCount: number
  totalCount: number
  processedCount: number
  successfulCount: number
  errorCount: number
  reusedCount: number
  newCount: number
  changedCount: number
  scheduledCount: number
  currentRepository: string | null
}

export async function fetchInventoryStatus(): Promise<InventoryStatus> {
  const response = await fetch('/api/inventory/status')

  if (!response.ok) {
    throw new Error(`Inventory status request failed with HTTP ${response.status}`)
  }

  return response.json() as Promise<InventoryStatus>
}

export async function startInventoryRefresh(): Promise<InventoryStatus> {
  const response = await fetch('/api/inventory/refresh', { method: 'POST' })

  if (!response.ok) {
    throw new Error(`Inventory refresh request failed with HTTP ${response.status}`)
  }

  return response.json() as Promise<InventoryStatus>
}

export async function startFullInventoryRefresh(): Promise<InventoryStatus> {
  const response = await fetch('/api/inventory/refresh/full', { method: 'POST' })

  if (!response.ok) {
    throw new Error(`Full inventory refresh request failed with HTTP ${response.status}`)
  }

  return response.json() as Promise<InventoryStatus>
}


export type AuthenticatedUser = {
  login: string
  name: string | null
  avatarUrl: string | null
}

export type AuthSession = {
  authEnabled: boolean
  authenticated: boolean
  user: AuthenticatedUser | null
}

export async function fetchAuthSession(): Promise<AuthSession> {
  const response = await fetch('/api/auth/session', { credentials: 'same-origin' })
  if (!response.ok) throw new Error(`Authentication session request failed with HTTP ${response.status}`)
  return response.json() as Promise<AuthSession>
}

export async function logout(): Promise<void> {
  const response = await fetch('/api/auth/logout', { method: 'POST', credentials: 'same-origin' })
  if (!response.ok) throw new Error(`Logout request failed with HTTP ${response.status}`)
}

export type ComplianceResult = 'PASS' | 'FAIL' | 'UNKNOWN' | 'NOT_APPLICABLE'
export type ComplianceSeverity = 'REQUIRED' | 'RECOMMENDED' | 'INFORMATIONAL'

export type ComplianceResultCounts = Record<ComplianceResult, number>

export type ComplianceGroupSummary = {
  groupKey: string
  groupName: string
  repositoryCount: number
  acceptedDeviationCount?: number
  resultCounts: ComplianceResultCounts
}

export type ComplianceRepositoryFailureSummary = {
  githubRepositoryId: number
  fullName: string
  requiredFailureCount: number
}

export type ComplianceRuleSummary = {
  ruleKey: string
  ruleName: string
  severity: ComplianceSeverity
  acceptedDeviationCount?: number
  resultCounts: ComplianceResultCounts
}

export type CompliancePortfolioSummary = {
  repositoryCount: number
  evaluatedRuleCount: number
  acceptedDeviationCount?: number
  resultCounts: ComplianceResultCounts
  severityResultCounts: Record<ComplianceSeverity, ComplianceResultCounts>
  groups: ComplianceGroupSummary[]
  repositoriesWithMostRequiredFailures: ComplianceRepositoryFailureSummary[]
  rules: ComplianceRuleSummary[]
}

export async function fetchComplianceSummary(): Promise<CompliancePortfolioSummary> {
  const response = await fetch('/api/compliance/summary')

  if (!response.ok) {
    throw new Error(`Compliance summary request failed with HTTP ${response.status}`)
  }

  return response.json() as Promise<CompliancePortfolioSummary>
}

export type RepositoryComplianceDetail = {
  ruleKey: string
  ruleName: string
  ruleType: string
  severity: ComplianceSeverity
  result: ComplianceResult
  reason: string
  observedValue: string | null
  evaluatedAt: string
  acceptedDeviation?: boolean
  exceptionReason?: string | null
  exceptionExpiresAt?: string | null
}

export async function fetchRepositoryCompliance(
  repositoryId: number,
): Promise<RepositoryComplianceDetail[]> {
  const response = await fetch('/api/compliance/repositories/' + repositoryId)

  if (!response.ok) {
    throw new Error(`Repository compliance request failed with HTTP ${response.status}`)
  }

  return response.json() as Promise<RepositoryComplianceDetail[]>
}

export type ComplianceRuleAffectedRepository = {
  githubRepositoryId: number
  fullName: string
  result: ComplianceResult
  reason: string
  observedValue: string | null
  acceptedDeviation?: boolean
  exceptionReason?: string | null
  exceptionExpiresAt?: string | null
}

export type ComplianceRuleDetail = {
  ruleKey: string
  ruleName: string
  description: string | null
  ruleType: string
  severity: ComplianceSeverity
  scope: 'ALL_REPOSITORIES' | 'SELECTED_GROUPS'
  parameters: Record<string, unknown>
  groups: string[]
  resultCounts: ComplianceResultCounts
  affectedRepositories: ComplianceRuleAffectedRepository[]
}

export async function fetchComplianceRuleDetail(
  ruleKey: string,
): Promise<ComplianceRuleDetail> {
  const response = await fetch('/api/compliance/rules/' + encodeURIComponent(ruleKey))

  if (!response.ok) {
    throw new Error(`Compliance rule detail request failed with HTTP ${response.status}`)
  }

  return response.json() as Promise<ComplianceRuleDetail>
}

export type RepositoryComplianceException = {
  githubRepositoryId: number
  ruleKey: string
  reason: string
  expiresAt: string | null
  state: 'ACTIVE' | 'EXPIRED'
  createdAt: string
  updatedAt: string
}

export async function saveRepositoryComplianceException(
  repositoryId: number,
  ruleKey: string,
  reason: string,
  expiresAt: string | null,
): Promise<RepositoryComplianceException> {
  const response = await fetch(
    '/api/compliance/repositories/' + repositoryId + '/exceptions/' + encodeURIComponent(ruleKey),
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ reason, expiresAt }),
    },
  )

  if (!response.ok) {
    throw new Error(`Compliance exception save failed with HTTP ${response.status}`)
  }

  return response.json() as Promise<RepositoryComplianceException>
}

export async function expireRepositoryComplianceException(
  repositoryId: number,
  ruleKey: string,
): Promise<void> {
  const response = await fetch(
    '/api/compliance/repositories/' + repositoryId + '/exceptions/' + encodeURIComponent(ruleKey) + '/expire',
    { method: 'POST' },
  )
  if (!response.ok) {
    throw new Error(`Compliance exception expire failed with HTTP ${response.status}`)
  }
}

export async function removeRepositoryComplianceException(
  repositoryId: number,
  ruleKey: string,
): Promise<void> {
  const response = await fetch(
    '/api/compliance/repositories/' + repositoryId + '/exceptions/' + encodeURIComponent(ruleKey),
    { method: 'DELETE' },
  )
  if (!response.ok) {
    throw new Error(`Compliance exception delete failed with HTTP ${response.status}`)
  }
}


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
