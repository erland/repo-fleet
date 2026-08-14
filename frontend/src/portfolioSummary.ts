import type { RepositorySummary } from './api'

export type PortfolioSummary = {
  total: number
  archived: number
  forks: number
  missingLicense: number
  missingActions: number
  missingRelease: number
  javaRepositories: number
  unknownLicense: number
  unknownActions: number
  unknownRelease: number
}

export function summarizePortfolio(repositories: RepositorySummary[]): PortfolioSummary {
  return repositories.reduce<PortfolioSummary>((summary, repository) => {
    summary.total += 1
    if (repository.archived) summary.archived += 1
    if (repository.fork) summary.forks += 1
    if (repository.languages.some((language) => language.toLocaleLowerCase() === 'java')) {
      summary.javaRepositories += 1
    }

    if (repository.license.analysisState !== 'COMPLETE' || repository.license.presence === 'UNKNOWN') {
      summary.unknownLicense += 1
    } else if (repository.license.presence === 'MISSING') {
      summary.missingLicense += 1
    }

    if (repository.githubActions.analysisState !== 'COMPLETE' || repository.githubActions.workflowsPresent === null) {
      summary.unknownActions += 1
    } else if (!repository.githubActions.workflowsPresent) {
      summary.missingActions += 1
    }

    if (repository.release.analysisState !== 'COMPLETE' || repository.release.releasePresent === null) {
      summary.unknownRelease += 1
    } else if (!repository.release.releasePresent) {
      summary.missingRelease += 1
    }

    return summary
  }, {
    total: 0,
    archived: 0,
    forks: 0,
    missingLicense: 0,
    missingActions: 0,
    missingRelease: 0,
    javaRepositories: 0,
    unknownLicense: 0,
    unknownActions: 0,
    unknownRelease: 0,
  })
}
