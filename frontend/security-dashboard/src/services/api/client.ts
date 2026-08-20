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
const apiBase = resolveApiBase(import.meta.env.VITE_API_BASE_URL, Boolean(import.meta.env.DEV))

export function apiOrigin(): string {
  return apiBase || 'http://localhost:8080'
}

/** Empty / local api-service URLs use the Vite `/api` proxy so the browser is same-origin. */
export function resolveApiBase(raw: unknown, isDev: boolean): string {
  const trimmed = String(raw ?? '')
    .trim()
    .replace(/\/$/, '')
    .replace(/\/api$/i, '')
  if (!trimmed) {
    return ''
  }
  if (isDev) {
    try {
      const url = new URL(trimmed)
      const localHost = url.hostname === 'localhost' || url.hostname === '127.0.0.1'
      const apiPort = url.port === '' || url.port === '8080'
      if (localHost && apiPort) {
        return ''
      }
    } catch {
      return ''
    }
  }
  return trimmed
}

function apiUrl(path: string): string {
  return `${apiBase}${path}`
}

function basicAuthHeader(): string {
  const token = `${user}:${password}`
  if (typeof btoa === 'function') {
    return `Basic ${btoa(token)}`
  }
  return `Basic ${Buffer.from(token, 'utf8').toString('base64')}`
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
    } else if (response.status === 502 || response.status === 503 || response.status === 504) {
      message = `api-service is not reachable through the dashboard proxy. Start it on port 8080.`
    } else if (response.status >= 500) {
      message = `The API is unavailable. Start api-service on port 8080.`
    }
  }
  if (response.status === 401) {
    message = 'API authentication failed. Start api-service and check analyst credentials.'
  } else if (response.status === 404) {
    message = 'The requested record was not found. Confirm api-service is running and VITE_API_BASE_URL is empty so the Vite proxy is used.'
  } else if (response.status === 502 || response.status === 503 || response.status === 504) {
    message = 'api-service is not reachable. Start it on port 8080, then retry.'
  } else if (looksLikeStackTrace(message)) {
    message = 'The API is unavailable. Start api-service on port 8080.'
  } else if (message.length > 280) {
    message = message.slice(0, 277) + '...'
  }
  return new ApiError(response.status, message, code)
}

export function apiErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError && error.message) {
    return error.message
  }
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallback
}

export async function apiGet<T>(path: string): Promise<T> {
  let response: Response
  try {
    response = await fetch(apiUrl(path), { headers: requestHeaders() })
  } catch {
    throw new ApiError(
      0,
      'Unable to reach api-service. Start it on port 8080 and open the dashboard at http://localhost:5173 (leave VITE_API_BASE_URL empty).',
    )
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
    throw new ApiError(
      0,
      'Unable to reach api-service. Start it on port 8080 and open the dashboard at http://localhost:5173 (leave VITE_API_BASE_URL empty).',
    )
  }
  if (!response.ok) {
    throw await parseError(response)
  }
  return (await response.json()) as T
}
