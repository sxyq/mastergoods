export type QueryValue = string | number | boolean | Date | null | undefined

type ApiEnvelope<T> = {
  code: number
  message: string
  data: T
  timestamp: number
}

type AuthRefreshResponse = {
  token: string
  refresh_token: string
}

export class AdminApiError extends Error {
  readonly status: number
  readonly code: number

  constructor(message: string, status: number, code = status) {
    super(message)
    this.name = 'AdminApiError'
    this.status = status
    this.code = code
  }
}

type AdminSessionFailureHandler = (error: AdminApiError) => void

// 管理员认证信息只保留在当前页面内存，不能写入浏览器存储。
let accessToken = ''
let refreshToken = ''
let authGeneration = 0
let refreshInFlight: Promise<number> | null = null
let sessionFailureHandler: AdminSessionFailureHandler | null = null

export function setAdminSessionFailureHandler(handler: AdminSessionFailureHandler | null): void {
  sessionFailureHandler = handler
}

export function getAccessToken(): string {
  return accessToken
}

export function setAdminAuthTokens(nextAccessToken: string, nextRefreshToken: string): void {
  accessToken = nextAccessToken
  refreshToken = nextRefreshToken
  authGeneration += 1
}

export function clearAccessToken(): void {
  accessToken = ''
  refreshToken = ''
  authGeneration += 1
}

export function isAdminApiError(error: unknown, status?: number): error is AdminApiError {
  return error instanceof AdminApiError && (status === undefined || error.status === status)
}

function invalidateAdminSession(error: AdminApiError): void {
  clearAccessToken()
  sessionFailureHandler?.(error)
}

export async function apiRequest<T>(
  path: string,
  init: RequestInit = {},
  query: Record<string, QueryValue> = {},
): Promise<T> {
  const url = buildUrl(path, query)
  const response = await requestWithSessionRefresh(url, path, init, 'application/json')

  const payload = await parsePayload<T>(response)
  if (!response.ok || payload.code !== 0) {
    throw new AdminApiError(payload.message || '请求管理员服务失败。', response.status, payload.code)
  }
  return payload.data
}

export async function apiDownload(path: string): Promise<Blob> {
  const response = await requestWithSessionRefresh(buildUrl(path, {}), path, {}, 'text/csv')
  if (!response.ok) {
    throw new AdminApiError('导出文件暂不可下载。', response.status, response.status)
  }
  return response.blob()
}

export function openApiEventStream<T>(
  path: string,
  query: Record<string, QueryValue>,
  handlers: { onEvent: (event: T, eventId?: string) => void; onError: (error: Error) => void; onComplete?: () => void },
): () => void {
  const controller = new AbortController()
  void consumeEventStream(path, query, controller.signal, handlers)
  return () => controller.abort()
}

function buildUrl(path: string, query: Record<string, QueryValue>): string {
  const base = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '') ?? ''
  const url = new URL(`${base}${path}`, window.location.origin)
  for (const [key, value] of Object.entries(query)) {
    if (value === null || value === undefined || value === '') continue
    url.searchParams.set(key, value instanceof Date ? value.toISOString() : String(value))
  }
  return base ? url.toString() : `${url.pathname}${url.search}`
}

async function parsePayload<T>(response: Response): Promise<ApiEnvelope<T>> {
  try {
    return await response.json() as ApiEnvelope<T>
  } catch {
    throw new AdminApiError('管理员服务返回了无法识别的响应。', response.status, response.status)
  }
}

async function consumeEventStream<T>(
  path: string,
  query: Record<string, QueryValue>,
  signal: AbortSignal,
  handlers: { onEvent: (event: T, eventId?: string) => void; onError: (error: Error) => void; onComplete?: () => void },
): Promise<void> {
  try {
    const response = await requestWithSessionRefresh(buildUrl(path, query), path, { signal }, 'text/event-stream')
    if (!response.ok || !response.body) throw new AdminApiError('无法建立事件流连接。', response.status, response.status)
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    while (!signal.aborted) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const frames = buffer.split(/\r?\n\r?\n/)
      buffer = frames.pop() ?? ''
      for (const frame of frames) dispatchEventFrame<T>(frame, handlers)
    }
    if (!signal.aborted) handlers.onComplete?.()
  } catch (reason) {
    if (signal.aborted) return
    handlers.onError(reason instanceof Error ? reason : new Error('事件流连接失败。'))
  }
}

async function requestWithSessionRefresh(
  url: string,
  path: string,
  init: RequestInit,
  accept: string,
): Promise<Response> {
  const request = async (): Promise<Response> => {
    const headers = new Headers(init.headers)
    headers.set('Accept', accept)
    if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
    if (accessToken && !headers.has('Authorization')) headers.set('Authorization', `Bearer ${accessToken}`)
    try {
      return await fetch(url, { ...init, headers, credentials: 'same-origin' })
    } catch {
      throw new AdminApiError('无法连接管理员服务，请检查网络后重试。', 0, 0)
    }
  }

  const first = await request()
  const hasExplicitAuthorization = new Headers(init.headers).has('Authorization')
  if (first.status !== 401) return first
  if (!refreshToken || path === '/v2/auth/refresh' || hasExplicitAuthorization) {
    if (accessToken && !hasExplicitAuthorization) {
      invalidateAdminSession(new AdminApiError('管理员会话已失效，请重新登录。', 401, 401))
    }
    return first
  }
  await refreshAdminSession()
  const retried = await request()
  if (retried.status === 401) {
    invalidateAdminSession(new AdminApiError('管理员会话已失效，请重新登录。', retried.status, retried.status))
  }
  return retried
}

async function refreshAdminSession(): Promise<number> {
  if (!refreshToken) throw new AdminApiError('管理员会话已失效，请重新登录。', 401, 401)
  if (refreshInFlight) return refreshInFlight
  const generation = authGeneration
  const tokenForRefresh = refreshToken
  refreshInFlight = (async () => {
    let response: Response
    try {
      response = await fetch(buildUrl('/v2/auth/refresh', {}), {
        method: 'POST',
        headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
        body: JSON.stringify({ refresh_token: tokenForRefresh }),
        credentials: 'same-origin',
      })
    } catch {
      return failRefresh(new AdminApiError('无法续期管理员会话，请检查网络后重试。', 0, 0))
    }
    let payload: ApiEnvelope<AuthRefreshResponse>
    try {
      payload = await parsePayload<AuthRefreshResponse>(response)
    } catch (error) {
      return failRefresh(error)
    }
    if (!response.ok || payload.code !== 0 || !payload.data?.token || !payload.data?.refresh_token) {
      return failRefresh(new AdminApiError(payload.message || '管理员会话已失效，请重新登录。', response.status, payload.code))
    }
    if (authGeneration !== generation || refreshToken !== tokenForRefresh) {
      throw new AdminApiError('管理员会话已改变，请重新发起操作。', 401, 401)
    }
    setAdminAuthTokens(payload.data.token, payload.data.refresh_token)
    return authGeneration
  })().finally(() => { refreshInFlight = null })
  return refreshInFlight
}

function failRefresh(reason: unknown): never {
  const error = reason instanceof AdminApiError
    ? reason
    : new AdminApiError('无法续期管理员会话，请检查网络后重试。', 0, 0)
  invalidateAdminSession(error)
  throw error
}

function dispatchEventFrame<T>(
  frame: string,
  handlers: { onEvent: (event: T, eventId?: string) => void; onError: (error: Error) => void },
): void {
  let eventId: string | undefined
  const data: string[] = []
  for (const line of frame.split(/\r?\n/)) {
    if (line.startsWith('id:')) eventId = line.slice(3).trim()
    if (line.startsWith('data:')) data.push(line.slice(5).trimStart())
  }
  if (!data.length) return
  try {
    handlers.onEvent(JSON.parse(data.join('\n')) as T, eventId)
  } catch {
    handlers.onError(new Error('事件流返回了无法识别的数据。'))
  }
}
