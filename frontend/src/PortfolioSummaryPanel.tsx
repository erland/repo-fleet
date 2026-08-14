import type { PortfolioSummary } from './portfolioSummary'

type PortfolioSummaryPanelProps = {
  summary: PortfolioSummary
  totalPortfolioCount: number
}

type IndicatorProps = {
  label: string
  value: number
  detail?: string
}

function Indicator({ label, value, detail }: IndicatorProps) {
  return (
    <div className="summary-card">
      <dt className="summary-label">{label}</dt>
      <dd className="summary-value">{value}</dd>
      {detail && <dd className="summary-detail">{detail}</dd>}
    </div>
  )
}

export function PortfolioSummaryPanel({
  summary,
  totalPortfolioCount,
}: PortfolioSummaryPanelProps) {
  const scopeLabel = summary.total === totalPortfolioCount
    ? `Summary for all ${totalPortfolioCount} repositories`
    : `Summary for ${summary.total} filtered repositories out of ${totalPortfolioCount}`

  return (
    <section className="summary-panel" aria-labelledby="summary-heading">
      <div className="summary-heading">
        <div>
          <p className="eyebrow">Portfolio signals</p>
          <h2 id="summary-heading">Summary</h2>
        </div>
        <span className="summary-scope">{scopeLabel}</span>
      </div>

      <dl className="summary-grid">
        <Indicator label="Repositories" value={summary.total} />
        <Indicator label="Missing LICENSE" value={summary.missingLicense} detail={summary.unknownLicense ? `${summary.unknownLicense} unknown` : undefined} />
        <Indicator label="Missing Actions" value={summary.missingActions} detail={summary.unknownActions ? `${summary.unknownActions} unknown` : undefined} />
        <Indicator label="Missing release" value={summary.missingRelease} detail={summary.unknownRelease ? `${summary.unknownRelease} unknown` : undefined} />
        <Indicator label="Java repositories" value={summary.javaRepositories} />
        <Indicator label="Archived" value={summary.archived} />
        <Indicator label="Forks" value={summary.forks} />
      </dl>
    </section>
  )
}
