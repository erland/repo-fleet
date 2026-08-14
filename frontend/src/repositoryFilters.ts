import type { LicensePresence, RepositorySummary, RepositoryVisibility } from './api'

export type PresenceFilter = 'ANY' | 'PRESENT' | 'MISSING'
export type BooleanFilter = 'ANY' | 'YES' | 'NO'
export type ActivityAgeFilter = 'ANY' | '7_DAYS' | '30_DAYS' | '90_DAYS' | '365_DAYS'

export type RepositoryFilters = {
  nameContains: string
  namePrefix: string
  owner: string
  visibility: 'ANY' | RepositoryVisibility
  archived: BooleanFilter
  fork: BooleanFilter
  topic: string
  topicPresence: PresenceFilter
  language: string
  languagePresence: PresenceFilter
  license: PresenceFilter
  actions: PresenceFilter
  release: PresenceFilter
  activityAge: ActivityAgeFilter
}

export const emptyRepositoryFilters: RepositoryFilters = {
  nameContains: '',
  namePrefix: '',
  owner: '',
  visibility: 'ANY',
  archived: 'ANY',
  fork: 'ANY',
  topic: '',
  topicPresence: 'ANY',
  language: '',
  languagePresence: 'ANY',
  license: 'ANY',
  actions: 'ANY',
  release: 'ANY',
  activityAge: 'ANY',
}

function normalize(value: string): string {
  return value.trim().toLocaleLowerCase()
}

function matchesBoolean(value: boolean, filter: BooleanFilter): boolean {
  if (filter === 'ANY') return true
  return filter === 'YES' ? value : !value
}

function matchesPresence(actual: boolean | null, filter: PresenceFilter): boolean {
  if (filter === 'ANY') return true
  if (actual === null) return false
  return filter === 'PRESENT' ? actual : !actual
}

function licensePresent(repository: RepositorySummary): boolean | null {
  if (repository.license.analysisState !== 'COMPLETE') return null
  if (repository.license.presence === 'UNKNOWN') return null
  return repository.license.presence === 'PRESENT'
}

function actionsPresent(repository: RepositorySummary): boolean | null {
  if (repository.githubActions.analysisState !== 'COMPLETE') return null
  return repository.githubActions.workflowsPresent
}

function releasePresent(repository: RepositorySummary): boolean | null {
  if (repository.release.analysisState !== 'COMPLETE') return null
  return repository.release.releasePresent
}

function activityTimestamp(repository: RepositorySummary): number | null {
  const raw = repository.activity.pushedAt ?? repository.activity.updatedAt
  if (!raw) return null

  const parsed = Date.parse(raw)
  return Number.isNaN(parsed) ? null : parsed
}

function activityAgeDays(filter: ActivityAgeFilter): number | null {
  switch (filter) {
    case '7_DAYS': return 7
    case '30_DAYS': return 30
    case '90_DAYS': return 90
    case '365_DAYS': return 365
    default: return null
  }
}

export function filterRepositories(
  repositories: RepositorySummary[],
  filters: RepositoryFilters,
  now: Date = new Date(),
): RepositorySummary[] {
  const contains = normalize(filters.nameContains)
  const prefix = normalize(filters.namePrefix)
  const owner = normalize(filters.owner)
  const topic = normalize(filters.topic)
  const language = normalize(filters.language)
  const maxActivityAge = activityAgeDays(filters.activityAge)
  const cutoff = maxActivityAge === null ? null : now.getTime() - maxActivityAge * 24 * 60 * 60 * 1000

  return repositories.filter((repository) => {
    const repositoryName = normalize(repository.name)
    if (contains && !repositoryName.includes(contains)) return false
    if (prefix && !repositoryName.startsWith(prefix)) return false
    if (owner && normalize(repository.owner) !== owner) return false
    if (filters.visibility !== 'ANY' && repository.visibility !== filters.visibility) return false
    if (!matchesBoolean(repository.archived, filters.archived)) return false
    if (!matchesBoolean(repository.fork, filters.fork)) return false

    if (filters.topicPresence !== 'ANY') {
      if (!topic) return false
      const present = repository.topics.some((item) => normalize(item) === topic)
      if (!matchesPresence(present, filters.topicPresence)) return false
    } else if (topic && !repository.topics.some((item) => normalize(item) === topic)) {
      return false
    }

    if (filters.languagePresence !== 'ANY') {
      if (!language) return false
      const present = repository.languages.some((item) => normalize(item) === language)
      if (!matchesPresence(present, filters.languagePresence)) return false
    } else if (language && !repository.languages.some((item) => normalize(item) === language)) {
      return false
    }

    if (!matchesPresence(licensePresent(repository), filters.license)) return false
    if (!matchesPresence(actionsPresent(repository), filters.actions)) return false
    if (!matchesPresence(releasePresent(repository), filters.release)) return false

    if (cutoff !== null) {
      const timestamp = activityTimestamp(repository)
      if (timestamp === null || timestamp < cutoff) return false
    }

    return true
  })
}

export function hasActiveRepositoryFilters(filters: RepositoryFilters): boolean {
  return Object.entries(filters).some(([key, value]) => value !== emptyRepositoryFilters[key as keyof RepositoryFilters])
}
