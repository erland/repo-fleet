import type { RepositorySummary } from './api'
import {
  selectedVisibleCount,
  type RepositorySelection,
} from './repositorySelection'

type RepositorySelectionBarProps = {
  selection: RepositorySelection
  visibleRepositories: RepositorySummary[]
  onSelectVisible: () => void
  onDeselectVisible: () => void
  onClear: () => void
}

export function RepositorySelectionBar({
  selection,
  visibleRepositories,
  onSelectVisible,
  onDeselectVisible,
  onClear,
}: RepositorySelectionBarProps) {
  const visibleSelected = selectedVisibleCount(selection, visibleRepositories)
  const allVisibleSelected = visibleRepositories.length > 0 && visibleSelected === visibleRepositories.length
  const selectedCountLabel = `${selection.size} selected`
  const visibleCountLabel = `${visibleSelected} of ${visibleRepositories.length} visible selected`

  return (
    <section className="selection-bar" aria-label="Repository selection">
      <div>
        <strong>{selectedCountLabel}</strong>
        <span className="selection-visible-count">{visibleCountLabel}</span>
      </div>
      <div className="selection-actions">
        <button
          className="secondary-button"
          type="button"
          onClick={allVisibleSelected ? onDeselectVisible : onSelectVisible}
          disabled={visibleRepositories.length === 0}
        >
          {allVisibleSelected ? 'Deselect visible' : 'Select visible'}
        </button>
        <button
          className="secondary-button"
          type="button"
          onClick={onClear}
          disabled={selection.size === 0}
        >
          Clear selection
        </button>
      </div>
    </section>
  )
}
