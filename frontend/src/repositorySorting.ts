import type { RepositorySummary } from './api'

export type RepositorySortField =
  | 'name'
  | 'owner'
  | 'activity'
  | 'primaryLanguage'
  | 'license'
  | 'actions'
  | 'release'

export type SortDirection = 'ASC' | 'DESC'

export type RepositorySort = {
  field: RepositorySortField
  direction: SortDirection
}

export const defaultRepositorySort: RepositorySort = {
  field: 'name',
  direction: 'ASC',
}

function normalize(value: string | null | undefined): string {
  return (value ?? '').trim().toLocaleLowerCase()
}

function activityTimestamp(repository: RepositorySummary): number {
  const raw = repository.activity.pushedAt ?? repository.activity.updatedAt
  if (!raw) return Number.NEGATIVE_INFINITY
  const parsed = Date.parse(raw)
  return Number.isNaN(parsed) ? Number.NEGATIVE_INFINITY : parsed
}

function licenseRank(repository: RepositorySummary): number {
  if (repository.license.analysisState !== 'COMPLETE') return 2
  if (repository.license.presence === 'PRESENT') return 0
  if (repository.license.presence === 'MISSING') return 1
  return 2
}

function actionsRank(repository: RepositorySummary): number {
  if (repository.githubActions.analysisState !== 'COMPLETE' || repository.githubActions.workflowsPresent === null) return 2
  return repository.githubActions.workflowsPresent ? 0 : 1
}

function releaseRank(repository: RepositorySummary): number {
  if (repository.release.analysisState !== 'COMPLETE' || repository.release.releasePresent === null) return 2
  return repository.release.releasePresent ? 0 : 1
}

function compareStrings(a: string, b: string): number {
  return a.localeCompare(b, undefined, { sensitivity: 'base', numeric: true })
}

function compareByField(a: RepositorySummary, b: RepositorySummary, field: RepositorySortField): number {
  switch (field) {
    case 'owner':
      return compareStrings(normalize(a.owner), normalize(b.owner))
    case 'activity':
      return activityTimestamp(a) - activityTimestamp(b)
    case 'primaryLanguage':
      return compareStrings(normalize(a.primaryLanguage), normalize(b.primaryLanguage))
    case 'license':
      return licenseRank(a) - licenseRank(b)
    case 'actions':
      return actionsRank(a) - actionsRank(b)
    case 'release':
      return releaseRank(a) - releaseRank(b)
    case 'name':
    default:
      return compareStrings(normalize(a.name), normalize(b.name))
  }
}

export function sortRepositories(
  repositories: RepositorySummary[],
  sort: RepositorySort,
): RepositorySummary[] {
  const direction = sort.direction === 'ASC' ? 1 : -1

  return repositories
    .map((repository, index) => ({ repository, index }))
    .sort((a, b) => {
      const primary = compareByField(a.repository, b.repository, sort.field) * direction
      if (primary !== 0) return primary

      const nameFallback = compareStrings(normalize(a.repository.name), normalize(b.repository.name))
      if (nameFallback !== 0) return nameFallback

      return a.index - b.index
    })
    .map(({ repository }) => repository)
}
