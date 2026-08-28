export type AdminRole = 'SUPER_ADMIN' | 'AUDIT_OBSERVER'

export type AdminPermission =
  | 'admin:session:view'
  | 'admin:overview:view'
  | 'admin:users:view'
  | 'admin:users:manage'
  | 'admin:stores:view'
  | 'admin:stores:manage'
  | 'admin:agent:view'
  | 'admin:agent:content:view'
  | 'admin:config:manage'
  | 'admin:audit:view'
  | 'admin:system:view'
  | 'admin:export:create'
  | 'admin:retention:manage'

export interface AdminDataScope {
  ownerUserIds: string[]
  storeIds: string[]
}

export interface AdminSession {
  adminUserId: string
  role: AdminRole
  permissions: AdminPermission[]
  scope: AdminDataScope
  contentAccess: boolean
  scopeCompleteness: 'COMPLETE' | 'PARTIAL' | 'UNKNOWN'
}

export type AdminTokenSource = 'EXACT' | 'ESTIMATED' | 'UNAVAILABLE'

export interface AdminRunSummary {
  runId: string
  conversationId: string | null
  actorUserId: string | null
  ownerUserId: string
  storeId: string | null
  terminalStatus: string
  modelId: string | null
  startedAt: string
  completedAt: string | null
  durationMs: number | null
  timeToFirstTokenMs: number | null
  iterationCount: number | null
  toolCallCount: number | null
  inputTokens: number | null
  outputTokens: number | null
  totalTokens: number | null
  tokenSource: AdminTokenSource
  contentRedacted: boolean
  scopeCompleteness: 'COMPLETE' | 'PARTIAL' | 'UNKNOWN'
}

export interface AdminEvent {
  eventId: string
  runId: string
  sequence: number
  eventType: string
  toolName: string | null
  callId: string | null
  occurredAt: string
  status: string
  durationMs: number | null
  argumentSummary: string | null
  resultSummary: string | null
  redactionState: 'FULL_ALLOWED' | 'PARTIAL' | 'REDACTED'
}
