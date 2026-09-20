import { useCallback, useEffect, useRef, useState } from 'react'
import {
  fetchComplianceSummary,
  fetchInventoryStatus,
  fetchRefreshDiagnostics,
  fetchRepositories,
  startFullInventoryRefresh,
  startInventoryRefresh,
  type CompliancePortfolioSummary,
  type InventoryStatus,
  type RefreshDiagnosticsSnapshot,
  type RepositorySummary,
} from './api'

const REFRESH_POLL_INTERVAL_MS = 1000

export function useRepositoryPortfolioData(enabled: boolean) {
  const [repositories, setRepositories] = useState<RepositorySummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [inventoryStatus, setInventoryStatus] = useState<InventoryStatus | null>(null)
  const [statusError, setStatusError] = useState<string | null>(null)
  const [refreshing, setRefreshing] = useState(false)
  const [complianceSummary, setComplianceSummary] = useState<CompliancePortfolioSummary | null>(null)
  const [complianceLoading, setComplianceLoading] = useState(false)
  const [complianceError, setComplianceError] = useState<string | null>(null)
  const [refreshDiagnostics, setRefreshDiagnostics] = useState<RefreshDiagnosticsSnapshot | null>(null)
  const [refreshDiagnosticsLoading, setRefreshDiagnosticsLoading] = useState(false)
  const [refreshDiagnosticsError, setRefreshDiagnosticsError] = useState<string | null>(null)
  const mountedRef = useRef(true)

  useEffect(() => {
    mountedRef.current = true
    return () => {
      mountedRef.current = false
    }
  }, [])

  const loadRepositories = useCallback(async (showInitialLoading = false) => {
    if (showInitialLoading) setLoading(true)

    try {
      const result = await fetchRepositories()
      if (!mountedRef.current) return
      setRepositories(result)
      setError(null)
    } catch {
      if (!mountedRef.current) return
      setError('Repository inventory could not be loaded from the backend.')
    } finally {
      if (mountedRef.current && showInitialLoading) setLoading(false)
    }
  }, [])

  const loadCompliance = useCallback(async () => {
    setComplianceLoading(true)
    try {
      const result = await fetchComplianceSummary()
      if (!mountedRef.current) return
      setComplianceSummary(result)
      setComplianceError(null)
    } catch {
      if (!mountedRef.current) return
      setComplianceError('Compliance summary could not be loaded from the backend.')
    } finally {
      if (mountedRef.current) setComplianceLoading(false)
    }
  }, [])

  const loadRefreshDiagnostics = useCallback(async () => {
    setRefreshDiagnosticsLoading(true)
    try {
      const result = await fetchRefreshDiagnostics()
      if (!mountedRef.current) return
      setRefreshDiagnostics(result)
      setRefreshDiagnosticsError(null)
    } catch {
      if (!mountedRef.current) return
      setRefreshDiagnosticsError('Refresh diagnostics could not be loaded from the backend.')
    } finally {
      if (mountedRef.current) setRefreshDiagnosticsLoading(false)
    }
  }, [])

  const loadStatus = useCallback(async () => {
    try {
      const result = await fetchInventoryStatus()
      if (!mountedRef.current) return null
      setInventoryStatus(result)
      setStatusError(null)
      return result
    } catch {
      if (!mountedRef.current) return null
      setStatusError('Inventory refresh status could not be loaded from the backend.')
      return null
    }
  }, [])

  useEffect(() => {
    if (!enabled) return
    void loadRepositories(true)
    void loadStatus()
    void loadCompliance()
    void loadRefreshDiagnostics()
  }, [enabled, loadCompliance, loadRefreshDiagnostics, loadRepositories, loadStatus])

  useEffect(() => {
    if (!enabled || inventoryStatus?.state !== 'RUNNING') return

    setRefreshing(true)
    const timer = window.setInterval(async () => {
      const [nextStatus] = await Promise.all([
        loadStatus(),
        loadRefreshDiagnostics(),
      ])
      if (!nextStatus) return

      await loadRepositories(false)
      if (nextStatus.state === 'RUNNING') return

      await Promise.all([
        loadCompliance(),
        loadRefreshDiagnostics(),
      ])

      window.clearInterval(timer)
      if (mountedRef.current) setRefreshing(false)
    }, REFRESH_POLL_INTERVAL_MS)

    return () => window.clearInterval(timer)
  }, [enabled, inventoryStatus?.state, loadCompliance, loadRefreshDiagnostics, loadRepositories, loadStatus])

  const startRefresh = useCallback(async (
    starter: () => Promise<InventoryStatus>,
    failureMessage: string,
  ) => {
    if (refreshing || inventoryStatus?.state === 'RUNNING') return

    setRefreshing(true)
    setStatusError(null)

    try {
      const started = await starter()
      if (!mountedRef.current) return
      setInventoryStatus(started)

      if (started.state !== 'RUNNING') {
        setRefreshing(false)
        await loadRepositories(false)
      }
    } catch {
      if (!mountedRef.current) return
      setRefreshing(false)
      setStatusError(failureMessage)
    }
  }, [inventoryStatus?.state, loadRepositories, refreshing])

  const refreshRepositories = useCallback(
    () => startRefresh(startInventoryRefresh, 'Repository refresh could not be started.'),
    [startRefresh],
  )

  const fullRefreshRepositories = useCallback(
    () => startRefresh(startFullInventoryRefresh, 'Full repository refresh could not be started.'),
    [startRefresh],
  )

  return {
    repositories,
    loading,
    error,
    inventoryStatus,
    statusError,
    refreshing,
    complianceSummary,
    complianceLoading,
    complianceError,
    refreshDiagnostics,
    refreshDiagnosticsLoading,
    refreshDiagnosticsError,
    reloadCompliance: loadCompliance,
    refreshRepositories,
    fullRefreshRepositories,
  }
}
