export type ServiceStatus = {
  service: string
  status: string
}

export async function fetchServiceStatus(): Promise<ServiceStatus> {
  const response = await fetch('/api/status')

  if (!response.ok) {
    throw new Error(`Backend status request failed with HTTP ${response.status}`)
  }

  return response.json() as Promise<ServiceStatus>
}
