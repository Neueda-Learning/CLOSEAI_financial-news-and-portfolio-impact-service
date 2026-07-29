const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? '/api/v1').replace(/\/$/, '')

export type PagedResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  asOf: string | null
  stale: boolean
}

export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...init,
    headers: {
      Accept: 'application/json',
      ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
      ...init?.headers,
    },
  })

  if (response.status === 204) return undefined as T
  if (!response.ok) {
    const detail = await response.json().catch(() => null) as { detail?: string; title?: string } | null
    throw new Error(detail?.detail ?? detail?.title ?? `API request failed (${response.status})`)
  }

  return response.json() as Promise<T>
}

export function asNumber(value: string | number | null | undefined) {
  return value == null ? 0 : Number(value)
}
