import { useState } from 'react'
import type { SavedRepositoryView } from './savedViews'

type SavedViewsPanelProps = {
  views: SavedRepositoryView[]
  storageAvailable: boolean
  onSave: (name: string) => void
  onLoad: (viewId: string) => void
  onDelete: (viewId: string) => void
}

export function SavedViewsPanel({
  views,
  storageAvailable,
  onSave,
  onLoad,
  onDelete,
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
    <section className="saved-views-panel" aria-labelledby="saved-views-heading">
      <div className="saved-views-heading">
        <div>
          <p className="eyebrow">Browser storage</p>
          <h2 id="saved-views-heading">Saved views</h2>
          <p className="saved-views-help" id="saved-views-description">
            Save the current filters and sorting in this browser. Repository selection is not included.
          </p>
        </div>
        <span className="saved-views-count">{savedCountLabel}</span>
      </div>

      {!storageAvailable && (
        <p className="saved-views-warning" role="status">
          Browser storage is unavailable. Saved views will only last for this page session.
        </p>
      )}

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
              <div>
                <button className="secondary-button" type="button" onClick={() => onLoad(view.id)} aria-label={`Load saved view ${view.name}`}>
                  Load
                </button>
                <button className="secondary-button" type="button" onClick={() => onDelete(view.id)} aria-label={`Delete saved view ${view.name}`}>
                  Delete
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
