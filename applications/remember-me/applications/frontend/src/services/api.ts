export type FieldErrors = Record<string, string>

export class ApiError extends Error {
  status: number
  fieldErrors: FieldErrors

  constructor(status: number, fieldErrors: FieldErrors = {}) {
    super(`API error ${status}`)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

export async function apiFetch<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    headers: init?.body ? { 'Content-Type': 'application/json' } : undefined,
    ...init,
  })
  if (!response.ok) {
    let fieldErrors: FieldErrors = {}
    if (response.status === 400) {
      fieldErrors = await response.json().catch(() => ({}))
    }
    throw new ApiError(response.status, fieldErrors)
  }
  return response.json() as Promise<T>
}
