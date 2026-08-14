import { renderToString } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import App from './App'

describe('App', () => {
  it('renders the RepoFleet application shell', () => {
    const html = renderToString(<App />)

    expect(html).toContain('RepoFleet')
    expect(html).toContain('Repository portfolio management')
    expect(html).toContain('Backend')
  })
})
