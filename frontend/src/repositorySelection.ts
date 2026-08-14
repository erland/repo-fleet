import type { RepositorySummary } from './api'

export type RepositorySelection = ReadonlySet<number>

export function toggleRepositorySelection(
  selection: RepositorySelection,
  repositoryId: number,
): Set<number> {
  const next = new Set(selection)
  if (next.has(repositoryId)) next.delete(repositoryId)
  else next.add(repositoryId)
  return next
}

export function selectVisibleRepositories(
  selection: RepositorySelection,
  visibleRepositories: RepositorySummary[],
): Set<number> {
  const next = new Set(selection)
  visibleRepositories.forEach((repository) => next.add(repository.id))
  return next
}

export function deselectVisibleRepositories(
  selection: RepositorySelection,
  visibleRepositories: RepositorySummary[],
): Set<number> {
  const visibleIds = new Set(visibleRepositories.map((repository) => repository.id))
  return new Set([...selection].filter((repositoryId) => !visibleIds.has(repositoryId)))
}

export function clearRepositorySelection(): Set<number> {
  return new Set<number>()
}

export function selectedVisibleCount(
  selection: RepositorySelection,
  visibleRepositories: RepositorySummary[],
): number {
  return visibleRepositories.reduce(
    (count, repository) => count + (selection.has(repository.id) ? 1 : 0),
    0,
  )
}
