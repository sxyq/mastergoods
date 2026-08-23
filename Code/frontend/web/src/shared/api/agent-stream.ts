import type { AgentChatPayload, AgentObservability, AgentResultBlock } from '@/shared/api/client'
import { ApiError, emitApiAuthEvent, preserveUnsafeIntegers } from '@/shared/api/client'
import { camelize } from '@/shared/utils/camelize'
import { API_BASE_URL } from '@/shared/api/config'

/**
 * Agent 运行统一终态。后端 REST、SSE、审计与 Web/Android/iOS 共用同一组大写值。
 * HTTP 200 或文本回答不能单独判定业务成功，必须依据 terminal_status。
 */
export type AgentTerminalStatus =
  | 'COMPLETED'
  | 'CONFIRMATION_PENDING'
  | 'FAILED'
  | 'BLOCKED'
  | 'CANCELLED'
  | 'EXHAUSTED'

/**
 * 终态事件公共字段：每个终态事件都携带大写 terminal_status；
 * 非成功终态必填 errorCode/safeMessage，EXHAUSTED 使用 completedTools/missingTargetTools。
 */
export interface AgentTerminalEventFields {
  terminalStatus: AgentTerminalStatus
  errorCode?: string | null
  safeMessage?: string | null
  completedTools?: string[]
  missingTargetTools?: string[]
}

export type AgentStreamEvent =
  | AgentRunStartedEvent
  | AgentSafetyCheckStartedEvent
  | AgentSafetyCheckPassedEvent
  | AgentSafetyCheckBlockedEvent
  | AgentPlanDeltaEvent
  | AgentToolStartedEvent
  | AgentToolProgressEvent
  | AgentToolCompletedEvent
  | AgentToolFailedEvent
  | AgentAnswerDeltaEvent
  | AgentAnswerCompletedEvent
  | AgentResultBlockEvent
  | AgentDraftCreatedEvent
  | AgentContextCompactedEvent
  | AgentRunCompletedEvent
  | AgentRunFailedEvent
  | AgentRunBlockedEvent
  | AgentRunExhaustedEvent
  | AgentRunCancelledEvent
  | AgentErrorEvent

export interface AgentRunStartedEvent {
  eventType: 'run_started'
  runId: string
  conversationId: string | number
  auditId?: string | null
  traceId?: string | null
  observability?: AgentObservability | null
  timestamp: number
}

export interface AgentSafetyCheckStartedEvent {
  eventType: 'safety_check_started'
  runId: string
  timestamp: number
}

export interface AgentSafetyCheckPassedEvent {
  eventType: 'safety_check_passed'
  runId: string
  timestamp: number
}

export interface AgentSafetyCheckBlockedEvent {
  eventType: 'safety_check_blocked'
  runId: string
  reason: string
  suggestedAction?: string | null
  timestamp: number
}

export interface AgentPlanDeltaEvent {
  eventType: 'plan_delta'
  runId: string
  planSource?: string | null
  content: string
  timestamp: number
}

export interface AgentToolStartedEvent {
  eventType: 'tool_started'
  eventId?: string | null
  seq?: number | null
  runId: string
  conversationId?: string | number | null
  toolCallId?: string | null
  toolName: string
  inputSummary?: string | null
  queryWindow?: unknown
  toolInput?: unknown
  startedAt?: number | null
  auditId?: string | null
  traceId?: string | null
  timestamp: number
}

export interface AgentToolProgressEvent {
  eventType: 'tool_progress'
  runId: string
  toolName: string
  message: string
  timestamp: number
}

export interface AgentToolCompletedEvent {
  eventType: 'tool_completed'
  eventId?: string | null
  seq?: number | null
  runId: string
  conversationId?: string | number | null
  toolCallId?: string | null
  toolName: string
  resultSummary?: string | null
  inputSummary?: string | null
  queryWindow?: unknown
  startedAt?: number | null
  completedAt?: number | null
  durationMs?: number | null
  returnedCount?: number | null
  totalCount?: number | null
  limit?: number | null
  isTruncated?: boolean | null
  evidence?: unknown
  nextCursor?: string | null
  auditId?: string | null
  traceId?: string | null
  timestamp: number
}

export interface AgentToolFailedEvent {
  eventType: 'tool_failed'
  eventId?: string | null
  seq?: number | null
  runId: string
  conversationId?: string | number | null
  toolCallId?: string | null
  toolName: string
  inputSummary?: string | null
  queryWindow?: unknown
  errorCode?: string | null
  safeMessage?: string | null
  errorSummary?: string | null
  startedAt?: number | null
  completedAt?: number | null
  durationMs?: number | null
  evidence?: unknown
  nextCursor?: string | null
  auditId?: string | null
  traceId?: string | null
  timestamp: number
}

export interface AgentAnswerDeltaEvent {
  eventType: 'answer_delta'
  eventId?: string | null
  seq?: number | null
  runId: string
  conversationId?: string | number | null
  delta: string
  deltaSource?: string | null
  auditId?: string | null
  traceId?: string | null
  observability?: AgentObservability | null
  timestamp: number
}

export interface AgentAnswerCompletedEvent {
  eventType: 'answer_completed'
  runId: string
  answer: string
  mode?: string | null
  llmStatus?: string | null
  planSource?: string | null
  auditId?: string | null
  traceId?: string | null
  observability?: AgentObservability | null
  timestamp: number
}

export interface AgentResultBlockEvent {
  eventType: 'result_block'
  runId: string
  block: AgentResultBlock
  timestamp: number
}

export interface AgentDraftCreatedEvent {
  eventType: 'draft_created'
  runId: string
  draftId: string | number
  draftType: string
  title: string
  status?: string | null
  toolName?: string | null
  summary?: string | null
  timestamp: number
}

export interface AgentContextCompactedEvent {
  eventType: 'context_compacted'
  runId: string
  checkpointId?: string | number | null
  sourceBoundaryMessageId?: string | number | null
  compactedCount: number
  summaryPreview?: string | null
  reason?: string | null
  reused?: boolean | null
  inputTokenEstimate?: number | null
  outputTokenEstimate?: number | null
  auditId?: string | null
  traceId?: string | null
  timestamp: number
}

export interface AgentRunCompletedEvent extends AgentTerminalEventFields {
  eventType: 'run_completed'
  runId: string
  terminalStatus: 'COMPLETED' | 'CONFIRMATION_PENDING'
  finalAnswer?: string | null
  mode?: string | null
  llmStatus?: string | null
  planSource?: string | null
  auditId?: string | null
  traceId?: string | null
  observability?: AgentObservability | null
  timestamp: number
}

export interface AgentRunFailedEvent extends AgentTerminalEventFields {
  eventType: 'run_failed'
  runId: string
  terminalStatus: 'FAILED'
  finalAnswer?: string | null
  mode?: string | null
  llmStatus?: string | null
  planSource?: string | null
  auditId?: string | null
  traceId?: string | null
  observability?: AgentObservability | null
  timestamp: number
}

export interface AgentRunBlockedEvent extends AgentTerminalEventFields {
  eventType: 'run_blocked'
  runId: string
  terminalStatus: 'BLOCKED'
  finalAnswer?: string | null
  mode?: string | null
  llmStatus?: string | null
  planSource?: string | null
  auditId?: string | null
  traceId?: string | null
  observability?: AgentObservability | null
  timestamp: number
}

export interface AgentRunExhaustedEvent extends AgentTerminalEventFields {
  eventType: 'run_exhausted'
  runId: string
  terminalStatus: 'EXHAUSTED'
  finalAnswer?: string | null
  mode?: string | null
  llmStatus?: string | null
  planSource?: string | null
  auditId?: string | null
  traceId?: string | null
  observability?: AgentObservability | null
  timestamp: number
}

export interface AgentRunCancelledEvent extends AgentTerminalEventFields {
  eventType: 'run_cancelled'
  runId: string
  terminalStatus: 'CANCELLED'
  reason?: string | null
  auditId?: string | null
  traceId?: string | null
  observability?: AgentObservability | null
  timestamp: number
}

export interface AgentErrorEvent {
  eventType: 'error'
  runId?: string | null
  code: string
  message: string
  timestamp: number
}

export interface AgentStreamSession {
  controller: AbortController
  done: Promise<void>
}

export interface AgentStreamReducerState {
  seenEventKeys: Set<string>
  terminalSeen: boolean
  lastEventId: string | null
}

/**
 * 终态事件类型集合。HTTP 200 或文本回答不能单独判定业务成功，
 * 必须收到这些事件之一并依据 terminal_status 判定。
 */
export const TERMINAL_EVENT_TYPES = [
  'run_completed',
  'run_failed',
  'run_blocked',
  'run_exhausted',
  'run_cancelled',
] as const

export type TerminalEventType = (typeof TERMINAL_EVENT_TYPES)[number]

export type AgentTerminalStreamEvent =
  | AgentRunCompletedEvent
  | AgentRunFailedEvent
  | AgentRunBlockedEvent
  | AgentRunExhaustedEvent
  | AgentRunCancelledEvent

export function isTerminalEvent(event: AgentStreamEvent): event is AgentTerminalStreamEvent {
  return TERMINAL_EVENT_TYPES.includes(event.eventType as TerminalEventType)
}

/**
 * 提取终态事件的 terminal_status；非终态事件返回 null。
 * 旧版缺少 terminal_status 的事件不能作为业务成功依据，返回 null 由调用方按失败处理。
 */
export function terminalStatusOf(event: AgentStreamEvent): AgentTerminalStatus | null {
  if (!isTerminalEvent(event)) {
    return null
  }
  const status = (event as AgentTerminalStreamEvent).terminalStatus
  return status ?? null
}

export function parseAgentStreamEvent(raw: string): AgentStreamEvent | null {
  const normalized = raw.trim()
  if (!normalized || normalized === '[DONE]') return null
  try {
    return camelize(JSON.parse(preserveUnsafeIntegers(normalized))) as AgentStreamEvent
  } catch {
    return {
      eventType: 'error',
      code: 'STREAM_PARSE_ERROR',
      message: `服务端返回了一条无法解析的 Agent 事件：${normalized.slice(0, 160)}`,
      timestamp: Date.now(),
    }
  }
}

export function acceptAgentStreamEvent(
  state: AgentStreamReducerState,
  event: AgentStreamEvent,
  sseId?: string | null,
): boolean {
  const keys = eventIdentityKeys(event, sseId)
  if (keys.some((key) => state.seenEventKeys.has(key)) || (state.terminalSeen && isTerminalEvent(event))) {
    if (sseId) state.lastEventId = sseId
    return false
  }
  keys.forEach((key) => state.seenEventKeys.add(key))
  if (sseId) state.lastEventId = sseId
  else {
    const eventId = (event as { eventId?: string | null }).eventId
    const seq = (event as { seq?: number | null }).seq
    state.lastEventId = eventId || (seq == null ? state.lastEventId : String(seq))
  }
  if (isTerminalEvent(event)) state.terminalSeen = true
  return true
}

export function streamAgentChat(
  token: string,
  payload: AgentChatPayload,
  onEvent: (event: AgentStreamEvent) => void,
): AgentStreamSession {
  const controller = new AbortController()
  const done = openAgentStream(token, payload, controller.signal, onEvent)
  return { controller, done }
}

async function openAgentStream(
  token: string,
  payload: AgentChatPayload,
  signal: AbortSignal,
  onEvent: (event: AgentStreamEvent) => void,
) {
  const state: AgentStreamReducerState = { seenEventKeys: new Set(), terminalSeen: false, lastEventId: null }
  let reconnectAttempt = 0
  while (!state.terminalSeen) {
    try {
      const headers: Record<string, string> = {
        Authorization: `Bearer ${token}`,
        Accept: 'text/event-stream',
        'Content-Type': 'application/json',
        'Cache-Control': 'no-cache',
      }
      if (state.lastEventId) headers['Last-Event-ID'] = state.lastEventId
      const response = await fetch(`${API_BASE_URL}/v2/agent/chat/stream`, {
        method: 'POST',
        headers,
        body: JSON.stringify({ conversation_id: payload.conversationId ?? null, message: payload.message, stream: true }),
        signal,
      })

      if (response.status === 401 || response.status === 403) emitApiAuthEvent(response.status)
      if (!response.ok) throw new ApiError(`request failed: ${response.status}`, response.status)
      if (!response.body) throw new ApiError('SSE 响应体为空', response.status)

      await readAgentSseResponse(response.body, state, onEvent, signal)
      return
    } catch (error) {
      if (signal.aborted) throw error
      if (error instanceof ApiError && error.status >= 400) throw error
      reconnectAttempt += 1
      if (reconnectAttempt > 3) throw error
      await waitForReconnect(reconnectAttempt, signal)
    }
  }
}

async function readAgentSseResponse(
  body: ReadableStream<Uint8Array>,
  state: AgentStreamReducerState,
  onEvent: (event: AgentStreamEvent) => void,
  signal: AbortSignal,
) {
  const reader = body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let eventData = ''
  let eventId: string | null = null
  const flush = () => {
    const event = parseAgentStreamEvent(eventData)
    eventData = ''
    if (event && acceptAgentStreamEvent(state, event, eventId)) onEvent(event)
    eventId = null
  }
  try {
    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      if (signal.aborted) throw new DOMException('The operation was aborted.', 'AbortError')
      buffer += decoder.decode(value, { stream: true })
      while (true) {
        const lineBreak = buffer.indexOf('\n')
        if (lineBreak < 0) break
        const rawLine = buffer.slice(0, lineBreak)
        buffer = buffer.slice(lineBreak + 1)
        const line = rawLine.endsWith('\r') ? rawLine.slice(0, -1) : rawLine
        if (!line.trim()) { flush(); continue }
        if (line.startsWith(':')) continue
        if (line.startsWith('data:')) { eventData += line.slice(5).trimStart() + '\n'; continue }
        if (line.startsWith('id:')) { eventId = line.slice(3).trimStart(); continue }
        if (line.startsWith('event:') || line.startsWith('retry:')) continue
        flush()
        eventData = line
        flush()
      }
    }
    buffer += decoder.decode()
    if (buffer.trim()) { eventData += buffer; flush() } else flush()
  } finally {
    reader.releaseLock()
  }
}

function eventIdentityKeys(event: AgentStreamEvent, sseId?: string | null): string[] {
  const keys: string[] = []
  if (sseId) keys.push(`sse:${sseId}`)
  const runId = (event as { runId?: string | null }).runId || ''
  const eventId = (event as { eventId?: string | null }).eventId
  const seq = (event as { seq?: number | null }).seq
  const callId = (event as { toolCallId?: string | null }).toolCallId
  if (eventId) keys.push(`event:${eventId}`)
  if (seq != null) keys.push(`seq:${runId}:${seq}`)
  if (callId) keys.push(`call:${event.eventType}:${runId}:${callId}`)
  if (isTerminalEvent(event) && runId) keys.push(`terminal:${runId}`)
  return keys
}

function waitForReconnect(attempt: number, signal: AbortSignal) {
  return new Promise<void>((resolve, reject) => {
    const timer = setTimeout(resolve, Math.min(2000, attempt * 250))
    signal.addEventListener('abort', () => { clearTimeout(timer); reject(new DOMException('The operation was aborted.', 'AbortError')) }, { once: true })
  })
}
