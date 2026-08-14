import { describe, expect, it } from 'vitest'
import type { RepositorySummary } from './api'
import { summarizePortfolio } from './portfolioSummary'

function repository(overrides: Partial<RepositorySummary> = {}): RepositorySummary {
  return {
    id: 1,
    owner: 'erland',
    name: 'repo',
    fullName: 'erland/repo',
    url: 'https://github.com/erland/repo',
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
    activity: { pushedAt: '2026-08-10T10:00:00Z', updatedAt: null },
    refreshStatus: { state: 'COMPLETE', message: null },
    ...overrides,
  }
}

describe('summarizePortfolio', () => {
  it('counts the central Phase 1 portfolio indicators', () => {
    const summary = summarizePortfolio([
      repository(),
      repository({
        id: 2,
        archived: true,
        fork: true,
        languages: ['TypeScript'],
        primaryLanguage: 'TypeScript',
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
      }),
    ])

    expect(summary).toMatchObject({
      total: 2,
      archived: 1,
      forks: 1,
      missingLicense: 1,
      missingActions: 1,
      missingRelease: 1,
      javaRepositories: 1,
    })
  })

  it('keeps unknown analysis separate from known missing capabilities', () => {
    const summary = summarizePortfolio([
      repository({
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
      }),
    ])

    expect(summary.missingLicense).toBe(0)
    expect(summary.missingActions).toBe(0)
    expect(summary.missingRelease).toBe(0)
    expect(summary.unknownLicense).toBe(1)
    expect(summary.unknownActions).toBe(1)
    expect(summary.unknownRelease).toBe(1)
  })

  it('matches Java case-insensitively across language lists', () => {
    expect(summarizePortfolio([
      repository({ languages: ['java'] }),
      repository({ id: 2, languages: ['TypeScript'] }),
    ]).javaRepositories).toBe(1)
  })
})
