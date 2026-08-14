import { describe, expect, it } from 'vitest'
import type { RepositorySummary } from './api'
import { sortRepositories } from './repositorySorting'

function repository(overrides: Partial<RepositorySummary> = {}): RepositorySummary {
  return {
    id: 1,
    owner: 'erland',
    name: 'beta',
    fullName: 'erland/beta',
    url: 'https://github.com/erland/beta',
    visibility: 'PRIVATE',
    archived: false,
    fork: false,
    defaultBranch: 'main',
    topics: [],
    languages: ['Java'],
    primaryLanguage: 'Java',
    license: { analysisState: 'COMPLETE', presence: 'PRESENT', recognized: true, key: 'mit', name: 'MIT License' },
    githubActions: { analysisState: 'COMPLETE', workflowsPresent: true, workflowCount: 1 },
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

describe('sortRepositories', () => {
  const repos = [
    repository({ id: 1, name: 'beta', owner: 'zeta', primaryLanguage: 'TypeScript', activity: { pushedAt: '2026-08-10T10:00:00Z', updatedAt: null } }),
    repository({
      id: 2,
      name: 'alpha',
      owner: 'alpha',
      primaryLanguage: 'Java',
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
      activity: { pushedAt: '2026-07-01T10:00:00Z', updatedAt: null },
    }),
    repository({
      id: 3,
      name: 'gamma',
      owner: 'beta',
      primaryLanguage: null,
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
      activity: { pushedAt: null, updatedAt: null },
    }),
  ]

  it('sorts by name and owner', () => {
    expect(sortRepositories(repos, { field: 'name', direction: 'ASC' }).map((item) => item.name))
      .toEqual(['alpha', 'beta', 'gamma'])
    expect(sortRepositories(repos, { field: 'owner', direction: 'DESC' }).map((item) => item.owner))
      .toEqual(['zeta', 'beta', 'alpha'])
  })

  it('sorts by activity and primary language', () => {
    expect(sortRepositories(repos, { field: 'activity', direction: 'DESC' }).map((item) => item.name))
      .toEqual(['beta', 'alpha', 'gamma'])
    expect(sortRepositories(repos, { field: 'primaryLanguage', direction: 'ASC' }).map((item) => item.name))
      .toEqual(['gamma', 'alpha', 'beta'])
  })

  it('sorts known present/missing/unknown maintenance states predictably', () => {
    expect(sortRepositories(repos, { field: 'license', direction: 'ASC' }).map((item) => item.name))
      .toEqual(['beta', 'alpha', 'gamma'])
    expect(sortRepositories(repos, { field: 'actions', direction: 'ASC' }).map((item) => item.name))
      .toEqual(['beta', 'alpha', 'gamma'])
    expect(sortRepositories(repos, { field: 'release', direction: 'ASC' }).map((item) => item.name))
      .toEqual(['beta', 'alpha', 'gamma'])
  })

  it('does not mutate the input array and applies deterministic tie-breaking', () => {
    const input = [
      repository({ id: 5, name: 'zulu', owner: 'same' }),
      repository({ id: 6, name: 'alpha', owner: 'same' }),
    ]
    const snapshot = [...input]

    const result = sortRepositories(input, { field: 'owner', direction: 'ASC' })

    expect(input).toEqual(snapshot)
    expect(result.map((item) => item.name)).toEqual(['alpha', 'zulu'])
  })
})
