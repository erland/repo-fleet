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
