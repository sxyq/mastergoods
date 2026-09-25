import type { Permission, StoreRole } from '@/entities/auth/roles'
import type { EntityId } from '@/shared/utils/id'
import { camelize } from '@/shared/utils/camelize'
import { API_BASE_URL } from '@/shared/api/config'

interface AuthRuntimeConfig {
  getRefreshToken: () => string
  onAuthRefreshed: (payload: AuthPayload) => void
  onAuthExpired: () => void
}

const authRuntime: Partial<AuthRuntimeConfig> = {}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export class ApiError extends Error {
  status: number
  code: number

  constructor(message: string, status: number, code = -1) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

export interface AuthPayload {
  userId: EntityId
  token: string
  refreshToken: string
  expiresIn: number
}

export interface UserProfile {
  id: EntityId
  phone: string
  nickname: string
  status: number
}

export interface CurrentStoreProfile {
  storeId: EntityId
  storeName: string
  ownerUserId: EntityId
  currentUserId: EntityId
  currentUserName: string
  currentUserPhone: string
  role: StoreRole
  title: string
  status: 0 | 1
  permissions: Permission[]
  memberCount: number
  enabledMemberCount: number
  disabledMemberCount: number
}

// agent-stream（SSE 基础）依赖的载荷类型。
export interface AgentResultBlock {
  blockType: string
  title: string | null
  data: unknown
}

export interface AgentObservability {
  requestId: string | null
  correlationId: string | null
  traceId: string | null
  auditId: string | null
  logRef: string | null
}

export interface AgentChatPayload {
  conversationId?: EntityId | null
  message: string
  stream?: boolean
}

export function configureAuthRuntime(config: AuthRuntimeConfig) {
  authRuntime.getRefreshToken = config.getRefreshToken
  authRuntime.onAuthRefreshed = config.onAuthRefreshed
  authRuntime.onAuthExpired = config.onAuthExpired
}

export async function login(phone: string, password: string) {
  return request<AuthPayload>('/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify({ phone, password }),
  })
}

async function refreshAuth(refreshToken: string) {
  return request<AuthPayload>('/v1/auth/refresh', {
    method: 'POST',
    body: JSON.stringify({ refreshToken }),
  })
}

export async function logout(token?: string) {
  return request<void>('/v1/auth/logout', {
    method: 'POST',
    headers: token ? authHeaders(token) : undefined,
  })
}

export async function fetchCurrentUser(token: string) {
  return request<UserProfile>('/v1/auth/users/me', {
    headers: authHeaders(token),
  })
}

export async function fetchCurrentStore(token: string) {
  return request<CurrentStoreProfile>('/v2/stores/current', {
    headers: authHeaders(token),
  })
}

async function request<T>(path: string, init: RequestInit = {}, hasRetriedAuth = false): Promise<T> {
  const requestHeaders = headersToRecord(init.headers)
  const headers = buildHeaders(requestHeaders, init.body)
  const hasAuthHeader = hasAuthorization(requestHeaders)
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers,
  })

  const rawText = await response.text()
  const payload = rawText ? safeParse<T>(rawText) : null

  if (response.status === 401 && !hasRetriedAuth && hasAuthHeader) {
    const refreshedToken = await tryRefreshAccessToken()
    if (refreshedToken) {
      return request<T>(path, withAuthorization(init, refreshedToken), true)
    }
    emitApiAuthEvent(401)
  } else if (response.status === 403 && hasAuthHeader) {
    emitApiAuthEvent(403)
  }

  if (!response.ok || !payload || payload.code !== 0) {
    const message = payload?.message || `request failed: ${response.status}`
    throw new ApiError(message, response.status, payload?.code ?? -1)
  }

  return camelize(payload.data) as T
}

function authHeaders(token: string) {
  return {
    Authorization: `Bearer ${token}`,
  }
}

function buildHeaders(headers: Record<string, string>, body?: BodyInit | null) {
  if (body instanceof FormData) {
    return headers
  }
  if (body != null && !hasContentType(headers)) {
    return { ...headers, 'Content-Type': 'application/json' }
  }
  return headers
}

function headersToRecord(headers?: HeadersInit): Record<string, string> {
  if (!headers) return {}
  if (headers instanceof Headers) {
    const record: Record<string, string> = {}
    headers.forEach((value, key) => {
      record[key] = value
    })
    return record
  }
  if (Array.isArray(headers)) {
    return Object.fromEntries(headers) as Record<string, string>
  }
  return headers as Record<string, string>
}

function hasAuthorization(headers: Record<string, string>) {
  return typeof headers.Authorization === 'string' || typeof headers.authorization === 'string'
}

function hasContentType(headers: Record<string, string>) {
  return Object.keys(headers).some(key => key.toLowerCase() === 'content-type')
}

function withAuthorization(init: RequestInit, token: string): RequestInit {
  const headers = headersToRecord(init.headers)
  headers.Authorization = `Bearer ${token}`
  return {
    ...init,
    headers,
  }
}

async function tryRefreshAccessToken() {
  const refreshToken = authRuntime.getRefreshToken?.()
  if (!refreshToken || !authRuntime.onAuthRefreshed) {
    authRuntime.onAuthExpired?.()
    return null
  }
  try {
    const refreshed = await refreshAuth(refreshToken)
    authRuntime.onAuthRefreshed(refreshed)
    return refreshed.token
  } catch {
    authRuntime.onAuthExpired?.()
    return null
  }
}

export function emitApiAuthEvent(status: 401 | 403) {
  if (typeof window === 'undefined') return
  window.dispatchEvent(new CustomEvent('zhihuiji:web:api-auth', { detail: { status } }))
}

function safeParse<T>(rawText: string) {
  try {
    return JSON.parse(preserveUnsafeIntegers(rawText)) as ApiResponse<T>
  } catch {
    return null
  }
}

export function preserveUnsafeIntegers(rawText: string) {
  const chunks: string[] = []
  let inString = false
  let isEscaped = false

  for (let index = 0; index < rawText.length; index += 1) {
    const char = rawText[index]

    if (inString) {
      chunks.push(char)
      if (isEscaped) {
        isEscaped = false
      } else if (char === '\\') {
        isEscaped = true
      } else if (char === '"') {
        inString = false
      }
      continue
    }

    if (char === '"') {
      inString = true
      chunks.push(char)
      continue
    }

    if (char === '-' || isDigit(char)) {
      let cursor = index + 1
      while (cursor < rawText.length && isNumberTokenChar(rawText[cursor])) {
        cursor += 1
      }

      const token = rawText.slice(index, cursor)
      chunks.push(shouldPreserveInteger(token) ? `"${token}"` : token)
      index = cursor - 1
      continue
    }

    chunks.push(char)
  }

  return chunks.join('')
}

function shouldPreserveInteger(token: string) {
  if (!/^-?\d+$/.test(token)) {
    return false
  }

  const normalized = token.startsWith('-') ? token.slice(1) : token
  if (normalized.length < 16) {
    return false
  }

  const value = BigInt(token)
  return value > BigInt(Number.MAX_SAFE_INTEGER) || value < BigInt(Number.MIN_SAFE_INTEGER)
}

function isDigit(char: string) {
  return char >= '0' && char <= '9'
}

function isNumberTokenChar(char: string) {
  return isDigit(char) || char === 'e' || char === 'E' || char === '+' || char === '-' || char === '.'
}
