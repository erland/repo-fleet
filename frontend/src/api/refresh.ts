export type InventoryRefreshState = 'NOT_STARTED' | 'RUNNING' | 'COMPLETED' | 'PARTIAL' | 'FAILED'

export type InventoryStatus = {
  state: InventoryRefreshState
  lastAttemptAt: string | null
  lastSuccessfulRefreshAt: string | null
  completedAt: string | null
  errorMessage: string | null
  repositoryCount: number
  totalCount: number
  processedCount: number
  successfulCount: number
  errorCount: number
  reusedCount: number
  newCount: number
  changedCount: number
  scheduledCount: number
  currentRepository: string | null
}

export async function fetchInventoryStatus(): Promise<InventoryStatus> {
  const response = await fetch('/api/inventory/status')

  if (!response.ok) {
    throw new Error(`Inventory status request failed with HTTP ${response.status}`)
  }

  return response.json() as Promise<InventoryStatus>
}

export async function startInventoryRefresh(): Promise<InventoryStatus> {
  const response = await fetch('/api/inventory/refresh', { method: 'POST' })

  if (!response.ok) {
    throw new Error(`Inventory refresh request failed with HTTP ${response.status}`)
  }

  return response.json() as Promise<InventoryStatus>
}

export async function startFullInventoryRefresh(): Promise<InventoryStatus> {
  const response = await fetch('/api/inventory/refresh/full', { method: 'POST' })

  if (!response.ok) {
    throw new Error(`Full inventory refresh request failed with HTTP ${response.status}`)
  }

  return response.json() as Promise<InventoryStatus>
}
