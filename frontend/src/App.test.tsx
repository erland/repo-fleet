import { renderToString } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import App from './App'
import { ComplianceOverviewPanel } from './ComplianceOverviewPanel'
import { InventoryRefreshPanel } from './InventoryRefreshPanel'
import { RefreshDiagnosticsPanel } from './RefreshDiagnosticsPanel'
import type { CompliancePortfolioSummary } from './api'
import { inventoryStatusFixture as inventoryStatus, repositoryFixture as repository } from './testFixtures'

describe('App', () => {
  it('renders the RepoFleet application shell', () => {
    const html = renderToString(<App />)

    expect(html).toContain('RepoFleet')
    expect(html).toContain('Repository portfolio management')
    expect(html).toContain('Checking authentication')
  })

  it('renders the authentication bootstrap in a semantic main region', () => {
    const html = renderToString(<App />)

    expect(html).toContain('<main')
    expect(html).toContain('Checking authentication')
  })
})

describe('InventoryRefreshPanel', () => {
  it('renders the idle state and refresh control', () => {
    const html = renderToString(
      <InventoryRefreshPanel status={null} statusError={null} refreshing={false} onRefresh={() => undefined} />,
    )

    expect(html).toContain('System status')
    expect(html).toContain('Not refreshed')
    expect(html).toContain('Last successful refresh:')
    expect(html).toContain('Never')
    expect(html).toContain('Refresh repositories')
  })

  it('renders a separate full refresh control when provided', () => {
    const html = renderToString(
      <InventoryRefreshPanel
        status={inventoryStatus()}
        statusError={null}
        refreshing={false}
        onRefresh={() => undefined}
        onFullRefresh={() => undefined}
      />,
    )

    expect(html).toContain('Full refresh')
  })

  it('renders refresh progress without hiding existing-data guidance', () => {
    const html = renderToString(
      <InventoryRefreshPanel
        status={inventoryStatus({
          state: 'RUNNING',
          processedCount: 3,
          totalCount: 10,
          successfulCount: 3,
          currentRepository: 'erland/repo-fleet',
        })}
        statusError={null}
        refreshing
        onRefresh={() => undefined}
      />,
    )

    expect(html).toContain('Refreshing')
    expect(html).toContain('3 of 10 repositories processed')
    expect(html).toContain('30%')
    expect(html).toContain('<details class="refresh-details" open=""')
    expect(html).toContain('erland/repo-fleet')
    expect(html).toContain('Existing repository data remains available')
  })

  it('shows a temporary GitHub rate-limit pause and resume time', () => {
    const html = renderToString(
      <InventoryRefreshPanel
        status={inventoryStatus({ state: 'RUNNING', totalCount: 10, processedCount: 4 })}
        statusError={null}
        refreshing
        diagnostics={{
          rateLimitRemaining: 0,
          rateLimitResetAt: '2026-09-19T06:00:00Z',
          rateLimitPaused: true,
          rateLimitResumeAt: '2026-09-19T06:00:01Z',
          rateLimitPauseReason: 'GitHub API rate limit reached',
          conditionalModifiedCount: 0,
          conditionalNotModifiedCount: 0,
          conditionalCachedFreshCount: 0,
          webhookTriggeredRefreshCount: 0,
          targetedFailedCount: 0,
          recentRuns: [],
          recentTargetedFailures: [],
        }}
        onRefresh={() => undefined}
      />,
    )

    expect(html).toContain('Temporarily paused by GitHub rate limiting')
    expect(html).toContain('Refresh will continue automatically')
  })

  it('renders success, partial failure and failed states', () => {
    const success = renderToString(
      <InventoryRefreshPanel status={inventoryStatus()} statusError={null} refreshing={false} onRefresh={() => undefined} />,
    )
    expect(success).toContain('Up to date')

    const partial = renderToString(
      <InventoryRefreshPanel
        status={inventoryStatus({ state: 'PARTIAL', successfulCount: 1, errorCount: 1 })}
        statusError={null}
        refreshing={false}
        onRefresh={() => undefined}
      />,
    )
    expect(partial).toContain('Needs attention')
    expect(partial).toContain('Refresh completed with warnings')
    expect(partial).toContain('Previously cached metadata may still be available')

    const failed = renderToString(
      <InventoryRefreshPanel
        status={inventoryStatus({
          state: 'FAILED',
          errorMessage: 'GitHub unavailable',
          successfulCount: 0,
          errorCount: 2,
        })}
        statusError={null}
        refreshing={false}
        onRefresh={() => undefined}
      />,
    )
    expect(failed).toContain('Refresh failed')
    expect(failed).toContain('GitHub unavailable')
  })

  it('associates refresh progress with its visible text label', () => {
    const html = renderToString(
      <InventoryRefreshPanel
        status={inventoryStatus({
          state: 'RUNNING',
          totalCount: 10,
          processedCount: 2,
          currentRepository: 'erland/repo-fleet',
        })}
        statusError={null}
        refreshing
        onRefresh={() => undefined}
      />,
    )

    expect(html).toContain('id="refresh-progress-label"')
    expect(html).toContain('aria-labelledby="refresh-progress-label"')
  })
})

describe('ComplianceOverviewPanel', () => {
  it('renders severity counts, filters, freshness and priority deviations', () => {
    const summary: CompliancePortfolioSummary = {
      repositoryCount: 2,
      evaluatedRuleCount: 4,
      resultCounts: { PASS: 2, FAIL: 1, UNKNOWN: 1, NOT_APPLICABLE: 0 },
      severityResultCounts: {
        REQUIRED: { PASS: 1, FAIL: 1, UNKNOWN: 0, NOT_APPLICABLE: 0 },
        RECOMMENDED: { PASS: 1, FAIL: 0, UNKNOWN: 1, NOT_APPLICABLE: 0 },
        INFORMATIONAL: { PASS: 0, FAIL: 0, UNKNOWN: 0, NOT_APPLICABLE: 0 },
      },
      groups: [{
        groupKey: 'services',
        groupName: 'Services',
        repositoryCount: 1,
        resultCounts: { PASS: 1, FAIL: 1, UNKNOWN: 0, NOT_APPLICABLE: 0 },
      }],
      repositoriesWithMostRequiredFailures: [{
        githubRepositoryId: 1001,
        fullName: 'erland/roman-nollpunkten',
        requiredFailureCount: 1,
      }],
      rules: [{
        ruleKey: 'license-required',
        ruleName: 'License required',
        severity: 'REQUIRED',
        resultCounts: { PASS: 1, FAIL: 1, UNKNOWN: 0, NOT_APPLICABLE: 0 },
      }],
    }

    const html = renderToString(
      <ComplianceOverviewPanel
        summary={summary}
        repositories={[{ ...repository, refreshStatus: { state: 'COMPLETE', message: null, freshness: 'STALE' } }]}
        loading={false}
        error={null}
      />,
    )

    expect(html).toContain('Compliance overview')
    expect(html).toContain('Required')
    expect(html).toContain('Recommended')
    expect(html).toContain('Informational')
    expect(html).toContain('All groups')
    expect(html).toContain('All rules')
    expect(html).toMatch(/1(?:<!-- -->)? stale/)
    expect(html).toContain('Highest-priority deviations')
    expect(html).toMatch(/1(?:<!-- -->)? required failure/)
  })
})

describe('RefreshDiagnosticsPanel', () => {
  it('renders API pressure, reuse and recent failures', () => {
    const html = renderToString(
      <RefreshDiagnosticsPanel
        loading={false}
        error={null}
        diagnostics={{
          rateLimitRemaining: 4321,
          rateLimitResetAt: '2026-09-18T18:00:00Z',
          rateLimitPaused: false,
          rateLimitResumeAt: null,
          rateLimitPauseReason: null,
          conditionalModifiedCount: 5,
          conditionalNotModifiedCount: 12,
          conditionalCachedFreshCount: 30,
          webhookTriggeredRefreshCount: 7,
          targetedFailedCount: 1,
          recentRuns: [{
            id: 1,
            triggerType: 'SCHEDULED_CONSISTENCY',
            startedAt: '2026-09-18T16:00:00Z',
            completedAt: '2026-09-18T16:02:00Z',
            durationMillis: 120000,
            finalState: 'COMPLETED',
            discoveredCount: 200,
            processedCount: 200,
            successfulCount: 200,
            errorCount: 0,
            reusedCount: 180,
            scheduledCount: 20,
            failureSummary: null,
          }],
          recentTargetedFailures: [{
            jobId: 9,
            githubRepositoryId: 1001,
            triggerType: 'WEBHOOK_RELEASES',
            attempts: 3,
            lastError: 'GitHub unavailable',
            completedAt: '2026-09-18T16:05:00Z',
          }],
        }}
      />,
    )

    expect(html).toContain('Refresh diagnostics')
    expect(html).toContain('4321')
    expect(html).toContain('12')
    expect(html).toContain('30')
    expect(html).toContain('fresh-cache skips')
    expect(html).toContain('SCHEDULED_CONSISTENCY')
    expect(html).toContain('180')
    expect(html).toContain('GitHub unavailable')
  })
})
