import { renderToString } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import App from './App'
import { RepositoryInventory } from './RepositoryInventory'
import type { RepositorySummary } from './api'

const repository: RepositorySummary = {
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

describe('App', () => {
  it('renders the RepoFleet application shell', () => {
    const html = renderToString(<App />)

    expect(html).toContain('RepoFleet')
    expect(html).toContain('Repository portfolio management')
    expect(html).toContain('Loading repository inventory')
  })
})

describe('RepositoryInventory', () => {
  it('renders a populated repository table', () => {
    const html = renderToString(<RepositoryInventory repositories={[repository]} loading={false} error={null} />)

    expect(html).toContain('roman-nollpunkten')
    expect(html).toContain('novel')
    expect(html).toContain('MIT License')
    expect(html).toContain('3 workflows')
    expect(html).toContain('v1.2.0')
  })

  it('renders the loading state', () => {
    const html = renderToString(<RepositoryInventory repositories={[]} loading error={null} />)
    expect(html).toContain('Loading repository inventory')
  })

  it('renders the empty state', () => {
    const html = renderToString(<RepositoryInventory repositories={[]} loading={false} error={null} />)
    expect(html).toContain('No repositories are available')
  })

  it('renders the backend error state', () => {
    const html = renderToString(
      <RepositoryInventory repositories={[]} loading={false} error="Repository inventory could not be loaded from the backend." />,
    )
    expect(html).toContain('Repository inventory could not be loaded')
  })

  it('keeps incomplete analysis distinct from missing capabilities', () => {
    const incomplete = {
      ...repository,
      id: 1002,
      name: 'legacy-java-tool',
      license: { analysisState: 'FAILED', presence: 'UNKNOWN', recognized: null, key: null, name: null },
      githubActions: { analysisState: 'PARTIAL', workflowsPresent: null, workflowCount: null },
      release: {
        analysisState: 'NOT_ANALYZED',
        releasePresent: null,
        latestReleaseName: null,
        latestReleaseTag: null,
        latestReleaseDate: null,
        latestReleasePrerelease: null,
      },
    } satisfies RepositorySummary

    const html = renderToString(<RepositoryInventory repositories={[incomplete]} loading={false} error={null} />)
    expect((html.match(/Unknown/g) ?? []).length).toBeGreaterThanOrEqual(3)
  })
})
