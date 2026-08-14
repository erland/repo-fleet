import { describe, expect, it } from 'vitest'
import type { RepositorySummary } from './api'
import {
  clearRepositorySelection,
  deselectVisibleRepositories,
  selectVisibleRepositories,
  selectedVisibleCount,
  toggleRepositorySelection,
} from './repositorySelection'

function repository(id: number): RepositorySummary {
  return {
    id,
    owner: 'erland',
    name: `repo-${id}`,
    fullName: `erland/repo-${id}`,
    url: `https://github.com/erland/repo-${id}`,
    visibility: 'PRIVATE',
    archived: false,
    fork: false,
    defaultBranch: 'main',
    topics: [],
    languages: [],
    primaryLanguage: null,
    license: { analysisState: 'NOT_ANALYZED', presence: 'UNKNOWN', recognized: null, key: null, name: null },
    githubActions: { analysisState: 'NOT_ANALYZED', workflowsPresent: null, workflowCount: null },
    release: {
      analysisState: 'NOT_ANALYZED',
      releasePresent: null,
      latestReleaseName: null,
      latestReleaseTag: null,
      latestReleaseDate: null,
      latestReleasePrerelease: null,
    },
    activity: { pushedAt: null, updatedAt: null },
    refreshStatus: { state: 'NOT_ANALYZED', message: null },
  }
}

describe('repository selection', () => {
  it('selects and deselects one repository using stable repository ids', () => {
    let selection = toggleRepositorySelection(new Set(), 42)
    expect([...selection]).toEqual([42])

    selection = toggleRepositorySelection(selection, 42)
    expect(selection.size).toBe(0)
  })

  it('selects all visible repositories while preserving hidden selections', () => {
    const visible = [repository(1), repository(2)]
    const result = selectVisibleRepositories(new Set([99]), visible)

    expect([...result].sort((a, b) => a - b)).toEqual([1, 2, 99])
  })

  it('deselects only visible repositories and preserves hidden selections', () => {
    const visible = [repository(1), repository(2)]
    const result = deselectVisibleRepositories(new Set([1, 2, 99]), visible)

    expect([...result]).toEqual([99])
  })

  it('reports selected visible count independently from total selected count', () => {
    const visible = [repository(1), repository(2), repository(3)]
    expect(selectedVisibleCount(new Set([2, 99]), visible)).toBe(1)
  })

  it('clears the complete selection explicitly', () => {
    expect(clearRepositorySelection().size).toBe(0)
  })
})
