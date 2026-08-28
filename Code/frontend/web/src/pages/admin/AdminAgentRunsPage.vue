<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ChevronLeft, ChevronRight, CircleAlert, RefreshCw, Search, ShieldAlert, X } from 'lucide-vue-next'
import AdminLayout from '@/features/admin/components/AdminLayout.vue'
import AdminPanelState from '@/features/admin/components/AdminPanelState.vue'
import AdminStatusBadge from '@/features/admin/components/AdminStatusBadge.vue'
import { useSession } from '@/app/stores/session'
import { fetchAdminContext, fetchAdminDrafts, fetchAdminEvents, fetchAdminMessages, fetchAdminRun, fetchAdminRuns, type AdminContextResponse, type AdminDraft, type AdminMessage } from '@/shared/api/admin'
import type { AdminEvent, AdminRunSummary } from '@/entities/admin/contracts'

const session = useSession()
const search = ref('')
const terminalStatus = ref('')
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

const statusLabel: Record<string, string> = { completed: '已完成', success: '已完成', running: '进行中', failed: '失败', cancelled: '已取消', exhausted: '已耗尽' }
const state = computed(() => loading.value ? 'loading' : error.value ? 'error' : items.value.length === 0 ? 'empty' : null)
const detailState = computed(() => detailLoading.value ? 'loading' : detailError.value ? 'error' : null)
const detailErrorMessage = computed(() => detailError.value instanceof Error ? detailError.value.message : '运行详情读取失败')
const listErrorMessage = computed(() => error.value instanceof Error ? error.value.message : 'Agent 运行列表读取失败')

async function load() {
  if (!session.token.value) { error.value = new Error('管理员会话已失效'); return }
  loading.value = true; error.value = null
  try {
    const result = await fetchAdminRuns(session.token.value, { runId: search.value.trim() || undefined, terminalStatus: terminalStatus.value || undefined, page: page.value, size })
    items.value = result.items; total.value = result.total; hasNext.value = result.hasNext
  } catch (cause) { error.value = cause }
  finally { loading.value = false }
}

async function openDetail(run: AdminRunSummary) {
  selected.value = run; detailLoading.value = true; detailError.value = null; events.value = []; messages.value = []; context.value = null; drafts.value = []
  if (!session.token.value) { detailError.value = new Error('管理员会话已失效'); detailLoading.value = false; return }
  try {
    const token = session.token.value
    const [full, eventPage, contextResult, draftResult, messageResult] = await Promise.all([
      fetchAdminRun(token, run.runId),
      fetchAdminEvents(token, run.runId, { includeContent: false }),
      fetchAdminContext(token, run.runId),
      fetchAdminDrafts(token, run.runId),
      run.conversationId ? fetchAdminMessages(token, run.conversationId, { includeContent: false, page: 0, size: 50 }) : Promise.resolve(null),
    ])
    selected.value = full; events.value = eventPage.items; context.value = contextResult; drafts.value = draftResult; messages.value = messageResult?.items ?? []
  } catch (cause) { detailError.value = cause }
  finally { detailLoading.value = false }
}

function closeDetail() { selected.value = null }
function previous() { if (page.value > 0) { page.value -= 1; void load() } }
function next() { if (hasNext.value) { page.value += 1; void load() } }
function date(value?: string | null) { return value ? new Date(value).toLocaleString('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }) : '-' }
function duration(value?: number | null) { return value == null ? '-' : value >= 1000 ? `${(value / 1000).toFixed(2)}s` : `${value}ms` }
function statusTone(value?: string) { return /completed|success/i.test(value ?? '') ? 'ok' : /failed|cancel/i.test(value ?? '') ? 'bad' : /running/i.test(value ?? '') ? 'warn' : '' }
function eventLabel(item: AdminEvent) { return item.toolName ? `${item.eventType} · ${item.toolName}` : item.eventType }

watch([search, terminalStatus], () => { page.value = 0; void load() })
onMounted(load)
</script>

<template>
  <AdminLayout active-id="agent">
    <section class="admin-page-v2">
      <header class="admin-page-v2__header"><div><div class="admin-page-v2__crumb">Admin / Agent / <strong>Runs</strong></div><h1>Agent 运行</h1><p>按运行 ID、终态和授权范围查看可追踪的 Agent 观测记录。</p></div><button class="admin-button-v2" type="button" :disabled="loading" @click="load"><RefreshCw :class="{ 'is-spinning': loading }" aria-hidden="true" />刷新</button></header>
      <div v-if="error" class="admin-error-v2" role="alert"><ShieldAlert aria-hidden="true" /><span>{{ listErrorMessage }}</span><button type="button" @click="load">重试</button></div>
      <div class="admin-grid"><article class="admin-card-v2 admin-span-12"><div class="admin-card-v2__header"><div><h2>运行记录</h2><p>仅显示服务端按管理员范围返回的摘要。</p></div><span class="admin-card-v2__meta">{{ total }} 条记录</span></div><div class="admin-card-v2__body admin-toolbar"><label class="admin-field"><Search aria-hidden="true" /><input v-model="search" type="search" placeholder="运行 ID" aria-label="按运行 ID 搜索" /></label><label class="admin-field"><select v-model="terminalStatus" aria-label="运行状态筛选"><option value="">全部状态</option><option value="COMPLETED">已完成</option><option value="RUNNING">进行中</option><option value="FAILED">失败</option><option value="CANCELLED">已取消</option></select></label></div><AdminPanelState v-if="state === 'loading'" state="loading" title="正在读取运行记录" /><AdminPanelState v-else-if="state === 'error'" state="error" :message="listErrorMessage" @retry="load" /><AdminPanelState v-else-if="state === 'empty'" state="empty" title="当前范围暂无运行记录" message="服务端没有返回可见的 Agent 运行，不会展示示例数据。" /><div v-else class="admin-table-wrap"><table class="admin-table-v2"><thead><tr><th>运行 ID</th><th>模型</th><th>开始时间</th><th>Token</th><th>工具</th><th>状态</th></tr></thead><tbody><tr v-for="run in items" :key="run.runId" tabindex="0" @click="openDetail(run)" @keydown.enter="openDetail(run)"><td><strong><code>{{ run.runId }}</code></strong><small>{{ run.storeId ? `门店 ${run.storeId}` : '门店范围未提供' }}</small></td><td>{{ run.modelId || '-' }}</td><td>{{ date(run.startedAt) }}</td><td>{{ run.totalTokens ?? '-' }}<small>{{ run.tokenSource }}</small></td><td>{{ run.toolCallCount ?? 0 }}</td><td><span class="admin-status-v2" :class="`admin-status-v2--${statusTone(run.terminalStatus)}`">{{ statusLabel[run.terminalStatus?.toLowerCase()] || run.terminalStatus || '未知' }}</span></td></tr></tbody></table></div><footer v-if="state !== 'loading' && !error && total > 0" class="admin-pagination"><span>第 {{ page + 1 }} 页</span><button type="button" :disabled="page === 0" aria-label="上一页" @click="previous"><ChevronLeft aria-hidden="true" /></button><button type="button" :disabled="!hasNext" aria-label="下一页" @click="next"><ChevronRight aria-hidden="true" /></button></footer></article></div>
    </section>
    <button v-if="selected" type="button" class="admin-detail-scrim" aria-label="关闭运行详情" @click="closeDetail" /><aside v-if="selected" class="admin-detail-drawer" aria-label="运行详情"><header class="admin-detail-drawer__head"><div><div class="admin-page-v2__crumb">RUN DETAIL</div><h2>运行详情</h2><p><code>{{ selected.runId }}</code></p></div><button class="admin-detail-drawer__close" type="button" aria-label="关闭运行详情" @click="closeDetail"><X aria-hidden="true" /></button></header><div class="admin-detail-drawer__body"><AdminPanelState v-if="detailState === 'loading'" state="loading" title="正在读取运行详情" /><AdminPanelState v-else-if="detailState === 'error'" state="error" :message="detailErrorMessage" @retry="openDetail(selected!)" /><template v-else><dl><dt>终态</dt><dd><AdminStatusBadge :status="statusTone(selected.terminalStatus) === 'ok' ? 'completed' : statusTone(selected.terminalStatus) === 'bad' ? 'failed' : 'running'" :label="statusLabel[selected.terminalStatus?.toLowerCase()] || selected.terminalStatus" /></dd><dt>Owner / 门店</dt><dd><code>{{ selected.ownerUserId }}</code> / <code>{{ selected.storeId || '-' }}</code></dd><dt>模型</dt><dd>{{ selected.modelId || '-' }}</dd><dt>耗时 / 首字</dt><dd>{{ duration(selected.durationMs) }} / {{ duration(selected.timeToFirstTokenMs) }}</dd><dt>Token</dt><dd>{{ selected.totalTokens ?? '-' }} <small>{{ selected.tokenSource }}{{ selected.contentRedacted ? ' · 内容已脱敏' : '' }}</small></dd></dl><section class="admin-card-v2 admin-card-v2--pad" style="margin-top:22px"><h2>事件时间线</h2><div v-if="events.length === 0" class="admin-empty-v2"><CircleAlert aria-hidden="true" /><div><strong>没有可见事件</strong><p>服务端未返回事件，或当前内容权限不包含事件摘要。</p></div></div><div v-else class="admin-timeline-v2"><div v-for="event in events" :key="`${event.eventId}-${event.sequence}`" class="admin-timeline-v2__item"><span class="admin-timeline-v2__time">#{{ event.sequence }}<br>{{ date(event.occurredAt) }}</span><div class="admin-timeline-v2__content"><strong>{{ eventLabel(event) }}</strong><p>{{ event.status }} · {{ duration(event.durationMs) }} · {{ event.redactionState }}</p></div></div></div></section><section v-if="context" class="admin-card-v2 admin-card-v2--pad" style="margin-top:14px"><h2>上下文窗口</h2><p>{{ context.contextWindowTokens ?? '-' }} tokens · {{ context.checkpoints.length }} 个检查点{{ context.contentRedacted ? ' · 内容已脱敏' : '' }}</p></section><section v-if="drafts.length" class="admin-card-v2 admin-card-v2--pad" style="margin-top:14px"><h2>草稿</h2><p v-for="draft in drafts" :key="draft.draftId">{{ draft.title }} · {{ draft.status }}{{ draft.contentRedacted ? ' · 内容已脱敏' : '' }}</p></section><section v-if="messages.length" class="admin-card-v2 admin-card-v2--pad" style="margin-top:14px"><h2>消息摘要</h2><p v-for="message in messages" :key="message.messageId">{{ message.role }} · {{ message.messageType }} · {{ message.redactionState }}</p></section></template></div></aside>
  </AdminLayout>
</template>
