import { describe, expect, it } from 'vitest'
import type { RepositorySummary } from './api'
import { emptyRepositoryFilters, filterRepositories } from './repositoryFilters'
import { sortRepositories } from './repositorySorting'

function repository(overrides: Partial<RepositorySummary> = {}): RepositorySummary {
  return {
    id: 1,
    owner: 'erland',
    name: 'repo-fleet',
    fullName: 'erland/repo-fleet',
    url: 'https://github.com/erland/repo-fleet',
    visibility: 'PRIVATE',
    archived: false,
    fork: false,
    defaultBranch: 'main',
    topics: ['portfolio', 'github'],
    languages: ['Java', 'TypeScript'],
    primaryLanguage: 'Java',
    license: { analysisState: 'COMPLETE', presence: 'PRESENT', recognized: true, key: 'mit', name: 'MIT License' },
    githubActions: { analysisState: 'COMPLETE', workflowsPresent: true, workflowCount: 2 },
    release: {
      analysisState: 'COMPLETE',
      releasePresent: true,
      latestReleaseName: 'v1',
      latestReleaseTag: 'v1.0.0',
      latestReleaseDate: '2026-08-01T10:00:00Z',
      latestReleasePrerelease: false,
    },
    activity: { pushedAt: '2026-08-10T10:00:00Z', updatedAt: '2026-08-10T10:00:00Z' },
    refreshStatus: { state: 'COMPLETE', message: null },
    ...overrides,
  }
}

describe('filterRepositories', () => {
  const repos = [
    repository(),
    repository({
      id: 2,
      owner: 'other',
      name: 'roman-alpha',
      fullName: 'other/roman-alpha',
      visibility: 'PUBLIC',
      archived: true,
      fork: true,
      topics: ['novel'],
      languages: ['Python'],
      primaryLanguage: 'Python',
      license: { analysisState: 'COMPLETE', presence: 'MISSING', recognized: false, key: null, name: null },
      githubActions: { analysisState: 'COMPLETE', workflowsPresent: false, workflowCount: 0 },
      release: {
        analysisState: 'COMPLETE',
        releasePresent: false,
        latestReleaseName: null,
        latestReleaseTag: null,
        latestReleaseDate: null,
        latestReleasePrerelease: null,
      },
      activity: { pushedAt: '2025-01-01T10:00:00Z', updatedAt: '2025-01-01T10:00:00Z' },
    }),
  ]

  it('supports name contains, prefix and owner filters', () => {
    expect(filterRepositories(repos, { ...emptyRepositoryFilters, nameContains: 'fleet' })).toHaveLength(1)
    expect(filterRepositories(repos, { ...emptyRepositoryFilters, namePrefix: 'roman-' })).toHaveLength(1)
    expect(filterRepositories(repos, { ...emptyRepositoryFilters, owner: 'OTHER' })).toHaveLength(1)
  })

  it('supports visibility, archive and fork filters', () => {
    const result = filterRepositories(repos, {
      ...emptyRepositoryFilters,
      visibility: 'PUBLIC',
      archived: 'YES',
      fork: 'YES',
    })
    expect(result.map((item) => item.name)).toEqual(['roman-alpha'])
  })

  it('supports topic and language presence and absence', () => {
    expect(filterRepositories(repos, {
      ...emptyRepositoryFilters,
      topic: 'novel',
      topicPresence: 'PRESENT',
    })).toHaveLength(1)

    expect(filterRepositories(repos, {
      ...emptyRepositoryFilters,
      language: 'Java',
      languagePresence: 'MISSING',
    }).map((item) => item.name)).toEqual(['roman-alpha'])
  })

  it('supports license, Actions and release presence filters', () => {
    const result = filterRepositories(repos, {
      ...emptyRepositoryFilters,
      license: 'MISSING',
      actions: 'MISSING',
      release: 'MISSING',
    })
    expect(result.map((item) => item.name)).toEqual(['roman-alpha'])
  })

  it('does not treat unknown analysis as missing', () => {
    const unknown = repository({
      id: 3,
      license: { analysisState: 'FAILED', presence: 'UNKNOWN', recognized: null, key: null, name: null },
      githubActions: { analysisState: 'FAILED', workflowsPresent: null, workflowCount: null },
      release: {
        analysisState: 'FAILED',
        releasePresent: null,
        latestReleaseName: null,
        latestReleaseTag: null,
        latestReleaseDate: null,
        latestReleasePrerelease: null,
      },
    })

    expect(filterRepositories([unknown], { ...emptyRepositoryFilters, license: 'MISSING' })).toHaveLength(0)
    expect(filterRepositories([unknown], { ...emptyRepositoryFilters, actions: 'MISSING' })).toHaveLength(0)
    expect(filterRepositories([unknown], { ...emptyRepositoryFilters, release: 'MISSING' })).toHaveLength(0)
  })

  it('supports recent activity windows', () => {
    const result = filterRepositories(
      repos,
      { ...emptyRepositoryFilters, activityAge: '30_DAYS' },
      new Date('2026-08-14T12:00:00Z'),
    )
    expect(result.map((item) => item.name)).toEqual(['repo-fleet'])
  })

  it('combines filter categories using AND semantics', () => {
    const result = filterRepositories(repos, {
      ...emptyRepositoryFilters,
      namePrefix: 'repo',
      topic: 'github',
      topicPresence: 'PRESENT',
      language: 'Java',
      languagePresence: 'PRESENT',
      license: 'PRESENT',
      actions: 'PRESENT',
      release: 'PRESENT',
    })
    expect(result.map((item) => item.name)).toEqual(['repo-fleet'])
  })

  it('works together with sorting after filtering', () => {
    const filtered = filterRepositories(repos, {
      ...emptyRepositoryFilters,
      visibility: 'PRIVATE',
    })
    const sorted = sortRepositories(filtered, { field: 'name', direction: 'DESC' })

    expect(sorted.map((item) => item.name)).toEqual(['repo-fleet'])
  })

})
