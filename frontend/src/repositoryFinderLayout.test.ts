import { describe, expect, it } from 'vitest'
import appSource from './App.tsx?raw'

describe('repository finder layout', () => {
  it('prioritizes repository discovery before portfolio and operations panels', () => {
    const finder = appSource.indexOf('className="repository-finder"')
    const filters = appSource.indexOf('<RepositoryFiltersPanel')
    const savedViews = appSource.indexOf('<SavedViewsPanel')
    const inventory = appSource.indexOf('<RepositoryInventory')
    const portfolio = appSource.indexOf('<PortfolioSummaryPanel')
    const refresh = appSource.indexOf('<InventoryRefreshPanel')

    expect(finder).toBeGreaterThan(-1)
    expect(filters).toBeGreaterThan(finder)
    expect(savedViews).toBeGreaterThan(filters)
    expect(inventory).toBeGreaterThan(savedViews)
    expect(portfolio).toBeGreaterThan(inventory)
    expect(refresh).toBeGreaterThan(inventory)
  })
})
