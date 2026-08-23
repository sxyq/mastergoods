<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useSession } from '@/app/stores/session'
import {
  cancelAgentDraftAction,
  cancelAgentRun,
  confirmAgentDraft,
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
  type AgentRunBlockedEvent,
  type AgentRunCancelledEvent,
  type AgentRunCompletedEvent,
  type AgentRunExhaustedEvent,
  type AgentRunFailedEvent,
  type AgentRunStartedEvent,
  type AgentStreamEvent,
  type AgentTerminalStatus,
  type AgentTerminalStreamEvent,
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
  terminal: AgentTerminalStreamEvent | null
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

type DraftEditorMode =
  | 'custom'
  | 'customer'
  | 'supplier'
  | 'product'
  | 'sale_order'
  | 'purchase_order'
  | 'pay_order'
  | 'finance_record'

interface DraftLineForm {
  productId: string
  productCode: string
  productName: string
  quantity: string
  unitPrice: string
}

interface DraftEditorForm {
  name: string
  phone: string
  groupId: string
  level: string
  status: string
  code: string
  categoryId: string
  unitId: string
  salePrice: string
  purchasePrice: string
  stock: string
  safeStock: string
  customerId: string
  customerName: string
  supplierId: string
  supplierName: string
  amount: string
  accountId: string
  discountAmount: string
  settlementMethod: string
  warehouseId: string
  method: string
  referenceNo: string
  notes: string
  items: DraftLineForm[]
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
const sidePanelError = ref('')
const cancelError = ref('')
const cancelRetryRunId = ref<string | null>(null)
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
const draftError = ref('')
const mobileSidebarOpen = ref(false)
const mobileSidepanelOpen = ref(false)
const dismissedDraftIds = ref<Set<string>>(new Set())
const draftActionPendingIds = ref<Set<string>>(new Set())
const confirmedDraftIds = ref<Set<string>>(new Set())
const draftConfirmErrors = ref<Record<string, string>>({})
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
const cancelInFlightRunIds = new Set<string>()
const cancelledRunIds = new Set<string>()
const stopRequested = ref(false)
const markdownSectionsCache = new Map<string, MarkdownSection[]>()
const blockDerivedCache = new WeakMap<AgentResultBlock, BlockDerivedState>()
const messageById = new Map<string, UiMessage>()
const HEADING_SECTION_REGEX = /^(#{1,3})\s+(.+)$/
const BULLET_SECTION_REGEX = /^[-*]\s+/
const ORDERED_LIST_REGEX = /^\d+\.\s+(.+)$/
const CODE_FENCE_REGEX = /^```(.*)$/
const TABLE_SEPARATOR_REGEX = /^[\s|:-]+$/
const INLINE_LINK_REGEX = /\[([^\]]+)\]\(([^)]+)\)/g
const INLINE_BOLD_REGEX = /\*\*([^*]+)\*\*/g
const INLINE_CODE_REGEX = /`([^`]+)`/g
const SAFE_LINK_SCHEME_REGEX = /^(https?:\/\/|mailto:)/i
const draftTypeSuggestions = [
  'note',
  'create_customer',
  'create_supplier',
  'create_product',
  'create_sale_order',
  'create_purchase_order',
  'create_pay_order',
  'create_finance_record',
]
const draftForm = reactive(createEmptyDraftForm())
const draftEditorMode = computed<DraftEditorMode>(() => draftEditorModeForType(draftType.value))
const draftStructuredPreview = computed(() => {
  const payload = buildStructuredDraftPayload(draftType.value)
  if (!payload) {
    return draftContentJson.value
  }
  return JSON.stringify(payload, null, 2)
})

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
  const [nextWorkbench, nextConversations, nextDrafts, nextTasks, nextNotifications] = await Promise.allSettled([
    fetchAgentWorkbench(session.token.value),
    fetchAgentConversations(session.token.value, { page: 0, limit: 50 }),
    fetchAgentDrafts(session.token.value, { page: 0, limit: 20 }),
    fetchAgentTasks(session.token.value),
    fetchAgentNotifications(session.token.value),
  ])
  const failures: string[] = []
  const readResult = <T>(result: PromiseSettledResult<T>, label: string, onValue: (value: T) => void) => {
    if (result.status === 'fulfilled') {
      onValue(result.value)
      return
    }
    const reason = result.reason instanceof Error ? result.reason.message : '请求失败'
    failures.push(`${label}：${reason}`)
  }

  readResult(nextWorkbench, '工作台', (value) => { workbench.value = value })
  readResult(nextConversations, '会话列表', (value) => { conversations.value = value })
  readResult(nextDrafts, '草稿列表', (value) => { drafts.value = value; draftError.value = '' })
  readResult(nextTasks, '任务列表', (value) => { tasks.value = value })
  readResult(nextNotifications, '通知列表', (value) => { notifications.value = value })
  sidePanelError.value = failures.length > 0 ? failures.join('；') : ''
  return conversations.value
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
  stopRequested.value = false
  cancelError.value = ''
  cancelRetryRunId.value = null
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
    if (isAbortError(sendErr) && stopRequested.value) {
      return
    }
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
    stopRequested.value = false
    await refreshSidePanel()
  }
}

async function requestServerCancel(runId: string) {
  if (!session.token.value || cancelInFlightRunIds.has(runId) || cancelledRunIds.has(runId)) return
  cancelInFlightRunIds.add(runId)
  try {
    await cancelAgentRun(session.token.value, runId)
    cancelledRunIds.add(runId)
    cancelError.value = ''
    cancelRetryRunId.value = null
  } catch (cancelErr) {
    cancelRetryRunId.value = runId
    cancelError.value = cancelErr instanceof Error ? cancelErr.message : '服务端取消失败'
  } finally {
    cancelInFlightRunIds.delete(runId)
  }
}

async function stopStreaming(showStatus = true) {
  if (stopping.value) return
  const controller = streamState.controller
  const runId = currentRunId.value
  if (!controller && !runId) return
  stopping.value = true
  stopRequested.value = true
  if (controller) controller.abort()
  if (showStatus) {
    markStreamingMessageError('已停止本地接收，正在取消服务端运行')
  }
  if (runId) await requestServerCancel(runId)
}

async function retryServerCancel() {
  if (!cancelRetryRunId.value) return
  await requestServerCancel(cancelRetryRunId.value)
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
    case 'run_failed':
      onRunFailed(messageId, event)
      break
    case 'run_blocked':
      onRunBlocked(messageId, event)
      break
    case 'run_exhausted':
      onRunExhausted(messageId, event)
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
      message.runTrace.terminal = event
    }
  })
}

function onRunFailed(messageId: string, event: AgentRunFailedEvent) {
  mutateMessage(messageId, (message) => {
    if (event.finalAnswer && !message.content.trim()) {
      message.content = event.finalAnswer
    }
    message.isStreaming = false
    const safeMessage = event.safeMessage || `运行失败（${event.errorCode || 'UNKNOWN'}）`
    if (!message.error) {
      message.error = safeMessage
    }
    if (message.runTrace) {
      message.runTrace.mode = event.mode || message.runTrace.mode
      message.runTrace.llmStatus = event.llmStatus || message.runTrace.llmStatus
      message.runTrace.planSource = event.planSource || message.runTrace.planSource
      message.runTrace.auditId = event.auditId || message.runTrace.auditId
      message.runTrace.traceId = event.traceId || message.runTrace.traceId
      message.runTrace.terminal = event
    }
  })
}

function onRunBlocked(messageId: string, event: AgentRunBlockedEvent) {
  mutateMessage(messageId, (message) => {
    if (event.finalAnswer && !message.content.trim()) {
      message.content = event.finalAnswer
    }
    message.isStreaming = false
    const safeMessage = event.safeMessage || '安全策略阻止了本次运行'
    if (!message.error) {
      message.error = safeMessage
    }
    if (message.runTrace) {
      message.runTrace.auditId = event.auditId || message.runTrace.auditId
      message.runTrace.traceId = event.traceId || message.runTrace.traceId
      message.runTrace.terminal = event
    }
  })
}

function onRunExhausted(messageId: string, event: AgentRunExhaustedEvent) {
  mutateMessage(messageId, (message) => {
    if (event.finalAnswer && !message.content.trim()) {
      message.content = event.finalAnswer
    }
    message.isStreaming = false
    const safeMessage = event.safeMessage || '已达到轮次或工具预算上限，未能完成全部目标工具'
    if (!message.error) {
      message.error = safeMessage
    }
    if (message.runTrace) {
      message.runTrace.auditId = event.auditId || message.runTrace.auditId
      message.runTrace.traceId = event.traceId || message.runTrace.traceId
      message.runTrace.terminal = event
    }
  })
}

function onRunCancelled(messageId: string, event: AgentRunCancelledEvent) {
  mutateMessage(messageId, (message) => {
    message.isStreaming = false
    if (!message.error) {
      message.error = event.reason || '本次生成已取消'
    }
    if (message.runTrace) {
      message.runTrace.terminal = event
    }
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
    // fetchSidePanel isolates individual failures; retain this guard for unexpected errors.
  }
}

async function retryPage() {
  await loadPage()
}

function closeMobilePanels() {
  mobileSidebarOpen.value = false
  mobileSidepanelOpen.value = false
}

function toggleMobileSidebar() {
  mobileSidebarOpen.value = !mobileSidebarOpen.value
  if (mobileSidebarOpen.value) {
    mobileSidepanelOpen.value = false
  }
}

function toggleMobileSidepanel() {
  mobileSidepanelOpen.value = !mobileSidepanelOpen.value
  if (mobileSidepanelOpen.value) {
    mobileSidebarOpen.value = false
  }
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
    draftError.value = ''
  } catch (loadErr) {
    draftError.value = loadErr instanceof Error ? loadErr.message : '草稿列表加载失败'
  }
}

async function retryDrafts() {
  await loadDrafts()
}

function openCreateDraftEditor() {
  editingDraftId.value = null
  draftTitle.value = ''
  draftType.value = 'note'
  draftContentJson.value = ''
  draftStatus.value = 'active'
  overwriteDraftForm(createEmptyDraftForm())
  isDraftEditorOpen.value = true
}

function openEditDraftEditor(draft: AgentDraft) {
  editingDraftId.value = draft.id
  draftTitle.value = draft.title
  draftType.value = draft.draftType
  draftContentJson.value = draft.contentJson
  draftStatus.value = draft.status === 'archived' ? 'archived' : 'active'
  populateDraftForm(draft.draftType, draft.contentJson)
  isDraftEditorOpen.value = true
}

async function saveDraft() {
  if (!session.token.value || !canWrite.value) return
  const normalizedTitle = draftTitle.value.trim()
  const normalizedType = draftType.value.trim()
  const contentJson = draftStructuredPreview.value.trim()
  if (!normalizedTitle) {
    draftError.value = '标题不能为空'
    return
  }
  if (!normalizedType) {
    draftError.value = '类型不能为空'
    return
  }
  if (!contentJson) {
    draftError.value = '内容不能为空'
    return
  }
  const validationError = validateDraftEditor()
  if (validationError) {
    draftError.value = validationError
    return
  }
  draftError.value = ''
  draftSaving.value = true
  try {
    if (editingDraftId.value) {
      await updateAgentDraft(session.token.value, editingDraftId.value, {
        draftType: normalizedType,
        title: normalizedTitle,
        contentJson,
        status: draftStatus.value,
      })
    } else {
      await createAgentDraft(session.token.value, {
        draftType: normalizedType,
        title: normalizedTitle,
        contentJson,
        status: draftStatus.value,
      })
    }
    isDraftEditorOpen.value = false
    await loadDrafts()
  } catch (saveErr) {
    draftError.value = saveErr instanceof Error ? saveErr.message : '草稿保存失败'
  } finally {
    draftSaving.value = false
  }
}

function validateDraftEditor(): string {
  if (draftEditorMode.value === 'customer' || draftEditorMode.value === 'supplier') {
    if (!draftForm.name.trim()) return '名称不能为空'
  }
  if (draftEditorMode.value === 'product') {
    if (!draftForm.code.trim()) return '商品编码不能为空'
    if (!draftForm.name.trim()) return '商品名称不能为空'
  }
  if (draftEditorMode.value === 'sale_order') {
    if (!draftForm.customerName.trim()) return '客户名称不能为空'
  }
  if (draftEditorMode.value === 'purchase_order') {
    if (!draftForm.supplierName.trim()) return '供应商名称不能为空'
  }
  if (draftEditorMode.value === 'pay_order') {
    if (!draftForm.supplierName.trim()) return '供应商名称不能为空'
    if (!draftForm.amount.trim()) return '付款金额不能为空'
  }
  if (draftEditorMode.value === 'finance_record') {
    if (!draftForm.amount.trim()) return '金额不能为空'
    if (!draftForm.categoryId.trim() && !draftForm.name.trim()) return '分类不能为空'
    if (!draftForm.status.trim()) return '类型不能为空'
  }
  return ''
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

function markDraftActionPending(draftId: string | number, pending: boolean) {
  const next = new Set(draftActionPendingIds.value)
  const key = String(draftId)
  if (pending) next.add(key)
  else next.delete(key)
  draftActionPendingIds.value = next
}

function isDraftActionPending(draftId: string | number) {
  return draftActionPendingIds.value.has(String(draftId))
}

async function confirmPendingDraft(draftEvent: AgentDraftCreatedEvent) {
  if (!session.token.value || !canWrite.value) return
  if (isDraftActionPending(draftEvent.draftId)) return
  markDraftActionPending(draftEvent.draftId, true)
  clearDraftConfirmError(draftEvent.draftId)
  try {
    await confirmAgentDraft(session.token.value, draftEvent.draftId)
    // 仅当 confirm 接口成功返回后才标记为已确认；后续业务结果展示依赖该状态。
    const next = new Set(confirmedDraftIds.value)
    next.add(String(draftEvent.draftId))
    confirmedDraftIds.value = next
    dismissDraft(draftEvent.draftId)
    await loadDrafts()
  } catch (confirmErr) {
    // 确认失败不得展示成功样式，错误信息单独展示在草稿卡片下方。
    const message = confirmErr instanceof Error ? confirmErr.message : '草稿确认失败'
    setDraftConfirmError(draftEvent.draftId, message)
    error.value = message
  } finally {
    markDraftActionPending(draftEvent.draftId, false)
  }
}

async function cancelPendingDraft(draftEvent: AgentDraftCreatedEvent) {
  if (!session.token.value || !canWrite.value) return
  if (isDraftActionPending(draftEvent.draftId)) return
  markDraftActionPending(draftEvent.draftId, true)
  clearDraftConfirmError(draftEvent.draftId)
  try {
    await cancelAgentDraftAction(session.token.value, draftEvent.draftId)
    // 取消草稿后不得展示成功样式；保持 confirmedDraftIds 不变。
    dismissDraft(draftEvent.draftId)
    await loadDrafts()
  } catch (cancelErr) {
    const message = cancelErr instanceof Error ? cancelErr.message : '草稿取消失败'
    setDraftConfirmError(draftEvent.draftId, message)
    error.value = message
  } finally {
    markDraftActionPending(draftEvent.draftId, false)
  }
}

function setDraftConfirmError(draftId: string | number, message: string) {
  draftConfirmErrors.value = { ...draftConfirmErrors.value, [String(draftId)]: message }
}

function clearDraftConfirmError(draftId: string | number) {
  const key = String(draftId)
  if (!(key in draftConfirmErrors.value)) return
  const next = { ...draftConfirmErrors.value }
  delete next[key]
  draftConfirmErrors.value = next
}

function draftConfirmErrorOf(draftId: string | number): string {
  return draftConfirmErrors.value[String(draftId)] || ''
}

function createEmptyDraftLine(): DraftLineForm {
  return {
    productId: '',
    productCode: '',
    productName: '',
    quantity: '',
    unitPrice: '',
  }
}

function createEmptyDraftForm(): DraftEditorForm {
  return {
    name: '',
    phone: '',
    groupId: '',
    level: '1',
    status: '1',
    code: '',
    categoryId: '',
    unitId: '',
    salePrice: '',
    purchasePrice: '',
    stock: '',
    safeStock: '0',
    customerId: '',
    customerName: '',
    supplierId: '',
    supplierName: '',
    amount: '',
    accountId: '',
    discountAmount: '',
    settlementMethod: '',
    warehouseId: '',
    method: '',
    referenceNo: '',
    notes: '',
    items: [createEmptyDraftLine()],
  }
}

function overwriteDraftForm(next: DraftEditorForm) {
  draftForm.name = next.name
  draftForm.phone = next.phone
  draftForm.groupId = next.groupId
  draftForm.level = next.level
  draftForm.status = next.status
  draftForm.code = next.code
  draftForm.categoryId = next.categoryId
  draftForm.unitId = next.unitId
  draftForm.salePrice = next.salePrice
  draftForm.purchasePrice = next.purchasePrice
  draftForm.stock = next.stock
  draftForm.safeStock = next.safeStock
  draftForm.customerId = next.customerId
  draftForm.customerName = next.customerName
  draftForm.supplierId = next.supplierId
  draftForm.supplierName = next.supplierName
  draftForm.amount = next.amount
  draftForm.accountId = next.accountId
  draftForm.discountAmount = next.discountAmount
  draftForm.settlementMethod = next.settlementMethod
  draftForm.warehouseId = next.warehouseId
  draftForm.method = next.method
  draftForm.referenceNo = next.referenceNo
  draftForm.notes = next.notes
  draftForm.items = next.items.length > 0 ? next.items : [createEmptyDraftLine()]
}

function draftEditorModeForType(type: string): DraftEditorMode {
  const normalized = type.trim().toLowerCase()
  if (normalized === 'create_customer') return 'customer'
  if (normalized === 'create_supplier') return 'supplier'
  if (normalized === 'create_product') return 'product'
  if (normalized === 'create_sale_order') return 'sale_order'
  if (normalized === 'create_purchase_order') return 'purchase_order'
  if (normalized === 'create_pay_order') return 'pay_order'
  if (normalized === 'create_finance_record') return 'finance_record'
  return 'custom'
}

function populateDraftForm(type: string, contentJson: string) {
  const next = createEmptyDraftForm()
  const parsed = parseDraftContent(contentJson)
  if (!parsed) {
    overwriteDraftForm(next)
    return
  }
  next.name = readDraftText(parsed, 'name')
  next.phone = readDraftText(parsed, 'phone')
  next.groupId = readDraftText(parsed, 'group_id', 'groupId')
  next.level = readDraftText(parsed, 'level') || next.level
  next.status = readDraftText(parsed, 'status') || next.status
  next.code = readDraftText(parsed, 'code')
  next.categoryId = readDraftText(parsed, 'category_id', 'categoryId')
  next.unitId = readDraftText(parsed, 'unit_id', 'unitId')
  next.salePrice = readDraftText(parsed, 'sale_price', 'salePrice')
  next.purchasePrice = readDraftText(parsed, 'purchase_price', 'purchasePrice')
  next.stock = readDraftText(parsed, 'stock')
  next.safeStock = readDraftText(parsed, 'safe_stock', 'safeStock') || next.safeStock
  next.customerId = readDraftText(parsed, 'customer_id', 'customerId')
  next.customerName = readDraftText(parsed, 'customer_name', 'customerName', 'customer')
  next.supplierId = readDraftText(parsed, 'supplier_id', 'supplierId')
  next.supplierName = readDraftText(parsed, 'supplier_name', 'supplierName', 'supplier')
  next.amount = readDraftText(parsed, 'amount')
  next.accountId = readDraftText(parsed, 'account_id', 'accountId')
  next.discountAmount = readDraftText(parsed, 'discount_amount', 'discountAmount')
  next.settlementMethod = readDraftText(parsed, 'settlement_method', 'settlementMethod')
  next.warehouseId = readDraftText(parsed, 'warehouse_id', 'warehouseId')
  next.method = readDraftText(parsed, 'method')
  next.referenceNo = readDraftText(parsed, 'reference_no', 'referenceNo')
  next.name = readDraftText(parsed, 'category') || next.name
  next.status = readDraftText(parsed, 'type') || next.status
  next.supplierName = readDraftText(parsed, 'partner_name', 'partnerName')
  next.notes = readDraftText(parsed, 'notes', 'remark', 'note')

  const items = readDraftItems(parsed, type)
  if (items.length > 0) {
    next.items = items
  }
  overwriteDraftForm(next)
}

function parseDraftContent(contentJson: string): Record<string, unknown> | null {
  if (!contentJson.trim()) return null
  try {
    const parsed = JSON.parse(contentJson)
    return isRecord(parsed) ? parsed : null
  } catch {
    return null
  }
}

function readDraftText(parsed: Record<string, unknown>, ...keys: string[]): string {
  for (const key of keys) {
    const value = parsed[key]
    if (typeof value === 'string') return value
    if (typeof value === 'number' || typeof value === 'boolean') return String(value)
  }
  return ''
}

function readDraftItems(parsed: Record<string, unknown>, type: string): DraftLineForm[] {
  const source = parsed.items
  if (!Array.isArray(source)) return []
  const nextItems: DraftLineForm[] = []
  for (let index = 0; index < source.length; index += 1) {
    const item = source[index]
    if (!isRecord(item)) continue
    nextItems.push({
      productId: readDraftText(item, 'product_id', 'productId'),
      productCode: readDraftText(item, 'product_code', 'productCode'),
      productName: readDraftText(item, 'product_name', 'productName'),
      quantity: readDraftText(item, 'quantity'),
      unitPrice: readDraftText(item, type.toLowerCase().includes('purchase') ? 'unit_cost' : 'unit_price', 'price'),
    })
  }
  return nextItems
}

function addDraftItemRow() {
  draftForm.items = [...draftForm.items, createEmptyDraftLine()]
}

function removeDraftItemRow(index: number) {
  if (draftForm.items.length <= 1) {
    draftForm.items = [createEmptyDraftLine()]
    return
  }
  draftForm.items = draftForm.items.filter((_, itemIndex) => itemIndex !== index)
}

function parseOptionalNumber(value: string): number | null {
  const trimmed = value.trim()
  if (!trimmed) return null
  const numeric = Number(trimmed)
  return Number.isFinite(numeric) ? numeric : null
}

function parseOptionalEntityId(value: string): string | null {
  return readQueryId(value)
}

function structuredStringOrNull(value: string): string | null {
  const trimmed = value.trim()
  return trimmed ? trimmed : null
}

function buildStructuredDraftPayload(type: string): Record<string, unknown> | null {
  const mode = draftEditorModeForType(type)
  if (mode === 'custom') {
    return null
  }
  if (mode === 'customer') {
    return {
      name: draftForm.name.trim(),
      phone: structuredStringOrNull(draftForm.phone) ?? '',
      level: parseOptionalNumber(draftForm.level) ?? 1,
      group_id: parseOptionalEntityId(draftForm.groupId),
      notes: structuredStringOrNull(draftForm.notes),
      status: parseOptionalNumber(draftForm.status) ?? 1,
    }
  }
  if (mode === 'supplier') {
      return {
        name: draftForm.name.trim(),
        phone: structuredStringOrNull(draftForm.phone) ?? '',
        group_id: parseOptionalEntityId(draftForm.groupId),
        notes: structuredStringOrNull(draftForm.notes),
        status: parseOptionalNumber(draftForm.status) ?? 1,
      }
  }
  if (mode === 'product') {
      return {
        code: draftForm.code.trim(),
        name: draftForm.name.trim(),
        category_id: parseOptionalEntityId(draftForm.categoryId),
        unit_id: parseOptionalEntityId(draftForm.unitId),
        sale_price: parseOptionalNumber(draftForm.salePrice) ?? 0,
        purchase_price: parseOptionalNumber(draftForm.purchasePrice) ?? 0,
        stock: parseOptionalNumber(draftForm.stock) ?? 0,
      safe_stock: parseOptionalNumber(draftForm.safeStock) ?? 0,
      status: parseOptionalNumber(draftForm.status) ?? 1,
    }
  }
  if (mode === 'sale_order') {
    return {
      customer_id: parseOptionalEntityId(draftForm.customerId),
      customer_name: draftForm.customerName.trim(),
      items: buildStructuredDraftItems('sale_order'),
      notes: structuredStringOrNull(draftForm.notes),
      discount_amount: parseOptionalNumber(draftForm.discountAmount),
    }
  }
  if (mode === 'purchase_order') {
    return {
      supplier_id: parseOptionalEntityId(draftForm.supplierId),
      supplier_name: draftForm.supplierName.trim(),
      items: buildStructuredDraftItems('purchase_order'),
      settlement_method: structuredStringOrNull(draftForm.settlementMethod),
      warehouse_id: parseOptionalEntityId(draftForm.warehouseId),
      notes: structuredStringOrNull(draftForm.notes),
      status: parseOptionalNumber(draftForm.status),
    }
  }
  if (mode === 'finance_record') {
    return {
      type: parseOptionalNumber(draftForm.status) ?? 2,
      category: draftForm.name.trim(),
      partnerName: structuredStringOrNull(draftForm.supplierName),
      amount: parseOptionalNumber(draftForm.amount) ?? 0,
      method: parseOptionalNumber(draftForm.method),
      notes: structuredStringOrNull(draftForm.notes),
    }
  }
  return {
    supplier_id: parseOptionalEntityId(draftForm.supplierId),
    supplier_name: draftForm.supplierName.trim(),
    amount: parseOptionalNumber(draftForm.amount) ?? 0,
    method: structuredStringOrNull(draftForm.method),
    reference_no: structuredStringOrNull(draftForm.referenceNo),
    notes: structuredStringOrNull(draftForm.notes),
    account_id: parseOptionalEntityId(draftForm.accountId),
    status: parseOptionalNumber(draftForm.status),
  }
}

function buildStructuredDraftItems(type: 'sale_order' | 'purchase_order') {
  const items: Record<string, unknown>[] = []
  for (const item of draftForm.items) {
    if (
      !item.productId.trim()
      && !item.productCode.trim()
      && !item.productName.trim()
      && !item.quantity.trim()
      && !item.unitPrice.trim()
    ) {
      continue
    }
    items.push({
      product_id: parseOptionalEntityId(item.productId),
      ...(type === 'purchase_order' ? { product_code: structuredStringOrNull(item.productCode) } : {}),
      product_name: structuredStringOrNull(item.productName),
      quantity: parseOptionalNumber(item.quantity),
      ...(type === 'purchase_order'
        ? { unit_cost: parseOptionalNumber(item.unitPrice) }
        : { unit_price: parseOptionalNumber(item.unitPrice) }),
    })
  }
  return items
}

function normalizeRole(role: string): UiRole {
  const normalized = role.trim().toLowerCase()
  if (normalized === 'assistant') return 'assistant'
  if (normalized === 'system') return 'system'
  return 'user'
}

watch([mobileSidebarOpen, mobileSidepanelOpen], ([sidebarOpen, sidepanelOpen]) => {
  if (typeof document === 'undefined') return
  document.body.style.overflow = sidebarOpen || sidepanelOpen ? 'hidden' : ''
})

onBeforeUnmount(() => {
  if (streamState.controller || currentRunId.value) {
    void stopStreaming(false)
  }
  if (typeof document === 'undefined') return
  document.body.style.overflow = ''
})

function isAbortError(value: unknown): boolean {
  return value instanceof DOMException
    ? value.name === 'AbortError'
    : value instanceof Error && value.name === 'AbortError'
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
    terminal: null,
  }
}

/**
 * 终态展示语义。HTTP 200 或存在文本回答不能单独判定业务成功，
 * 必须依据 terminal_status 字段。缺少 terminal_status 的事件按未知处理。
 */
interface TerminalDisplay {
  status: AgentTerminalStatus | 'UNKNOWN'
  tone: 'success' | 'warning' | 'error' | 'muted' | 'info'
  title: string
  description: string
  showSuccess: boolean
}

const TERMINAL_DISPLAY_PRESETS: Record<AgentTerminalStatus, Omit<TerminalDisplay, 'status'>> = {
  COMPLETED: {
    tone: 'success',
    title: '运行完成',
    description: '本次运行已成功完成。',
    showSuccess: true,
  },
  CONFIRMATION_PENDING: {
    tone: 'warning',
    title: '草稿待确认',
    description: '草稿已生成，等待用户确认后才会写入正式业务数据。',
    showSuccess: false,
  },
  FAILED: {
    tone: 'error',
    title: '运行失败',
    description: '工具、模型或上下文错误导致本次运行失败。',
    showSuccess: false,
  },
  BLOCKED: {
    tone: 'error',
    title: '安全阻止',
    description: '安全或权限策略拒绝执行。',
    showSuccess: false,
  },
  CANCELLED: {
    tone: 'muted',
    title: '已取消',
    description: '本次运行已被取消，未写入业务数据。',
    showSuccess: false,
  },
  EXHAUSTED: {
    tone: 'warning',
    title: '轮次耗尽',
    description: '已达到轮次或工具预算上限，未完成全部目标工具。',
    showSuccess: false,
  },
}

function describeTerminal(trace: UiRunTrace | null): TerminalDisplay | null {
  if (!trace || !trace.terminal) return null
  const event = trace.terminal
  const status = event.terminalStatus as AgentTerminalStatus | 'UNKNOWN'
  if (status === 'UNKNOWN' || !(status in TERMINAL_DISPLAY_PRESETS)) {
    return {
      status: 'UNKNOWN',
      tone: 'warning',
      title: '终态未知',
      description: '未收到明确的 terminal_status，不能判定为业务成功。',
      showSuccess: false,
    }
  }
  const preset = TERMINAL_DISPLAY_PRESETS[status as AgentTerminalStatus]
  return { ...preset, status }
}

function terminalErrorCode(trace: UiRunTrace | null): string | null {
  const terminal = trace?.terminal
  if (!terminal) return null
  const code = (terminal as AgentTerminalStreamEvent).errorCode
  return code || null
}

function terminalSafeMessage(trace: UiRunTrace | null): string | null {
  const terminal = trace?.terminal
  if (!terminal) return null
  const message = (terminal as AgentTerminalStreamEvent).safeMessage
  return message || null
}

function terminalCompletedTools(trace: UiRunTrace | null): string[] {
  const terminal = trace?.terminal
  if (!terminal) return []
  const tools = (terminal as AgentTerminalStreamEvent).completedTools
  return Array.isArray(tools) ? tools : []
}

function terminalMissingTools(trace: UiRunTrace | null): string[] {
  const terminal = trace?.terminal
  if (!terminal) return []
  const tools = (terminal as AgentTerminalStreamEvent).missingTargetTools
  return Array.isArray(tools) ? tools : []
}

function compactedSummary(compacted: AgentContextCompactedEvent | null): string {
  if (!compacted) return ''
  const count = compacted.compactedCount ?? 0
  const reason = compacted.reason || 'context_budget_threshold'
  const preview = compacted.summaryPreview?.trim() || ''
  const reusedSuffix = compacted.reused ? '（复用已有检查点）' : ''
  const head = `已压缩 ${count} 条历史消息，原因：${reason}${reusedSuffix}`
  if (!preview) return head
  // 仅展示摘要预览，不展示敏感原文；截断以防过长。
  const trimmedPreview = preview.length > 240 ? `${preview.slice(0, 240)}…` : preview
  return `${head}。摘要预览：${trimmedPreview}`
}

function isDraftConfirmed(draftId: string | number | null | undefined): boolean {
  if (draftId == null) return false
  return confirmedDraftIds.value.has(String(draftId))
}

function localId(prefix: string) {
  return `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function renderInlineMarkdown(text: string): string {
  if (!text) return ''
  const escaped = escapeHtml(text)
  return escaped
    .replace(INLINE_LINK_REGEX, (_match, label: string, url: string) => {
      const trimmed = url.trim()
      if (SAFE_LINK_SCHEME_REGEX.test(trimmed)) {
        return `<a href="${trimmed}" target="_blank" rel="noopener">${label}</a>`
      }
      return label
    })
    .replace(INLINE_BOLD_REGEX, '<strong>$1</strong>')
    .replace(INLINE_CODE_REGEX, '<code>$1</code>')
}

function parseTableRow(line: string): string[] {
  const trimmed = line.trim()
  if (!trimmed.startsWith('|')) return []
  const inner = trimmed.replace(/^\|/, '').replace(/\|$/, '')
  return inner.split('|').map((cell) => cell.trim())
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
  let bulletBuffer: string[] = []
  let orderedBuffer: string[] = []
  let paragraphBuffer: string[] = []
  let codeBuffer: string[] = []
  let codeLang = ''
  let inCode = false
  let tableHeaders: string[] = []
  let tableRows: string[][] = []
  let inTable = false

  const flushBullet = () => {
    if (bulletBuffer.length > 0) {
      sections.push({ type: 'list', items: bulletBuffer })
      bulletBuffer = []
    }
  }

  const flushOrdered = () => {
    if (orderedBuffer.length > 0) {
      sections.push({ type: 'ordered-list', items: orderedBuffer })
      orderedBuffer = []
    }
  }

  const flushParagraph = () => {
    if (paragraphBuffer.length > 0) {
      sections.push({ type: 'paragraph', text: paragraphBuffer.join(' ') })
      paragraphBuffer = []
    }
  }

  const flushTable = () => {
    if (tableHeaders.length > 0) {
      sections.push({ type: 'table', tableHeaders, tableRows })
    }
    tableHeaders = []
    tableRows = []
    inTable = false
  }

  for (const rawLine of lines) {
    const line = rawLine.trim()

    if (inCode) {
      const closeFence = line.match(CODE_FENCE_REGEX)
      if (closeFence) {
        sections.push({ type: 'code', code: codeBuffer.join('\n'), lang: codeLang })
        codeBuffer = []
        codeLang = ''
        inCode = false
      } else {
        codeBuffer.push(rawLine)
      }
      continue
    }

    const openFence = line.match(CODE_FENCE_REGEX)
    if (openFence) {
      flushBullet()
      flushOrdered()
      flushParagraph()
      flushTable()
      inCode = true
      codeLang = openFence[1] || ''
      continue
    }

    if (!line) {
      flushBullet()
      flushOrdered()
      flushParagraph()
      flushTable()
      continue
    }

    if (line.startsWith('|')) {
      const cells = parseTableRow(line)
      if (cells.length > 0) {
        if (!inTable) {
          tableHeaders = cells
          inTable = true
        } else if (TABLE_SEPARATOR_REGEX.test(line)) {
          // separator row, skip
        } else {
          tableRows.push(cells)
        }
        flushBullet()
        flushOrdered()
        flushParagraph()
        continue
      }
    } else if (inTable) {
      flushTable()
    }

    const heading = line.match(HEADING_SECTION_REGEX)
    if (heading) {
      flushBullet()
      flushOrdered()
      flushParagraph()
      flushTable()
      sections.push({
        type: 'heading',
        level: heading[1].length,
        text: heading[2].trim(),
      })
      continue
    }

    if (BULLET_SECTION_REGEX.test(line)) {
      flushOrdered()
      flushParagraph()
      flushTable()
      bulletBuffer.push(line.replace(BULLET_SECTION_REGEX, '').trim())
      continue
    }

    const ordered = line.match(ORDERED_LIST_REGEX)
    if (ordered) {
      flushBullet()
      flushParagraph()
      flushTable()
      orderedBuffer.push(ordered[1].trim())
      continue
    }

    flushBullet()
    flushOrdered()
    flushTable()
    paragraphBuffer.push(line)
  }

  flushBullet()
  flushOrdered()
  flushParagraph()
  if (inCode) {
    sections.push({ type: 'code', code: codeBuffer.join('\n'), lang: codeLang })
  }
  flushTable()

  if (markdownSectionsCache.size > 256) {
    markdownSectionsCache.clear()
  }
  markdownSectionsCache.set(normalized, sections)
  return sections
}

function toolStatusLabel(status: string): string {
  if (status === 'running') return '执行中'
  if (status === 'completed') return '已完成'
  if (status === 'failed') return '失败'
  return status
}

function visibleToolCall(trace: UiRunTrace | null): UiToolCall | null {
  if (!trace || trace.toolCalls.length === 0) return null
  for (let index = trace.toolCalls.length - 1; index >= 0; index -= 1) {
    if (trace.toolCalls[index].status === 'running') {
      return trace.toolCalls[index]
    }
  }
  return trace.toolCalls[trace.toolCalls.length - 1]
}

function toolProgressTitle(toolCall: UiToolCall | null): string {
  if (!toolCall) return ''
  if (toolCall.status === 'running') return '正在执行工具'
  if (toolCall.status === 'failed') return '最近工具失败'
  return '最近完成工具'
}

function toolProgressSummary(toolCall: UiToolCall | null): string {
  if (!toolCall) return ''
  return toolCall.resultSummary || toolCall.inputSummary || toolCall.errorMessage || '等待更多进度...'
}

function draftIcon(draftType: string): string {
  const type = draftType.toLowerCase()
  if (type.includes('sale')) return '销'
  if (type.includes('purchase')) return '采'
  if (type.includes('customer')) return '客'
  if (type.includes('supplier')) return '供'
  return '草'
}

function draftTypeLabel(draftType: string): string {
  const type = draftType.toLowerCase()
  if (type.includes('sale') && type.includes('order')) return '销售单草稿'
  if (type.includes('purchase') && type.includes('order')) return '采购单草稿'
  if (type.includes('customer')) return '客户新建草稿'
  if (type.includes('supplier')) return '供应商新建草稿'
  if (type.includes('note')) return '备注草稿'
  return draftType
}

function pushDraftField(
  fields: { label: string; value: string }[],
  obj: Record<string, unknown>,
  key: string,
  label: string,
) {
  const value = readDraftDisplayValue(obj, key)
  if (value != null) {
    fields.push({ label, value })
  }
}

function readDraftDisplayValue(obj: Record<string, unknown>, key: string): string | null {
  const candidates = [key, toSnakeCaseKey(key), toCamelCaseKey(key)]
  for (const candidate of candidates) {
    const value = obj[candidate]
    if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
      return String(value)
    }
  }
  return null
}

function toSnakeCaseKey(value: string): string {
  return value.replace(/[A-Z]/g, (segment) => `_${segment.toLowerCase()}`)
}

function toCamelCaseKey(value: string): string {
  return value.replace(/_([a-z])/g, (_, char: string) => char.toUpperCase())
}

function renderDraftContent(draft: {
  draftType: string
  title: string
  draftId?: string | number
  contentJson?: string
}): string {
  const type = draft.draftType.toLowerCase()
  const fields: { label: string; value: string }[] = []
  let parsed: Record<string, unknown> | null = null
  if (draft.contentJson) {
    try {
      const obj = JSON.parse(draft.contentJson)
      if (isRecord(obj)) parsed = obj
    } catch {
      // keep parsed null, fall back below
    }
  }

  if (parsed) {
    if (type.includes('sale')) {
      pushDraftField(fields, parsed, 'customerName', '客户')
      pushDraftField(fields, parsed, 'customer', '客户')
      pushDraftField(fields, parsed, 'discountAmount', '优惠')
      if (Array.isArray(parsed.items)) fields.push({ label: '商品行数', value: String(parsed.items.length) })
      else if (Array.isArray(parsed.lines)) fields.push({ label: '商品行数', value: String(parsed.lines.length) })
      pushDraftField(fields, parsed, 'notes', '备注')
      pushDraftField(fields, parsed, 'remark', '备注')
    } else if (type.includes('purchase')) {
      pushDraftField(fields, parsed, 'supplierName', '供应商')
      pushDraftField(fields, parsed, 'supplier', '供应商')
      if (Array.isArray(parsed.items)) fields.push({ label: '商品行数', value: String(parsed.items.length) })
      else if (Array.isArray(parsed.lines)) fields.push({ label: '商品行数', value: String(parsed.lines.length) })
      pushDraftField(fields, parsed, 'notes', '备注')
    } else if (type.includes('pay')) {
      pushDraftField(fields, parsed, 'supplierName', '供应商')
      pushDraftField(fields, parsed, 'amount', '金额')
      pushDraftField(fields, parsed, 'accountId', '账户 ID')
      pushDraftField(fields, parsed, 'referenceNo', '参考号')
      pushDraftField(fields, parsed, 'notes', '备注')
    } else if (type.includes('finance')) {
      pushDraftField(fields, parsed, 'type', '类型')
      pushDraftField(fields, parsed, 'category', '分类')
      pushDraftField(fields, parsed, 'amount', '金额')
      pushDraftField(fields, parsed, 'partnerName', '往来方')
      pushDraftField(fields, parsed, 'notes', '备注')
    } else if (type.includes('product')) {
      pushDraftField(fields, parsed, 'code', '编码')
      pushDraftField(fields, parsed, 'name', '名称')
      pushDraftField(fields, parsed, 'salePrice', '售价')
      pushDraftField(fields, parsed, 'purchasePrice', '成本价')
      pushDraftField(fields, parsed, 'stock', '库存')
    } else if (type.includes('customer') || type.includes('supplier')) {
      pushDraftField(fields, parsed, 'name', '名称')
      pushDraftField(fields, parsed, 'phone', '电话')
      pushDraftField(fields, parsed, 'contact', '联系方式')
      pushDraftField(fields, parsed, 'email', '邮箱')
      pushDraftField(fields, parsed, 'notes', '备注')
    }
  }

  if (fields.length === 0) {
    if (draft.draftId != null) {
      fields.push({ label: '草稿 ID', value: String(draft.draftId) })
    }
    if (draft.contentJson) {
      fields.push({ label: '原始内容', value: draft.contentJson })
    }
  }

  const rows = fields
    .map(
      (field) =>
        `<div class="agent-draft-row"><span>${escapeHtml(field.label)}</span><strong>${escapeHtml(field.value)}</strong></div>`,
    )
    .join('')
  return `<dl class="agent-draft-rows">${rows}</dl>`
}

function dismissDraft(draftId: string | number) {
  const next = new Set(dismissedDraftIds.value)
  next.add(String(draftId))
  dismissedDraftIds.value = next
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
      </div>
      <div class="hero-actions">
        <button type="button" class="ghost-action" :disabled="!canWrite" @click="handleCreateConversation">新建会话</button>
      </div>
    </section>

    <PageStatusBanner
      v-if="error"
      tone="error"
      title="页面加载异常"
      :message="error"
      action-label="重新加载"
      @action="retryPage"
    />
    <PageStatusBanner v-if="loading" tone="info" title="正在同步" message="正在加载 AI 工作台..." />
    <PageStatusBanner
      v-if="sidePanelError"
      tone="warning"
      title="部分工作台数据加载失败"
      :message="sidePanelError"
      action-label="重试"
      @action="retryPage"
    />
    <PageStatusBanner
      v-if="cancelError"
      tone="error"
      title="服务端取消失败"
      :message="cancelError"
      action-label="重试取消"
      @action="retryServerCancel"
    />
    <PageStatusBanner
      v-if="!canView && isApiSource"
      tone="warning"
      title="无查看权限"
      message="当前角色没有 AI 助手查看权限。"
    />

    <section class="agent-layout" v-if="canView">
      <button
        v-if="mobileSidebarOpen || mobileSidepanelOpen"
        type="button"
        class="agent-mobile-backdrop"
        aria-label="关闭移动端面板"
        @click="closeMobilePanels"
      ></button>
      <aside class="panel agent-sidebar" :class="{ 'is-mobile-open': mobileSidebarOpen }">
        <div class="panel-head">
          <div>
            <p class="eyebrow">会话列表</p>
            <h3>最近会话</h3>
          </div>
          <button
            type="button"
            class="ghost-action agent-mobile-close"
            aria-label="关闭会话列表"
            @click="closeMobilePanels"
          >
            关闭
          </button>
        </div>
        <div class="agent-conversation-list">
          <button
            v-for="conversation in conversations"
            :key="conversation.id"
            type="button"
            class="agent-conversation-item"
            :class="{ active: sameEntityId(conversation.id, selectedConversationId) }"
            @click="selectedConversationId = conversation.id; closeMobilePanels()"
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
          <div class="agent-chat-head">
            <button
              type="button"
              class="ghost-action agent-mobile-toggle"
              aria-label="打开会话列表"
              @click="toggleMobileSidebar"
            >
              会话
            </button>
            <button
              type="button"
              class="ghost-action agent-mobile-toggle"
              aria-label="打开工作台"
              @click="toggleMobileSidepanel"
            >
              工作台
            </button>
            <div>
              <p class="eyebrow">对话区</p>
              <h3>{{ selectedConversationTitle }}</h3>
            </div>
          </div>
          <span class="session-source">{{ currentRunId || '等待提问' }}</span>
        </div>

        <div class="agent-message-stream">
          <PageEmptyState
            v-if="!loading && messages.length === 0"
            title="暂无对话消息"
            message="当前会话还没有消息。"
          />
          <article v-for="message in messages" :key="message.id" class="agent-message" :data-role="message.role">
            <header>
              <strong>{{ message.role === 'user' ? '你' : message.role === 'assistant' ? '智慧记 AI' : '系统' }}</strong>
              <span>{{ formatDateTime(message.createdAt) }}</span>
            </header>
            <div
              v-if="message.role === 'assistant' && visibleToolCall(message.runTrace)"
              class="agent-live-tool-banner"
              :data-status="visibleToolCall(message.runTrace)?.status"
            >
              <span class="agent-live-tool-banner__label">{{ toolProgressTitle(visibleToolCall(message.runTrace)) }}</span>
              <strong class="agent-live-tool-banner__name">{{ visibleToolCall(message.runTrace)?.toolName }}</strong>
              <span class="agent-live-tool-banner__status">{{ toolStatusLabel(visibleToolCall(message.runTrace)?.status || '') }}</span>
              <span class="agent-live-tool-banner__summary">{{ toolProgressSummary(visibleToolCall(message.runTrace)) }}</span>
            </div>
            <div class="agent-markdown" v-if="message.content">
              <template v-for="(section, index) in renderMarkdownSections(message.content)" :key="`${message.id}-${index}`">
                <h4 v-if="section.type === 'heading' && section.level === 1" v-html="renderInlineMarkdown(section.text || '')"></h4>
                <h5 v-else-if="section.type === 'heading' && section.level === 2" v-html="renderInlineMarkdown(section.text || '')"></h5>
                <h6 v-else-if="section.type === 'heading'" v-html="renderInlineMarkdown(section.text || '')"></h6>
                <ul v-else-if="section.type === 'list'">
                  <li v-for="(item, itemIndex) in section.items" :key="itemIndex" v-html="renderInlineMarkdown(item)"></li>
                </ul>
                <ol v-else-if="section.type === 'ordered-list'">
                  <li v-for="(item, itemIndex) in section.items" :key="itemIndex" v-html="renderInlineMarkdown(item)"></li>
                </ol>
                <pre v-else-if="section.type === 'code'" class="agent-markdown__code"><code>{{ section.code }}</code></pre>
                <div v-else-if="section.type === 'table'" class="agent-markdown__table">
                  <table>
                    <thead>
                      <tr>
                        <th v-for="(header, headerIndex) in section.tableHeaders" :key="headerIndex" v-html="renderInlineMarkdown(header)"></th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(row, rowIndex) in section.tableRows" :key="rowIndex">
                        <td v-for="(cell, cellIndex) in row" :key="cellIndex" v-html="renderInlineMarkdown(cell)"></td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <p v-else v-html="renderInlineMarkdown(section.text || '')"></p>
              </template>
            </div>
            <p v-else-if="message.isStreaming" class="agent-markdown__placeholder">正在生成...</p>
            <div
              v-if="message.role === 'assistant' && describeTerminal(message.runTrace)"
              class="agent-terminal-banner"
              :data-tone="describeTerminal(message.runTrace)?.tone"
              :data-status="describeTerminal(message.runTrace)?.status"
            >
              <div class="agent-terminal-banner__head">
                <span class="agent-terminal-banner__title">{{ describeTerminal(message.runTrace)?.title }}</span>
                <span class="agent-terminal-banner__status">{{ describeTerminal(message.runTrace)?.status }}</span>
              </div>
              <p class="agent-terminal-banner__desc">{{ describeTerminal(message.runTrace)?.description }}</p>
              <p v-if="terminalErrorCode(message.runTrace)" class="agent-terminal-banner__meta">
                错误码：{{ terminalErrorCode(message.runTrace) }}
              </p>
              <p v-if="terminalSafeMessage(message.runTrace)" class="agent-terminal-banner__meta">
                {{ terminalSafeMessage(message.runTrace) }}
              </p>
              <div
                v-if="describeTerminal(message.runTrace)?.status === 'EXHAUSTED' && (terminalCompletedTools(message.runTrace).length || terminalMissingTools(message.runTrace).length)"
                class="agent-terminal-banner__tools"
              >
                <div v-if="terminalCompletedTools(message.runTrace).length" class="agent-terminal-banner__tool-group">
                  <span>已完成工具</span>
                  <ul>
                    <li v-for="tool in terminalCompletedTools(message.runTrace)" :key="`done-${tool}`">{{ tool }}</li>
                  </ul>
                </div>
                <div v-if="terminalMissingTools(message.runTrace).length" class="agent-terminal-banner__tool-group">
                  <span>未完成目标工具</span>
                  <ul>
                    <li v-for="tool in terminalMissingTools(message.runTrace)" :key="`miss-${tool}`">{{ tool }}</li>
                  </ul>
                </div>
              </div>
              <p
                v-if="describeTerminal(message.runTrace)?.status === 'CONFIRMATION_PENDING' && message.runTrace?.draft && isDraftConfirmed(message.runTrace.draft.draftId)"
                class="agent-terminal-banner__meta agent-terminal-banner__meta--ok"
              >
                草稿已确认，业务结果已写入。
              </p>
            </div>
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
                  <div class="agent-tool-pills">
                    <div
                      v-for="toolCall in message.runTrace.toolCalls"
                      :key="toolCall.key"
                      class="agent-tool-pill"
                      :data-status="toolCall.status"
                    >
                      <span class="agent-tool-pill__dot" aria-hidden="true"></span>
                      <span class="agent-tool-pill__name">{{ toolCall.toolName }}</span>
                      <span class="agent-tool-pill__status">{{ toolStatusLabel(toolCall.status) }}</span>
                      <span v-if="toolCall.durationMs" class="agent-tool-pill__duration">{{ formatDuration(toolCall.durationMs) }}</span>
                      <small
                        v-if="toolCall.inputSummary || toolCall.resultSummary || toolCall.errorMessage"
                        class="agent-tool-pill__summary"
                      >
                        {{ toolCall.inputSummary || toolCall.resultSummary || toolCall.errorMessage }}
                      </small>
                    </div>
                  </div>
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

                <div
                  v-if="message.runTrace.draft && !dismissedDraftIds.has(String(message.runTrace.draft.draftId))"
                  class="agent-trace-block"
                >
                  <strong>待确认草稿</strong>
                  <div class="agent-draft-card" :data-status="message.runTrace.draft.status || 'active'">
                    <div class="agent-draft-card__head">
                      <span class="agent-draft-card__icon" aria-hidden="true">{{ draftIcon(message.runTrace.draft.draftType) }}</span>
                      <div class="agent-draft-card__head-text">
                        <strong>{{ message.runTrace.draft.title }}</strong>
                        <span>{{ draftTypeLabel(message.runTrace.draft.draftType) }}</span>
                      </div>
                    </div>
                    <div class="agent-draft-card__body" v-html="renderDraftContent(message.runTrace.draft)"></div>
                    <p v-if="message.runTrace.draft.summary" class="agent-draft-card__summary">
                      {{ message.runTrace.draft.summary }}
                    </p>
                    <p class="agent-draft-card__hint">
                      草稿仅作为预览，确认后才会调用 <code>POST /v2/agent/drafts/{id}/confirm</code> 写入正式业务数据。
                    </p>
                  <div class="agent-draft-card__actions">
                    <button
                      v-if="canWrite"
                      type="button"
                      :disabled="!canWrite || isDraftActionPending(message.runTrace.draft.draftId)"
                        @click="confirmPendingDraft(message.runTrace.draft)"
                      >
                        {{ isDraftActionPending(message.runTrace.draft.draftId) ? '处理中...' : '确认' }}
                      </button>
                    <button
                      v-if="canWrite"
                      type="button"
                        class="ghost-action"
                        :disabled="isDraftActionPending(message.runTrace.draft.draftId)"
                        @click="cancelPendingDraft(message.runTrace.draft)"
                    >
                      取消
                    </button>
                  </div>
                  <p
                    v-if="draftConfirmErrorOf(message.runTrace.draft.draftId)"
                    class="form-error agent-draft-card__error"
                  >
                    {{ draftConfirmErrorOf(message.runTrace.draft.draftId) }}
                  </p>
                  </div>
                </div>

                <div v-if="message.runTrace.compacted" class="agent-trace-block">
                  <strong>上下文压缩</strong>
                  <p class="agent-compacted-summary">{{ compactedSummary(message.runTrace.compacted) }}</p>
                  <ul v-if="message.runTrace.compacted.checkpointId != null || message.runTrace.compacted.sourceBoundaryMessageId != null" class="agent-compacted-meta">
                    <li v-if="message.runTrace.compacted.checkpointId != null">检查点 ID：{{ message.runTrace.compacted.checkpointId }}</li>
                    <li v-if="message.runTrace.compacted.sourceBoundaryMessageId != null">边界消息 ID：{{ message.runTrace.compacted.sourceBoundaryMessageId }}</li>
                  </ul>
                </div>
              </div>
            </div>
          </article>
        </div>

        <div class="agent-input-box">
          <textarea v-model="inputText" rows="4" placeholder="输入问题" />
          <div class="form-actions">
            <button type="button" :disabled="!canWrite || sending" @click="sendMessage()">{{ sending ? '发送中...' : '发送问题' }}</button>
            <button type="button" class="ghost-action" :disabled="stopping || (!sending && !streamState.controller && !currentRunId)" @click="stopStreaming()">
              {{ stopping ? '正在停止...' : '停止生成' }}
            </button>
          </div>
        </div>
      </article>

      <aside class="panel agent-sidepanel" :class="{ 'is-mobile-open': mobileSidepanelOpen }">
        <div class="panel-head">
          <div>
            <p class="eyebrow">工作台</p>
            <h3>工作台</h3>
          </div>
          <button
            type="button"
            class="ghost-action agent-mobile-close"
            aria-label="关闭工作台"
            @click="closeMobilePanels"
          >
            关闭
          </button>
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
            <PageStatusBanner
              v-if="draftError && !isDraftEditorOpen"
              tone="error"
              title="草稿列表加载失败"
              :message="draftError"
              action-label="重试"
              @action="retryDrafts"
            />
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
              <p v-if="draftError" class="form-error" role="alert">{{ draftError }}</p>
              <label class="compact-field">
                <span>标题</span>
                <input v-model="draftTitle" type="text" placeholder="草稿标题" />
              </label>
              <label class="compact-field">
                <span>类型</span>
                <input v-model="draftType" type="text" list="agent-draft-type-options" placeholder="例如 create_customer" />
              </label>
              <datalist id="agent-draft-type-options">
                <option v-for="typeOption in draftTypeSuggestions" :key="typeOption" :value="typeOption"></option>
              </datalist>
              <label class="compact-field">
                <span>状态</span>
                <select v-model="draftStatus">
                  <option value="active">active</option>
                  <option value="archived">archived</option>
                </select>
              </label>
              <div v-if="draftEditorMode === 'customer' || draftEditorMode === 'supplier'" class="draft-editor-structured">
                <div class="draft-editor-grid">
                  <label class="compact-field">
                    <span>名称</span>
                    <input v-model="draftForm.name" type="text" placeholder="例如 张三 / 华北供应商" />
                  </label>
                  <label class="compact-field">
                    <span>电话</span>
                    <input v-model="draftForm.phone" type="text" placeholder="联系电话" />
                  </label>
                  <label class="compact-field">
                    <span>分组 ID</span>
                    <input v-model="draftForm.groupId" type="text" placeholder="可留空" />
                  </label>
                  <label v-if="draftEditorMode === 'customer'" class="compact-field">
                    <span>等级</span>
                    <input v-model="draftForm.level" type="text" placeholder="默认 1" />
                  </label>
                  <label class="compact-field draft-editor-grid__full">
                    <span>备注</span>
                    <textarea v-model="draftForm.notes" rows="3" placeholder="补充说明"></textarea>
                  </label>
                </div>
              </div>
              <div v-else-if="draftEditorMode === 'product'" class="draft-editor-structured">
                <div class="draft-editor-grid">
                  <label class="compact-field">
                    <span>编码</span>
                    <input v-model="draftForm.code" type="text" placeholder="商品编码" />
                  </label>
                  <label class="compact-field">
                    <span>名称</span>
                    <input v-model="draftForm.name" type="text" placeholder="商品名称" />
                  </label>
                  <label class="compact-field">
                    <span>分类 ID</span>
                    <input v-model="draftForm.categoryId" type="text" placeholder="可留空" />
                  </label>
                  <label class="compact-field">
                    <span>单位 ID</span>
                    <input v-model="draftForm.unitId" type="text" placeholder="可留空" />
                  </label>
                  <label class="compact-field">
                    <span>售价</span>
                    <input v-model="draftForm.salePrice" type="text" placeholder="0.00" />
                  </label>
                  <label class="compact-field">
                    <span>成本价</span>
                    <input v-model="draftForm.purchasePrice" type="text" placeholder="0.00" />
                  </label>
                  <label class="compact-field">
                    <span>库存</span>
                    <input v-model="draftForm.stock" type="text" placeholder="0" />
                  </label>
                  <label class="compact-field">
                    <span>安全库存</span>
                    <input v-model="draftForm.safeStock" type="text" placeholder="0" />
                  </label>
                </div>
              </div>
              <div v-else-if="draftEditorMode === 'pay_order'" class="draft-editor-structured">
                <div class="draft-editor-grid">
                  <label class="compact-field">
                    <span>供应商 ID</span>
                    <input v-model="draftForm.supplierId" type="text" placeholder="可留空" />
                  </label>
                  <label class="compact-field">
                    <span>供应商名称</span>
                    <input v-model="draftForm.supplierName" type="text" placeholder="供应商名称" />
                  </label>
                  <label class="compact-field">
                    <span>金额</span>
                    <input v-model="draftForm.amount" type="text" placeholder="0.00" />
                  </label>
                  <label class="compact-field">
                    <span>账户 ID</span>
                    <input v-model="draftForm.accountId" type="text" placeholder="可留空" />
                  </label>
                  <label class="compact-field">
                    <span>付款方式</span>
                    <input v-model="draftForm.method" type="text" placeholder="例如 bank_transfer" />
                  </label>
                  <label class="compact-field">
                    <span>参考号</span>
                    <input v-model="draftForm.referenceNo" type="text" placeholder="可留空" />
                  </label>
                  <label class="compact-field draft-editor-grid__full">
                    <span>备注</span>
                    <textarea v-model="draftForm.notes" rows="3" placeholder="付款说明"></textarea>
                  </label>
                </div>
              </div>
              <div v-else-if="draftEditorMode === 'finance_record'" class="draft-editor-structured">
                <div class="draft-editor-grid">
                  <label class="compact-field">
                    <span>类型</span>
                    <select v-model="draftForm.status">
                      <option value="1">收入</option>
                      <option value="2">支出</option>
                    </select>
                  </label>
                  <label class="compact-field">
                    <span>分类</span>
                    <input v-model="draftForm.name" type="text" placeholder="例如 办公用品 / 销售收入" />
                  </label>
                  <label class="compact-field">
                    <span>金额</span>
                    <input v-model="draftForm.amount" type="text" placeholder="0.00" />
                  </label>
                  <label class="compact-field">
                    <span>往来方</span>
                    <input v-model="draftForm.supplierName" type="text" placeholder="可留空" />
                  </label>
                  <label class="compact-field">
                    <span>方式</span>
                    <input v-model="draftForm.method" type="text" placeholder="数字编码，可留空" />
                  </label>
                  <label class="compact-field draft-editor-grid__full">
                    <span>备注</span>
                    <textarea v-model="draftForm.notes" rows="3" placeholder="流水说明"></textarea>
                  </label>
                </div>
              </div>
              <div v-else-if="draftEditorMode === 'sale_order' || draftEditorMode === 'purchase_order'" class="draft-editor-structured">
                <div class="draft-editor-grid">
                  <label v-if="draftEditorMode === 'sale_order'" class="compact-field">
                    <span>客户 ID</span>
                    <input v-model="draftForm.customerId" type="text" placeholder="可留空" />
                  </label>
                  <label v-else class="compact-field">
                    <span>供应商 ID</span>
                    <input v-model="draftForm.supplierId" type="text" placeholder="可留空" />
                  </label>
                  <label v-if="draftEditorMode === 'sale_order'" class="compact-field">
                    <span>客户名称</span>
                    <input v-model="draftForm.customerName" type="text" placeholder="客户名称" />
                  </label>
                  <label v-else class="compact-field">
                    <span>供应商名称</span>
                    <input v-model="draftForm.supplierName" type="text" placeholder="供应商名称" />
                  </label>
                  <label v-if="draftEditorMode === 'sale_order'" class="compact-field">
                    <span>优惠金额</span>
                    <input v-model="draftForm.discountAmount" type="text" placeholder="可留空" />
                  </label>
                  <label v-else class="compact-field">
                    <span>结算方式</span>
                    <input v-model="draftForm.settlementMethod" type="text" placeholder="可留空" />
                  </label>
                  <label v-if="draftEditorMode === 'purchase_order'" class="compact-field">
                    <span>仓库 ID</span>
                    <input v-model="draftForm.warehouseId" type="text" placeholder="可留空" />
                  </label>
                  <label class="compact-field draft-editor-grid__full">
                    <span>备注</span>
                    <textarea v-model="draftForm.notes" rows="3" placeholder="单据说明"></textarea>
                  </label>
                </div>
                <div class="draft-line-editor">
                  <div class="draft-line-editor__head">
                    <strong>商品行</strong>
                    <button type="button" class="ghost-action" @click="addDraftItemRow">新增一行</button>
                  </div>
                  <div v-for="(item, itemIndex) in draftForm.items" :key="`draft-item-${itemIndex}`" class="draft-line-row">
                    <label class="compact-field">
                      <span>商品 ID</span>
                      <input v-model="item.productId" type="text" placeholder="可留空" />
                    </label>
                    <label v-if="draftEditorMode === 'purchase_order'" class="compact-field">
                      <span>商品编码</span>
                      <input v-model="item.productCode" type="text" placeholder="可留空" />
                    </label>
                    <label class="compact-field">
                      <span>商品名称</span>
                      <input v-model="item.productName" type="text" placeholder="商品名称" />
                    </label>
                    <label class="compact-field">
                      <span>数量</span>
                      <input v-model="item.quantity" type="text" placeholder="0" />
                    </label>
                    <label class="compact-field">
                      <span>{{ draftEditorMode === 'purchase_order' ? '成本价' : '单价' }}</span>
                      <input v-model="item.unitPrice" type="text" placeholder="0.00" />
                    </label>
                    <button type="button" class="ghost-action danger-link draft-line-row__remove" @click="removeDraftItemRow(itemIndex)">
                      删除本行
                    </button>
                  </div>
                </div>
              </div>
              <label v-else class="compact-field">
                <span>内容</span>
                <textarea v-model="draftContentJson" rows="6" placeholder="草稿内容（JSON 或文本）"></textarea>
              </label>
              <label v-if="draftEditorMode !== 'custom'" class="compact-field">
                <span>JSON 预览</span>
                <textarea :value="draftStructuredPreview" rows="8" readonly class="draft-editor-preview"></textarea>
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
.agent-markdown :deep(ul),
.agent-markdown :deep(ol) {
  margin: 0;
}

.agent-markdown :deep(ul),
.agent-markdown :deep(ol) {
  padding-left: 22px;
}

.agent-markdown :deep(strong) {
  font-weight: 800;
}

.agent-markdown :deep(a) {
  color: var(--primary);
  text-decoration: underline;
}

.agent-markdown :deep(code) {
  padding: 1px 5px;
  border-radius: 4px;
  background: rgba(15, 23, 42, 0.08);
  font-family: "SFMono-Regular", Menlo, Consolas, monospace;
  font-size: 0.92em;
}

.agent-markdown__code {
  margin: 0;
  padding: 12px;
  overflow-x: auto;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: #0d1117;
  color: #e6edf3;
}

.agent-markdown__code code {
  background: transparent;
  color: inherit;
  font-family: "SFMono-Regular", Menlo, Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
}

.agent-markdown__table {
  overflow-x: auto;
}

.agent-markdown__table table {
  width: 100%;
  min-width: 0;
  border-collapse: collapse;
  font-size: 13px;
}

.agent-markdown__table th,
.agent-markdown__table td {
  padding: 8px 10px;
  border: 1px solid var(--line);
  text-align: left;
  vertical-align: top;
}

.agent-markdown__table th {
  background: #fafbfc;
  color: var(--muted);
  font-weight: 800;
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

.draft-editor-structured {
  display: grid;
  gap: 10px;
}

.draft-editor-grid {
  display: grid;
  gap: 8px;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
}

.draft-editor-grid__full {
  grid-column: 1 / -1;
}

.draft-line-editor {
  display: grid;
  gap: 8px;
  padding: 10px;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: rgba(15, 23, 42, 0.03);
}

.draft-line-editor__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.draft-line-row {
  display: grid;
  gap: 8px;
  grid-template-columns: repeat(auto-fit, minmax(132px, 1fr));
  padding: 10px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid var(--line);
}

.draft-line-row__remove {
  align-self: end;
}

.draft-editor-preview {
  font-family: "SFMono-Regular", Menlo, Consolas, monospace;
  font-size: 12px;
}

.agent-chat-head {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.agent-mobile-toggle,
.agent-mobile-close {
  display: none;
}

.agent-mobile-backdrop {
  display: none;
}

.agent-tool-pills {
  display: grid;
  gap: 8px;
}

.agent-tool-pill {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: #fff;
  font-size: 12px;
}

.agent-tool-pill__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--muted);
  flex-shrink: 0;
}

.agent-tool-pill[data-status="running"] .agent-tool-pill__dot {
  background: #2563eb;
  animation: agent-tool-pulse 1.2s ease-in-out infinite;
}

.agent-tool-pill[data-status="completed"] .agent-tool-pill__dot {
  background: var(--green);
}

.agent-tool-pill[data-status="failed"] .agent-tool-pill__dot {
  background: var(--red);
}

.agent-tool-pill__name {
  font-weight: 800;
  color: var(--text);
}

.agent-tool-pill__status {
  color: var(--muted);
  font-weight: 700;
}

.agent-tool-pill[data-status="running"] .agent-tool-pill__status {
  color: #2563eb;
}

.agent-tool-pill[data-status="completed"] .agent-tool-pill__status {
  color: var(--green);
}

.agent-tool-pill[data-status="failed"] .agent-tool-pill__status {
  color: var(--red);
}

.agent-tool-pill__duration {
  color: var(--faint);
  font-size: 11px;
}

.agent-tool-pill__summary {
  width: 100%;
  color: var(--muted);
  font-size: 11px;
  line-height: 1.5;
}

.agent-live-tool-banner {
  display: grid;
  gap: 4px;
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: 14px;
  background: #eef4ff;
  border: 1px solid #c7d8ff;
}

.agent-live-tool-banner__label {
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #3157a6;
}

.agent-live-tool-banner__name {
  font-size: 14px;
  color: #16315f;
}

.agent-live-tool-banner__status {
  font-size: 12px;
  font-weight: 700;
  color: #2563eb;
}

.agent-live-tool-banner__summary {
  font-size: 12px;
  line-height: 1.5;
  color: #4b5563;
}

.agent-live-tool-banner[data-status="completed"] {
  background: #f0fdf4;
  border-color: #bbf7d0;
}

.agent-live-tool-banner[data-status="completed"] .agent-live-tool-banner__label,
.agent-live-tool-banner[data-status="completed"] .agent-live-tool-banner__name,
.agent-live-tool-banner[data-status="completed"] .agent-live-tool-banner__status {
  color: #166534;
}

.agent-live-tool-banner[data-status="failed"] {
  background: #fff1f2;
  border-color: #fecdd3;
}

.agent-live-tool-banner[data-status="failed"] .agent-live-tool-banner__label,
.agent-live-tool-banner[data-status="failed"] .agent-live-tool-banner__name,
.agent-live-tool-banner[data-status="failed"] .agent-live-tool-banner__status {
  color: #be123c;
}

@keyframes agent-tool-pulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
    box-shadow: 0 0 0 0 rgba(37, 99, 235, 0.5);
  }
  50% {
    opacity: 0.65;
    transform: scale(1.3);
    box-shadow: 0 0 0 4px rgba(37, 99, 235, 0);
  }
}

.agent-draft-card {
  display: grid;
  gap: 10px;
  padding: 14px;
  border: 2px dashed #d4a017;
  border-radius: var(--radius);
  background: #fffbea;
}

.agent-draft-card__head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.agent-draft-card__icon {
  display: inline-grid;
  place-items: center;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border-radius: 8px;
  background: #fde68a;
  color: #92400e;
  font-size: 14px;
  font-weight: 900;
}

.agent-draft-card__head-text {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.agent-draft-card__head-text strong {
  color: var(--text);
  font-size: 14px;
}

.agent-draft-card__head-text span {
  color: var(--muted);
  font-size: 12px;
}

.agent-draft-card__body {
  font-size: 13px;
}

.agent-draft-card__body :deep(.agent-draft-rows) {
  display: grid;
  gap: 6px;
  margin: 0;
}

.agent-draft-card__body :deep(.agent-draft-row) {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.agent-draft-card__body :deep(.agent-draft-row span) {
  color: var(--muted);
  font-size: 12px;
  font-weight: 800;
}

.agent-draft-card__body :deep(.agent-draft-row strong) {
  color: var(--text);
  font-size: 13px;
  text-align: right;
  overflow-wrap: anywhere;
}

.agent-draft-card__actions {
  display: flex;
  gap: 8px;
}

.agent-draft-card__hint {
  margin: 0;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.5;
}

.agent-draft-card__hint code {
  padding: 1px 5px;
  border-radius: 4px;
  background: rgba(15, 23, 42, 0.08);
  font-family: "SFMono-Regular", Menlo, Consolas, monospace;
  font-size: 11px;
}

.agent-draft-card__summary {
  margin: 0;
  color: var(--text);
  font-size: 12px;
  line-height: 1.5;
}

.agent-draft-card__error {
  margin: 0;
}

.agent-terminal-banner {
  display: grid;
  gap: 6px;
  margin-top: 10px;
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-left-width: 4px;
  border-radius: 12px;
  background: #f8fafc;
  font-size: 13px;
  line-height: 1.5;
}

.agent-terminal-banner[data-tone="success"] {
  border-color: #bbf7d0;
  border-left-color: var(--green, #16a34a);
  background: #f0fdf4;
}

.agent-terminal-banner[data-tone="warning"] {
  border-color: #fde68a;
  border-left-color: #d97706;
  background: #fffbeb;
}

.agent-terminal-banner[data-tone="error"] {
  border-color: #fecdd3;
  border-left-color: var(--red, #dc2626);
  background: #fff1f2;
}

.agent-terminal-banner[data-tone="muted"] {
  border-color: #e5e7eb;
  border-left-color: #6b7280;
  background: #f9fafb;
}

.agent-terminal-banner[data-tone="info"] {
  border-color: #c7d8ff;
  border-left-color: #2563eb;
  background: #eef4ff;
}

.agent-terminal-banner__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.agent-terminal-banner__title {
  font-weight: 800;
  color: var(--text);
}

.agent-terminal-banner__status {
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.08);
  color: var(--text);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.agent-terminal-banner[data-tone="success"] .agent-terminal-banner__status {
  background: rgba(22, 163, 74, 0.12);
  color: #166534;
}

.agent-terminal-banner[data-tone="warning"] .agent-terminal-banner__status {
  background: rgba(217, 119, 6, 0.14);
  color: #92400e;
}

.agent-terminal-banner[data-tone="error"] .agent-terminal-banner__status {
  background: rgba(220, 38, 38, 0.12);
  color: #991b1b;
}

.agent-terminal-banner[data-tone="muted"] .agent-terminal-banner__status {
  background: rgba(75, 85, 99, 0.12);
  color: #374151;
}

.agent-terminal-banner__desc {
  margin: 0;
  color: var(--text);
}

.agent-terminal-banner__meta {
  margin: 0;
  color: var(--muted);
  font-size: 12px;
}

.agent-terminal-banner__meta--ok {
  color: #166534;
  font-weight: 700;
}

.agent-terminal-banner__tools {
  display: grid;
  gap: 8px;
  margin-top: 4px;
  padding: 8px 10px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.6);
}

.agent-terminal-banner__tool-group {
  display: grid;
  gap: 4px;
  font-size: 12px;
}

.agent-terminal-banner__tool-group span {
  color: var(--muted);
  font-weight: 800;
}

.agent-terminal-banner__tool-group ul {
  margin: 0;
  padding-left: 18px;
  color: var(--text);
}

.agent-compacted-summary {
  margin: 0;
  color: var(--text);
  font-size: 13px;
  line-height: 1.5;
}

.agent-compacted-meta {
  display: grid;
  gap: 2px;
  margin: 6px 0 0;
  padding: 0;
  list-style: none;
  color: var(--muted);
  font-size: 12px;
}

@media (max-width: 767px) {
  .agent-mobile-toggle,
  .agent-mobile-close {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 44px;
    min-width: 44px;
    padding: 0 12px;
  }

  .agent-mobile-backdrop {
    display: block;
    position: fixed;
    inset: 0;
    z-index: 45;
    border: 0;
    background: rgba(15, 23, 42, 0.4);
    backdrop-filter: blur(2px);
  }
}
</style>
