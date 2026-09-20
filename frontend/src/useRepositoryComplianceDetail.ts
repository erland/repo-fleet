import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  expireRepositoryComplianceException,
  fetchRepositoryCompliance,
  removeRepositoryComplianceException,
  saveRepositoryComplianceException,
  type RepositoryComplianceDetail,
  type RepositorySummary,
} from './api'

type UseRepositoryComplianceDetailOptions = {
  repositories: RepositorySummary[]
  onComplianceChanged: () => Promise<void>
}

export function useRepositoryComplianceDetail({
  repositories,
  onComplianceChanged,
}: UseRepositoryComplianceDetailOptions) {
  const [repositoryId, setRepositoryId] = useState<number | null>(null)
  const [compliance, setCompliance] = useState<RepositoryComplianceDetail[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const mountedRef = useRef(true)

  useEffect(() => {
    mountedRef.current = true
    return () => {
      mountedRef.current = false
    }
  }, [])

  const repository = useMemo(
    () => repositories.find((candidate) => candidate.id === repositoryId) ?? null,
    [repositories, repositoryId],
  )

  const reload = useCallback(async (targetRepositoryId: number) => {
    setLoading(true)
    try {
      const result = await fetchRepositoryCompliance(targetRepositoryId)
      if (!mountedRef.current) return
      setCompliance(result)
      setError(null)
    } catch {
      if (!mountedRef.current) return
      setError('Repository compliance detail could not be loaded.')
    } finally {
      if (mountedRef.current) setLoading(false)
    }
  }, [])

  const open = useCallback((targetRepositoryId: number) => {
    setRepositoryId(targetRepositoryId)
    setCompliance([])
    setError(null)
    void reload(targetRepositoryId)
  }, [reload])

  const saveException = useCallback(async (
    targetRepositoryId: number,
    ruleKey: string,
    reason: string,
    expiresAt: string | null,
  ) => {
    await saveRepositoryComplianceException(targetRepositoryId, ruleKey, reason, expiresAt)
    await Promise.all([
      reload(targetRepositoryId),
      onComplianceChanged(),
    ])
  }, [onComplianceChanged, reload])

  const expireException = useCallback(async (
    targetRepositoryId: number,
    ruleKey: string,
  ) => {
    await expireRepositoryComplianceException(targetRepositoryId, ruleKey)
    await Promise.all([
      reload(targetRepositoryId),
      onComplianceChanged(),
    ])
  }, [onComplianceChanged, reload])

  const removeException = useCallback(async (
    targetRepositoryId: number,
    ruleKey: string,
  ) => {
    await removeRepositoryComplianceException(targetRepositoryId, ruleKey)
    await Promise.all([
      reload(targetRepositoryId),
      onComplianceChanged(),
    ])
  }, [onComplianceChanged, reload])

  const close = useCallback(() => {
    setRepositoryId(null)
    setCompliance([])
    setError(null)
    setLoading(false)
  }, [])

  return {
    repository,
    compliance,
    loading,
    error,
    open,
    saveException,
    expireException,
    removeException,
    close,
  }
}
