import type { AgentChatPayload, AgentObservability, AgentResultBlock } from '@/shared/api/client'
import { ApiError } from '@/shared/api/client'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://127.0.0.1:18080'

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
  timestamp: number
}

export interface AgentContextCompactedEvent {
  eventType: 'context_compacted'
  runId: string
  compactedCount: number
  summary: string
  timestamp: number
}

export interface AgentRunCompletedEvent {
  eventType: 'run_completed'
  runId: string
  finalAnswer?: string | null
  mode?: string | null
  llmStatus?: string | null
  planSource?: string | null
  auditId?: string | null
  traceId?: string | null
  observability?: AgentObservability | null
  timestamp: number
}

export interface AgentRunCancelledEvent {
  eventType: 'run_cancelled'
  runId: string
  reason?: string | null
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
  const response = await fetch(`${API_BASE_URL}/v2/agent/chat/stream`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: 'text/event-stream',
      'Content-Type': 'application/json',
      'Cache-Control': 'no-cache',
    },
    body: JSON.stringify({
      conversation_id: payload.conversationId ?? null,
      message: payload.message,
      stream: true,
    }),
    signal,
  })

  if (response.status === 401 || response.status === 403) {
    emitApiAuthEvent(response.status)
  }

  if (!response.ok) {
    throw new ApiError(`request failed: ${response.status}`, response.status)
  }

  if (!response.body) {
    throw new ApiError('SSE 响应体为空', response.status)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let eventData = ''

  try {
    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      while (true) {
        const lineBreak = buffer.indexOf('\n')
        if (lineBreak < 0) break
        const rawLine = buffer.slice(0, lineBreak)
        buffer = buffer.slice(lineBreak + 1)
        const line = rawLine.endsWith('\r') ? rawLine.slice(0, -1) : rawLine

        if (!line.trim()) {
          flushEventData(eventData, onEvent)
          eventData = ''
          continue
        }
        if (line.startsWith(':')) continue
        if (line.startsWith('data:')) {
          eventData += `${line.slice(5).trimStart()}\n`
          continue
        }
        if (line.startsWith('event:') || line.startsWith('id:') || line.startsWith('retry:')) {
          continue
        }

        flushEventData(eventData, onEvent)
        eventData = ''
        flushEventData(line, onEvent)
      }
    }

    buffer += decoder.decode()
    if (buffer.trim()) {
      flushEventData(eventData, onEvent)
      eventData = ''
      flushEventData(buffer, onEvent)
    } else {
      flushEventData(eventData, onEvent)
    }
  } finally {
    reader.releaseLock()
  }
}

function flushEventData(raw: string, onEvent: (event: AgentStreamEvent) => void) {
  const normalized = raw.trim()
  if (!normalized || normalized === '[DONE]') return
  const parsed = parseEvent(normalized)
  if (parsed) onEvent(parsed)
}

function parseEvent(raw: string): AgentStreamEvent | null {
  try {
    return camelize(JSON.parse(preserveUnsafeIntegers(raw))) as AgentStreamEvent
  } catch {
    return {
      eventType: 'error',
      code: 'STREAM_PARSE_ERROR',
      message: `服务端返回了一条无法解析的 Agent 事件：${raw.slice(0, 160)}`,
      timestamp: Date.now(),
    }
  }
}

function emitApiAuthEvent(status: 401 | 403) {
  if (typeof window === 'undefined') return
  window.dispatchEvent(new CustomEvent('zhihuiji:web:api-auth', { detail: { status } }))
}

function preserveUnsafeIntegers(rawText: string) {
  let result = ''
  let inString = false
  let isEscaped = false

  for (let index = 0; index < rawText.length; index += 1) {
    const char = rawText[index]

    if (inString) {
      result += char
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
      result += char
      continue
    }

    if (char === '-' || isDigit(char)) {
      let cursor = index + 1
      while (cursor < rawText.length && /[0-9eE+.-]/.test(rawText[cursor])) {
        cursor += 1
      }

      const token = rawText.slice(index, cursor)
      result += shouldPreserveInteger(token) ? `"${token}"` : token
      index = cursor - 1
      continue
    }

    result += char
  }

  return result
}

function shouldPreserveInteger(token: string) {
  if (!/^-?\d+$/.test(token)) return false
  const normalized = token.startsWith('-') ? token.slice(1) : token
  if (normalized.length < 16) return false
  const value = BigInt(token)
  return value > BigInt(Number.MAX_SAFE_INTEGER) || value < BigInt(Number.MIN_SAFE_INTEGER)
}

function isDigit(char: string) {
  return char >= '0' && char <= '9'
}

function camelize(value: unknown): unknown {
  if (Array.isArray(value)) {
    return value.map((item) => camelize(item))
  }
  if (!value || typeof value !== 'object') {
    return value
  }
  return Object.fromEntries(
    Object.entries(value as Record<string, unknown>).map(([key, nested]) => [
      key.replace(/_([a-z])/g, (_, char: string) => char.toUpperCase()),
      camelize(nested),
    ]),
  )
}
