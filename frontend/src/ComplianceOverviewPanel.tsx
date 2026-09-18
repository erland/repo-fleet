import { useEffect, useMemo, useState } from 'react'
import type {
  CompliancePortfolioSummary,
  ComplianceRuleDetail,
  ComplianceResultCounts,
  RepositorySummary,
  fetchComplianceRuleDetail,
} from './api'

type ComplianceOverviewPanelProps = {
  summary: CompliancePortfolioSummary | null
  repositories: RepositorySummary[]
  loading: boolean
  error: string | null
}

const emptyCounts: ComplianceResultCounts = {
  PASS: 0,
  FAIL: 0,
  UNKNOWN: 0,
  NOT_APPLICABLE: 0,
}

function CountCard({ label, counts }: { label: string; counts: ComplianceResultCounts }) {
  return (
    <div className="compliance-count-card">
      <h3>{label}</h3>
      <dl>
        <div><dt>Fail</dt><dd>{counts.FAIL}</dd></div>
        <div><dt>Unknown</dt><dd>{counts.UNKNOWN}</dd></div>
        <div><dt>Pass</dt><dd>{counts.PASS}</dd></div>
        <div><dt>N/A</dt><dd>{counts.NOT_APPLICABLE}</dd></div>
      </dl>
    </div>
  )
}

export function ComplianceOverviewPanel({ summary, repositories, loading, error }: ComplianceOverviewPanelProps) {
  const [groupKey, setGroupKey] = useState('')
  const [ruleKey, setRuleKey] = useState('')
  const [ruleDetail, setRuleDetail] = useState<ComplianceRuleDetail | null>(null)
  const [ruleDetailLoading, setRuleDetailLoading] = useState(false)
  const [ruleDetailError, setRuleDetailError] = useState<string | null>(null)

  const selectedCounts = useMemo(() => {
    if (!summary) return emptyCounts
    if (ruleKey) return summary.rules.find((rule) => rule.ruleKey === ruleKey)?.resultCounts ?? emptyCounts
    if (groupKey) return summary.groups.find((group) => group.groupKey === groupKey)?.resultCounts ?? emptyCounts
    return summary.resultCounts
  }, [groupKey, ruleKey, summary])

  useEffect(() => {
    if (!ruleKey) {
      setRuleDetail(null)
      setRuleDetailError(null)
      setRuleDetailLoading(false)
      return
    }

    let active = true
    setRuleDetailLoading(true)
    setRuleDetailError(null)
    void fetchComplianceRuleDetail(ruleKey)
      .then((detail) => {
        if (!active) return
        setRuleDetail(detail)
      })
      .catch(() => {
        if (!active) return
        setRuleDetailError('Rule detail could not be loaded.')
        setRuleDetail(null)
      })
      .finally(() => {
        if (active) setRuleDetailLoading(false)
      })

    return () => { active = false }
  }, [ruleKey])

  const staleCount = repositories.filter((repository) => repository.refreshStatus?.freshness === 'STALE').length
  const refreshingCount = repositories.filter((repository) => repository.refreshStatus?.freshness === 'REFRESHING').length

  const selectedLabel = ruleKey
    ? summary?.rules.find((rule) => rule.ruleKey === ruleKey)?.ruleName ?? 'Selected rule'
    : groupKey
      ? summary?.groups.find((group) => group.groupKey === groupKey)?.groupName ?? 'Selected group'
      : 'Portfolio · ' + (summary?.repositoryCount ?? 0) + ' repositories'

  return (
    <section className="compliance-panel" aria-labelledby="compliance-heading">
      <div className="compliance-heading">
        <div>
          <p className="eyebrow">Repository standards</p>
          <h2 id="compliance-heading">Compliance overview</h2>
          <p className="compliance-help">Focus first on required-rule failures and unknown results that need fresh data.</p>
        </div>
        <div className="compliance-freshness" aria-label="Compliance data freshness">
          <span>{staleCount} stale</span>
          <span>{refreshingCount} refreshing</span>
        </div>
      </div>

      {loading && <p role="status">Loading compliance summary…</p>}
      {error && <p className="compliance-error" role="alert">{error}</p>}

      {summary && (
        <>
          <div className="compliance-filter-row">
            <label>
              Group
              <select value={groupKey} onChange={(event) => { setGroupKey(event.target.value); setRuleKey('') }}>
                <option value="">All groups</option>
                {summary.groups.map((group) => <option key={group.groupKey} value={group.groupKey}>{group.groupName}</option>)}
              </select>
            </label>

            <label>
              Rule
              <select value={ruleKey} onChange={(event) => { setRuleKey(event.target.value); setGroupKey('') }}>
                <option value="">All rules</option>
                {summary.rules.map((rule) => <option key={rule.ruleKey} value={rule.ruleKey}>{rule.ruleName}</option>)}
              </select>
            </label>
          </div>

          <div className="compliance-selected-summary">
            <CountCard label={selectedLabel} counts={selectedCounts} />
          </div>

          <div className="compliance-severity-grid">
            <CountCard label="Required" counts={summary.severityResultCounts.REQUIRED} />
            <CountCard label="Recommended" counts={summary.severityResultCounts.RECOMMENDED} />
            <CountCard label="Informational" counts={summary.severityResultCounts.INFORMATIONAL} />
          </div>

          {ruleKey && (
            <div className="compliance-rule-detail">
              {ruleDetailLoading && <p role="status">Loading rule detail…</p>}
              {ruleDetailError && <p className="compliance-error" role="alert">{ruleDetailError}</p>}
              {ruleDetail && (
                <>
                  <div className="compliance-rule-detail-heading">
                    <div>
                      <h3>{ruleDetail.ruleName}</h3>
                      <p>{ruleDetail.description ?? 'No description.'}</p>
                    </div>
                    <span>{ruleDetail.severity}</span>
                  </div>
                  <dl className="compliance-rule-meta">
                    <div><dt>Scope</dt><dd>{ruleDetail.scope}</dd></div>
                    <div><dt>Groups</dt><dd>{ruleDetail.groups.length ? ruleDetail.groups.join(', ') : 'All repositories'}</dd></div>
                  </dl>

                  <div className="compliance-rule-affected">
                    <h4>Affected repositories</h4>
                    {ruleDetail.affectedRepositories.length === 0 ? (
                      <p>No failed or unknown repositories for this rule.</p>
                    ) : (
                      <ul>
                        {ruleDetail.affectedRepositories.map((repository) => (
                          <li key={repository.githubRepositoryId}>
                            <div>
                              <strong>{repository.fullName}</strong>
                              <span>{repository.result}</span>
                            </div>
                            <p>{repository.reason}</p>
                            {repository.observedValue && <small>Observed: {repository.observedValue}</small>}
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                </>
              )}
            </div>
          )}

          {summary.repositoriesWithMostRequiredFailures.length > 0 && (
            <div className="compliance-priority-list">
              <h3>Highest-priority deviations</h3>
              <ol>
                {summary.repositoriesWithMostRequiredFailures.slice(0, 5).map((repository) => (
                  <li key={repository.githubRepositoryId}>
                    <span>{repository.fullName}</span>
                    <strong>{repository.requiredFailureCount} required failure{repository.requiredFailureCount === 1 ? '' : 's'}</strong>
                  </li>
                ))}
              </ol>
            </div>
          )}
        </>
      )}
    </section>
  )
}
