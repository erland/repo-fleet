import type { InventoryStatus, RepositorySummary } from './api'

export const repositoryFixture: RepositorySummary = {
  id: 1001,
  owner: 'erland',
  name: 'roman-nollpunkten',
  fullName: 'erland/roman-nollpunkten',
  url: 'https://github.com/erland/roman-nollpunkten',
  visibility: 'PRIVATE',
  archived: false,
  fork: false,
  defaultBranch: 'main',
  topics: ['novel', 'publishing'],
  languages: ['Python', 'Markdown'],
  primaryLanguage: 'Python',
  license: { analysisState: 'COMPLETE', presence: 'PRESENT', recognized: true, key: 'mit', name: 'MIT License' },
  githubActions: { analysisState: 'COMPLETE', workflowsPresent: true, workflowCount: 3 },
  release: {
    analysisState: 'COMPLETE',
    releasePresent: true,
    latestReleaseName: 'v1.2.0',
    latestReleaseTag: 'v1.2.0',
    latestReleaseDate: '2026-08-10T17:30:00Z',
    latestReleasePrerelease: false,
  },
  activity: { pushedAt: '2026-08-12T14:15:00Z', updatedAt: '2026-08-12T14:16:30Z' },
  refreshStatus: { state: 'COMPLETE', message: null },
}

export const inventoryStatusFixture = (overrides: Partial<InventoryStatus> = {}): InventoryStatus => ({
  state: 'COMPLETED',
  lastAttemptAt: '2026-08-14T10:00:00Z',
  lastSuccessfulRefreshAt: '2026-08-14T10:01:00Z',
  completedAt: '2026-08-14T10:01:00Z',
  errorMessage: null,
  repositoryCount: 2,
  totalCount: 2,
  processedCount: 2,
  successfulCount: 2,
  errorCount: 0,
  currentRepository: null,
  running: false,
  ...overrides,
})
