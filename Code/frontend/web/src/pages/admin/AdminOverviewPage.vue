<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import {
  Activity,
  ArrowUpRight,
  BarChart3,
  Bot,
  CircleAlert,
  CircleCheck,
  Database,
  Eye,
  ShieldAlert,
  Store,
  UsersRound,
  Wrench,
} from 'lucide-vue-next'
import AdminLayout from '@/features/admin/components/AdminLayout.vue'
import AdminPageHeader from '@/features/admin/components/AdminPageHeader.vue'
import AdminPanelState from '@/features/admin/components/AdminPanelState.vue'
import AdminScopeFilter from '@/features/admin/components/AdminScopeFilter.vue'
import AdminStatusBadge from '@/features/admin/components/AdminStatusBadge.vue'
import { useSession } from '@/app/stores/session'
import { useAdminSession } from '@/app/stores/admin-session'
import { useRouter } from 'vue-router'
import {
  fetchAdminAuditEvents,
  fetchAdminEvents,
  fetchAdminOverview,
  fetchAdminRuns,
  fetchAdminUsage,
  type AdminAuditEvent,
  type AdminOverviewPayload,
} from '@/shared/api/admin'
import type { AdminEvent, AdminRunSummary } from '@/entities/admin/contracts'

const session = useSession()
const adminSession = useAdminSession()
const router = useRouter()
const range = ref('近 30 天')
const overview = ref<AdminOverviewPayload | null>(null)
const overviewLoading = ref(false)
const overviewError = ref<unknown>(null)
const recentRuns = ref<AdminRunSummary[]>([])
const recentRunsLoading = ref(false)
const recentRunsError = ref<unknown>(null)
const recentUsage = ref<{ totalTokens: number; durationMs: number; estimated: number; total: number } | null>(null)
const usageLoading = ref(false)
const usageError = ref<unknown>(null)
const recentAudit = ref<AdminAuditEvent[]>([])
const auditLoading = ref(false)
const auditError = ref<unknown>(null)
const liveEvents = ref<AdminEvent[]>([])
const liveEventsLoading = ref(false)
const liveEventsError = ref<unknown>(null)
const liveEventsIntegrity = ref(true)
const ownerUserId = ref('')
const storeId = ref('')
const requestSerial = ref(0)
const rangeDays: Record<string, number> = { 今天: 1, '近 7 天': 7, '近 30 天': 30, '近 90 天': 90 }

const metricConfig: Record<string, { label: string; icon: typeof UsersRound; tone: string }> = {
  users: { label: '用户总数', icon: UsersRound, tone: 'neutral' },
  stores: { label: '门店总数', icon: Store, tone: 'blue' },
  agent_runs: { label: 'Agent 运行', icon: Bot, tone: 'purple' },
  agent_tool_calls: { label: '工具调用', icon: Activity, tone: 'green' },
  total_tokens: { label: 'Token 总量', icon: Database, tone: 'amber' },
  totalTokens: { label: 'Token 总量', icon: Database, tone: 'amber' },
}

const metrics = computed(() => (overview.value?.metrics ?? []).map((item) => ({
  ...item,
  ...(metricConfig[item.key] ?? { label: item.key, icon: BarChart3, tone: 'neutral' }),
})))
const trendMax = computed(() => Math.max(...(overview.value?.trend.map((point) => point.value) ?? [1]), 1))
const chartPoints = computed(() => (overview.value?.trend ?? []).map((point) => ({
  ...point,
  height: point.value <= 0 ? 3 : Math.max(7, Math.round(point.value / trendMax.value * 100)),
  label: formatTrendLabel(point.at),
})))
const totalLoading = computed(() => overviewLoading.value || recentRunsLoading.value || usageLoading.value || auditLoading.value || liveEventsLoading.value)
const scopeLabel = computed(() => {
  if (!overview.value?.scope) return '范围未返回'
  const scope = overview.value.scope
  if (scope.allOwners) return scope.storeIds.length ? `全部 Owner · ${scope.storeIds.length} 个门店` : '全部 Owner · 门店未限定'
  return `${scope.ownerUserIds.length} 个 Owner · ${scope.storeIds.length ? `${scope.storeIds.length} 个门店` : '门店未限定'}`
})
const activeRuns = computed(() => recentRuns.value.filter((run) => /running|started/i.test(run.terminalStatus ?? '')).length)
const eventIntegrityLabel = computed(() => liveEvents.value.length === 0 ? '暂无事件' : liveEventsIntegrity.value ? '序列完整' : '序列有缺口')
const toolDomains = computed(() => {
  const counts = new Map<string, number>()
  for (const event of liveEvents.value) {
    if (!event.toolName) continue
    const domain = toolDomain(event.toolName)
    counts.set(domain, (counts.get(domain) ?? 0) + 1)
  }
  const total = [...counts.values()].reduce((sum, value) => sum + value, 0)
  return [...counts.entries()].sort((a, b) => b[1] - a[1]).map(([label, value]) => ({ label, value, percent: total ? Math.round(value / total * 100) : 0 }))
})
const overviewErrorMessage = computed(() => messageFor(overviewError.value, '平台总览读取失败'))
const recentRunsErrorMessage = computed(() => messageFor(recentRunsError.value, '最近运行读取失败'))
const usageErrorMessage = computed(() => messageFor(usageError.value, '用量摘要读取失败'))
const auditErrorMessage = computed(() => messageFor(auditError.value, '最近审计读取失败'))
const liveEventsErrorMessage = computed(() => messageFor(liveEventsError.value, '实时事件摘要读取失败'))
const resourceErrorMessage = computed(() => {
  const messages = [
    usageError.value ? usageErrorMessage.value : '',
    recentRunsError.value ? recentRunsErrorMessage.value : '',
  ].filter(Boolean)
  return messages.join('；') || '资源摘要读取失败'
})

function messageFor(value: unknown, fallback: string) {
  return value instanceof Error ? value.message : fallback
}

function formatNumber(value: number | null | undefined) {
  if (value == null) return '-'
  return new Intl.NumberFormat('zh-CN', { notation: value >= 1000000 ? 'compact' : 'standard', maximumFractionDigits: 1 }).format(value)
}

function formatTrendLabel(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.valueOf()) ? value : date.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' })
}

function date(value?: string | null) {
  return value ? new Date(value).toLocaleString('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }) : '-'
}

function duration(value?: number | null) {
  if (value == null) return '-'
  return value >= 1000 ? `${(value / 1000).toFixed(1)}s` : `${value}ms`
}

function statusTone(status?: string | null) {
  return /completed|success|ok|healthy|up/i.test(status ?? '') ? 'ok' : /failed|error|down|cancel/i.test(status ?? '') ? 'bad' : /running|pending|warn|degraded/i.test(status ?? '') ? 'warn' : ''
}

function statusType(status?: string | null) {
  return statusTone(status) === 'ok' ? 'completed' : statusTone(status) === 'bad' ? 'failed' : statusTone(status) === 'warn' ? 'running' : 'attention'
}

function runLabel(status?: string | null) {
  const normalized = status?.toLowerCase() ?? ''
  return normalized === 'completed' || normalized === 'success' ? '已完成' : normalized === 'running' ? '进行中' : normalized === 'failed' ? '失败' : normalized === 'cancelled' ? '已取消' : status || '未知'
}

function auditTone(result?: string | null) {
  return /success|ok|allow/i.test(result ?? '') ? 'ok' : /fail|deny|expired/i.test(result ?? '') ? 'bad' : 'warn'
}

function toolDomain(name: string) {
  const value = name.toLowerCase()
  if (/stock|inventory|product|warehouse/.test(value)) return '库存与商品'
  if (/sale|order|customer|supplier/.test(value)) return '销售与伙伴'
  if (/finance|payment|report|invoice/.test(value)) return '财务与报表'
  return '其他工具'
}

function hasCompleteEventSequences(events: AdminEvent[]) {
  const sequencesByRun = new Map<string, number[]>()
  for (const event of events) {
    const sequences = sequencesByRun.get(event.runId) ?? []
    sequences.push(event.sequence)
    sequencesByRun.set(event.runId, sequences)
  }
  return [...sequencesByRun.values()].every((sequences) => {
    const ordered = sequences.slice().sort((left, right) => left - right)
    return ordered.every((sequence, index) => index === 0 || sequence === ordered[index - 1] + 1)
  })
}

function windowParams() {
  const to = new Date()
  const from = new Date(to.getTime() - (rangeDays[range.value] ?? 30) * 86400000)
  return { from: from.toISOString(), to: to.toISOString() }
}

async function loadOverview(serial = ++requestSerial.value) {
  if (!session.token.value) { overviewError.value = new Error('管理员会话已失效'); return }
  overviewLoading.value = true
  overviewError.value = null
  try {
    const result = await fetchAdminOverview(session.token.value, { ...windowParams(), ownerUserId: ownerUserId.value || undefined, storeId: storeId.value || undefined })
    if (serial === requestSerial.value) overview.value = result
  } catch (cause) {
    if (serial === requestSerial.value) { overviewError.value = cause; overview.value = null }
  } finally { if (serial === requestSerial.value) overviewLoading.value = false }
}

async function loadRecentRuns(serial = requestSerial.value) {
  if (!session.token.value) { recentRunsError.value = new Error('管理员会话已失效'); return }
  recentRunsLoading.value = true
  recentRunsError.value = null
  try {
    const result = await fetchAdminRuns(session.token.value, { ...windowParams(), ownerUserId: ownerUserId.value || undefined, storeId: storeId.value || undefined, page: 0, size: 6 })
    if (serial === requestSerial.value) recentRuns.value = result.items
  } catch (cause) {
    if (serial === requestSerial.value) { recentRunsError.value = cause; recentRuns.value = [] }
  } finally { if (serial === requestSerial.value) recentRunsLoading.value = false }
}

async function loadUsage(serial = requestSerial.value) {
  if (!session.token.value) { usageError.value = new Error('管理员会话已失效'); return }
  usageLoading.value = true
  usageError.value = null
  try {
    const result = await fetchAdminUsage(session.token.value, { ...windowParams(), ownerUserId: ownerUserId.value || undefined, storeId: storeId.value || undefined, page: 0, size: 50 })
    if (serial !== requestSerial.value) return
    const values = result.items
    recentUsage.value = {
      totalTokens: values.reduce((sum, item) => sum + (item.totalTokens ?? 0), 0),
      durationMs: values.length ? Math.round(values.reduce((sum, item) => sum + (item.durationMs ?? 0), 0) / values.length) : 0,
      estimated: values.filter((item) => item.estimated).length,
      total: result.total,
    }
  } catch (cause) {
    if (serial === requestSerial.value) { usageError.value = cause; recentUsage.value = null }
  } finally { if (serial === requestSerial.value) usageLoading.value = false }
}

async function loadAudit(serial = requestSerial.value) {
  if (!session.token.value) { auditError.value = new Error('管理员会话已失效'); return }
  auditLoading.value = true
  auditError.value = null
  try {
    const result = await fetchAdminAuditEvents(session.token.value, { ...windowParams(), ownerUserId: ownerUserId.value || undefined, storeId: storeId.value || undefined, page: 0, size: 6 })
    if (serial === requestSerial.value) recentAudit.value = result.items
  } catch (cause) {
    if (serial === requestSerial.value) { auditError.value = cause; recentAudit.value = [] }
  } finally { if (serial === requestSerial.value) auditLoading.value = false }
}

async function loadLiveEvents(serial = requestSerial.value) {
  liveEventsLoading.value = true
  liveEventsError.value = null
  liveEventsIntegrity.value = true
  try {
    const runs = recentRuns.value.slice(0, 4)
    const pages = await Promise.all(runs.map((run) => fetchAdminEvents(session.token.value, run.runId, { includeContent: false, ownerUserId: ownerUserId.value || undefined, storeId: storeId.value || undefined })))
    if (serial !== requestSerial.value) return
    const normalizedEvents = pages.flatMap((page, index) => page.items.map((event) => ({ ...event, runId: event.runId || runs[index].runId })))
    liveEventsIntegrity.value = pages.every((page) => page.eventIntegrity) && hasCompleteEventSequences(normalizedEvents)
    liveEvents.value = normalizedEvents.sort((a, b) => Date.parse(b.occurredAt) - Date.parse(a.occurredAt)).slice(0, 8)
  } catch (cause) {
    if (serial === requestSerial.value) { liveEventsError.value = cause; liveEvents.value = [] }
  } finally { if (serial === requestSerial.value) liveEventsLoading.value = false }
}

async function loadAll() {
  const serial = ++requestSerial.value
  liveEvents.value = []
  liveEventsIntegrity.value = true
  await Promise.all([loadOverview(serial), loadRecentRuns(serial), loadUsage(serial), loadAudit(serial)])
  await loadLiveEvents(serial)
}

function applyScope() { void loadAll() }

function openRun(runId: string) {
  void router.push({ path: '/admin/agent/runs', query: { runId } })
}

watch(range, () => { void loadAll() })
onMounted(async () => { if (await adminSession.ensure(session.token.value)) await loadAll() })
</script>

<template>
  <AdminLayout active-id="overview">
    <section class="admin-page-v2 admin-overview-page">
      <AdminPageHeader v-model:range="range" title="平台总览" :refreshing="totalLoading" @refresh="loadAll" />
      <div class="admin-toolbar admin-overview-scope">
        <AdminScopeFilter v-model:owner-user-id="ownerUserId" v-model:store-id="storeId" :owner-user-ids="adminSession.session.value?.ownerUserIds" :store-ids="adminSession.session.value?.storeIds" />
        <button class="admin-button-v2" type="button" @click="applyScope">应用范围</button>
      </div>
      <div v-if="overviewError" class="admin-error-v2" role="alert"><ShieldAlert aria-hidden="true" /><span>{{ overviewErrorMessage }}</span><button type="button" @click="loadOverview()">重试</button></div>

      <div class="admin-grid admin-metric-grid">
        <template v-if="overviewLoading">
          <article v-for="index in 4" :key="`metric-loading-${index}`" class="admin-card-v2 admin-stat-v2 admin-span-3 admin-skeleton-card" aria-label="正在读取指标" />
        </template>
        <template v-else-if="metrics.length">
          <article v-for="metric in metrics" :key="metric.key" class="admin-card-v2 admin-stat-v2 admin-span-3" :class="`admin-stat-v2--${metric.tone}`">
            <div class="admin-stat-v2__label"><span>{{ metric.label }}</span><component :is="metric.icon" aria-hidden="true" /></div>
            <strong>{{ formatNumber(metric.value) }}</strong>
            <small>{{ metric.unit || (overview?.estimated ? '估算值' : '服务端统计') }}</small>
          </article>
        </template>
        <article v-else class="admin-card-v2 admin-span-12"><AdminPanelState state="empty" title="当前范围没有指标" message="服务端没有返回当前时间范围的汇总指标。" /></article>
      </div>

      <div class="admin-grid">
        <article class="admin-card-v2 admin-span-8">
          <div class="admin-card-v2__header"><div><h2>Agent 调用趋势</h2><p>时间范围：{{ overview?.from ? date(overview.from) : range }} · 数据更新时间：{{ overview?.generatedAt ? date(overview.generatedAt) : '-' }}</p></div><span class="admin-card-v2__meta">{{ overview?.estimated ? 'ESTIMATED' : 'EXACT' }}</span></div>
          <AdminPanelState v-if="overviewLoading" state="loading" title="正在读取趋势" />
          <AdminPanelState v-else-if="overviewError" state="error" :message="overviewErrorMessage" @retry="loadOverview()" />
          <AdminPanelState v-else-if="!overview || chartPoints.length === 0" state="empty" title="当前范围暂无趋势数据" message="服务端没有返回当前时间范围的趋势点。" />
          <div v-else class="admin-overview-chart" role="img" :aria-label="`Agent 调用趋势，共 ${chartPoints.length} 个时间点`">
            <div v-for="point in chartPoints" :key="point.at" class="admin-overview-chart__bar" :title="`${point.label} · ${point.value}`"><i :style="{ height: `${point.height}%` }" /><span>{{ point.label }}</span></div>
          </div>
        </article>
        <article class="admin-card-v2 admin-span-4 admin-card-v2--pad admin-scope-card">
          <div class="admin-section-kicker"><Eye aria-hidden="true" />数据口径</div>
          <h2>{{ scopeLabel }}</h2>
          <p>当前请求由服务端按管理员授权范围计算。筛选条件不会扩大授权。</p>
          <dl><dt>范围完整性</dt><dd>{{ overview?.scopeCompleteness || 'UNKNOWN' }}</dd><dt>统计生成</dt><dd>{{ overview?.generatedAt ? date(overview.generatedAt) : '-' }}</dd><dt>内容模式</dt><dd>{{ overview?.scope?.contentMode || 'METADATA_ONLY' }}</dd></dl>
        </article>
      </div>

      <div class="admin-grid">
        <article class="admin-card-v2 admin-span-4 admin-card-v2--pad">
          <div class="admin-card-v2__header admin-card-v2__header--flush"><div><h2>资源摘要</h2><p>仅统计当前返回的服务端数据。</p></div><Database aria-hidden="true" /></div>
          <AdminPanelState v-if="usageLoading || recentRunsLoading" state="loading" title="正在读取资源摘要" />
          <AdminPanelState v-else-if="usageError || recentRunsError" state="error" :message="resourceErrorMessage" @retry="loadAll" />
          <div v-else class="admin-resource-list"><div><span>进行中运行</span><strong>{{ activeRuns }}</strong></div><div><span>用量记录</span><strong>{{ recentUsage?.total ?? '-' }}</strong></div><div><span>Token 总量</span><strong>{{ recentUsage ? formatNumber(recentUsage.totalTokens) : '-' }}</strong></div><div><span>平均耗时</span><strong>{{ recentUsage ? duration(recentUsage.durationMs) : '-' }}</strong></div><small v-if="recentUsage?.estimated">{{ recentUsage.estimated }} 条记录使用估算用量</small><small v-else>未返回估算记录</small></div>
        </article>
        <article class="admin-card-v2 admin-span-4 admin-card-v2--pad">
          <div class="admin-card-v2__header admin-card-v2__header--flush"><div><h2>工具域分布</h2><p>按已返回的工具名称归类。</p></div><Wrench aria-hidden="true" /></div>
          <AdminPanelState v-if="liveEventsLoading" state="loading" title="正在读取工具事件" />
          <AdminPanelState v-else-if="liveEventsError" state="error" :message="liveEventsErrorMessage" @retry="loadLiveEvents()" />
          <AdminPanelState v-else-if="!toolDomains.length" state="empty" title="暂无工具事件" message="当前范围没有可归类的工具调用。" />
          <div v-else class="admin-domain-list"><div v-for="domain in toolDomains" :key="domain.label"><div><span>{{ domain.label }}</span><strong>{{ domain.value }}</strong></div><div class="admin-domain-track"><i :style="{ width: `${domain.percent}%` }" /></div></div></div>
        </article>
        <article class="admin-card-v2 admin-span-4 admin-card-v2--pad">
          <div class="admin-card-v2__header admin-card-v2__header--flush"><div><h2>事件流状态</h2><p>读取最近运行的持久化事件摘要。</p></div><Activity aria-hidden="true" /></div>
          <AdminPanelState v-if="liveEventsLoading" state="loading" title="正在读取事件" />
          <AdminPanelState v-else-if="liveEventsError" state="error" :message="liveEventsErrorMessage" @retry="loadLiveEvents()" />
          <div v-else class="admin-event-status"><div class="admin-event-status__headline"><CircleCheck v-if="eventIntegrityLabel === '序列完整'" aria-hidden="true" /><CircleAlert v-else aria-hidden="true" /><strong>{{ eventIntegrityLabel }}</strong></div><p>{{ liveEvents.length ? `${liveEvents.length} 条最近事件 · 最后更新时间 ${date(liveEvents[0]?.occurredAt)}` : '服务端尚未返回事件记录。' }}</p><span class="admin-card-v2__meta">不会根据缺失事件补写运行事实</span></div>
        </article>
      </div>

      <div class="admin-grid">
        <article class="admin-card-v2 admin-span-7">
          <div class="admin-card-v2__header"><div><h2>最近 Agent 运行</h2><p>点击运行可进入完整事件、消息、草稿和上下文详情。</p></div><RouterLink class="admin-inline-link" to="/admin/agent/runs">查看全部 <ArrowUpRight aria-hidden="true" /></RouterLink></div>
          <AdminPanelState v-if="recentRunsLoading" state="loading" title="正在读取最近运行" />
          <AdminPanelState v-else-if="recentRunsError" state="error" :message="recentRunsErrorMessage" @retry="loadRecentRuns()" />
          <AdminPanelState v-else-if="!recentRuns.length" state="empty" title="当前范围暂无运行" message="服务端没有返回可见的 Agent 运行记录。" />
          <div v-else class="admin-table-wrap"><table class="admin-table-v2 admin-table-v2--compact"><thead><tr><th>运行</th><th>发起者 / 门店</th><th>耗时</th><th>状态</th></tr></thead><tbody><tr v-for="run in recentRuns" :key="run.runId" tabindex="0" @click="openRun(run.runId)" @keydown.enter="openRun(run.runId)"><td><strong><code>{{ run.runId }}</code></strong><small>{{ run.modelId || '模型未提供' }}</small></td><td><code>{{ run.actorUserId || '-' }}</code><small>{{ run.storeId || '门店未提供' }}</small></td><td>{{ duration(run.durationMs) }}</td><td><AdminStatusBadge :status="statusType(run.terminalStatus)" :label="runLabel(run.terminalStatus)" /></td></tr></tbody></table></div>
        </article>
        <article class="admin-card-v2 admin-span-5">
          <div class="admin-card-v2__header"><div><h2>最近审计</h2><p>查看、拒绝、配置和导出动作均由服务端记录。</p></div><RouterLink class="admin-inline-link" to="/admin/audit">查看审计 <ArrowUpRight aria-hidden="true" /></RouterLink></div>
          <AdminPanelState v-if="auditLoading" state="loading" title="正在读取最近审计" />
          <AdminPanelState v-else-if="auditError" state="error" :message="auditErrorMessage" @retry="loadAudit()" />
          <AdminPanelState v-else-if="!recentAudit.length" state="empty" title="暂无审计记录" message="服务端没有返回当前范围的审计事件。" />
          <div v-else class="admin-audit-list"><button v-for="item in recentAudit" :key="item.eventId" type="button" @click="$router.push('/admin/audit')"><span class="admin-audit-list__time">{{ date(item.occurredAt) }}</span><span class="admin-audit-list__main"><strong>{{ item.action }}</strong><small>{{ item.resourceType || '系统' }} · {{ item.summary || '无摘要' }}</small></span><span class="admin-status-v2" :class="`admin-status-v2--${auditTone(item.result)}`">{{ item.result || '未知' }}</span></button></div>
        </article>
      </div>

      <div class="admin-grid">
        <article class="admin-card-v2 admin-span-12">
          <div class="admin-card-v2__header"><div><h2>实时日志摘要</h2><p>此处读取最近运行的已持久化事件；运行详情页才会建立单次运行的 SSE 观测流。</p></div><span class="admin-card-v2__meta">{{ liveEvents.length }} 条 · {{ eventIntegrityLabel }}</span></div>
          <AdminPanelState v-if="liveEventsLoading" state="loading" title="正在读取事件摘要" />
          <AdminPanelState v-else-if="liveEventsError" state="error" :message="liveEventsErrorMessage" @retry="loadLiveEvents()" />
          <AdminPanelState v-else-if="!liveEvents.length" state="empty" title="暂无实时日志" message="服务端没有返回当前范围内的事件记录，不使用本地预置日志。" />
          <div v-else class="admin-log-list"><div v-for="event in liveEvents" :key="`${event.eventId}-${event.sequence}`" class="admin-log-row"><span class="admin-log-row__time">{{ date(event.occurredAt) }}</span><code>#{{ event.sequence }}</code><span class="admin-log-row__event">{{ event.eventType }}<small v-if="event.toolName">{{ event.toolName }} · {{ event.callId || 'call ID 未提供' }}</small></span><span class="admin-status-v2" :class="`admin-status-v2--${statusTone(event.status)}`">{{ event.status || '未知' }}</span><span class="admin-log-row__summary">{{ event.resultSummary || event.argumentSummary || (event.redactionState === 'REDACTED' ? '内容已脱敏' : '无摘要') }}</span></div></div>
        </article>
      </div>
    </section>
  </AdminLayout>
</template>

<style scoped>
.admin-overview-page { padding-bottom: 72px; }
.admin-overview-scope { max-width: 1400px; margin: 0 auto 14px; }
.admin-stat-v2--blue .admin-stat-v2__label svg { color: #477eac; }
.admin-stat-v2--purple .admin-stat-v2__label svg { color: #8063a8; }
.admin-stat-v2--green .admin-stat-v2__label svg { color: #4d9270; }
.admin-stat-v2--amber .admin-stat-v2__label svg { color: #af8745; }
.admin-skeleton-card { min-height: 126px; background: linear-gradient(90deg, #fff 25%, #f6f6f3 50%, #fff 75%); background-size: 240% 100%; animation: admin-skeleton 1.2s ease-in-out infinite; }
.admin-card-v2__header--flush { margin: -20px -20px 18px; padding: 0 0 16px; }
.admin-overview-chart { display: flex; height: 236px; align-items: flex-end; gap: 7px; padding: 26px 20px 32px; }
.admin-overview-chart__bar { position: relative; display: flex; height: 100%; flex: 1; min-width: 8px; align-items: flex-end; }
.admin-overview-chart__bar i { display: block; width: 100%; min-height: 3px; border-radius: 4px 4px 1px 1px; background: #477eac; }
.admin-overview-chart__bar span { position: absolute; right: 50%; bottom: -22px; color: #a0a0a0; font-size: 9px; transform: translateX(50%); white-space: nowrap; }
.admin-scope-card .admin-section-kicker { display: flex; align-items: center; gap: 6px; color: #8f8f8b; font-size: 10px; letter-spacing: .08em; text-transform: uppercase; }
.admin-scope-card .admin-section-kicker svg { width: 14px; }
.admin-scope-card h2 { margin-top: 14px; font-size: 17px; line-height: 1.35; }
.admin-scope-card dl { display: grid; grid-template-columns: 92px 1fr; gap: 9px 12px; margin: 22px 0 0; }
.admin-scope-card dt { color: #a0a0a0; font-size: 10px; }
.admin-scope-card dd { margin: 0; color: #454545; font-size: 11px; word-break: break-word; }
.admin-resource-list { display: grid; gap: 14px; }
.admin-resource-list > div { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; border-bottom: 1px solid #f0f0ed; padding-bottom: 10px; }
.admin-resource-list > div:last-of-type { border-bottom: 0; padding-bottom: 0; }
.admin-resource-list span { color: #737373; font-size: 11px; }
.admin-resource-list strong { color: #1d1d1f; font-size: 18px; font-weight: 620; }
.admin-resource-list small { color: #a0a0a0; font-size: 10px; }
.admin-domain-list { display: grid; gap: 17px; }
.admin-domain-list > div > div:first-child { display: flex; justify-content: space-between; color: #555; font-size: 11px; }
.admin-domain-list strong { color: #1d1d1f; font-weight: 620; }
.admin-domain-track { height: 5px; margin-top: 8px; overflow: hidden; border-radius: 3px; background: #f0f0ed; }
.admin-domain-track i { display: block; height: 100%; border-radius: inherit; background: #8063a8; }
.admin-event-status { display: grid; gap: 13px; }
.admin-event-status__headline { display: flex; align-items: center; gap: 8px; }
.admin-event-status__headline svg { width: 18px; color: #4d9270; }
.admin-event-status__headline svg + strong { color: #327a4b; font-size: 15px; }
.admin-event-status__headline svg:not(:first-child) { color: #af8745; }
.admin-event-status p { line-height: 1.65; }
.admin-inline-link { display: inline-flex; align-items: center; gap: 4px; color: #555; font-size: 11px; text-decoration: none; }
.admin-inline-link:hover { color: #1d1d1f; text-decoration: underline; }
.admin-inline-link svg { width: 13px; }
.admin-table-v2--compact { min-width: 560px; }
.admin-table-v2--compact td { padding-top: 11px; padding-bottom: 11px; }
.admin-audit-list { display: grid; }
.admin-audit-list button { display: grid; grid-template-columns: 108px 1fr auto; align-items: center; gap: 10px; border: 0; border-bottom: 1px solid #f0f0ed; background: #fff; padding: 13px 20px; text-align: left; cursor: pointer; }
.admin-audit-list button:last-child { border-bottom: 0; }
.admin-audit-list button:hover { background: #fafaf8; }
.admin-audit-list__time { color: #a0a0a0; font-size: 10px; }
.admin-audit-list__main { min-width: 0; }
.admin-audit-list__main strong, .admin-audit-list__main small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.admin-audit-list__main strong { color: #1d1d1f; font-size: 11px; }
.admin-audit-list__main small { margin-top: 4px; color: #888; font-size: 10px; }
.admin-log-list { display: grid; }
.admin-log-row { display: grid; grid-template-columns: 132px 44px minmax(150px, 1fr) auto minmax(180px, 1.2fr); align-items: center; gap: 12px; border-bottom: 1px solid #f0f0ed; padding: 12px 20px; color: #555; font-size: 11px; }
.admin-log-row:last-child { border-bottom: 0; }
.admin-log-row__time { color: #a0a0a0; font-size: 10px; }
.admin-log-row > code { color: #888; font-size: 10px; }
.admin-log-row__event { color: #1d1d1f; font-weight: 600; }
.admin-log-row__event small { display: block; margin-top: 4px; color: #888; font-size: 10px; font-weight: 400; }
.admin-log-row__summary { overflow: hidden; color: #737373; text-overflow: ellipsis; white-space: nowrap; }
@keyframes admin-skeleton { 0% { background-position: 200% 0; } 100% { background-position: -40% 0; } }
@media (max-width: 1100px) { .admin-log-row { grid-template-columns: 112px 38px minmax(130px, 1fr) auto; }.admin-log-row__summary { grid-column: 3 / -1; } }
@media (max-width: 620px) { .admin-audit-list button { grid-template-columns: 1fr auto; }.admin-audit-list__time { grid-column: 1 / -1; }.admin-log-row { grid-template-columns: 1fr auto; gap: 7px; }.admin-log-row > code { grid-row: 1; grid-column: 2; }.admin-log-row__event { grid-column: 1 / -1; }.admin-log-row__summary { grid-column: 1 / -1; white-space: normal; }.admin-overview-chart { gap: 3px; padding-left: 12px; padding-right: 12px; }.admin-overview-chart__bar span { font-size: 8px; transform: translateX(50%) rotate(-35deg); transform-origin: top right; } }
@media (prefers-reduced-motion: reduce) { .admin-skeleton-card { animation: none; } }
</style>
