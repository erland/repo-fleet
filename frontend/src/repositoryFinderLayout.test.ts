import { describe, expect, it } from 'vitest'
import appSource from './App.tsx?raw'
import inventorySource from './RepositoryInventory.tsx?raw'
import launcherSource from './RepositoryLauncherToolbar.tsx?raw'

describe('repository finder layout', () => {
  it('keeps the compact repository launcher directly before repository results', () => {
    const navigation = appSource.indexOf('className="workspace-navigation"')
    const launcher = appSource.indexOf('<RepositoryLauncherToolbar')
    const inventory = appSource.indexOf('<RepositoryInventory')
    const savedViewManagement = appSource.indexOf('<SavedViewsPanel')
    const refresh = appSource.indexOf('<InventoryRefreshPanel')
    const portfolio = appSource.indexOf('<PortfolioSummaryPanel')
    const diagnostics = appSource.indexOf('<RefreshDiagnosticsPanel')

    expect(navigation).toBeGreaterThan(-1)
    expect(launcher).toBeGreaterThan(navigation)
    expect(inventory).toBeGreaterThan(launcher)
    expect(savedViewManagement).toBeGreaterThan(inventory)
    expect(refresh).toBeGreaterThan(inventory)
    expect(portfolio).toBeGreaterThan(refresh)
    expect(diagnostics).toBeGreaterThan(portfolio)

    expect(appSource).not.toContain('<RepositoryFiltersPanel')
    expect(appSource).not.toContain('<RepositorySortControls')
    expect(appSource).toContain('managementOnly')
    expect(appSource).toContain('Insights & diagnostics')
    expect(appSource).toContain('Skip to main content')
    expect(appSource).toContain('aria-controls="repositories-workspace"')
    expect(appSource).toContain('aria-controls="insights-workspace"')
  })

  it('keeps the mobile discovery flow structurally compact and detail-first', () => {
    expect(appSource).toContain('className="app-header"')
    expect(appSource).toContain('className="workspace-navigation"')
    expect(appSource).toContain('<RepositoryLauncherToolbar')
    expect(appSource).toContain('<RepositoryInventory')

    expect(launcherSource).toContain('className="launcher-primary"')
    expect(launcherSource).toContain('Search repositories…')
    expect(launcherSource).toContain('aria-label="Saved view"')
    expect(launcherSource).toContain('aria-label="Sort repositories"')
    expect(launcherSource).toContain('className="launcher-filter-button"')

    expect(inventorySource).toContain('mobile-repository-signals')
    expect(inventorySource).toContain('repository-row-interactive')
    expect(inventorySource).toContain('onOpenDetails(repository.id)')
    expect(inventorySource).toContain('mobile-github-link')
  })
})
