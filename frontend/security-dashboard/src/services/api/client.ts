export class ApiError extends Error {
  readonly status: number
  readonly code?: string

  constructor(status: number, message: string, code?: string) {
    super(message)
    this.status = status
    this.code = code
  }
}

const user = import.meta.env.VITE_API_USER || 'analyst'
const password = import.meta.env.VITE_API_PASSWORD || 'analyst_change_me'

function basicAuthHeader(): string {
  return `Basic ${btoa(`${user}:${password}`)}`
}

export async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(path, {
    headers: {
      Accept: 'application/json',
      Authorization: basicAuthHeader(),
      'X-Correlation-Id': crypto.randomUUID(),
    },
  })
  if (!response.ok) {
    let message = 'Unable to load data. Please try again.'
    let code: string | undefined
    try {
      const body = (await response.json()) as { message?: string; code?: string }
      if (body.message) {
        message = body.message
      }
      code = body.code
    } catch {
      if (response.status === 401) {
        message = 'API authentication failed. Start api-service and check analyst credentials.'
      } else if (response.status === 404) {
        message = 'The requested record was not found.'
      } else if (response.status >= 500) {
        message = 'The API is unavailable. Start api-service and try again.'
      }
    }
    if (response.status === 401) {
      message = 'API authentication failed. Start api-service and check analyst credentials.'
    }
    throw new ApiError(response.status, message, code)
  }
  return (await response.json()) as T
}
