import { apiDownload, apiRequest, openApiEventStream, type QueryValue } from './client'

export type PageResponse<T> = {
  items: T[]
  total: number
  page: number
  size: number
  has_next?: boolean
  generated_at?: string
  scope_completeness?: string
}

export type AdminSession = {
  admin_user_id: string
  role: 'SUPER_ADMIN' | 'AUDIT_OBSERVER' | string
  permissions: string[]
  owner_user_ids: string[]
  store_ids: string[]
  content_access: boolean
  scope_completeness: 'COMPLETE' | 'PARTIAL' | string
}

export type AdminOverviewMetric = {
  key: string
  value: number | null
  unit: 'count' | 'percent' | 'milliseconds' | string
  availability: 'AVAILABLE' | 'UNAVAILABLE' | string
}

export type AdminOverview = {
  from: string
  to: string
  metrics: AdminOverviewMetric[]
  trend: Array<{ at: string; value: number }>
  estimated: boolean
  scope_completeness: 'COMPLETE' | 'PARTIAL' | string
  generated_at: string
}

export type AdminRun = {
  run_id: string
  conversation_id: string | null
  actor_user_id: string | null
  owner_user_id: string | null
  store_id: string | null
  terminal_status: string | null
  model_id: string | null
  started_at: string | null
  completed_at: string | null
  duration_ms: number | null
  time_to_first_token_ms: number | null
  iteration_count: number | null
  tool_call_count: number | null
  input_tokens: number | null
  output_tokens: number | null
  total_tokens: number | null
  token_source: 'EXACT' | 'ESTIMATED' | 'UNAVAILABLE' | string
  content_redacted: boolean
  scope_completeness: string
}

export type AdminUser = {
  user_id: string
  phone_masked: string | null
  nickname: string | null
  status: string | null
  created_at: string | null
  updated_at: string | null
  version: number
}

export type AdminStore = {
  store_id: string
  owner_user_id: string
  name: string | null
  status: string | null
  member_count: number
  created_at: string | null
  updated_at: string | null
  version: number
}

export type AdminMember = {
  user_id: string
  store_id: string
  nickname: string | null
  phone_masked: string | null
  role: string | null
  title: string | null
  status: string | null
  created_at: string | null
  updated_at: string | null
  version: number
}

export type AdminUsage = {
  run_id: string | null
  model_id: string | null
  bucket_start: string | null
  bucket_end: string | null
  request_count: number | null
  input_tokens: number | null
  output_tokens: number | null
  total_tokens: number | null
  duration_ms: number | null
  time_to_first_token_ms: number | null
  average_duration_ms: number | null
  p95_duration_ms: number | null
  average_time_to_first_token_ms: number | null
  p95_time_to_first_token_ms: number | null
  token_source: 'EXACT' | 'ESTIMATED' | 'UNAVAILABLE' | string
  estimated: boolean
  scope_completeness: string
}

export type AdminUsagePage = {
  items: AdminUsage[]
  total: number
  generated_at: string
  from: string | null
  to: string | null
  granularity: string | null
  scope_completeness: string | null
}

export type AdminRunEvent = {
  event_id: string
  run_id: string
  sequence: number
  event_type: string
  tool_name: string | null
  call_id: string | null
  occurred_at: string | null
  status: string | null
  duration_ms: number | null
  argument_summary: string | null
  result_summary: string | null
  redaction_state: 'FULL_ALLOWED' | 'PARTIAL' | 'REDACTED' | string
}

export type AdminRunEventPage = {
  items: AdminRunEvent[]
  total: number
  event_integrity: boolean
}

export type AdminMessage = {
  message_id: string
  conversation_id: string
  run_id: string | null
  role: string
  message_type: string | null
  content: string | null
  redaction_state: string | null
  occurred_at: string | null
}

export type AdminContextCheckpoint = {
  checkpoint_id: string
  conversation_id: string
  source_boundary_message_id: string | null
  source_message_count: number | null
  summary_version: number | null
  context_policy_version: number | null
  tool_schema_version: number | null
  revision: number | null
  quality: string | null
  status: string | null
  model_name: string | null
  estimated_input_tokens: number | null
  estimated_output_tokens: number | null
  created_at: string | null
  updated_at: string | null
  content_redacted: boolean
}

export type AdminContext = {
  run_id: string
  conversation_id: string | null
  context_window_tokens: number | null
  estimated_input_tokens: number | null
  estimated_output_tokens: number | null
  checkpoints: AdminContextCheckpoint[]
  content_redacted: boolean
  scope_completeness: string
  context_window_source: string | null
}

export type AdminDraft = {
  draft_id: string
  conversation_id: string
  draft_type: string
  title: string | null
  status: string
  created_at: string | null
  updated_at: string | null
  content_redacted: boolean
  confirmed_by: string | null
  confirmed_at: string | null
  business_reference: string | null
  failure_reason: string | null
}

export type AdminAgentConfig = {
  model_id: string | null
  agent_enabled: boolean
  enabled_tools: string[]
  version: number
  effective_state: string | null
  effective_at: string | null
  updated_by: string | null
}

export type AdminRetentionPolicy = {
  version: number
  audit_days: number
  message_days: number
  tool_result_days: number
  metrics_days: number
  content_mode: string
  effective_at: string | null
  updated_by: string | null
}

export type AdminHealthComponent = {
  service_name: string
  status: string
  version: string | null
  checked_at: string | null
  error_summary: string | null
  queue_depth: number | null
}

export type AdminHealthError = {
  component: string
  category: string
  summary: string
  occurred_at: string | null
}

export type AdminHealth = {
  status: string
  version: string | null
  generated_at: string | null
  components: AdminHealthComponent[]
  errors: AdminHealthError[]
}

export type AdminExportJob = {
  export_id: string
  export_type: string
  fields: string[]
  status: string
  created_at: string | null
  expires_at: string | null
  completed_at: string | null
  download_url: string | null
  content_redacted: boolean
  error_summary: string | null
  download_count: number
}

export type AdminAuditEvent = {
  event_id: string
  occurred_at: string
  actor_admin_user_id: string | null
  role: string | null
  action: string
  resource_type: string | null
  resource_id: string | null
  owner_user_id: string | null
  store_id: string | null
  result: string
  source_ip: string | null
  user_agent_summary: string | null
  request_id: string | null
  summary: string | null
  reason: string | null
}

export type AdminQuery = Record<string, QueryValue>

export function getAdminSession(): Promise<AdminSession> {
  return apiRequest('/v2/admin/session')
}

export function getAdminOverview(query: AdminQuery = {}): Promise<AdminOverview> {
  return apiRequest('/v2/admin/overview', {}, query)
}

export function getAdminRuns(query: AdminQuery = {}): Promise<PageResponse<AdminRun>> {
  return apiRequest('/v2/admin/agent/runs', {}, query)
}

export function getAdminAuditEvents(query: AdminQuery = {}): Promise<PageResponse<AdminAuditEvent>> {
  return apiRequest('/v2/admin/audit/events', {}, query)
}

export function getAdminUsers(query: AdminQuery = {}): Promise<PageResponse<AdminUser>> {
  return apiRequest('/v2/admin/users', {}, query)
}

export function getAdminStores(query: AdminQuery = {}): Promise<PageResponse<AdminStore>> {
  return apiRequest('/v2/admin/stores', {}, query)
}

export function getAdminStoreMembers(storeId: string, query: AdminQuery = {}): Promise<PageResponse<AdminMember>> {
  return apiRequest(`/v2/admin/stores/${encodeURIComponent(storeId)}/members`, {}, query)
}

export function updateAdminUser(userId: string, body: Record<string, unknown>): Promise<AdminUser> {
  return apiRequest(`/v2/admin/users/${encodeURIComponent(userId)}`, { method: 'PATCH', body: JSON.stringify(body) })
}

export function updateAdminStore(storeId: string, body: Record<string, unknown>): Promise<AdminStore> {
  return apiRequest(`/v2/admin/stores/${encodeURIComponent(storeId)}`, { method: 'PATCH', body: JSON.stringify(body) })
}

export function updateAdminMember(storeId: string, userId: string, body: Record<string, unknown>): Promise<AdminMember> {
  return apiRequest(`/v2/admin/stores/${encodeURIComponent(storeId)}/members/${encodeURIComponent(userId)}`, { method: 'PATCH', body: JSON.stringify(body) })
}

export function getAdminUsage(query: AdminQuery = {}): Promise<AdminUsagePage> {
  return apiRequest('/v2/admin/agent/usage', {}, query)
}

export function getAdminRun(runId: string, query: AdminQuery = {}): Promise<AdminRun> {
  return apiRequest(`/v2/admin/agent/runs/${encodeURIComponent(runId)}`, {}, query)
}

export function getAdminRunEvents(runId: string, query: AdminQuery = {}): Promise<AdminRunEventPage> {
  return apiRequest(`/v2/admin/agent/runs/${encodeURIComponent(runId)}/events`, {}, query)
}

export function getAdminMessages(conversationId: string, query: AdminQuery = {}): Promise<PageResponse<AdminMessage>> {
  return apiRequest(`/v2/admin/agent/conversations/${encodeURIComponent(conversationId)}/messages`, {}, query)
}

export function getAdminRunContext(runId: string, query: AdminQuery = {}): Promise<AdminContext> {
  return apiRequest(`/v2/admin/agent/runs/${encodeURIComponent(runId)}/context`, {}, query)
}

export function getAdminRunDrafts(runId: string, query: AdminQuery = {}): Promise<AdminDraft[]> {
  return apiRequest(`/v2/admin/agent/runs/${encodeURIComponent(runId)}/drafts`, {}, query)
}

export function streamAdminRunEvents(
  runId: string,
  afterSequence: number | null,
  handlers: { onEvent: (event: AdminRunEvent) => void; onError: (error: Error) => void; onComplete?: () => void },
): () => void {
  return openApiEventStream<AdminRunEvent>(`/v2/admin/agent/runs/${encodeURIComponent(runId)}/events/stream`, {
    afterSequence: afterSequence ?? undefined,
  }, handlers)
}

export function getAdminAgentConfig(query: AdminQuery = {}): Promise<AdminAgentConfig> {
  return apiRequest('/v2/admin/agent/config', {}, query)
}

export function updateAdminAgentConfig(body: Record<string, unknown>): Promise<AdminAgentConfig> {
  return apiRequest('/v2/admin/agent/config', { method: 'PATCH', body: JSON.stringify(body) })
}

export function getAdminHealth(): Promise<AdminHealth> {
  return apiRequest('/v2/admin/system/health')
}

export function getAdminRetentionPolicy(): Promise<AdminRetentionPolicy> {
  return apiRequest('/v2/admin/retention')
}

export function updateAdminRetentionPolicy(body: Record<string, unknown>): Promise<AdminRetentionPolicy> {
  return apiRequest('/v2/admin/retention', { method: 'PATCH', body: JSON.stringify(body) })
}

export function getAdminExportJobs(query: AdminQuery = {}): Promise<PageResponse<AdminExportJob>> {
  return apiRequest('/v2/admin/exports', {}, query)
}

export function createAdminExport(body: Record<string, unknown>): Promise<AdminExportJob> {
  return apiRequest('/v2/admin/exports', { method: 'POST', body: JSON.stringify(body) })
}

export function downloadAdminExport(exportId: string): Promise<Blob> {
  return apiDownload(`/v2/admin/exports/${encodeURIComponent(exportId)}/download`)
}
