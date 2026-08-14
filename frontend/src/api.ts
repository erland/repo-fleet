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
