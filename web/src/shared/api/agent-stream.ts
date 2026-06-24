import type { AgentChatPayload, AgentObservability, AgentResultBlock } from '@/shared/api/client'
import { ApiError } from '@/shared/api/client'
import { camelize } from '@/shared/utils/camelize'
import { API_BASE_URL } from '@/shared/api/config'

const MAX_SAFE_INTEGER_BIGINT = BigInt(Number.MAX_SAFE_INTEGER)
const MIN_SAFE_INTEGER_BIGINT = BigInt(Number.MIN_SAFE_INTEGER)

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
      const chunk = decoder.decode(value, { stream: true })
      if (chunk) buffer += chunk

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
          eventData += line.slice(5).trimStart() + '\n'
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
  const chunks: string[] = []
  let inString = false
  let isEscaped = false
  const length = rawText.length

  for (let index = 0; index < length; index += 1) {
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
      while (cursor < length && isNumberTokenChar(rawText[cursor])) {
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
  const startIndex = token[0] === '-' ? 1 : 0
  const normalizedLength = token.length - startIndex
  if (normalizedLength < 16) return false
  for (let index = startIndex; index < token.length; index += 1) {
    const code = token.charCodeAt(index)
    if (code < 48 || code > 57) return false
  }
  const value = BigInt(token)
  return value > MAX_SAFE_INTEGER_BIGINT || value < MIN_SAFE_INTEGER_BIGINT
}

function isDigit(char: string) {
  const code = char.charCodeAt(0)
  return code >= 48 && code <= 57
}

function isNumberTokenChar(char: string) {
  return isDigit(char) || char === 'e' || char === 'E' || char === '+' || char === '-' || char === '.'
}
