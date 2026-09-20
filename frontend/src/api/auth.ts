export type AuthenticatedUser = {
  login: string
  name: string | null
  avatarUrl: string | null
}

export type AuthSession = {
  authEnabled: boolean
  authenticated: boolean
  user: AuthenticatedUser | null
}

export async function fetchAuthSession(): Promise<AuthSession> {
  const response = await fetch('/api/auth/session', { credentials: 'same-origin' })
  if (!response.ok) throw new Error(`Authentication session request failed with HTTP ${response.status}`)
  return response.json() as Promise<AuthSession>
}

export async function logout(): Promise<void> {
  const response = await fetch('/api/auth/logout', { method: 'POST', credentials: 'same-origin' })
  if (!response.ok) throw new Error(`Logout request failed with HTTP ${response.status}`)
}
