import { describe, expect, it } from 'vitest'
import appSource from './App.tsx?raw'
import inventorySource from './RepositoryInventory.tsx?raw'
import launcherSource from './RepositoryLauncherToolbar.tsx?raw'
import workspaceSource from './RepositoryWorkspace.tsx?raw'

describe('repository finder layout', () => {
  it('keeps the compact repository launcher directly before repository results', () => {
    const navigation = appSource.indexOf('className="workspace-navigation"')
    const workspace = appSource.indexOf('<RepositoryWorkspace')
    const portfolio = appSource.indexOf('<PortfolioSummaryPanel')
    const diagnostics = appSource.indexOf('<RefreshDiagnosticsPanel')

    expect(navigation).toBeGreaterThan(-1)
    expect(workspace).toBeGreaterThan(navigation)
    expect(portfolio).toBeGreaterThan(workspace)
    expect(diagnostics).toBeGreaterThan(portfolio)

    expect(appSource).not.toContain('<RepositoryLauncherToolbar')
    expect(appSource).not.toContain('<RepositoryInventory')
    expect(workspaceSource).toContain('<RepositoryLauncherToolbar')
    expect(workspaceSource).toContain('<RepositoryInventory')
    expect(workspaceSource).toContain('<SavedViewsPanel')
    expect(workspaceSource).toContain('<InventoryRefreshPanel')
    expect(workspaceSource).toContain('managementOnly')
    expect(appSource).toContain('Insights & diagnostics')
    expect(appSource).toContain('Skip to main content')
    expect(appSource).toContain('aria-controls="repositories-workspace"')
    expect(appSource).toContain('aria-controls="insights-workspace"')
  })

  it('keeps the mobile discovery flow structurally compact and detail-first', () => {
    expect(appSource).toContain('className="app-header"')
    expect(appSource).toContain('className="workspace-navigation"')
    expect(appSource).toContain('<RepositoryWorkspace')
    expect(workspaceSource).toContain('<RepositoryLauncherToolbar')
    expect(workspaceSource).toContain('<RepositoryInventory')

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
