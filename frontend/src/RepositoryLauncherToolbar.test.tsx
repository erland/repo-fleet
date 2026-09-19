import { renderToString } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import { RepositoryLauncherToolbar } from './RepositoryLauncherToolbar'
import { emptyRepositoryFilters } from './repositoryFilters'
import { defaultRepositorySort } from './repositorySorting'
import { createSavedView } from './savedViews'

describe('RepositoryLauncherToolbar', () => {
  it('keeps search, saved view, sort and filters in one compact launcher', () => {
    const view = createSavedView(
      'Java missing LICENSE',
      { ...emptyRepositoryFilters, language: 'Java', license: 'MISSING' },
      { field: 'activity', direction: 'DESC' },
      'view-1',
    )

    const html = renderToString(
      <RepositoryLauncherToolbar
        filters={emptyRepositoryFilters}
        onFiltersChange={() => undefined}
        views={[view]}
        activeViewId={null}
        onShowAll={() => undefined}
        onLoadView={() => undefined}
        sort={defaultRepositorySort}
        onSortChange={() => undefined}
        totalCount={200}
        filteredCount={200}
      />,
    )

    expect(html).toContain('Search repositories')
    expect(html).toContain('Search repositories…')
    expect(html).toContain('aria-label="Saved view"')
    expect(html).toContain('All repositories')
    expect(html).toContain('Java missing LICENSE')
    expect(html).toContain('aria-label="Sort repositories"')
    expect(html).toContain('Name A–Z')
    expect(html).toContain('Filters')
    expect(html).toContain('200 repositories')
  })

  it('shows active advanced filter count without rendering the filter drawer permanently', () => {
    const html = renderToString(
      <RepositoryLauncherToolbar
        filters={{ ...emptyRepositoryFilters, owner: 'erland', license: 'MISSING' }}
        onFiltersChange={() => undefined}
        views={[]}
        activeViewId={null}
        onShowAll={() => undefined}
        onLoadView={() => undefined}
        sort={defaultRepositorySort}
        onSortChange={() => undefined}
        totalCount={20}
        filteredCount={4}
      />,
    )

    expect(html).toMatch(/Filters(?:<!-- -->)? · 2/)
    expect(html).toContain('aria-haspopup="dialog"')
    expect(html).toContain('aria-expanded="false"')
    expect(html).not.toContain('role="dialog"')
    expect(html).toContain('4 of 20 repositories')
  })
})
