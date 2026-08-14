import { useEffect, useState } from 'react'
import { fetchServiceStatus, type ServiceStatus } from './api'

export default function App() {
  const [status, setStatus] = useState<ServiceStatus | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true

    fetchServiceStatus()
      .then((result) => {
        if (active) setStatus(result)
      })
      .catch(() => {
        if (active) setError('Backend connection unavailable')
      })

    return () => {
      active = false
    }
  }, [])

  return (
    <main className="app-shell">
      <section className="hero">
        <p className="eyebrow">Repository portfolio management</p>
        <h1>RepoFleet</h1>
        <p className="intro">
          Analytics and maintenance for a large GitHub repository portfolio.
        </p>

        <div className="status-card" aria-live="polite">
          <span className="status-label">Backend</span>
          {status && <strong>{status.status}</strong>}
          {!status && !error && <span>Checking…</span>}
          {error && <strong>{error}</strong>}
        </div>
      </section>
    </main>
  )
}
