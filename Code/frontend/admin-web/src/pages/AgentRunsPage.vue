<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Activity, Bot, ChevronRight, CircleAlert, Clock3, FileText, Filter, MessageSquareText, RefreshCw, Search, X } from 'lucide-vue-next'
import { adminSession } from '@/app/stores/admin-session'
import { getAdminMessages, getAdminRun, getAdminRunContext, getAdminRunDrafts, getAdminRunEvents, getAdminRuns, getAdminUsage, streamAdminRunEvents, type AdminContext, type AdminDraft, type AdminMessage, type AdminRun, type AdminRunEvent, type AdminUsage } from '@/shared/api/admin'
import { formatDateTime, formatDuration, formatNumber } from '@/shared/utils/format'
import AdminPageHeader from '@/shared/components/AdminPageHeader.vue'
import StatePanel from '@/shared/components/StatePanel.vue'
import UsageChart from '@/features/agent-observability/UsageChart.vue'

const runId = ref('')
const modelId = ref('')
const terminalStatus = ref('')
const rangeDays = ref(30)
const page = ref(0)
const pageSize = 20
const runs = ref<AdminRun[]>([])
const total = ref(0)
const usage = ref<AdminUsage[]>([])
const loading = ref(true)
const error = ref('')
const selectedRun = ref<AdminRun | null>(null)
const events = ref<AdminRunEvent[]>([])
const eventIntegrity = ref(true)
const detailLoading = ref(false)
const detailError = ref('')
const runDetailError = ref('')
const contextError = ref('')
const draftsError = ref('')
const messagesError = ref('')
const messages = ref<AdminMessage[]>([])
const context = ref<AdminContext | null>(null)
const drafts = ref<AdminDraft[]>([])
const streaming = ref(false)
const streamError = ref('')
let closeStream: (() => void) | null = null
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let streamGeneration = 0
let detailGeneration = 0
const maxReconnectAttempts = 4
let reconnectAttempts = 0

const queryRange = computed(() => { const to = new Date(); const from = new Date(to.getTime() - rangeDays.value * 86_400_000); return { from, to } })
const hasNext = computed(() => (page.value + 1) * pageSize < total.value)
const eventItems = computed(() => [...events.value].sort((a, b) => a.sequence - b.sequence))
const normalizedEvents = computed(() => eventItems.value.map((event) => ({ ...event, normalizedType: normalizeToken(event.event_type) })))
const plannedTools = computed(() => normalizedEvents.value.filter((event) => event.normalizedType === 'plan' || event.normalizedType === 'plan_delta'))
const toolEvents = computed(() => normalizedEvents.value.filter((event) => event.normalizedType.startsWith('tool_')))
const toolPairs = computed(() => {
  const pairs = new Map<string, { callId: string; started: AdminRunEvent | null; finished: AdminRunEvent | null }>()
  for (const event of toolEvents.value) {
    const callId = event.call_id || `sequence-${event.sequence}`
    const pair = pairs.get(callId) ?? { callId, started: null, finished: null }
    if (event.normalizedType === 'tool_started') pair.started = event
    if (['tool_completed', 'tool_failed', 'tool_skipped'].includes(event.normalizedType)) pair.finished = event
    pairs.set(callId, pair)
  }
  return [...pairs.values()]
})
const formalAnswers = computed(() => messages.value.filter((message) => message.role.toLowerCase() === 'assistant' && ['answer', 'final', 'formal_answer', 'message'].includes(normalizeToken(message.message_type || 'message'))))

function normalizeToken(value: string | null | undefined): string { return (value || '').trim().toLowerCase().replace(/[.-]/g, '_') }
function display(value: string | number | null | undefined): string { return value === null || value === undefined || value === '' ? '未提供' : String(value) }
function requestError(reason: unknown, fallback: string): string { return reason instanceof Error && reason.message ? reason.message : fallback }
function isCurrentDetail(generation: number, selectedRunId: string): boolean { return generation === detailGeneration && selectedRun.value?.run_id === selectedRunId }

async function load(): Promise<void> {
  loading.value = true; error.value = ''
  const common = { from: queryRange.value.from, to: queryRange.value.to, modelId: modelId.value.trim() || undefined }
  const [runResult, usageResult] = await Promise.allSettled([
    getAdminRuns({ ...common, runId: runId.value.trim() || undefined, terminalStatus: terminalStatus.value || undefined, page: page.value, size: pageSize }),
    getAdminUsage({ ...common, granularity: 'day', page: 0, size: 90 }),
  ])
  if (runResult.status === 'fulfilled') { runs.value = runResult.value.items; total.value = runResult.value.total }
  else { runs.value = []; total.value = 0; error.value = runResult.reason instanceof Error ? runResult.reason.message : '无法读取运行记录。' }
  if (usageResult.status === 'fulfilled') usage.value = usageResult.value.items
  else usage.value = []
  loading.value = false
}

async function openRun(run: AdminRun): Promise<void> {
  closeEventStream()
  const generation = ++detailGeneration
  selectedRun.value = run; detailLoading.value = true; detailError.value = ''; streamError.value = ''; events.value = []; messages.value = []; context.value = null; drafts.value = []
  runDetailError.value = ''; contextError.value = ''; draftsError.value = ''; messagesError.value = ''; eventIntegrity.value = true

  try {
    const detailedRun = await getAdminRun(run.run_id)
    if (!isCurrentDetail(generation, run.run_id)) return
    if (detailedRun.run_id === run.run_id) selectedRun.value = detailedRun
    else runDetailError.value = '详情响应与当前选择的运行不一致，已保留列表摘要。'
  } catch (reason) {
    if (!isCurrentDetail(generation, run.run_id)) return
    runDetailError.value = requestError(reason, '无法读取运行详情，已保留列表摘要。')
  }

  if (!isCurrentDetail(generation, run.run_id)) return
  const detailRun = selectedRun.value ?? run
  const [eventResult, contextResult, draftResult, messageResult] = await Promise.allSettled([
    getAdminRunEvents(run.run_id),
    getAdminRunContext(run.run_id),
    getAdminRunDrafts(run.run_id),
    detailRun.conversation_id ? getAdminMessages(detailRun.conversation_id, { includeContent: false, page: 0, size: 30 }) : Promise.resolve(null),
  ])
  if (!isCurrentDetail(generation, run.run_id)) return
  if (eventResult.status === 'fulfilled') { events.value = eventResult.value.items; eventIntegrity.value = eventResult.value.event_integrity }
  else detailError.value = requestError(eventResult.reason, '无法读取运行事件。')
  if (contextResult.status === 'fulfilled') context.value = contextResult.value
  else contextError.value = requestError(contextResult.reason, '无法读取上下文检查点。')
  if (draftResult.status === 'fulfilled') drafts.value = draftResult.value
  else draftsError.value = requestError(draftResult.reason, '无法读取关联草稿。')
  if (messageResult.status === 'fulfilled' && messageResult.value) messages.value = messageResult.value.items
  else if (messageResult.status === 'rejected') messagesError.value = requestError(messageResult.reason, '无法读取会话消息。')
  detailLoading.value = false
  if (normalizeToken(detailRun.terminal_status) === 'running') { reconnectAttempts = 0; startEventStream() }
}
function applyFilters(): void { page.value = 0; void load() }
function retrySelectedRun(): void { if (selectedRun.value) void openRun(selectedRun.value) }
function clearRun(): void { closeEventStream(); detailGeneration += 1; selectedRun.value = null; detailLoading.value = false; detailError.value = ''; runDetailError.value = ''; contextError.value = ''; draftsError.value = ''; messagesError.value = ''; events.value = []; messages.value = []; context.value = null; drafts.value = [] }
function statusLabel(value: string | null): string { const labels: Record<string, string> = { completed: '已完成', confirmation_pending: '待确认', running: '进行中', failed: '失败', blocked: '已阻塞', cancelled: '已取消', exhausted: '已耗尽' }; const normalized = normalizeToken(value); return normalized ? (labels[normalized] ?? value ?? '未知') : '未知' }
function statusClass(value: string | null): string { const normalized = normalizeToken(value); if (normalized === 'completed') return 'success'; if (['failed', 'blocked', 'cancelled', 'exhausted'].includes(normalized)) return 'danger'; if (['running', 'confirmation_pending'].includes(normalized)) return 'attention'; return 'muted' }
function eventLabel(event: AdminRunEvent): string { return event.tool_name ? `${event.event_type} · ${event.tool_name}` : event.event_type }
function eventIsTerminal(event: AdminRunEvent): boolean { return ['run_completed', 'run_failed', 'run_cancelled', 'run_blocked', 'run_exhausted'].includes(normalizeToken(event.event_type)) }
function startEventStream(): void {
  if (!selectedRun.value || streaming.value) return
  if (reconnectTimer) { clearTimeout(reconnectTimer); reconnectTimer = null }
  streamError.value = ''; streaming.value = true
  const selectedRunId = selectedRun.value.run_id
  const afterSequence = eventItems.value.at(-1)?.sequence ?? null
  const generation = streamGeneration
  const detailGenerationAtStart = detailGeneration
  const stop = streamAdminRunEvents(selectedRunId, afterSequence, {
    onEvent(event) {
      if (!isCurrentDetail(detailGenerationAtStart, selectedRunId) || generation !== streamGeneration || event.run_id !== selectedRunId) return
      if (!event.event_id || events.value.some((item) => item.event_id === event.event_id || item.sequence === event.sequence)) return
      events.value = [...events.value, event]
      if (eventIsTerminal(event)) closeEventStream()
    },
    onError(reason) {
      if (!isCurrentDetail(detailGenerationAtStart, selectedRunId) || generation !== streamGeneration) return
      closeStream = null; streaming.value = false; streamError.value = reason.message
      scheduleReconnect(selectedRunId, generation, detailGenerationAtStart)
    },
    onComplete() {
      if (!isCurrentDetail(detailGenerationAtStart, selectedRunId) || generation !== streamGeneration) return
      closeStream = null; streaming.value = false
      if (!eventItems.value.some(eventIsTerminal)) scheduleReconnect(selectedRunId, generation, detailGenerationAtStart)
    },
  })
  closeStream = () => { stop(); if (closeStream) closeStream = null }
}
function scheduleReconnect(runId: string, generation = streamGeneration, detailGenerationAtSchedule = detailGeneration): void {
  if (reconnectAttempts >= maxReconnectAttempts || !isCurrentDetail(detailGenerationAtSchedule, runId) || generation !== streamGeneration) return
  const delay = 500 * (2 ** reconnectAttempts); reconnectAttempts += 1
  reconnectTimer = setTimeout(() => { reconnectTimer = null; if (isCurrentDetail(detailGenerationAtSchedule, runId) && generation === streamGeneration) startEventStream() }, delay)
}
function closeEventStream(): void { streamGeneration += 1; if (reconnectTimer) clearTimeout(reconnectTimer); reconnectTimer = null; closeStream?.(); closeStream = null; streaming.value = false }
async function reloadMessages(): Promise<void> {
  const run = selectedRun.value
  if (!run?.conversation_id) return
  const generation = detailGeneration
  const selectedRunId = run.run_id
  try {
    const result = await getAdminMessages(run.conversation_id, { includeContent: adminSession.session?.content_access ?? false, page: 0, size: 30 })
    if (!isCurrentDetail(generation, selectedRunId)) return
    messages.value = result.items
    messagesError.value = ''
  } catch (reason) {
    if (isCurrentDetail(generation, selectedRunId)) messagesError.value = requestError(reason, '无法读取会话消息。')
  }
}
watch(rangeDays, () => { page.value = 0; void load() })
onMounted(load)
onBeforeUnmount(() => { detailGeneration += 1; closeEventStream() })
</script>

<template>
  <section>
    <AdminPageHeader eyebrow="AGENT / OBSERVABILITY" title="Agent 运行" description="按运行、模型、状态和时间范围核验 Agent 执行事实。"><template #actions><label class="range"><Clock3 aria-hidden="true" /><select v-model.number="rangeDays" aria-label="时间范围"><option :value="7">近 7 天</option><option :value="30">近 30 天</option><option :value="90">近 90 天</option></select></label><button class="outline-button" type="button" :disabled="loading" @click="load"><RefreshCw :class="{ spinning: loading }" aria-hidden="true" />刷新</button></template></AdminPageHeader>
    <div class="agent-grid"><section class="runs-area"><div class="filter-panel"><label><Search aria-hidden="true" /><input v-model="runId" placeholder="运行 ID" @keyup.enter="applyFilters" /></label><label><Bot aria-hidden="true" /><input v-model="modelId" placeholder="模型 ID" @keyup.enter="applyFilters" /></label><label><Filter aria-hidden="true" /><select v-model="terminalStatus"><option value="">全部终态</option><option value="completed">已完成</option><option value="running">进行中</option><option value="failed">失败</option><option value="confirmation_pending">待确认</option></select></label><button class="outline-button" type="button" @click="applyFilters">应用筛选</button></div>
      <UsageChart :items="usage" :model-id="modelId" :loading="loading" />
      <section class="table-panel" aria-labelledby="runs-title"><header><div><h2 id="runs-title">运行记录</h2><p>{{ formatNumber(total) }} 条记录</p></div></header><StatePanel v-if="error" state="error" :detail="error" @retry="load" /><div v-else class="table-scroll"><table><thead><tr><th>运行</th><th>模型与范围</th><th class="numeric">Token / 耗时</th><th>状态</th><th aria-label="详情" /></tr></thead><tbody><tr v-for="run in runs" :key="run.run_id" :class="{ selected: selectedRun?.run_id === run.run_id }" tabindex="0" @click="openRun(run)" @keydown.enter="openRun(run)" @keydown.space.prevent="openRun(run)"><td><strong class="mono">{{ run.run_id }}</strong><small>{{ formatDateTime(run.started_at) }}</small></td><td><strong>{{ run.model_id || '未记录模型' }}</strong><small>Owner {{ run.owner_user_id || '—' }} / 门店 {{ run.store_id || '—' }}</small></td><td class="numeric"><strong>{{ formatNumber(run.total_tokens) }}</strong><small>{{ formatDuration(run.duration_ms) }}</small></td><td><span class="status" :class="`status--${statusClass(run.terminal_status)}`"><i />{{ statusLabel(run.terminal_status) }}</span></td><td><ChevronRight aria-hidden="true" /></td></tr><tr v-if="!loading && !runs.length"><td colspan="5" class="empty">暂无匹配运行记录。</td></tr></tbody></table></div><footer><span>第 {{ page + 1 }} 页</span><div><button type="button" :disabled="page <= 0" @click="page--; load()">上一页</button><button type="button" :disabled="!hasNext" @click="page++; load()">下一页</button></div></footer></section></section>
      <aside v-if="selectedRun" class="run-drawer" aria-labelledby="drawer-title">
        <header><div><p>RUN DETAIL</p><h2 id="drawer-title">运行详情</h2></div><button type="button" aria-label="关闭运行详情" @click="clearRun"><X /></button></header>
        <span class="run-id">run_id · {{ selectedRun.run_id }}</span>
        <p v-if="runDetailError" class="detail-request-error" role="alert">{{ runDetailError }} <button type="button" @click="retrySelectedRun">重试详情</button></p>
        <dl><div><dt>conversation_id</dt><dd class="mono">{{ display(selectedRun.conversation_id) }}</dd></div><div><dt>用户 / Owner / 门店</dt><dd>用户 {{ display(selectedRun.actor_user_id) }} / Owner {{ display(selectedRun.owner_user_id) }} / 门店 {{ display(selectedRun.store_id) }}</dd></div><div><dt>模型</dt><dd>{{ display(selectedRun.model_id) }}</dd></div><div><dt>终态</dt><dd><span class="status" :class="`status--${statusClass(selectedRun.terminal_status)}`"><i />{{ statusLabel(selectedRun.terminal_status) }}</span></dd></div><div><dt>Token</dt><dd>输入 {{ formatNumber(selectedRun.input_tokens) }} · 输出 {{ formatNumber(selectedRun.output_tokens) }} · 总量 {{ formatNumber(selectedRun.total_tokens) }} <small>来源 {{ display(selectedRun.token_source) }}</small></dd></div><div><dt>时间</dt><dd>首字延迟 {{ formatDuration(selectedRun.time_to_first_token_ms) }} · 总耗时 {{ formatDuration(selectedRun.duration_ms) }}</dd></div><div><dt>轮次 / 工具调用</dt><dd>{{ display(selectedRun.iteration_count) }} / {{ display(selectedRun.tool_call_count) }}</dd></div></dl>
        <section class="event-section" aria-labelledby="event-title">
          <header><div><h3 id="event-title">工具事件</h3><span v-if="!eventIntegrity" class="integrity"><CircleAlert aria-hidden="true" />事件序列不完整</span><span v-else-if="streaming" class="integrity integrity--live"><Activity aria-hidden="true" />实时更新中</span></div><button type="button" class="refresh-events" :disabled="detailLoading" @click="openRun(selectedRun)"><RefreshCw aria-hidden="true" />刷新</button></header>
          <p v-if="streamError" class="stream-error">{{ streamError }}<template v-if="reconnectAttempts < maxReconnectAttempts"> · 将自动重连（{{ reconnectAttempts }}/{{ maxReconnectAttempts }}）</template><template v-else> · 自动重连已停止</template></p>
          <StatePanel v-if="detailError" state="error" :detail="detailError" @retry="retrySelectedRun" />
          <div v-else-if="detailLoading" class="event-loading">正在读取持久化事件。</div>
          <div v-else>
            <div class="event-summary"><span>计划 {{ plannedTools.length }} 条</span><span>实际工具 {{ toolEvents.length }} 条</span><span>call_id 配对 {{ toolPairs.filter((pair) => pair.started && pair.finished).length }}/{{ toolPairs.length }}</span></div>
            <ol class="event-list"><li v-for="event in normalizedEvents" :key="event.event_id"><span class="sequence">{{ event.sequence }}</span><div><strong>{{ eventLabel(event) }}</strong><small>{{ formatDateTime(event.occurred_at) }} · {{ display(event.status) }} · 脱敏 {{ display(event.redaction_state) }}<template v-if="event.call_id"> · call_id {{ event.call_id }}</template><template v-if="event.duration_ms !== null"> · 工具耗时 {{ formatDuration(event.duration_ms) }}</template></small><p v-if="event.argument_summary">参数：{{ event.argument_summary }}</p><p v-if="event.result_summary">结果：{{ event.result_summary }}</p></div></li><li v-if="!eventItems.length" class="empty-event">暂无持久化工具事件。</li></ol>
          </div>
        </section>
        <section class="evidence-section"><header><h3><FileText aria-hidden="true" />上下文与草稿</h3></header><p v-if="contextError" class="detail-request-error" role="alert">{{ contextError }} <button type="button" @click="retrySelectedRun">重试上下文</button></p><p v-else-if="context">窗口 {{ formatNumber(context.context_window_tokens) }}（{{ display(context.context_window_source) }}）· 估算输入 {{ formatNumber(context.estimated_input_tokens) }} · 输出 {{ formatNumber(context.estimated_output_tokens) }} · 检查点 {{ context.checkpoints.length }}</p><p v-else-if="detailLoading" class="event-loading">正在读取上下文检查点。</p><p v-else>暂无上下文检查点。</p><ul v-if="!contextError && context?.checkpoints.length"><li v-for="checkpoint in context.checkpoints" :key="checkpoint.checkpoint_id"><strong>检查点 {{ checkpoint.checkpoint_id }}</strong><small>状态 {{ display(checkpoint.status) }} · 质量 {{ display(checkpoint.quality) }} · revision {{ display(checkpoint.revision) }} · {{ formatDateTime(checkpoint.updated_at || checkpoint.created_at) }}</small><p>消息 {{ display(checkpoint.source_message_count) }} · 输入 {{ formatNumber(checkpoint.estimated_input_tokens) }} · 输出 {{ formatNumber(checkpoint.estimated_output_tokens) }}</p></li></ul><p v-if="draftsError" class="detail-request-error" role="alert">{{ draftsError }} <button type="button" @click="retrySelectedRun">重试草稿</button></p><ul v-else-if="drafts.length"><li v-for="draft in drafts" :key="draft.draft_id"><strong>{{ draft.title || draft.draft_type }}</strong><small>状态 {{ display(draft.status) }} · {{ formatDateTime(draft.updated_at) }}</small></li></ul><p v-else-if="!detailLoading">暂无关联草稿。</p></section>
        <section class="evidence-section"><header><h3><MessageSquareText aria-hidden="true" />正式回答与会话</h3><button v-if="selectedRun.conversation_id" type="button" @click="reloadMessages">{{ adminSession.session?.content_access ? '读取已授权正文' : '刷新摘要' }}</button></header><p v-if="messagesError" class="detail-request-error" role="alert">{{ messagesError }} <button type="button" @click="reloadMessages">重试消息</button></p><p v-if="formalAnswers.length" class="formal-answer">{{ formalAnswers.map((message) => message.content).filter(Boolean).join('\n') }}</p><p v-else>暂无正式回答正文（当前权限或后端未提供）。</p><ul v-if="messages.length"><li v-for="message in messages" :key="message.message_id"><strong>{{ message.role }} · {{ message.message_type || 'message' }}</strong><small>{{ formatDateTime(message.occurred_at) }} · {{ message.redaction_state || 'REDACTED' }}</small><p v-if="message.content">{{ message.content }}</p></li></ul><p v-else-if="!messagesError">暂无会话消息摘要。</p></section>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.range, .outline-button { display: inline-flex; height: 32px; align-items: center; gap: 7px; border: 1px solid var(--admin-border); border-radius: 999px; background: #fff; padding: 0 10px; color: var(--admin-foreground); font-size: 12px; }.range :deep(svg), .outline-button :deep(svg) { width: 14px; height: 14px; color: var(--admin-muted); stroke-width: 1.7; }.range select { border: 0; background: transparent; color: var(--admin-foreground); outline: 0; }.outline-button { cursor: pointer; }.outline-button:hover:not(:disabled) { background: var(--admin-secondary); }.outline-button:disabled { cursor: wait; opacity: .65; }.spinning { animation: spin .8s linear infinite; }.agent-grid { display: grid; grid-template-columns: minmax(0, 1fr) 330px; gap: 8px; margin-top: 22px; }.runs-area { min-width: 0; }.filter-panel { display: grid; grid-template-columns: 1.2fr 1fr 130px auto; gap: 8px; margin-bottom: 8px; }.filter-panel label { display: flex; min-width: 0; height: 32px; align-items: center; gap: 7px; border: 1px solid var(--admin-border); border-radius: 6px; background: #fff; padding: 0 9px; color: var(--admin-muted); }.filter-panel :deep(svg) { width: 14px; height: 14px; flex: 0 0 14px; }.filter-panel input, .filter-panel select { min-width: 0; width: 100%; border: 0; background: transparent; color: var(--admin-foreground); outline: 0; font-size: 11px; }.table-panel { margin-top: 8px; border: 1px solid var(--admin-border); border-radius: 8px; background: #fff; padding: 18px; }.table-panel header { display: flex; justify-content: space-between; }.table-panel h2 { margin: 0; font-size: 14px; font-weight: 500; }.table-panel header p { margin: 5px 0 0; color: var(--admin-muted); font-size: 11px; }.table-scroll { overflow-x: auto; margin-top: 15px; }table { width: 100%; min-width: 720px; border-collapse: collapse; }th { height: 34px; border-bottom: 1px solid var(--admin-border); color: var(--admin-muted); font-size: 11px; font-weight: 400; text-align: left; }th:nth-child(1) { width: 29%; }th:nth-child(2) { width: 30%; }th:nth-child(3) { width: 19%; }th:nth-child(4) { width: 16%; }td { height: 60px; border-bottom: 1px solid var(--admin-border); padding-right: 8px; font-size: 11px; }tbody tr { cursor: pointer; }tbody tr:hover, tbody tr.selected { background: #fafaf8; }td strong, td small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }td strong { font-weight: 500; }td small { margin-top: 5px; color: var(--admin-muted); font-size: 10px; }.mono, .run-id { color: #65659d; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 10px; }.numeric { padding-right: 10px; text-align: right; font-variant-numeric: tabular-nums; }.status { display: inline-flex; align-items: center; gap: 5px; border-radius: 999px; padding: 4px 7px; font-size: 10px; }.status i { width: 5px; height: 5px; border-radius: 50%; background: currentColor; }.status--success { background: var(--admin-positive-bg); color: var(--admin-positive); }.status--attention { background: var(--admin-attention-bg); color: var(--admin-attention); }.status--danger { background: var(--admin-danger-bg); color: var(--admin-danger); }.status--muted { background: var(--admin-secondary); color: var(--admin-muted); }td > :deep(svg) { width: 15px; height: 15px; color: var(--admin-muted); }.empty { height: 120px; color: var(--admin-muted); text-align: center; }.table-panel footer { display: flex; align-items: center; justify-content: space-between; padding-top: 12px; color: var(--admin-muted); font-size: 10px; }.table-panel footer div { display: flex; gap: 6px; }.table-panel footer button { height: 28px; border: 1px solid var(--admin-border); border-radius: 5px; background: #fff; padding: 0 8px; cursor: pointer; font-size: 10px; }.table-panel footer button:disabled { cursor: not-allowed; opacity: .5; }.run-drawer { position: sticky; top: 24px; align-self: start; max-height: calc(100vh - 48px); overflow-y: auto; border: 1px solid var(--admin-border); border-radius: 8px; background: #fff; padding: 20px; }.run-drawer > header, .event-section header { display: flex; align-items: flex-start; justify-content: space-between; gap: 10px; }.run-drawer header p { margin: 0; color: var(--admin-muted); font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 9px; letter-spacing: .08em; }.run-drawer h2 { margin: 8px 0 0; font-size: 16px; font-weight: 500; }.run-drawer header > button { display: grid; width: 28px; height: 28px; place-items: center; border: 0; border-radius: 5px; background: transparent; color: var(--admin-muted); cursor: pointer; }.run-drawer header > button:hover { background: var(--admin-secondary); }.run-drawer header > button :deep(svg) { width: 15px; height: 15px; }.run-id { display: block; margin: 18px 0; overflow-wrap: anywhere; }.run-drawer dl { margin: 0; }.run-drawer dl div { border-bottom: 1px solid var(--admin-border); padding: 11px 0; }.run-drawer dt { color: var(--admin-muted); font-size: 10px; }.run-drawer dd { margin: 6px 0 0; font-size: 11px; overflow-wrap: anywhere; }.run-drawer dd small { margin-left: 4px; color: var(--admin-muted); font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 9px; }.event-section { margin-top: 20px; }.event-section h3 { margin: 0; font-size: 12px; font-weight: 500; }.integrity { display: flex; align-items: center; gap: 4px; margin-top: 5px; color: var(--admin-attention); font-size: 9px; }.integrity :deep(svg) { width: 12px; height: 12px; }.refresh-events { display: inline-flex; align-items: center; gap: 5px; border: 0; background: transparent; color: var(--admin-muted); cursor: pointer; font-size: 10px; }.refresh-events :deep(svg) { width: 13px; height: 13px; }.event-loading { padding: 30px 0; color: var(--admin-muted); font-size: 11px; text-align: center; }.event-list { display: grid; gap: 0; margin: 14px 0 0; padding: 0; list-style: none; }.event-list li { display: grid; grid-template-columns: 24px minmax(0, 1fr); gap: 9px; border-left: 1px solid var(--admin-border); padding: 0 0 16px 11px; }.event-list li:last-child { padding-bottom: 0; }.sequence { display: grid; width: 20px; height: 20px; place-items: center; margin-left: -22px; border: 1px solid var(--admin-border); border-radius: 50%; background: #fff; color: var(--admin-muted); font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 9px; }.event-list strong, .event-list small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.event-list strong { font-size: 10px; font-weight: 500; }.event-list small { margin-top: 4px; color: var(--admin-muted); font-size: 9px; }.event-list p { margin: 7px 0 0; color: var(--admin-muted); font-size: 9px; line-height: 1.45; overflow-wrap: anywhere; }.event-list .empty-event { display: block; border: 0; padding: 24px 0; color: var(--admin-muted); text-align: center; font-size: 10px; }@keyframes spin { to { transform: rotate(360deg); } }@media (max-width: 1100px) { .agent-grid { grid-template-columns: 1fr; }.run-drawer { position: static; max-height: none; }}@media (max-width: 720px) { .filter-panel { grid-template-columns: 1fr 1fr; }.filter-panel > :last-child { grid-column: span 2; }}@media (max-width: 480px) { .filter-panel { grid-template-columns: 1fr; }.filter-panel > :last-child { grid-column: auto; }}
.integrity--live { color: var(--admin-positive); }
.stream-error { margin: 9px 0 0; color: var(--admin-attention); font-size: 9px; line-height: 1.45; }
.evidence-section { margin-top: 20px; border-top: 1px solid var(--admin-border); padding-top: 16px; }
.evidence-section header { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.evidence-section h3 { display: flex; align-items: center; gap: 6px; margin: 0; font-size: 12px; font-weight: 500; }
.evidence-section h3 :deep(svg) { width: 14px; height: 14px; color: var(--admin-muted); }
.evidence-section header button { border: 0; background: transparent; color: var(--admin-muted); cursor: pointer; font-size: 9px; }
.evidence-section > p { margin: 10px 0 0; color: var(--admin-muted); font-size: 10px; line-height: 1.45; }
.evidence-section ul { display: grid; margin: 11px 0 0; padding: 0; list-style: none; }
.evidence-section li { border-bottom: 1px solid var(--admin-border); padding: 8px 0; }
.evidence-section li strong, .evidence-section li small { display: block; }
.evidence-section li strong { font-size: 10px; font-weight: 500; }
.evidence-section li small { margin-top: 4px; color: var(--admin-muted); font-size: 9px; }
.evidence-section li p { margin: 7px 0 0; color: var(--admin-muted); font-size: 10px; line-height: 1.45; overflow-wrap: anywhere; }
.event-summary { display: flex; flex-wrap: wrap; gap: 6px; margin: 12px 0 0; color: var(--admin-muted); font-size: 9px; }
.event-summary span { border: 1px solid var(--admin-border); border-radius: 999px; padding: 4px 6px; }
.formal-answer { white-space: pre-wrap; color: var(--admin-foreground) !important; }
tbody tr:focus-visible { outline: 2px solid var(--admin-focus); outline-offset: -2px; }
.detail-request-error { margin: 9px 0 0; color: var(--admin-danger); font-size: 9px; line-height: 1.45; }
.detail-request-error button { margin-left: 4px; border: 0; background: transparent; color: inherit; cursor: pointer; font-size: inherit; text-decoration: underline; }
</style>
