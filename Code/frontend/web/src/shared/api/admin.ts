import type { AdminPermission, AdminRole, AdminSession } from '@/entities/admin/contracts'
import type { AdminEvent, AdminRunSummary } from '@/entities/admin/contracts'
import { requestAdmin, requestAdminStream } from '@/shared/api/client'
import { camelize } from '@/shared/utils/camelize'

export const adminApiPaths = {
  session: '/v2/admin/session',
  overview: '/v2/admin/overview',
  users: '/v2/admin/users',
  stores: '/v2/admin/stores',
  agentRuns: '/v2/admin/agent/runs',
  agentUsage: '/v2/admin/agent/usage',
  agentConfig: '/v2/admin/agent/config',
  auditEvents: '/v2/admin/audit/events',
  systemHealth: '/v2/admin/system/health',
  exports: '/v2/admin/exports',
  retention: '/v2/admin/retention',
} as const

export interface AdminSessionContract extends AdminSession {
  role: AdminRole
  permissions: AdminPermission[]
}

/**
 * Admin API paths and DTO boundary. Request functions are intentionally added
 * with the authenticated admin session implementation in the I1 batch.
 */
export const adminApiContract = Object.freeze(adminApiPaths)

export interface AdminPage<T> {
  items: T[]
  page: number
  size: number
  total: number
  hasNext: boolean
  generatedAt?: string
  scope?: AdminScopePayload
  scopeCompleteness?: 'COMPLETE' | 'PARTIAL' | 'UNKNOWN' | string
}

export interface AdminScopePayload {
  allOwners: boolean
  ownerUserIds: string[]
  storeIds: string[]
  includeInactive: boolean
  contentMode: string
}

export interface AdminOverviewPayload {
  from?: string
  to?: string
  metrics: Array<{ key: string; value: number; unit?: string }>
  trend: Array<{ at: string; value: number }>
  estimated: boolean
  scopeCompleteness?: string
  generatedAt?: string
  scope?: AdminScopePayload
}

export interface AdminConfigPayload {
  modelId: string | null
  agentEnabled: boolean
  version: number
  effectiveState: string
}

export interface AdminStoreSummary {
  storeId: string
  ownerUserId: string
  name: string
  status: string
  memberCount: number
  createdAt: string
  updatedAt: string
}

export interface AdminMessage {
  messageId: string
  conversationId: string
  runId: string | null
  role: string
  messageType: string
  content: string | null
  redactionState: string
  occurredAt: string
}

export interface AdminEventPage {
  items: AdminEvent[]
  total: number
  eventIntegrity: boolean
}

export interface AdminContextResponse {
  runId: string
  conversationId: string | null
  contextWindowTokens: number | null
  estimatedInputTokens: number | null
  estimatedOutputTokens: number | null
  checkpoints: Array<Record<string, unknown>>
  contentRedacted: boolean
  scopeCompleteness?: string
}

export interface AdminDraft {
  draftId: string
  conversationId: string | null
  draftType: string
  title: string
  status: string
  createdAt: string
  updatedAt: string
  contentRedacted: boolean
}

export interface AdminUsage {
  runId: string
  modelId: string | null
  inputTokens: number | null
  outputTokens: number | null
  totalTokens: number | null
  durationMs: number | null
  timeToFirstTokenMs: number | null
  tokenSource: string
  estimated: boolean
  scopeCompleteness?: string
}

export interface AdminAuditEvent {
  eventId?: string
  actorAdminUserId?: string | null
  action: string
  resourceType?: string | null
  resourceId?: string | null
  result?: string | null
  occurredAt?: string
  summary?: string | null
}

export interface AdminHealthService {
  serviceName?: string
  status?: string
  version?: string
  checkedAt?: string
  errorSummary?: string | null
}

export interface AdminHealthPayload {
  status?: string
  version?: string
  generatedAt?: string
  services?: AdminHealthService[]
  errors?: Array<Record<string, unknown>>
}

export interface AdminExportJob {
  exportId: string
  exportType?: string
  status?: string
  createdAt?: string
  expiresAt?: string
  downloadUrl?: string | null
  contentRedacted?: boolean
}

export interface AdminRetentionPayload {
  version?: number
  auditDays?: number
  messageDays?: number
  toolResultDays?: number
  metricsDays?: number
  effectiveAt?: string
  contentMode?: string
}

function query(params: Record<string, string | number | boolean | undefined | null>) {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') search.set(key, String(value))
  }
  const encoded = search.toString()
  return encoded ? `?${encoded}` : ''
}

export function fetchAdminSession(token: string) {
  return requestAdmin<AdminSessionContract>(token, adminApiPaths.session)
}

export function fetchAdminOverview(token: string, params: { from?: string; to?: string; ownerUserId?: string; storeId?: string } = {}) {
  return requestAdmin<AdminOverviewPayload>(token, `${adminApiPaths.overview}${query(params)}`)
}

export function fetchAdminUsers(token: string, params: { query?: string; ownerUserId?: string; storeId?: string; page?: number; size?: number } = {}) {
  return requestAdmin<AdminPage<AdminUserPayload>>(token, `${adminApiPaths.users}${query(params)}`)
}

export interface AdminUserPayload {
  userId: string
  phoneMasked: string
  nickname: string
  status: string
  createdAt: string
  updatedAt: string
}

export function fetchAdminStores(token: string, params: { ownerUserId?: string; storeId?: string; page?: number; size?: number } = {}) {
  return requestAdmin<AdminPage<AdminStoreSummary>>(token, `${adminApiPaths.stores}${query(params)}`)
}

export function fetchAdminRuns(token: string, params: { runId?: string; conversationId?: string; terminalStatus?: string; from?: string; to?: string; ownerUserId?: string; storeId?: string; page?: number; size?: number } = {}) {
  return requestAdmin<AdminPage<AdminRunSummary>>(token, `${adminApiPaths.agentRuns}${query(params)}`)
}

export function fetchAdminRun(token: string, runId: string, params: { ownerUserId?: string; storeId?: string } = {}) {
  return requestAdmin<AdminRunSummary>(token, `/v2/admin/agent/runs/${encodeURIComponent(runId)}${query(params)}`)
}

export function fetchAdminMessages(token: string, conversationId: string, params: { includeContent?: boolean; ownerUserId?: string; storeId?: string; page?: number; size?: number } = {}) {
  return requestAdmin<AdminPage<AdminMessage>>(token, `/v2/admin/agent/conversations/${encodeURIComponent(conversationId)}/messages${query(params)}`)
}

export function fetchAdminEvents(token: string, runId: string, params: { afterSequence?: number; includeContent?: boolean; ownerUserId?: string; storeId?: string } = {}) {
  return requestAdmin<AdminEventPage>(token, `/v2/admin/agent/runs/${encodeURIComponent(runId)}/events${query(params)}`)
}

export async function streamAdminEvents(
  token: string,
  runId: string,
  params: { afterSequence?: number; includeContent?: boolean; ownerUserId?: string; storeId?: string } = {},
  onEvent: (event: AdminEvent) => void,
  signal?: AbortSignal,
) {
  const response = await requestAdminStream(token, `/v2/admin/agent/runs/${encodeURIComponent(runId)}/events/stream${query(params)}`, signal)
  if (!response.body) return
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  try {
    while (true) {
      const chunk = await reader.read()
      if (chunk.done) break
      buffer += decoder.decode(chunk.value, { stream: true })
      const frames = buffer.split(/\r?\n\r?\n/)
      buffer = frames.pop() ?? ''
      for (const frame of frames) {
        const data = frame.split(/\r?\n/).filter((line) => line.startsWith('data:')).map((line) => line.slice(5).trim()).join('\n')
        if (!data) continue
        try {
          const parsed = camelize(JSON.parse(data)) as AdminEvent
          if (parsed && typeof parsed === 'object' && typeof parsed.sequence === 'number') onEvent(parsed)
        } catch {
          // A malformed SSE frame is ignored; the persisted list remains the source of truth.
        }
      }
    }
  } finally {
    reader.releaseLock()
  }
}

export function fetchAdminUsage(token: string, params: { from?: string; to?: string; ownerUserId?: string; storeId?: string; page?: number; size?: number } = {}) {
  return requestAdmin<{ items: AdminUsage[]; total: number; generatedAt?: string }>(token, adminApiPaths.agentUsage + query(params))
}

export function fetchAdminContext(token: string, runId: string, params: { ownerUserId?: string; storeId?: string } = {}) {
  return requestAdmin<AdminContextResponse>(token, `/v2/admin/agent/runs/${encodeURIComponent(runId)}/context${query(params)}`)
}

export function fetchAdminDrafts(token: string, runId: string, params: { ownerUserId?: string; storeId?: string } = {}) {
  return requestAdmin<AdminDraft[]>(token, `/v2/admin/agent/runs/${encodeURIComponent(runId)}/drafts${query(params)}`)
}

export function fetchAdminConfig(token: string) {
  return requestAdmin<AdminConfigPayload>(token, adminApiPaths.agentConfig)
}

export function fetchAdminAuditEvents(token: string, params: { from?: string; to?: string; action?: string; resourceType?: string; result?: string; page?: number; size?: number } = {}) {
  return requestAdmin<AdminPage<AdminAuditEvent>>(token, `${adminApiPaths.auditEvents}${query(params)}`)
}

export function fetchAdminSystemHealth(token: string, params: { serviceName?: string; from?: string; to?: string } = {}) {
  return requestAdmin<AdminHealthPayload>(token, `${adminApiPaths.systemHealth}${query(params)}`)
}

export function fetchAdminExports(token: string, params: { page?: number; size?: number } = {}) {
  return requestAdmin<AdminPage<AdminExportJob>>(token, `${adminApiPaths.exports}${query(params)}`)
}

export function createAdminExport(token: string, payload: { exportType: string; fields: string[]; from?: string; to?: string; ownerUserId?: string; storeId?: string; reason: string; idempotencyKey: string }) {
  return requestAdmin<AdminExportJob>(token, adminApiPaths.exports, { method: 'POST', body: JSON.stringify(payload) })
}

export function fetchAdminRetention(token: string) {
  return requestAdmin<AdminRetentionPayload>(token, adminApiPaths.retention)
}
