<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { Bot, ChevronLeft, ChevronRight, CircleAlert, CircleCheck, CircleX, Clock3, FileText, Layers3, ListTree, MessageSquare, RefreshCw, Search, ShieldAlert, Wrench, X } from 'lucide-vue-next'
import { useRoute } from 'vue-router'
import AdminLayout from '@/features/admin/components/AdminLayout.vue'
import AdminPanelState from '@/features/admin/components/AdminPanelState.vue'
import AdminStatusBadge from '@/features/admin/components/AdminStatusBadge.vue'
import AdminScopeFilter from '@/features/admin/components/AdminScopeFilter.vue'
import { useSession } from '@/app/stores/session'
import { useAdminSession } from '@/app/stores/admin-session'
import { fetchAdminContext, fetchAdminDrafts, fetchAdminEvents, fetchAdminMessages, fetchAdminRun, fetchAdminRuns, fetchAdminUsage, streamAdminEvents, type AdminContextResponse, type AdminDraft, type AdminMessage, type AdminUsage } from '@/shared/api/admin'
import type { AdminEvent, AdminRunSummary } from '@/entities/admin/contracts'

const session = useSession()
const adminSession = useAdminSession()
const route = useRoute()
const search = ref('')
const terminalStatus = ref('')
const actorUserId = ref('')
const toolName = ref('')
const runModelId = ref('')
const runFrom = ref('')
const runTo = ref('')
const runRangeError = ref('')
const page = ref(0)
const size = 20
const items = ref<AdminRunSummary[]>([])
const total = ref(0)
const hasNext = ref(false)
const loading = ref(false)
const error = ref<unknown>(null)
const selected = ref<AdminRunSummary | null>(null)
const detailLoading = ref(false)
const detailError = ref<unknown>(null)
const events = ref<AdminEvent[]>([])
const messages = ref<AdminMessage[]>([])
const context = ref<AdminContextResponse | null>(null)
const drafts = ref<AdminDraft[]>([])
const eventIntegrity = ref(true)
const usage = ref<AdminUsage[]>([])
const usageTotal = ref(0)
const usageLoading = ref(false)
const usageError = ref<unknown>(null)
const ownerUserId = ref('')
const storeId = ref('')
const modelId = ref('')
const usageFrom = ref('')
const usageTo = ref('')
const granularity = ref<'HOUR' | 'DAY' | 'WEEK'>('DAY')
const usageRangeError = ref('')
const usageGeneratedAt = ref<string | null>(null)
const usageReturnedFrom = ref<string | null>(null)
const usageReturnedTo = ref<string | null>(null)
const usageReturnedGranularity = ref<string | null>(null)
const streamState = ref<'idle' | 'connecting' | 'connected' | 'reconnecting' | 'closed'>('idle')
const includeContent = ref(false)
const detailSerial = ref(0)
let listSerial = 0
let streamController: AbortController | null = null

const statusLabel: Record<string, string> = { completed: '已完成', success: '已完成', running: '进行中', failed: '失败', cancelled: '已取消', exhausted: '已耗尽' }
const canRead = computed(() => adminSession.can('admin.agent.run.read'))
const state = computed(() => !canRead.value ? 'blocked' : loading.value ? 'loading' : error.value ? 'error' : items.value.length === 0 ? 'empty' : null)
const detailState = computed(() => detailLoading.value ? 'loading' : detailError.value ? 'error' : null)
const detailErrorMessage = computed(() => detailError.value instanceof Error ? detailError.value.message : '运行详情读取失败')
const listErrorMessage = computed(() => error.value instanceof Error ? error.value.message : 'Agent 运行列表读取失败')
const selectedScope = computed(() => selected.value ? { ownerUserId: selected.value.ownerUserId || undefined, storeId: selected.value.storeId || undefined } : {})
const actualEvents = computed(() => events.value.filter((event) => eventKind(event) === 'tool'))
const planEvents = computed(() => events.value.filter((event) => eventKind(event) === 'plan'))
const timelineHasGap = computed(() => !eventIntegrity.value || events.value.some((event, index) => index > 0 && event.sequence !== events.value[index - 1].sequence + 1))

async function load() {
  const serial = ++listSerial
  if (!session.token.value) { if (serial === listSerial) error.value = new Error('管理员会话已失效'); return }
  if (!canRead.value) { if (serial === listSerial) { error.value = null; items.value = []; total.value = 0; hasNext.value = false } return }
  const from = toIso(runFrom.value)
  const to = toIso(runTo.value)
  if (runFrom.value && !from) { rejectRunRange('开始时间格式无效'); return }
  if (runTo.value && !to) { rejectRunRange('结束时间格式无效'); return }
  if (from && to && new Date(from).getTime() >= new Date(to).getTime()) {
    rejectRunRange('运行开始时间必须早于结束时间')
    return
  }
  runRangeError.value = ''
  loading.value = true; error.value = null
  try {
    const result = await fetchAdminRuns(session.token.value, {
      runId: search.value.trim() || undefined,
      actorUserId: actorUserId.value.trim() || undefined,
      toolName: toolName.value.trim() || undefined,
      modelId: runModelId.value.trim() || undefined,
      terminalStatus: terminalStatus.value || undefined,
      from,
      to,
      ownerUserId: ownerUserId.value || undefined,
      storeId: storeId.value || undefined,
      page: page.value,
      size,
    })
    if (serial !== listSerial) return
    items.value = result.items; total.value = result.total; hasNext.value = result.hasNext
  } catch (cause) {
    if (serial !== listSerial) return
    error.value = cause; items.value = []
  } finally {
    if (serial === listSerial) loading.value = false
  }
}

function rejectRunRange(message: string) {
  runRangeError.value = message
  loading.value = false
  error.value = null
  items.value = []
  total.value = 0
  hasNext.value = false
}

async function loadUsage() {
  if (!session.token.value) { usageError.value = new Error('管理员会话已失效'); return }
  if (!canRead.value) { usageError.value = null; usage.value = []; usageTotal.value = 0; return }
  const from = usageFrom.value ? new Date(usageFrom.value).toISOString() : undefined
  const to = usageTo.value ? new Date(usageTo.value).toISOString() : undefined
  if (from && to && new Date(from).getTime() >= new Date(to).getTime()) {
    usageRangeError.value = '用量开始时间必须早于结束时间'
    usageError.value = null
    usage.value = []
    usageTotal.value = 0
    return
  }
  usageRangeError.value = ''
  usageLoading.value = true; usageError.value = null
  try {
    const result = await fetchAdminUsage(session.token.value, {
      from,
      to,
      modelId: modelId.value.trim() || undefined,
      granularity: granularity.value,
      ownerUserId: ownerUserId.value || undefined,
      storeId: storeId.value || undefined,
      page: 0,
      size: 50,
    })
    usage.value = result.items
    usageTotal.value = result.total
    usageGeneratedAt.value = result.generatedAt ?? null
    usageReturnedFrom.value = result.from ?? null
    usageReturnedTo.value = result.to ?? null
    usageReturnedGranularity.value = result.granularity ?? granularity.value
  }
  catch (cause) { usageError.value = cause; usage.value = [] }
  finally { usageLoading.value = false }
}

async function openDetail(run: AdminRunSummary) {
  stopStream()
  const serial = ++detailSerial.value
  selected.value = run; detailLoading.value = true; detailError.value = null; events.value = []; messages.value = []; context.value = null; drafts.value = []; eventIntegrity.value = true; includeContent.value = false
  if (!session.token.value) { detailError.value = new Error('管理员会话已失效'); detailLoading.value = false; return }
  try {
    const token = session.token.value
    const scope = { ownerUserId: run.ownerUserId || undefined, storeId: run.storeId || undefined }
    const [full, eventPage, contextResult, draftResult, messageResult] = await Promise.all([
      fetchAdminRun(token, run.runId, scope),
      fetchAdminEvents(token, run.runId, { ...scope, includeContent: false }),
      fetchAdminContext(token, run.runId, scope),
      fetchAdminDrafts(token, run.runId, scope),
      run.conversationId ? fetchAdminMessages(token, run.conversationId, { ...scope, includeContent: false, page: 0, size: 50 }) : Promise.resolve(null),
    ])
    if (serial !== detailSerial.value) return
    selected.value = full; events.value = eventPage.items.slice().sort((a, b) => a.sequence - b.sequence); eventIntegrity.value = eventPage.eventIntegrity; context.value = contextResult; drafts.value = draftResult; messages.value = messageResult?.items ?? []
    if (!isTerminal(full.terminalStatus)) void startStream(full.runId)
  } catch (cause) { if (serial === detailSerial.value) detailError.value = cause }
  finally { if (serial === detailSerial.value) detailLoading.value = false }
}

function closeDetail() { detailSerial.value += 1; stopStream(); selected.value = null; detailError.value = null; events.value = []; messages.value = []; context.value = null; drafts.value = [] }

function revealContent() {
  if (selected.value && adminSession.can('admin.agent.content.read')) { includeContent.value = true; void openDetailWithContent(selected.value) }
}

async function openDetailWithContent(run: AdminRunSummary) {
  stopStream(); const serial = ++detailSerial.value; detailLoading.value = true; detailError.value = null
  if (!session.token.value) { detailError.value = new Error('管理员会话已失效'); detailLoading.value = false; return }
  try {
    const scope = { ownerUserId: run.ownerUserId || undefined, storeId: run.storeId || undefined }
    const [eventPage, messageResult] = await Promise.all([
      fetchAdminEvents(session.token.value, run.runId, { ...scope, includeContent: true }),
      run.conversationId ? fetchAdminMessages(session.token.value, run.conversationId, { ...scope, includeContent: true, page: 0, size: 50 }) : Promise.resolve(null),
    ])
    if (serial !== detailSerial.value || !selected.value || selected.value.runId !== run.runId) return
    events.value = eventPage.items.slice().sort((a, b) => a.sequence - b.sequence); eventIntegrity.value = eventPage.eventIntegrity; messages.value = messageResult?.items ?? []
    if (!isTerminal(run.terminalStatus)) void startStream(run.runId)
  } catch (cause) { if (serial === detailSerial.value) detailError.value = cause }
  finally { if (serial === detailSerial.value) detailLoading.value = false }
}

function applyScope() { page.value = 0; void load(); void loadUsage() }
function applyRunFilters() { page.value = 0; void load() }
function resetRunFilters() {
  search.value = ''; terminalStatus.value = ''; actorUserId.value = ''; toolName.value = ''; runModelId.value = ''; runFrom.value = ''; runTo.value = ''
  page.value = 0
  void load()
}
function applyUsageFilters() { void loadUsage() }
function previous() { if (page.value > 0) { page.value -= 1; void load() } }
function next() { if (hasNext.value) { page.value += 1; void load() } }
function date(value?: string | null) { return value ? new Date(value).toLocaleString('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }) : '-' }
function duration(value?: number | null) { return value == null ? '-' : value >= 1000 ? `${(value / 1000).toFixed(2)}s` : `${value}ms` }
function usageSourceType(value?: string | null): 'completed' | 'attention' | 'unavailable' { return value === 'EXACT' ? 'completed' : value === 'ESTIMATED' ? 'attention' : 'unavailable' }
function usageSourceLabel(value?: string | null) { return value === 'EXACT' ? '精确' : value === 'ESTIMATED' ? '估算' : '不可用' }
function contextWindowSourceLabel(value?: string | null) { return value === 'CONFIGURED_OVERRIDE' ? '配置覆盖' : value === 'KNOWN_MODEL' ? '已知模型' : value === 'CONSERVATIVE_FALLBACK' ? '保守估算' : '来源未知' }
function usageBucket(item: AdminUsage) { return item.bucketStart ? `${date(item.bucketStart)} - ${date(item.bucketEnd)}` : item.runId || '未提供时间桶' }
function usageMetric(value?: number | null) { return value == null ? '-' : `${value}ms` }
function toIso(value: string) {
  if (!value) return undefined
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? undefined : parsed.toISOString()
}
function statusTone(value?: string | null) { return /completed|success|ok/i.test(value ?? '') ? 'ok' : /failed|cancel|error/i.test(value ?? '') ? 'bad' : /running|started|progress/i.test(value ?? '') ? 'warn' : '' }
function statusType(value?: string | null) { return statusTone(value) === 'ok' ? 'completed' : statusTone(value) === 'bad' ? 'failed' : statusTone(value) === 'warn' ? 'running' : 'attention' }
function eventKind(item: AdminEvent) {
  if (/^plan[._-]delta$/i.test(item.eventType) || /plan[._-]/i.test(item.eventType)) return 'plan'
  if (/context[._-]/i.test(item.eventType)) return 'context'
  if (/answer[._-]/i.test(item.eventType)) return 'answer'
  if (/^tool[._-]/i.test(item.eventType)) return 'tool'
  if (/^run[._-]/i.test(item.eventType)) return 'run'
  return 'other'
}
function eventLabel(item: AdminEvent) {
  const kind = eventKind(item)
  if (kind === 'plan') return '模型计划摘要'
  if (kind === 'tool') return item.toolName ? `工具 · ${item.toolName}` : '工具事件'
  if (kind === 'context') return '上下文处理'
  if (kind === 'answer') return '正式回答'
  if (kind === 'run') return '运行状态'
  return item.eventType
}
function eventStatusLabel(value?: string | null) { const normalized = value?.toUpperCase() ?? ''; return { STARTED: '开始', PROGRESS: '进行中', COMPLETED: '完成', FAILED: '失败', CANCELLED: '已取消' }[normalized] || value || '未知' }
function isTerminal(status?: string | null) { return /completed|failed|cancelled|exhausted|success/i.test(status ?? '') }
function lastSequence() { return events.value.reduce((max, event) => Math.max(max, event.sequence), 0) }
function appendEvents(incoming: AdminEvent[]) {
  const known = new Set(events.value.map((event) => `${event.eventId}:${event.sequence}`))
  for (const event of incoming) { const key = `${event.eventId}:${event.sequence}`; if (!known.has(key)) { known.add(key); events.value.push(event) } }
  events.value.sort((a, b) => a.sequence - b.sequence)
}
async function startStream(runId: string, retry = true) {
  if (!session.token.value || !selected.value || selected.value.runId !== runId || isTerminal(selected.value.terminalStatus)) return
  streamController = new AbortController(); streamState.value = retry ? 'reconnecting' : 'connecting'
  try {
    streamState.value = 'connected'
    await streamAdminEvents(session.token.value, runId, { ...selectedScope.value, afterSequence: lastSequence(), includeContent: includeContent.value }, (event) => {
      if (!selected.value || selected.value.runId !== runId) return
      appendEvents([event])
      if (/run[._-](completed|failed|cancelled|exhausted)/i.test(event.eventType)) { selected.value = { ...selected.value, terminalStatus: event.eventType.split(/[._-]/).pop() ?? selected.value.terminalStatus }; stopStream(); streamState.value = 'closed' }
    }, streamController.signal, (integrity) => { eventIntegrity.value = eventIntegrity.value && integrity })
    if (selected.value && selected.value.runId === runId && !isTerminal(selected.value.terminalStatus)) streamState.value = 'closed'
  } catch (cause) {
    if (streamController?.signal.aborted) return
    streamState.value = 'reconnecting'
    if (retry) {
      try { const replay = await fetchAdminEvents(session.token.value, runId, { ...selectedScope.value, afterSequence: lastSequence(), includeContent: false }); eventIntegrity.value = eventIntegrity.value && replay.eventIntegrity; appendEvents(replay.items); await startStream(runId, false) }
      catch (replayError) { detailError.value = replayError; streamState.value = 'closed' }
    } else { detailError.value = cause; streamState.value = 'closed' }
  }
}
function stopStream() { streamController?.abort(); streamController = null; streamState.value = 'idle' }

watch([search, terminalStatus], () => { page.value = 0; void load() })
onMounted(async () => {
  if (!await adminSession.ensure(session.token.value)) return
  const queryRunId = route.query.runId
  if (typeof queryRunId === 'string' && queryRunId.trim()) search.value = queryRunId.trim()
  await load(); void loadUsage()
  if (typeof queryRunId === 'string') { const target = items.value.find((item) => item.runId === queryRunId); if (target) void openDetail(target) }
})
onUnmounted(stopStream)
</script>

<template>
  <AdminLayout active-id="agent">
    <section class="admin-page-v2">
      <header class="admin-page-v2__header"><div><div class="admin-page-v2__crumb">Admin / Agent / <strong>Runs</strong></div><h1>Agent 运行</h1><p>按运行 ID、发起者、门店范围和终态查看可追踪的 Agent 观测记录。</p></div><button class="admin-button-v2" type="button" :disabled="loading" @click="load"><RefreshCw :class="{ 'is-spinning': loading }" aria-hidden="true" />刷新</button></header>
      <div v-if="error" class="admin-error-v2" role="alert"><ShieldAlert aria-hidden="true" /><span>{{ listErrorMessage }}</span><button type="button" @click="load">重试</button></div>
      <div class="admin-toolbar admin-runs-scope"><AdminScopeFilter v-model:owner-user-id="ownerUserId" v-model:store-id="storeId" :owner-user-ids="adminSession.session.value?.ownerUserIds" :store-ids="adminSession.session.value?.storeIds" /><button class="admin-button-v2" type="button" @click="applyScope">应用范围</button></div>
      <div class="admin-grid"><article class="admin-card-v2 admin-span-12"><div class="admin-card-v2__header"><div><h2>运行记录</h2><p>仅显示服务端按管理员范围返回的摘要；未结束运行不会被渲染为完成。</p></div><span class="admin-card-v2__meta">{{ total }} 条记录</span></div><div class="admin-card-v2__body admin-toolbar admin-run-filters"><label class="admin-field"><Search aria-hidden="true" /><input v-model="search" type="search" placeholder="运行 ID" aria-label="按运行 ID 搜索" /></label><label class="admin-field"><Search aria-hidden="true" /><input v-model="actorUserId" type="search" inputmode="numeric" placeholder="发起者 ID" aria-label="按发起者 ID 筛选" /></label><label class="admin-field"><Wrench aria-hidden="true" /><input v-model="toolName" type="search" placeholder="工具名" aria-label="按工具名筛选" /></label><label class="admin-field"><Bot aria-hidden="true" /><input v-model="runModelId" type="search" placeholder="模型 ID" aria-label="按模型 ID 筛选运行" /></label><label class="admin-field"><Clock3 aria-hidden="true" /><select v-model="terminalStatus" aria-label="运行状态筛选"><option value="">全部状态</option><option value="COMPLETED">已完成</option><option value="RUNNING">进行中</option><option value="FAILED">失败</option><option value="CANCELLED">已取消</option><option value="EXHAUSTED">已耗尽</option></select></label><label class="admin-datetime"><Clock3 aria-hidden="true" /><span>开始</span><input v-model="runFrom" type="datetime-local" aria-label="运行开始时间" /></label><label class="admin-datetime"><Clock3 aria-hidden="true" /><span>结束</span><input v-model="runTo" type="datetime-local" aria-label="运行结束时间" /></label><button class="admin-button-v2" type="button" :disabled="loading" @click="applyRunFilters"><RefreshCw :class="{ 'is-spinning': loading }" aria-hidden="true" />应用筛选</button><button class="admin-button-v2" type="button" :disabled="loading" @click="resetRunFilters">清除筛选</button></div><p v-if="runRangeError" class="admin-filter-error" role="alert">{{ runRangeError }}</p><AdminPanelState v-if="state === 'blocked'" state="blocked" title="运行观测受控" message="当前管理员会话没有 Agent 运行读取权限，页面不会请求或展示运行数据。" /><AdminPanelState v-else-if="state === 'loading'" state="loading" title="正在读取运行记录" /><AdminPanelState v-else-if="state === 'error'" state="error" :message="listErrorMessage" @retry="load" /><AdminPanelState v-else-if="state === 'empty'" state="empty" title="当前范围暂无运行记录" message="服务端没有返回可见的 Agent 运行，不会展示示例数据。" /><div v-else class="admin-table-wrap"><table class="admin-table-v2"><caption class="sr-only">Agent 运行记录</caption><thead><tr><th>运行 ID</th><th>发起者 / 门店</th><th>模型</th><th>开始时间</th><th>Token</th><th>工具</th><th>状态</th></tr></thead><tbody><tr v-for="run in items" :key="run.runId" tabindex="0" @click="openDetail(run)" @keydown.enter="openDetail(run)"><td><strong><code>{{ run.runId }}</code></strong><small>{{ run.conversationId ? `会话 ${run.conversationId}` : '会话未提供' }}</small></td><td><code>{{ run.actorUserId || '-' }}</code><small>{{ run.storeId || '门店未提供' }}</small></td><td>{{ run.modelId || '-' }}</td><td>{{ date(run.startedAt) }}</td><td>{{ run.totalTokens ?? '-' }}<small>{{ run.tokenSource }}</small></td><td>{{ run.toolCallCount ?? 0 }}</td><td><AdminStatusBadge :status="statusType(run.terminalStatus)" :label="statusLabel[run.terminalStatus?.toLowerCase()] || run.terminalStatus || '未知'" /></td></tr></tbody></table></div><footer v-if="state !== 'loading' && state !== 'blocked' && !error && total > 0" class="admin-pagination"><span>第 {{ page + 1 }} 页</span><button type="button" :disabled="page === 0" aria-label="上一页" @click="previous"><ChevronLeft aria-hidden="true" /></button><button type="button" :disabled="!hasNext" aria-label="下一页" @click="next"><ChevronRight aria-hidden="true" /></button></footer></article><article class="admin-card-v2 admin-span-12"><div class="admin-card-v2__header"><div><h2>Token 与耗时</h2><p>调用 `/v2/admin/agent/usage` 返回的用量页，精确、估算和不可用来源分开标记。</p></div><span class="admin-card-v2__meta">{{ usageTotal }} 条<span v-if="usageReturnedGranularity"> · {{ usageReturnedGranularity }}</span></span></div><div v-if="canRead" class="admin-card-v2__body admin-toolbar admin-usage-filters"><label class="admin-field"><Search aria-hidden="true" /><input v-model="modelId" type="search" placeholder="模型 ID（精确）" aria-label="按模型 ID 精确筛选" /></label><label class="admin-datetime"><Clock3 aria-hidden="true" /><span>开始</span><input v-model="usageFrom" type="datetime-local" aria-label="用量开始时间" /></label><label class="admin-datetime"><Clock3 aria-hidden="true" /><span>结束</span><input v-model="usageTo" type="datetime-local" aria-label="用量结束时间" /></label><label class="admin-field"><Clock3 aria-hidden="true" /><select v-model="granularity" aria-label="用量时间粒度"><option value="HOUR">按小时</option><option value="DAY">按天</option><option value="WEEK">按周</option></select></label><button class="admin-button-v2" type="button" :disabled="usageLoading" @click="applyUsageFilters"><RefreshCw :class="{ 'is-spinning': usageLoading }" aria-hidden="true" />应用筛选</button></div><p v-if="usageReturnedFrom || usageGeneratedAt" class="admin-usage-meta">统计范围：{{ date(usageReturnedFrom) }} - {{ date(usageReturnedTo) }}<span v-if="usageGeneratedAt"> · 生成于 {{ date(usageGeneratedAt) }}</span></p><AdminPanelState v-if="!canRead" state="blocked" title="用量统计受控" message="当前管理员会话没有 Agent 运行读取权限，页面不会请求或展示用量数据。" /><AdminPanelState v-else-if="usageLoading" state="loading" title="正在读取用量" /><AdminPanelState v-else-if="usageError" state="error" :message="usageError instanceof Error ? usageError.message : '用量读取失败'" @retry="loadUsage" /><AdminPanelState v-else-if="usageRangeError" state="error" :message="usageRangeError" @retry="loadUsage" /><AdminPanelState v-else-if="usage.length === 0" state="empty" title="当前范围暂无用量" message="服务端没有返回 Token 或耗时统计。" /><div v-else class="admin-table-wrap"><table class="admin-table-v2"><caption class="sr-only">Token 与耗时</caption><thead><tr><th>时间桶</th><th>模型</th><th>请求数</th><th>输入 / 输出</th><th>总 Token</th><th>平均耗时 / P95</th><th>平均首字 / P95</th><th>来源</th></tr></thead><tbody><tr v-for="item in usage" :key="item.runId || `${item.bucketStart ?? 'bucket'}-${item.modelId ?? 'model'}`"><td><code>{{ usageBucket(item) }}</code></td><td>{{ item.modelId || '-' }}</td><td>{{ item.requestCount ?? '-' }}</td><td>{{ item.inputTokens ?? '-' }} / {{ item.outputTokens ?? '-' }}</td><td>{{ item.totalTokens ?? '-' }}</td><td>{{ usageMetric(item.averageDurationMs ?? item.durationMs) }} / {{ usageMetric(item.p95DurationMs) }}</td><td>{{ usageMetric(item.averageTimeToFirstTokenMs ?? item.timeToFirstTokenMs) }} / {{ usageMetric(item.p95TimeToFirstTokenMs) }}</td><td><AdminStatusBadge :status="usageSourceType(item.tokenSource)" :label="usageSourceLabel(item.tokenSource)" /><small>{{ item.estimated ? '估算值' : item.scopeCompleteness || '服务端统计' }}</small></td></tr></tbody></table></div></article></div>
    </section>
    <button v-if="selected" type="button" class="admin-detail-scrim" aria-label="关闭运行详情" @click="closeDetail" />
    <aside v-if="selected" class="admin-detail-drawer admin-run-drawer" aria-label="运行详情"><header class="admin-detail-drawer__head"><div><div class="admin-page-v2__crumb">RUN DETAIL</div><h2>运行详情</h2><p><code>{{ selected.runId }}</code></p></div><button class="admin-detail-drawer__close" type="button" aria-label="关闭运行详情" title="关闭运行详情" @click="closeDetail"><X aria-hidden="true" /></button></header><div class="admin-detail-drawer__body"><AdminPanelState v-if="detailState === 'loading'" state="loading" title="正在读取运行详情" /><AdminPanelState v-else-if="detailState === 'error'" state="error" :message="detailErrorMessage" @retry="openDetail(selected!)" /><template v-else>
      <section class="admin-run-summary"><div class="admin-run-summary__status"><AdminStatusBadge :status="statusType(selected.terminalStatus)" :label="statusLabel[selected.terminalStatus?.toLowerCase()] || selected.terminalStatus || '未知'" /><span class="admin-card-v2__meta">{{ selected.contentRedacted ? '默认已脱敏' : '仅展示服务端授权字段' }}</span></div><dl><dt>发起者</dt><dd><code>{{ selected.actorUserId || '未提供' }}</code></dd><dt>Owner / 门店</dt><dd><code>{{ selected.ownerUserId }}</code> / <code>{{ selected.storeId || '未提供' }}</code></dd><dt>会话</dt><dd><code>{{ selected.conversationId || '未提供' }}</code></dd><dt>模型</dt><dd>{{ selected.modelId || '未提供' }}</dd><dt>时间</dt><dd>{{ date(selected.startedAt) }} → {{ date(selected.completedAt) }}</dd><dt>耗时 / 首字</dt><dd>{{ duration(selected.durationMs) }} / {{ duration(selected.timeToFirstTokenMs) }}</dd><dt>Token</dt><dd>{{ selected.totalTokens ?? '未生成' }} <small>{{ selected.tokenSource }}{{ selected.inputTokens != null || selected.outputTokens != null ? ` · 输入 ${selected.inputTokens ?? '-'} / 输出 ${selected.outputTokens ?? '-'}` : '' }}</small></dd><dt>轮次 / 工具</dt><dd>{{ selected.iterationCount ?? '-' }} / {{ selected.toolCallCount ?? '-' }}</dd></dl></section>
      <section class="admin-detail-section"><div class="admin-detail-section__head"><div><h3><ListTree aria-hidden="true" />事件时间线</h3><p>计划事件只代表模型计划；实际执行以工具事件为准。计划 {{ planEvents.length }} · 实际 {{ actualEvents.length }}</p></div><span class="admin-card-v2__meta">{{ streamState === 'connected' ? '实时连接中' : streamState === 'reconnecting' ? '补读/重连中' : streamState === 'closed' ? '流已结束' : '持久化记录' }} · {{ timelineHasGap ? '序列有缺口' : '序列完整' }}</span></div><div v-if="!events.length" class="admin-empty-v2"><CircleAlert aria-hidden="true" /><div><strong>没有可见事件</strong><p>服务端未返回事件，或当前内容权限不包含事件摘要。</p></div></div><div v-else class="admin-timeline-v2"><div v-for="event in events" :key="`${event.eventId}-${event.sequence}`" class="admin-timeline-v2__item" :class="`admin-timeline-v2__item--${eventKind(event)}`"><span class="admin-timeline-v2__time">#{{ event.sequence }}<br>{{ date(event.occurredAt) }}</span><div class="admin-timeline-v2__content"><div class="admin-event-title"><CircleCheck v-if="statusTone(event.status) === 'ok'" aria-hidden="true" /><CircleX v-else-if="statusTone(event.status) === 'bad'" aria-hidden="true" /><Wrench v-else-if="eventKind(event) === 'tool'" aria-hidden="true" /><Layers3 v-else-if="eventKind(event) === 'context'" aria-hidden="true" /><FileText v-else aria-hidden="true" /><strong>{{ eventLabel(event) }}</strong><span class="admin-event-kind">{{ eventKind(event) === 'plan' ? '计划' : eventKind(event) === 'tool' ? '实际调用' : '运行证据' }}</span></div><p>{{ eventStatusLabel(event.status) }} · {{ duration(event.durationMs) }} · {{ event.redactionState }}</p><code v-if="event.callId" class="admin-event-call-id">callId {{ event.callId }}</code><div v-if="event.argumentSummary || event.resultSummary" class="admin-event-summary"><span v-if="event.argumentSummary">参数：{{ event.argumentSummary }}</span><span v-if="event.resultSummary">结果：{{ event.resultSummary }}</span></div><span v-else-if="event.redactionState === 'REDACTED'" class="admin-event-redacted">参数与结果已脱敏</span></div></div></div></section>
      <section class="admin-detail-section"><div class="admin-detail-section__head"><div><h3><Layers3 aria-hidden="true" />上下文窗口</h3><p>只展示预算、检查点和脱敏元数据，不包含隐藏推理。</p></div><span class="admin-card-v2__meta">{{ context?.scopeCompleteness || 'UNKNOWN' }}</span></div><AdminPanelState v-if="!context" state="empty" title="尚未返回上下文" message="服务端没有提供当前运行的上下文检查点。" /><div v-else><div class="admin-context-metrics"><div><span>模型窗口</span><strong>{{ context.contextWindowTokens ?? '-' }}</strong><small>{{ contextWindowSourceLabel(context.contextWindowSource) }}</small></div><div><span>估算输入</span><strong>{{ context.estimatedInputTokens ?? '-' }}</strong></div><div><span>估算输出</span><strong>{{ context.estimatedOutputTokens ?? '-' }}</strong></div><div><span>检查点</span><strong>{{ context.checkpoints.length }}</strong></div></div><div v-if="context.checkpoints.length" class="admin-checkpoint-list"><div v-for="checkpoint in context.checkpoints" :key="checkpoint.checkpointId || `${checkpoint.createdAt}-${checkpoint.revision}`"><div><strong>{{ checkpoint.quality || checkpoint.status || '检查点' }}</strong><span>{{ date(checkpoint.createdAt) }}</span></div><p><code>{{ checkpoint.checkpointId || '-' }}</code> · {{ checkpoint.modelName || '模型未提供' }} · {{ checkpoint.sourceMessageCount ?? '-' }} 条消息</p><small>边界 {{ checkpoint.sourceBoundaryMessageId || '-' }} · 输入 {{ checkpoint.estimatedInputTokens ?? '-' }} / 输出 {{ checkpoint.estimatedOutputTokens ?? '-' }} · 版本 {{ checkpoint.revision ?? '-' }}</small></div></div></div></section>
      <section class="admin-detail-section"><div class="admin-detail-section__head"><div><h3><FileText aria-hidden="true" />草稿与正式写入</h3><p>后台只读展示状态，不会确认草稿或触发业务写入。</p></div><span class="admin-card-v2__meta">{{ drafts.length }} 个草稿</span></div><AdminPanelState v-if="!drafts.length" state="empty" title="当前运行没有草稿" message="服务端没有返回创建类工具的草稿记录。" /><div v-else class="admin-draft-list"><div v-for="draft in drafts" :key="draft.draftId"><div><strong>{{ draft.title || draft.draftType || '未命名草稿' }}</strong><span class="admin-status-v2" :class="`admin-status-v2--${/confirmed|written/i.test(draft.status) ? 'ok' : /failed|rejected/i.test(draft.status) ? 'bad' : 'warn'}`">{{ draft.status || '未知' }}</span></div><p><code>{{ draft.draftId }}</code> · {{ draft.draftType || '类型未提供' }}</p><small>{{ date(draft.updatedAt || draft.createdAt) }} · {{ draft.contentRedacted ? '内容已脱敏' : '仅显示授权摘要' }}{{ draft.confirmedBy ? ` · 确认人 ${draft.confirmedBy}` : '' }}{{ draft.businessReference ? ` · 业务引用 ${draft.businessReference}` : '' }}</small><small v-if="draft.failureReason" class="admin-text-danger">失败原因：{{ draft.failureReason }}</small></div></div></section>
      <section class="admin-detail-section"><div class="admin-detail-section__head"><div><h3><MessageSquare aria-hidden="true" />消息摘要</h3><p>正文默认隐藏；每次查看正文都会重新校验内容权限并记录访问审计。</p></div><span class="admin-card-v2__meta">{{ messages.length }} 条</span></div><AdminPanelState v-if="!messages.length" state="empty" title="没有可见消息" message="服务端没有返回当前会话消息，或消息已清理。" /><div v-else class="admin-message-list"><div v-for="message in messages" :key="message.messageId"><div><strong>{{ message.role || '未知角色' }}</strong><span>{{ message.messageType || '消息' }}</span><span class="admin-status-v2" :class="message.redactionState === 'FULL_ALLOWED' ? 'admin-status-v2--ok' : 'admin-status-v2--warn'">{{ message.redactionState }}</span></div><small><code>{{ message.messageId }}</code> · {{ date(message.occurredAt) }} · run {{ message.runId || '-' }}</small><p>{{ message.content || '正文未返回' }}</p></div></div></section>
    </template></div></aside>
    <button v-if="selected && adminSession.can('admin.agent.content.read') && !includeContent" class="admin-content-reveal" type="button" @click="revealContent">查看已授权内容</button>
  </AdminLayout>
</template>

<style scoped>
.admin-runs-scope { max-width: 1400px; margin: 0 auto 14px; }
.admin-run-filters { align-items: stretch; }
.admin-run-filters .admin-field input { min-width: 118px; }
.admin-filter-error { margin: -4px 20px 12px; color: #a1443e; font-size: 11px; }
.admin-run-drawer { width: min(660px, 100vw); }
.admin-run-summary { display: grid; gap: 18px; }
.admin-run-summary__status { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.admin-run-summary dl { display: grid; grid-template-columns: 112px 1fr; gap: 13px 16px; margin: 0; }
.admin-run-summary dt { color: #a0a0a0; font-size: 11px; }
.admin-run-summary dd { margin: 0; color: #424242; font-size: 12px; word-break: break-word; }
.admin-run-summary dd small { display: block; margin-top: 4px; color: #8d8d89; font-size: 10px; }
.admin-detail-section { margin-top: 24px; border-top: 1px solid #e8e8e5; padding-top: 19px; }
.admin-detail-section__head { display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; margin-bottom: 14px; }
.admin-detail-section__head h3 { display: flex; align-items: center; gap: 7px; margin: 0; font-size: 14px; }
.admin-detail-section__head h3 svg { width: 15px; color: #8f8f8b; }
.admin-detail-section__head p { margin-top: 5px; font-size: 11px; }
.admin-timeline-v2__item--plan .admin-timeline-v2__content::before { background: #8063a8; }
.admin-timeline-v2__item--tool .admin-timeline-v2__content::before { background: #477eac; }
.admin-event-title { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; }
.admin-event-title svg { width: 14px; color: #8f8f8b; }
.admin-event-title strong { font-size: 12px; }
.admin-event-kind { border-radius: 999px; background: #f3f3f1; padding: 3px 6px; color: #777; font-size: 9px; }
.admin-event-call-id { display: inline-block; margin-top: 5px; color: #777; font-size: 10px; }
.admin-event-summary { display: grid; gap: 4px; margin-top: 8px; color: #666; font-size: 10px; line-height: 1.5; }
.admin-event-redacted { display: inline-block; margin-top: 7px; color: #a0a0a0; font-size: 10px; }
.admin-context-metrics { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.admin-context-metrics > div { border: 1px solid #e8e8e5; border-radius: 6px; padding: 10px; }
.admin-context-metrics span { display: block; color: #8d8d89; font-size: 10px; }
.admin-context-metrics strong { display: block; margin-top: 6px; color: #1d1d1f; font-size: 15px; font-weight: 620; }
.admin-context-metrics small { display: block; margin-top: 4px; color: #a0a0a0; font-size: 10px; }
.admin-checkpoint-list, .admin-draft-list, .admin-message-list { display: grid; gap: 8px; margin-top: 12px; }
.admin-checkpoint-list > div, .admin-draft-list > div, .admin-message-list > div { border: 1px solid #e8e8e5; border-radius: 6px; padding: 11px 12px; }
.admin-checkpoint-list > div > div, .admin-draft-list > div > div, .admin-message-list > div > div { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.admin-checkpoint-list strong, .admin-draft-list strong, .admin-message-list strong { color: #1d1d1f; font-size: 11px; }
.admin-checkpoint-list span, .admin-draft-list p, .admin-message-list span, .admin-message-list small, .admin-checkpoint-list p, .admin-checkpoint-list small, .admin-draft-list small { color: #888; font-size: 10px; }
.admin-checkpoint-list p, .admin-draft-list p, .admin-message-list small { margin: 6px 0 0; }
.admin-checkpoint-list small, .admin-draft-list small { display: block; margin-top: 5px; line-height: 1.5; }
.admin-message-list p { margin: 8px 0 0; color: #999; font-size: 10px; line-height: 1.5; }
.admin-text-danger { color: #a1443e !important; }
.admin-content-reveal { position: fixed; top: 72px; right: 24px; z-index: 47; border: 1px solid #d9d9d5; border-radius: 7px; background: #fff; color: #1d1d1f; padding: 8px 12px; font-size: 12px; box-shadow: 0 8px 24px rgba(29,29,31,.1); cursor: pointer; }
.admin-usage-filters { align-items: stretch; }
.admin-datetime { display: flex; min-height: 34px; align-items: center; gap: 7px; border: 1px solid #e8e8e5; border-radius: 7px; background: #fff; padding: 0 10px; color: #737373; font-size: 11px; }
.admin-datetime svg { width: 15px; color: #a0a0a0; }
.admin-datetime input { min-width: 150px; border: 0; outline: 0; color: #454545; font-size: 11px; }
.admin-usage-meta { margin: 0 20px 14px !important; color: #8d8d89 !important; font-size: 10px !important; }
.sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; }
@media (max-width: 620px) { .admin-context-metrics { grid-template-columns: repeat(2, 1fr); }.admin-run-summary dl { grid-template-columns: 94px 1fr; }.admin-content-reveal { top: auto; right: 16px; bottom: 16px; }.admin-datetime { width: 100%; }.admin-datetime input { min-width: 0; flex: 1; } }
</style>
