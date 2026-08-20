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
const apiBase = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')

export function apiOrigin(): string {
  return apiBase || 'http://localhost:8080'
}

function apiUrl(path: string): string {
  return `${apiBase}${path}`
}

function basicAuthHeader(): string {
  return `Basic ${btoa(`${user}:${password}`)}`
}

function requestHeaders(json = false): HeadersInit {
  return {
    Accept: 'application/json',
    Authorization: basicAuthHeader(),
    'X-Correlation-Id': crypto.randomUUID(),
    ...(json ? { 'Content-Type': 'application/json' } : {}),
  }
}

function looksLikeStackTrace(message: string): boolean {
  return message.includes('\tat ') || /Exception:|Caused by:/.test(message)
}

async function parseError(response: Response): Promise<ApiError> {
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
      message = `The API is unavailable. Start api-service at ${apiOrigin()}.`
    }
  }
  if (response.status === 401) {
    message = 'API authentication failed. Start api-service and check analyst credentials.'
  } else if (response.status === 404) {
    message = 'The requested record was not found.'
  } else if (response.status >= 500 || looksLikeStackTrace(message)) {
    message = `The API is unavailable. Start api-service at ${apiOrigin()}.`
  } else if (message.length > 280) {
    message = message.slice(0, 277) + '...'
  }
  return new ApiError(response.status, message, code)
}

export async function apiGet<T>(path: string): Promise<T> {
  let response: Response
  try {
    response = await fetch(apiUrl(path), { headers: requestHeaders() })
  } catch {
    throw new ApiError(0, `Unable to reach the API at ${apiOrigin()}.`)
  }
  if (!response.ok) {
    throw await parseError(response)
  }
  return (await response.json()) as T
}

export async function apiPost<T>(path: string, body: unknown): Promise<T> {
  let response: Response
  try {
    response = await fetch(apiUrl(path), {
      method: 'POST',
      headers: requestHeaders(true),
      body: JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, `Unable to reach the API at ${apiOrigin()}.`)
  }
  if (!response.ok) {
    throw await parseError(response)
  }
  return (await response.json()) as T
}
