export class ApiError extends Error {
  readonly status: number
  readonly code?: string

  constructor(status: number, message: string, code?: string) {
    super(message)
    this.status = status
    this.code = code
  }
}

export async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(path, {
    headers: {
      Accept: 'application/json',
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
      if (response.status === 404) {
        message = 'The requested record was not found.'
      } else if (response.status >= 500) {
        message = 'The API is unavailable. Start api-service and try again.'
      }
    }
    throw new ApiError(response.status, message, code)
  }
  return (await response.json()) as T
}
