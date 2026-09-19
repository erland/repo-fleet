import { useState } from 'react'
import type { SavedRepositoryView } from './savedViews'

type SavedViewsPanelProps = {
  views: SavedRepositoryView[]
  activeViewId: string | null
  storageAvailable: boolean
  onShowAll: () => void
  onSave: (name: string) => void
  onLoad: (viewId: string) => void
  onDelete: (viewId: string) => void
  managementOnly?: boolean
}

export function SavedViewsPanel({
  views,
  activeViewId,
  storageAvailable,
  onShowAll,
  onSave,
  onLoad,
  onDelete,
  managementOnly = false,
}: SavedViewsPanelProps) {
  const [name, setName] = useState('')
  const savedCountLabel = `${views.length} saved ${views.length === 1 ? 'view' : 'views'}`

  const save = () => {
    const normalized = name.trim()
    if (!normalized) return
    onSave(normalized)
    setName('')
  }

  return (
    <section
      className={managementOnly ? 'saved-views-panel saved-views-panel-management' : 'saved-views-panel'}
      aria-labelledby="saved-views-heading"
    >
      {!managementOnly && (
        <div className="saved-views-heading">
          <div>
            <p className="eyebrow">Saved views</p>
            <h2 id="saved-views-heading">Quick categories</h2>
            <p className="saved-views-help" id="saved-views-description">
              Choose a saved view to apply its filters and sorting immediately.
            </p>
          </div>
          <span className="saved-views-count">{savedCountLabel}</span>
        </div>
      )}

      {managementOnly && <h2 className="sr-only" id="saved-views-heading">Manage saved views</h2>}

      {!storageAvailable && (
        <p className="saved-views-warning" role="status">
          Browser storage is unavailable. Saved views will only last for this page session.
        </p>
      )}

      {!managementOnly && (
        <div className="saved-view-shortcuts" role="group" aria-label="Repository views">
          <button
            className={`saved-view-chip ${activeViewId === null ? 'saved-view-chip-active' : ''}`}
            type="button"
            aria-pressed={activeViewId === null}
            onClick={onShowAll}
          >
            All repositories
          </button>

          {views.map((view) => (
            <button
              key={view.id}
              className={`saved-view-chip ${activeViewId === view.id ? 'saved-view-chip-active' : ''}`}
              type="button"
              aria-pressed={activeViewId === view.id}
              onClick={() => onLoad(view.id)}
            >
              {view.name}
            </button>
          ))}
        </div>
      )}

      <details className="saved-view-management">
        <summary>{managementOnly ? 'Manage saved views' : 'Manage views'}</summary>

        <p className="saved-views-help">
          <span id="saved-views-description">Save the current filters and sorting in this browser. Repository selection is not included.</span>
        </p>

        <div className="saved-view-create">
          <label>
            <span>View name</span>
            <input
              value={name}
              onChange={(event) => setName(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter') {
                  event.preventDefault()
                  save()
                }
              }}
              placeholder="e.g. Java repos missing LICENSE"
              aria-describedby="saved-views-description"
            />
          </label>
          <button className="secondary-button" type="button" onClick={save} disabled={!name.trim()}>
            Save current view
          </button>
        </div>

        {views.length === 0 ? (
          <p className="saved-views-empty">No saved views yet.</p>
        ) : (
          <ul className="saved-view-list">
            {views.map((view) => (
              <li key={view.id}>
                <span>{view.name}</span>
                <button
                  className="secondary-button"
                  type="button"
                  onClick={() => onDelete(view.id)}
                  aria-label={`Delete saved view ${view.name}`}
                >
                  Delete
                </button>
              </li>
            ))}
          </ul>
        )}
      </details>
    </section>
  )
}
