import { describe, expect, it } from 'vitest'
import appSource from './App.tsx?raw'

describe('repository finder layout', () => {
  it('keeps repository discovery primary and analytics in secondary navigation', () => {
    const navigation = appSource.indexOf('className="workspace-navigation"')
    const finder = appSource.indexOf('className="repository-finder"')
    const filters = appSource.indexOf('<RepositoryFiltersPanel')
    const savedViews = appSource.indexOf('<SavedViewsPanel')
    const inventory = appSource.indexOf('<RepositoryInventory')
    const refresh = appSource.indexOf('<InventoryRefreshPanel')
    const insightsCondition = appSource.indexOf("workspaceView === 'repositories'")
    const portfolio = appSource.indexOf('<PortfolioSummaryPanel')
    const diagnostics = appSource.indexOf('<RefreshDiagnosticsPanel')

    expect(navigation).toBeGreaterThan(-1)
    expect(insightsCondition).toBeGreaterThan(navigation)
    expect(finder).toBeGreaterThan(insightsCondition)
    expect(filters).toBeGreaterThan(finder)
    expect(savedViews).toBeGreaterThan(filters)
    expect(inventory).toBeGreaterThan(savedViews)
    expect(refresh).toBeGreaterThan(inventory)
    expect(portfolio).toBeGreaterThan(refresh)
    expect(diagnostics).toBeGreaterThan(portfolio)
    expect(appSource).toContain('Insights & diagnostics')
    expect(appSource).toContain("workspaceView === 'insights'")
  })
})
