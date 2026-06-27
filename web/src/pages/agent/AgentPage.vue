<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useSession } from '@/app/stores/session'
import {
  cancelAgentRun,
  createAgentConversation,
  createAgentDraft,
  deleteAgentConversation,
  deleteAgentDraft,
  fetchAgentConversations,
  fetchAgentDrafts,
  fetchAgentMessages,
  fetchAgentNotifications,
  fetchAgentRunAudit,
  fetchAgentTasks,
  fetchAgentWorkbench,
  markAgentNotificationRead,
  updateAgentDraft,
  type AgentConversation,
  type AgentDraft,
  type AgentNotification,
  type AgentResultBlock,
  type AgentRunAudit,
  type AgentTask,
  type AgentWorkbench,
} from '@/shared/api/client'
import {
  type AgentAnswerCompletedEvent,
  type AgentAnswerDeltaEvent,
  type AgentContextCompactedEvent,
  type AgentDraftCreatedEvent,
  type AgentErrorEvent,
  type AgentPlanDeltaEvent,
  type AgentRunCancelledEvent,
  type AgentRunCompletedEvent,
  type AgentRunStartedEvent,
  type AgentStreamEvent,
  type AgentToolCompletedEvent,
  type AgentToolFailedEvent,
  type AgentToolProgressEvent,
  type AgentToolStartedEvent,
  streamAgentChat,
} from '@/shared/api/agent-stream'
import { readQueryId, sameEntityId, type EntityId } from '@/shared/utils/id'
import { formatDateTime, formatDuration } from '@/shared/utils/business'
import PageEmptyState from '@/shared/ui/PageEmptyState.vue'
import PageStatusBanner from '@/shared/ui/PageStatusBanner.vue'

type UiRole = 'user' | 'assistant' | 'system'

interface UiToolCall {
  key: string
  toolName: string
  status: string
  inputSummary: string
  resultSummary: string
  durationMs: number | null
  errorMessage: string
}

interface UiRunTrace {
  runId: string | null
  auditId: string | null
  traceId: string | null
  mode: string | null
  llmStatus: string | null
  planSource: string | null
  planSteps: AgentPlanDeltaEvent[]
  toolCalls: UiToolCall[]
  resultBlocks: AgentResultBlock[]
  draft: AgentDraftCreatedEvent | null
  compacted: AgentContextCompactedEvent | null
}

interface UiMessage {
  id: string
  serverId?: EntityId
  conversationId: EntityId | null
  role: UiRole
  content: string
  createdAt: number
  isStreaming: boolean
  error: string
  showTrace: boolean
  runTrace: UiRunTrace | null
}

interface MarkdownSection {
  type: 'heading' | 'list' | 'ordered-list' | 'paragraph' | 'code' | 'table'
  level?: number
  text?: string
  items?: string[]
  code?: string
  lang?: string
  tableHeaders?: string[]
  tableRows?: string[][]
}

interface BlockDerivedState {
  normalizedType: string
  tableRows: Record<string, unknown>[]
  tableHeaders: string[]
  listItems: string[]
  objectEntries: [string, unknown][]
}

const route = useRoute()
const session = useSession()

const workbench = ref<AgentWorkbench | null>(null)
const conversations = ref<AgentConversation[]>([])
const drafts = ref<AgentDraft[]>([])
const tasks = ref<AgentTask[]>([])
const notifications = ref<AgentNotification[]>([])
const messages = ref<UiMessage[]>([])
const selectedConversationId = ref<EntityId | null>(null)
const loading = ref(false)
const sending = ref(false)
const stopping = ref(false)
const error = ref('')
const inputText = ref('')
const currentRunId = ref<string | null>(null)
const currentStreamMessageId = ref<string | null>(null)
const auditDrawerOpen = ref(false)
const auditLoading = ref(false)
const auditRecord = ref<AgentRunAudit | null>(null)
const consumedQueryQuestion = ref(false)
const isDraftEditorOpen = ref(false)
const editingDraftId = ref<EntityId | null>(null)
const draftTitle = ref('')
const draftType = ref('note')
const draftContentJson = ref('')
const draftStatus = ref<'active' | 'archived'>('active')
const draftSaving = ref(false)
const canWrite = computed(() => session.hasPermission(['agent:write']))
const canView = computed(() => session.hasPermission(['agent:view']))
const isApiSource = computed(() => session.source.value === 'api' && Boolean(session.token.value))
const queryConversationId = computed(() => readQueryId(route.query.conversationId))
const queryQuestion = computed(() => {
  const raw = route.query.q
  return typeof raw === 'string' ? raw.trim() : ''
})
const selectedConversationTitle = computed(
  () => conversations.value.find((item) => sameEntityId(item.id, selectedConversationId.value))?.title || '新会话',
)

const streamState = reactive<{
  controller: AbortController | null
  done: Promise<void> | null
}>({
  controller: null,
  done: null,
})
const markdownSectionsCache = new Map<string, MarkdownSection[]>()
const blockDerivedCache = new WeakMap<AgentResultBlock, BlockDerivedState>()
const messageById = new Map<string, UiMessage>()
const HEADING_SECTION_REGEX = /^(#{1,3})\s+(.+)$/
const BULLET_SECTION_REGEX = /^[-*]\s+/
const ORDERED_LIST_REGEX = /^\d+\.\s+(.+)$/
const CODE_FENCE_REGEX = /^```(.*)$/
const TABLE_SEPARATOR_REGEX = /^[\s|:-]+$/

watch(
  [() => session.source.value, () => session.token.value],
  async () => {
    if (!isApiSource.value || !session.token.value) {
      workbench.value = null
      conversations.value = []
      messages.value = []
      messageById.clear()
      error.value = ''
      return
    }
    await loadPage()
  },
  { immediate: true },
)

watch(selectedConversationId, async (nextId, prevId) => {
  if (!nextId || sameEntityId(nextId, prevId) || !session.token.value) return
  await loadMessages(nextId)
})

async function fetchSidePanel() {
  const [nextWorkbench, nextConversations, nextDrafts, nextTasks, nextNotifications] = await Promise.all([
    fetchAgentWorkbench(session.token.value),
    fetchAgentConversations(session.token.value, { page: 0, limit: 50 }),
    fetchAgentDrafts(session.token.value, { page: 0, limit: 20 }),
    fetchAgentTasks(session.token.value),
    fetchAgentNotifications(session.token.value),
  ])
  workbench.value = nextWorkbench
  conversations.value = nextConversations
  drafts.value = nextDrafts
  tasks.value = nextTasks
  notifications.value = nextNotifications
  return nextConversations
}

async function loadPage() {
  if (!session.token.value) return
  loading.value = true
  error.value = ''
  try {
    const nextConversations = await fetchSidePanel()
    selectedConversationId.value = queryConversationId.value ?? nextConversations[0]?.id ?? null
    if (selectedConversationId.value) {
      await loadMessages(selectedConversationId.value)
    } else {
      messages.value = []
      messageById.clear()
    }

    if (queryQuestion.value && !consumedQueryQuestion.value) {
      consumedQueryQuestion.value = true
      inputText.value = queryQuestion.value
      await sendMessage(queryQuestion.value)
    }
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : 'AI 助手页面加载失败'
  } finally {
    loading.value = false
  }
}

async function loadMessages(conversationId: EntityId) {
  if (!session.token.value) return
  try {
    const rows = await fetchAgentMessages(session.token.value, conversationId, { page: 0, limit: 80 })
    const nextMessages = new Array<UiMessage>(rows.length)
    for (let index = 0; index < rows.length; index += 1) {
      const row = rows[index]
      nextMessages[index] = {
        id: `server-${row.id}`,
        serverId: row.id,
        conversationId: row.conversationId,
        role: normalizeRole(row.role),
        content: row.content,
        createdAt: row.createdAt,
        isStreaming: false,
        error: '',
        showTrace: false,
        runTrace: null,
      }
    }
    messages.value = nextMessages
    messageById.clear()
    for (const message of nextMessages) {
      messageById.set(message.id, message)
    }
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : '会话消息加载失败'
  }
}

async function handleCreateConversation() {
  if (!session.token.value || !canWrite.value) return
  try {
    const created = await createAgentConversation(session.token.value, {
      title: '新会话',
    })
    conversations.value = [created, ...conversations.value]
    selectedConversationId.value = created.id
    messages.value = []
  } catch (createErr) {
    error.value = createErr instanceof Error ? createErr.message : '新会话创建失败'
  }
}

async function handleDeleteConversation(conversationId: EntityId) {
  if (!session.token.value || !canWrite.value) return
  try {
    await deleteAgentConversation(session.token.value, conversationId)
    conversations.value = conversations.value.filter((item) => !sameEntityId(item.id, conversationId))
    if (sameEntityId(selectedConversationId.value, conversationId)) {
      selectedConversationId.value = conversations.value[0]?.id ?? null
      if (!selectedConversationId.value) {
        messages.value = []
        messageById.clear()
      }
    }
  } catch (deleteErr) {
    error.value = deleteErr instanceof Error ? deleteErr.message : '会话删除失败'
  }
}

async function sendMessage(presetText?: string) {
  if (!session.token.value || !canWrite.value || sending.value) return
  const text = (presetText ?? inputText.value).trim()
  if (!text) return

  sending.value = true
  error.value = ''
  auditDrawerOpen.value = false
  auditRecord.value = null

  try {
    let conversationId = selectedConversationId.value
    if (!conversationId) {
      const created = await createAgentConversation(session.token.value, {
        title: text.slice(0, 20) || '新会话',
      })
      conversations.value = [created, ...conversations.value]
      conversationId = created.id
      selectedConversationId.value = created.id
    }

    const now = Date.now()
    const userMessage: UiMessage = {
      id: localId('user'),
      conversationId,
      role: 'user',
      content: text,
      createdAt: now,
      isStreaming: false,
      error: '',
      showTrace: false,
      runTrace: null,
    }
    const assistantMessage: UiMessage = {
      id: localId('assistant'),
      conversationId,
      role: 'assistant',
      content: '',
      createdAt: now + 1,
      isStreaming: true,
      error: '',
      showTrace: false,
      runTrace: createEmptyRunTrace(),
    }
    currentStreamMessageId.value = assistantMessage.id
    messages.value.push(userMessage, assistantMessage)
    messageById.set(userMessage.id, userMessage)
    messageById.set(assistantMessage.id, assistantMessage)
    inputText.value = ''

    const sessionStream = streamAgentChat(session.token.value, {
      conversationId,
      message: text,
      stream: true,
    }, (event) => handleStreamEvent(assistantMessage.id, event))

    streamState.controller = sessionStream.controller
    streamState.done = sessionStream.done

    await sessionStream.done
  } catch (sendErr) {
    const message = sendErr instanceof Error ? sendErr.message : 'AI 对话发送失败'
    error.value = message
    markStreamingMessageError(message)
  } finally {
    sending.value = false
    stopping.value = false
    streamState.controller = null
    streamState.done = null
    currentRunId.value = null
    currentStreamMessageId.value = null
    await refreshSidePanel()
  }
}

async function stopStreaming() {
  if (!streamState.controller) return
  stopping.value = true
  streamState.controller.abort()
  const runId = currentRunId.value
  markStreamingMessageError('已停止本地接收，正在取消服务端运行')
  if (runId && session.token.value) {
    try {
      await cancelAgentRun(session.token.value, runId)
    } catch (cancelErr) {
      error.value = cancelErr instanceof Error ? cancelErr.message : '服务端取消失败'
    }
  }
}

function handleStreamEvent(messageId: string, event: AgentStreamEvent) {
  switch (event.eventType) {
    case 'run_started':
      onRunStarted(messageId, event)
      break
    case 'plan_delta':
      mutateMessage(messageId, (message) => {
        message.runTrace?.planSteps.push(event)
      })
      break
    case 'tool_started':
      onToolStarted(messageId, event)
      break
    case 'tool_progress':
      onToolProgress(messageId, event)
      break
    case 'tool_completed':
      onToolCompleted(messageId, event)
      break
    case 'tool_failed':
      onToolFailed(messageId, event)
      break
    case 'answer_delta':
      onAnswerDelta(messageId, event)
      break
    case 'answer_completed':
      onAnswerCompleted(messageId, event)
      break
    case 'result_block':
      mutateMessage(messageId, (message) => {
        message.runTrace?.resultBlocks.push(event.block)
      })
      break
    case 'draft_created':
      mutateMessage(messageId, (message) => {
        if (message.runTrace) message.runTrace.draft = event
      })
      break
    case 'context_compacted':
      mutateMessage(messageId, (message) => {
        if (message.runTrace) message.runTrace.compacted = event
      })
      break
    case 'run_completed':
      onRunCompleted(messageId, event)
      break
    case 'run_cancelled':
      onRunCancelled(messageId, event)
      break
    case 'error':
      onErrorEvent(messageId, event)
      break
    case 'safety_check_blocked':
      onErrorEvent(messageId, {
        eventType: 'error',
        runId: event.runId,
        code: 'SAFETY_BLOCKED',
        message: event.reason,
        timestamp: event.timestamp,
      })
      break
    default:
      break
  }
}

function onRunStarted(messageId: string, event: AgentRunStartedEvent) {
  currentRunId.value = event.runId
  if (!sameEntityId(selectedConversationId.value, event.conversationId)) {
    selectedConversationId.value = event.conversationId
  }
  mutateMessage(messageId, (message) => {
    message.conversationId = event.conversationId
    if (message.runTrace) {
      message.runTrace.runId = event.runId
      message.runTrace.auditId = event.auditId || null
      message.runTrace.traceId = event.traceId || null
    }
  })
}

function onToolStarted(messageId: string, event: AgentToolStartedEvent) {
  mutateMessage(messageId, (message) => {
    if (!message.runTrace) return
    message.runTrace.toolCalls.push({
      key: event.toolCallId || event.eventId || `${event.toolName}-${message.runTrace.toolCalls.length}`,
      toolName: event.toolName,
      status: 'running',
      inputSummary: event.inputSummary || '',
      resultSummary: '',
      durationMs: null,
      errorMessage: '',
    })
  })
}

function onToolProgress(messageId: string, event: AgentToolProgressEvent) {
  mutateMessage(messageId, (message) => {
    const toolCall = message.runTrace?.toolCalls.find((item) => item.toolName === event.toolName && item.status === 'running')
    if (toolCall) {
      toolCall.resultSummary = event.message
    }
  })
}

function onToolCompleted(messageId: string, event: AgentToolCompletedEvent) {
  mutateMessage(messageId, (message) => {
    const key = event.toolCallId || event.eventId || event.toolName
    const toolCall = message.runTrace?.toolCalls.find((item) => item.key === key || item.toolName === event.toolName)
    if (toolCall) {
      toolCall.status = 'completed'
      toolCall.resultSummary = event.resultSummary || ''
      toolCall.durationMs = event.durationMs ?? null
    }
  })
}

function onToolFailed(messageId: string, event: AgentToolFailedEvent) {
  mutateMessage(messageId, (message) => {
    const key = event.toolCallId || event.eventId || event.toolName
    const toolCall = message.runTrace?.toolCalls.find((item) => item.key === key || item.toolName === event.toolName)
    if (toolCall) {
      toolCall.status = 'failed'
      toolCall.errorMessage = event.safeMessage || event.errorSummary || '工具调用失败'
      toolCall.durationMs = event.durationMs ?? null
    }
  })
}

function onAnswerDelta(messageId: string, event: AgentAnswerDeltaEvent) {
  mutateMessage(messageId, (message) => {
    message.content += event.delta
  })
}

function onAnswerCompleted(messageId: string, event: AgentAnswerCompletedEvent) {
  mutateMessage(messageId, (message) => {
    message.content = event.answer || message.content
    if (message.runTrace) {
      message.runTrace.mode = event.mode || null
      message.runTrace.llmStatus = event.llmStatus || null
      message.runTrace.planSource = event.planSource || null
      message.runTrace.auditId = event.auditId || message.runTrace.auditId
      message.runTrace.traceId = event.traceId || message.runTrace.traceId
    }
  })
}

function onRunCompleted(messageId: string, event: AgentRunCompletedEvent) {
  mutateMessage(messageId, (message) => {
    if (event.finalAnswer && !message.content.trim()) {
      message.content = event.finalAnswer
    }
    message.isStreaming = false
    if (message.runTrace) {
      message.runTrace.mode = event.mode || message.runTrace.mode
      message.runTrace.llmStatus = event.llmStatus || message.runTrace.llmStatus
      message.runTrace.planSource = event.planSource || message.runTrace.planSource
      message.runTrace.auditId = event.auditId || message.runTrace.auditId
      message.runTrace.traceId = event.traceId || message.runTrace.traceId
    }
  })
}

function onRunCancelled(messageId: string, event: AgentRunCancelledEvent) {
  mutateMessage(messageId, (message) => {
    message.isStreaming = false
    message.error = event.reason || '本次生成已取消'
  })
}

function onErrorEvent(messageId: string, event: AgentErrorEvent) {
  mutateMessage(messageId, (message) => {
    message.isStreaming = false
    message.error = event.message
  })
}

function mutateMessage(messageId: string, mutate: (message: UiMessage) => void) {
  const target = messageById.get(messageId)
  if (!target) return
  mutate(target)
}

function markStreamingMessageError(message: string) {
  if (!currentStreamMessageId.value) return
  mutateMessage(currentStreamMessageId.value, (item) => {
    item.isStreaming = false
    item.error = message
  })
}

async function refreshSidePanel() {
  if (!session.token.value) return
  try {
    await fetchSidePanel()
  } catch {
    // keep current side panel state if refresh fails
  }
}

async function retryPage() {
  await loadPage()
}

async function openAudit(runId: string) {
  if (!session.token.value) return
  auditLoading.value = true
  auditDrawerOpen.value = true
  try {
    auditRecord.value = await fetchAgentRunAudit(session.token.value, runId)
  } catch (auditErr) {
    error.value = auditErr instanceof Error ? auditErr.message : '运行审计加载失败'
  } finally {
    auditLoading.value = false
  }
}

async function readNotification(id: EntityId) {
  if (!session.token.value) return
  try {
    const updated = await markAgentNotificationRead(session.token.value, id)
    const nextNotifications = notifications.value.slice()
    for (let index = 0; index < nextNotifications.length; index += 1) {
      if (nextNotifications[index].id === updated.id) {
        nextNotifications[index] = updated
        break
      }
    }
    notifications.value = nextNotifications
  } catch (markErr) {
    error.value = markErr instanceof Error ? markErr.message : '通知标记失败'
  }
}

async function loadDrafts() {
  if (!session.token.value) return
  try {
    drafts.value = await fetchAgentDrafts(session.token.value, { page: 0, limit: 20 })
  } catch {
    // keep current drafts if refresh fails
  }
}

function openCreateDraftEditor() {
  editingDraftId.value = null
  draftTitle.value = ''
  draftType.value = 'note'
  draftContentJson.value = ''
  draftStatus.value = 'active'
  isDraftEditorOpen.value = true
}

function openEditDraftEditor(draft: AgentDraft) {
  editingDraftId.value = draft.id
  draftTitle.value = draft.title
  draftType.value = draft.draftType
  draftContentJson.value = draft.contentJson
  draftStatus.value = draft.status === 'archived' ? 'archived' : 'active'
  isDraftEditorOpen.value = true
}

async function saveDraft() {
  if (!session.token.value || !canWrite.value) return
  if (!draftTitle.value.trim() || !draftType.value.trim() || !draftContentJson.value.trim()) return
  draftSaving.value = true
  try {
    if (editingDraftId.value) {
      await updateAgentDraft(session.token.value, editingDraftId.value, {
        draftType: draftType.value,
        title: draftTitle.value,
        contentJson: draftContentJson.value,
        status: draftStatus.value,
      })
    } else {
      await createAgentDraft(session.token.value, {
        draftType: draftType.value,
        title: draftTitle.value,
        contentJson: draftContentJson.value,
        status: draftStatus.value,
      })
    }
    isDraftEditorOpen.value = false
    await loadDrafts()
  } catch (saveErr) {
    error.value = saveErr instanceof Error ? saveErr.message : '草稿保存失败'
  } finally {
    draftSaving.value = false
  }
}

async function removeDraft(draft: AgentDraft) {
  if (!session.token.value || !canWrite.value) return
  if (!confirm(`确认删除草稿「${draft.title}」？`)) return
  try {
    await deleteAgentDraft(session.token.value, draft.id)
    await loadDrafts()
  } catch (deleteErr) {
    error.value = deleteErr instanceof Error ? deleteErr.message : '草稿删除失败'
  }
}

function normalizeRole(role: string): UiRole {
  const normalized = role.trim().toLowerCase()
  if (normalized === 'assistant') return 'assistant'
  if (normalized === 'system') return 'system'
  return 'user'
}

function createEmptyRunTrace(): UiRunTrace {
  return {
    runId: null,
    auditId: null,
    traceId: null,
    mode: null,
    llmStatus: null,
    planSource: null,
    planSteps: [],
    toolCalls: [],
    resultBlocks: [],
    draft: null,
    compacted: null,
  }
}

function localId(prefix: string) {
  return `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function renderMarkdownSections(content: string): MarkdownSection[] {
  const normalized = content.trim()
  if (!normalized) {
    return []
  }
  const cached = markdownSectionsCache.get(normalized)
  if (cached) {
    return cached
  }
  const lines = normalized.split(/\r?\n/)
  const sections: MarkdownSection[] = []
  let listBuffer: string[] = []
  let paragraphBuffer: string[] = []

  const flushList = () => {
    if (listBuffer.length > 0) {
      sections.push({ type: 'list', items: listBuffer })
      listBuffer = []
    }
  }

  const flushParagraph = () => {
    if (paragraphBuffer.length > 0) {
      sections.push({ type: 'paragraph', text: paragraphBuffer.join(' ') })
      paragraphBuffer = []
    }
  }

  for (const rawLine of lines) {
    const line = rawLine.trim()
    if (!line) {
      flushList()
      flushParagraph()
      continue
    }
    const heading = line.match(HEADING_SECTION_REGEX)
    if (heading) {
      flushList()
      flushParagraph()
      sections.push({
        type: 'heading',
        level: heading[1].length,
        text: heading[2].trim(),
      })
      continue
    }
    if (BULLET_SECTION_REGEX.test(line)) {
      flushParagraph()
      listBuffer.push(line.replace(BULLET_SECTION_REGEX, '').trim())
      continue
    }
    flushList()
    paragraphBuffer.push(line)
  }

  flushList()
  flushParagraph()
  if (markdownSectionsCache.size > 256) {
    markdownSectionsCache.clear()
  }
  markdownSectionsCache.set(normalized, sections)
  return sections
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function normalizeBlockType(block: AgentResultBlock) {
  return deriveBlockState(block).normalizedType
}

function blockTableRows(block: AgentResultBlock): Record<string, unknown>[] {
  return deriveBlockState(block).tableRows
}

function blockTableHeaders(block: AgentResultBlock) {
  return deriveBlockState(block).tableHeaders
}

function isTableBlock(block: AgentResultBlock) {
  return normalizeBlockType(block).includes('table') || blockTableRows(block).length > 0
}

function blockListItems(block: AgentResultBlock): string[] {
  return deriveBlockState(block).listItems
}

function isListBlock(block: AgentResultBlock) {
  const type = normalizeBlockType(block)
  return type.includes('list') || type.includes('bullet') || blockListItems(block).length > 0
}

function blockObjectEntries(block: AgentResultBlock) {
  return deriveBlockState(block).objectEntries
}

function isObjectBlock(block: AgentResultBlock) {
  const type = normalizeBlockType(block)
  return type.includes('kpi') || type.includes('card') || blockObjectEntries(block).length > 0
}

function formatBlockValue(value: unknown) {
  if (value == null) return '--'
  if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
    return String(value)
  }
  return JSON.stringify(value, null, 2)
}

function deriveBlockState(block: AgentResultBlock): BlockDerivedState {
  const cached = blockDerivedCache.get(block)
  if (cached) {
    return cached
  }

  const normalizedType = block.blockType.trim().toLowerCase()
  let tableRows: Record<string, unknown>[] = []
  let listItems: string[] = []
  let objectEntries: [string, unknown][] = []

  if (Array.isArray(block.data)) {
    const rows: Record<string, unknown>[] = []
    const items: string[] = []
    let allRecords = true
    for (let index = 0; index < block.data.length; index += 1) {
      const item = block.data[index]
      if (isRecord(item)) {
        rows.push(item)
      } else {
        allRecords = false
        if (typeof item === 'string' || typeof item === 'number' || typeof item === 'boolean') {
          items.push(String(item))
        }
      }
    }
    if (allRecords) {
      tableRows = rows
    }
    listItems = items
  } else if (isRecord(block.data) && Array.isArray(block.data.rows)) {
    const rows = block.data.rows
    let allRecords = true
    const nextRows: Record<string, unknown>[] = []
    for (let index = 0; index < rows.length; index += 1) {
      const row = rows[index]
      if (isRecord(row)) {
        nextRows.push(row)
      } else {
        allRecords = false
        break
      }
    }
    if (allRecords) {
      tableRows = nextRows
    }
  }

  if (isRecord(block.data) && Array.isArray(block.data.items)) {
    const items = block.data.items
    const nextItems: string[] = []
    for (let index = 0; index < items.length; index += 1) {
      const item = items[index]
      if (typeof item === 'string' || typeof item === 'number' || typeof item === 'boolean') {
        nextItems.push(String(item))
      }
    }
    listItems = nextItems
  }

  if (isRecord(block.data) && !Array.isArray(block.data.rows)) {
    const entries: [string, unknown][] = []
    for (const key in block.data) {
      if (Object.prototype.hasOwnProperty.call(block.data, key)) {
        entries.push([key, block.data[key]])
      }
    }
    objectEntries = entries
  }

  const derived = {
    normalizedType,
    tableRows,
    tableHeaders: tableRows.length > 0 ? Object.keys(tableRows[0]) : [],
    listItems,
    objectEntries,
  }
  blockDerivedCache.set(block, derived)
  return derived
}
</script>

<template>
  <section class="business-page agent-page stitch-inspired-page stitch-agent-page">
    <section class="screen-hero">
      <div>
        <p class="eyebrow">AI 助手 / Agent</p>
        <h2>AI 智能助手专页</h2>
        <p>真实连接 `/v2/agent/chat/stream`，支持流式回答、停止生成、运行轨迹与审计查看。</p>
      </div>
      <div class="hero-actions">
        <button type="button" class="ghost-action" :disabled="!canWrite" @click="handleCreateConversation">新建会话</button>
      </div>
    </section>

    <PageStatusBanner
      v-if="!isApiSource"
      tone="warning"
      title="演示模式"
      message="当前是演示模式。这一页只在真实登录后使用 AI 助手。"
    />
    <PageStatusBanner
      v-else-if="error"
      tone="error"
      title="页面加载异常"
      :message="error"
      action-label="重新加载"
      @action="retryPage"
    />
    <PageStatusBanner v-if="loading" tone="info" title="正在同步" message="正在加载 AI 工作台..." />
    <PageStatusBanner
      v-if="!canView && isApiSource"
      tone="warning"
      title="无查看权限"
      message="当前角色没有 AI 助手查看权限。"
    />

    <section class="agent-layout" v-if="canView">
      <aside class="panel agent-sidebar">
        <div class="panel-head">
          <div>
            <p class="eyebrow">会话列表</p>
            <h3>最近会话</h3>
          </div>
        </div>
        <div class="agent-conversation-list">
          <button
            v-for="conversation in conversations"
            :key="conversation.id"
            type="button"
            class="agent-conversation-item"
            :class="{ active: sameEntityId(conversation.id, selectedConversationId) }"
            @click="selectedConversationId = conversation.id"
          >
            <div>
              <strong>{{ conversation.title }}</strong>
              <span>{{ formatDateTime(conversation.lastMessageAt || conversation.updatedAt) }}</span>
            </div>
            <small>{{ conversation.latestSummary || conversation.status }}</small>
            <span class="danger-link" v-if="canWrite" @click.stop="handleDeleteConversation(conversation.id)">删除</span>
          </button>
        </div>
      </aside>

      <article class="panel agent-chat-panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">对话区</p>
            <h3>{{ selectedConversationTitle }}</h3>
          </div>
          <span class="session-source">{{ currentRunId || '等待提问' }}</span>
        </div>

        <div class="agent-message-stream">
          <PageEmptyState
            v-if="!loading && messages.length === 0"
            title="暂无对话消息"
            message="先发一句问题，AI 会在这里返回流式回答。"
          />
          <article v-for="message in messages" :key="message.id" class="agent-message" :data-role="message.role">
            <header>
              <strong>{{ message.role === 'user' ? '你' : message.role === 'assistant' ? '智慧记 AI' : '系统' }}</strong>
              <span>{{ formatDateTime(message.createdAt) }}</span>
            </header>
            <div class="agent-markdown" v-if="message.content">
              <template v-for="(section, index) in renderMarkdownSections(message.content)" :key="`${message.id}-${index}`">
                <h4 v-if="section.type === 'heading' && section.level === 1">{{ section.text }}</h4>
                <h5 v-else-if="section.type === 'heading' && section.level === 2">{{ section.text }}</h5>
                <h6 v-else-if="section.type === 'heading'">{{ section.text }}</h6>
                <ul v-else-if="section.type === 'list'">
                  <li v-for="item in section.items" :key="item">{{ item }}</li>
                </ul>
                <p v-else>{{ section.text }}</p>
              </template>
            </div>
            <p v-else-if="message.isStreaming" class="agent-markdown__placeholder">正在生成...</p>
            <p v-if="message.error" class="form-error">{{ message.error }}</p>

            <div v-if="message.runTrace" class="agent-run-trace">
              <div class="form-actions">
                <button type="button" class="ghost-action" @click="message.showTrace = !message.showTrace">
                  {{ message.showTrace ? '收起轨迹' : '查看轨迹' }}
                </button>
                <button
                  v-if="message.runTrace.runId"
                  type="button"
                  class="ghost-action"
                  @click="openAudit(message.runTrace.runId)"
                >
                  查看审计
                </button>
              </div>

              <div v-if="message.showTrace" class="agent-trace-body">
                <div class="agent-trace-meta">
                  <span>运行 ID {{ message.runTrace.runId || '--' }}</span>
                  <span>模式 {{ message.runTrace.mode || '--' }}</span>
                  <span>模型状态 {{ message.runTrace.llmStatus || '--' }}</span>
                  <span>计划来源 {{ message.runTrace.planSource || '--' }}</span>
                </div>

                <div v-if="message.runTrace.planSteps.length" class="agent-trace-block">
                  <strong>计划片段</strong>
                  <ul>
                    <li v-for="step in message.runTrace.planSteps" :key="`${step.timestamp}-${step.content}`">{{ step.content }}</li>
                  </ul>
                </div>

                <div v-if="message.runTrace.toolCalls.length" class="agent-trace-block">
                  <strong>工具调用</strong>
                  <ul>
                    <li v-for="toolCall in message.runTrace.toolCalls" :key="toolCall.key">
                      {{ toolCall.toolName }} / {{ toolCall.status }}
                      <small>{{ toolCall.inputSummary || toolCall.resultSummary || toolCall.errorMessage }}</small>
                      <small v-if="toolCall.durationMs">{{ formatDuration(toolCall.durationMs) }}</small>
                    </li>
                  </ul>
                </div>

                <div v-if="message.runTrace.resultBlocks.length" class="agent-trace-block">
                  <strong>结果块</strong>
                  <div class="agent-result-blocks">
                    <article v-for="(block, index) in message.runTrace.resultBlocks" :key="`${block.blockType}-${index}`" class="detail-card">
                      <p class="eyebrow">{{ block.blockType }}</p>
                      <strong>{{ block.title || '结构化结果' }}</strong>
                      <div v-if="isTableBlock(block)" class="agent-result-table">
                        <table>
                          <thead>
                            <tr>
                              <th v-for="header in blockTableHeaders(block)" :key="header">{{ header }}</th>
                            </tr>
                          </thead>
                          <tbody>
                            <tr v-for="(row, rowIndex) in blockTableRows(block)" :key="rowIndex">
                              <td v-for="header in blockTableHeaders(block)" :key="`${rowIndex}-${header}`">{{ formatBlockValue(row[header]) }}</td>
                            </tr>
                          </tbody>
                        </table>
                      </div>
                      <ul v-else-if="isListBlock(block)" class="agent-result-list">
                        <li v-for="item in blockListItems(block)" :key="item">{{ item }}</li>
                      </ul>
                      <dl v-else-if="isObjectBlock(block)" class="agent-result-kv">
                        <template v-for="[key, value] in blockObjectEntries(block)" :key="key">
                          <dt>{{ key }}</dt>
                          <dd>{{ formatBlockValue(value) }}</dd>
                        </template>
                      </dl>
                      <pre v-else>{{ JSON.stringify(block.data, null, 2) }}</pre>
                    </article>
                  </div>
                </div>

                <div v-if="message.runTrace.draft" class="agent-trace-block">
                  <strong>待确认草稿</strong>
                  <p>{{ message.runTrace.draft.title }} / {{ message.runTrace.draft.draftType }}</p>
                </div>

                <div v-if="message.runTrace.compacted" class="agent-trace-block">
                  <strong>上下文压缩</strong>
                  <p>{{ message.runTrace.compacted.summary }}</p>
                </div>
              </div>
            </div>
          </article>
        </div>

        <div class="agent-input-box">
          <textarea v-model="inputText" rows="4" placeholder="例如：帮我看下今天的应收风险、低库存和销售趋势" />
          <div class="form-actions">
            <button type="button" :disabled="!canWrite || sending" @click="sendMessage()">{{ sending ? '发送中...' : '发送问题' }}</button>
            <button type="button" class="ghost-action" :disabled="!sending && !streamState.controller" @click="stopStreaming">
              {{ stopping ? '正在停止...' : '停止生成' }}
            </button>
          </div>
        </div>
      </article>

      <aside class="panel agent-sidepanel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">工作台</p>
            <h3>{{ workbench?.greeting || '智慧记 AI 助手' }}</h3>
          </div>
        </div>

        <div class="detail-stack">
          <article v-if="workbench?.quickQuestions?.length" class="detail-card">
            <p class="eyebrow">快捷问题</p>
            <div class="form-actions">
              <button
                v-for="question in workbench.quickQuestions"
                :key="question"
                type="button"
                class="ghost-action"
                :disabled="!canWrite || sending"
                @click="sendMessage(question)"
              >
                {{ question }}
              </button>
            </div>
          </article>

          <article class="detail-card">
            <div class="draft-card-head">
              <p class="eyebrow">待处理草稿</p>
              <button v-if="canWrite" type="button" class="ghost-action" @click="openCreateDraftEditor">新建草稿</button>
            </div>
            <div v-if="drafts.length" class="mini-list">
              <div v-for="draft in drafts" :key="draft.id" class="draft-item">
                <div class="draft-item-main">
                  <strong>{{ draft.title }}</strong>
                  <span>{{ draft.draftType }} / {{ draft.status }}</span>
                </div>
                <div v-if="canWrite" class="draft-item-actions">
                  <button type="button" class="ghost-action" @click="openEditDraftEditor(draft)">编辑</button>
                  <button type="button" class="ghost-action danger-link" @click="removeDraft(draft)">删除</button>
                </div>
              </div>
            </div>
            <PageEmptyState v-else title="暂无草稿" message="当前没有待处理草稿。" />
            <div v-if="isDraftEditorOpen" class="draft-editor">
              <label class="compact-field">
                <span>标题</span>
                <input v-model="draftTitle" type="text" placeholder="草稿标题" />
              </label>
              <label class="compact-field">
                <span>类型</span>
                <input v-model="draftType" type="text" placeholder="例如 note" />
              </label>
              <label class="compact-field">
                <span>状态</span>
                <select v-model="draftStatus">
                  <option value="active">active</option>
                  <option value="archived">archived</option>
                </select>
              </label>
              <label class="compact-field">
                <span>内容</span>
                <textarea v-model="draftContentJson" rows="4" placeholder="草稿内容（JSON 或文本）"></textarea>
              </label>
              <div class="form-actions">
                <button type="button" :disabled="draftSaving" @click="saveDraft">{{ draftSaving ? '保存中...' : '保存' }}</button>
                <button type="button" class="ghost-action" :disabled="draftSaving" @click="isDraftEditorOpen = false">取消</button>
              </div>
            </div>
          </article>

          <article class="detail-card">
            <p class="eyebrow">任务与通知</p>
            <div class="mini-list">
              <div v-for="task in tasks.slice(0, 4)" :key="task.id">
                <strong>{{ task.title }}</strong>
                <span>{{ task.statusLabel }} / {{ task.progress ?? 0 }}%</span>
              </div>
            </div>
            <div class="mini-list agent-notifications">
              <div v-for="notification in notifications.slice(0, 6)" :key="notification.id">
                <strong>{{ notification.title }}</strong>
                <span>{{ notification.body }}</span>
                <button type="button" class="ghost-action" :disabled="notification.isRead" @click="readNotification(notification.id)">
                  {{ notification.isRead ? '已读' : '标为已读' }}
                </button>
              </div>
            </div>
          </article>
        </div>
      </aside>
    </section>

    <section v-if="auditDrawerOpen" class="panel agent-audit-drawer">
      <div class="panel-head">
        <div>
          <p class="eyebrow">运行审计</p>
          <h3>{{ auditRecord?.runId || '审计详情' }}</h3>
        </div>
        <button type="button" class="ghost-action" @click="auditDrawerOpen = false">关闭</button>
      </div>
      <p v-if="auditLoading" class="form-success">正在加载运行审计...</p>
      <div v-else-if="auditRecord" class="detail-stack">
        <article class="detail-card">
          <dl class="detail-list">
            <div>
              <dt>状态</dt>
              <dd>{{ auditRecord.status }}</dd>
            </div>
            <div>
              <dt>模式</dt>
              <dd>{{ auditRecord.mode || '--' }}</dd>
            </div>
            <div>
              <dt>模型状态</dt>
              <dd>{{ auditRecord.llmStatus || '--' }}</dd>
            </div>
            <div>
              <dt>工具数</dt>
              <dd>{{ auditRecord.toolCount ?? 0 }}</dd>
            </div>
            <div>
              <dt>事件数</dt>
              <dd>{{ auditRecord.eventCount ?? 0 }}</dd>
            </div>
            <div>
              <dt>告警</dt>
              <dd>{{ auditRecord.warnings.join(' / ') || '--' }}</dd>
            </div>
          </dl>
        </article>
        <article class="detail-card">
          <p class="eyebrow">审计事件</p>
          <div class="mini-list">
            <div v-for="event in auditRecord.events" :key="`${event.eventId}-${event.seq}`">
              <strong>{{ event.eventType }}</strong>
              <span>{{ formatDateTime(event.createdAt) }}</span>
            </div>
          </div>
        </article>
      </div>
    </section>
  </section>
</template>

<style scoped>
.agent-markdown {
  display: grid;
  gap: 8px;
  white-space: normal;
}

.agent-markdown :deep(h4),
.agent-markdown :deep(h5),
.agent-markdown :deep(h6) {
  margin: 0;
}

.agent-markdown :deep(p),
.agent-markdown :deep(ul) {
  margin: 0;
}

.agent-markdown__placeholder {
  margin: 0;
  color: var(--text-secondary);
}

.agent-result-table {
  overflow-x: auto;
}

.agent-result-table table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.agent-result-table th,
.agent-result-table td {
  padding: 8px 10px;
  border-bottom: 1px solid var(--line);
  text-align: left;
  vertical-align: top;
}

.agent-result-list,
.agent-result-kv {
  margin: 0;
}

.agent-result-kv {
  display: grid;
  grid-template-columns: minmax(96px, 140px) 1fr;
  gap: 6px 10px;
}

.agent-result-kv dt {
  font-weight: 600;
}

.agent-result-kv dd {
  margin: 0;
}

.draft-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.draft-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.draft-item-main {
  display: grid;
  gap: 2px;
}

.draft-item-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.draft-editor {
  display: grid;
  gap: 8px;
  margin-top: 8px;
}
</style>
