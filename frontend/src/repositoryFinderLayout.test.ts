import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import appSource from './App.tsx?raw'

const stylesSource = readFileSync(new URL('./styles.css', import.meta.url), 'utf8')

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

  it('keeps the mobile discovery shell compact without shrinking primary touch targets', () => {
    expect(stylesSource).toContain('@media (max-width: 720px)')
    expect(stylesSource).toContain('.app-header .intro')
    expect(stylesSource).toContain('display: none')
    expect(stylesSource).toContain('.workspace-tab')
    expect(stylesSource).toContain('min-height: 2.75rem')
    expect(stylesSource).toContain('.repository-finder')
    expect(stylesSource).toContain('margin-bottom: .5rem')
    expect(stylesSource).toContain('.repository-table tbody')
    expect(stylesSource).toContain('gap: .35rem')
    expect(stylesSource).toContain('.repository-table td[data-label="Topics"]')
    expect(stylesSource).toContain('.mobile-github-link { display: none; }')
  })
})
