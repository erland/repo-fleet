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

export type RepositoryRefreshStatus = {
  state: AnalysisState
  message: string | null
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
  currentRepository: string | null
  running: boolean
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
