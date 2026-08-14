import { useEffect, useState } from 'react'
import { fetchRepositories, type RepositorySummary } from './api'
import { RepositoryInventory } from './RepositoryInventory'

export default function App() {
  const [repositories, setRepositories] = useState<RepositorySummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true

    fetchRepositories()
      .then((result) => {
        if (!active) return
        setRepositories(result)
        setError(null)
      })
      .catch(() => {
        if (!active) return
        setError('Repository inventory could not be loaded from the backend.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [])

  return (
    <main className="app-shell">
      <header className="app-header">
        <div>
          <p className="eyebrow">Repository portfolio management</p>
          <h1>RepoFleet</h1>
          <p className="intro">Analytics and maintenance for a large GitHub repository portfolio.</p>
        </div>
      </header>

      <RepositoryInventory repositories={repositories} loading={loading} error={error} />
    </main>
  )
}
