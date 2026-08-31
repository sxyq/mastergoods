<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Activity, Bot, Clock3, RefreshCw, ShieldCheck, Store, UsersRound, Wrench } from 'lucide-vue-next'
import OverviewMetricCard from './OverviewMetricCard.vue'
import OverviewResourceSummary from './OverviewResourceSummary.vue'
import OverviewTrendChart from './OverviewTrendChart.vue'
import { getAdminAuditEvents, getAdminOverview, getAdminRuns, type AdminAuditEvent, type AdminOverview, type AdminOverviewMetric, type AdminRun } from '@/shared/api/admin'
import { formatDateTime, formatDuration, formatNumber, formatPercent } from '@/shared/utils/format'

const rangeDays = ref(30)
const loading = ref(true)
const refreshing = ref(false)
const overview = ref<AdminOverview | null>(null)
const runs = ref<AdminRun[]>([])
const auditEvents = ref<AdminAuditEvent[]>([])
const loadError = ref('')
const supplementalError = ref(false)
let requestSequence = 0

const range = computed(() => {
  const to = new Date()
  const from = new Date(to.getTime() - rangeDays.value * 24 * 60 * 60 * 1000)
  return { from, to }
})

const metrics = computed(() => new Map(overview.value?.metrics.map((metric) => [metric.key, metric]) ?? []))
const selectedRangeText = computed(() => overview.value ? `${formatDateTime(overview.value.from)} 至 ${formatDateTime(overview.value.to)}` : `近 ${rangeDays.value} 天`)

function metric(key: string): AdminOverviewMetric | undefined {
  return metrics.value.get(key)
}

const cards = computed(() => [
  { key: 'users', label: '用户总数', icon: UsersRound, value: formatNumber(metric('users')?.value), detail: '授权范围内的用户', unavailable: metric('users')?.availability === 'UNAVAILABLE' },
  { key: 'stores', label: '门店总数', icon: Store, value: formatNumber(metric('stores')?.value), detail: '授权范围内的门店', unavailable: metric('stores')?.availability === 'UNAVAILABLE' },
  { key: 'agent_runs', label: 'Agent 运行', icon: Bot, value: formatNumber(metric('agent_runs')?.value), detail: selectedRangeText.value, unavailable: metric('agent_runs')?.availability === 'UNAVAILABLE' },
  { key: 'agent_tool_calls', label: '工具调用', icon: Wrench, value: formatNumber(metric('agent_tool_calls')?.value), detail: selectedRangeText.value, unavailable: metric('agent_tool_calls')?.availability === 'UNAVAILABLE' },
  { key: 'agent_success_rate', label: '终态完成率', icon: ShieldCheck, value: formatPercent(metric('agent_success_rate')?.value), detail: '已完成 / 已进入终态', unavailable: metric('agent_success_rate')?.availability === 'UNAVAILABLE' },
])

async function loadPage(): Promise<void> {
  const sequence = ++requestSequence
  const query = { from: range.value.from, to: range.value.to }
  const [overviewResult, runsResult, auditResult] = await Promise.allSettled([
    getAdminOverview(query),
    getAdminRuns({ ...query, page: 0, size: 5 }),
    getAdminAuditEvents({ ...query, page: 0, size: 5 }),
  ])

  if (sequence !== requestSequence) return

  if (overviewResult.status === 'fulfilled') {
    overview.value = overviewResult.value
    loadError.value = ''
  } else {
    loadError.value = overviewResult.reason instanceof Error ? overviewResult.reason.message : '无法读取平台总览。'
  }

  supplementalError.value = runsResult.status === 'rejected' || auditResult.status === 'rejected'
  if (runsResult.status === 'fulfilled') runs.value = runsResult.value.items
  if (auditResult.status === 'fulfilled') auditEvents.value = auditResult.value.items
}

async function refresh(): Promise<void> {
  refreshing.value = true
  await loadPage()
  refreshing.value = false
}

async function changeRange(): Promise<void> {
  loading.value = true
  await loadPage()
  loading.value = false
}

function statusLabel(value: string | null): string {
  const labels: Record<string, string> = {
    completed: '已完成', confirmation_pending: '待确认', running: '进行中', failed: '失败', blocked: '已阻塞', cancelled: '已取消', exhausted: '已耗尽',
  }
  return value ? labels[value] ?? value : '未知'
}

function statusClass(value: string | null): string {
  if (value === 'completed') return 'status--success'
  if (value === 'running' || value === 'confirmation_pending') return 'status--attention'
  if (value === 'failed' || value === 'blocked' || value === 'cancelled') return 'status--danger'
  return 'status--muted'
}

onMounted(async () => {
  await loadPage()
  loading.value = false
})
</script>

<template>
  <section class="overview-workspace">
    <header class="page-header">
      <div>
        <p class="eyebrow">PLATFORM / OVERVIEW</p>
        <h1>平台总览</h1>
        <p class="page-description">查看授权范围内的组织规模、Agent 运行和管理员活动。</p>
      </div>
      <div class="page-actions">
        <label class="range-control">
          <span class="visually-hidden">统计时间范围</span>
          <Clock3 aria-hidden="true" />
          <select v-model.number="rangeDays" aria-label="统计时间范围" @change="changeRange">
            <option :value="7">近 7 天</option>
            <option :value="30">近 30 天</option>
            <option :value="90">近 90 天</option>
          </select>
        </label>
        <button class="button button--outline" type="button" :disabled="refreshing" @click="refresh">
          <RefreshCw :class="{ spinning: refreshing }" aria-hidden="true" />
          刷新
        </button>
      </div>
    </header>

    <div v-if="loadError" class="error-banner" role="alert">
      <strong>平台总览暂不可用</strong>
      <span>{{ loadError }}</span>
      <button type="button" class="button button--outline" @click="refresh">重试</button>
    </div>

    <template v-else>
      <p v-if="overview?.scope_completeness === 'PARTIAL'" class="scope-notice">
        当前数据仅覆盖你被授权的 owner 或门店范围。
      </p>
      <p v-if="supplementalError" class="scope-notice scope-notice--warning">
        最近运行或审计记录暂不可用；其余平台指标仍基于最新成功响应展示。
      </p>

      <div class="metric-grid" :aria-busy="loading || undefined">
        <OverviewMetricCard v-for="item in cards" :key="item.key" :label="item.label" :value="item.value" :detail="item.detail" :icon="item.icon" :unavailable="item.unavailable" />
      </div>

      <div class="dashboard-grid">
        <OverviewTrendChart :points="overview?.trend ?? []" :from="overview?.from" :to="overview?.to" :loading="loading" />
        <OverviewResourceSummary
          :success-rate="metric('agent_success_rate')?.value ?? null"
          :active-runs="metric('agent_active_runs')?.value ?? null"
          :tool-calls="metric('agent_tool_calls')?.value ?? null"
          :total-runs="metric('agent_runs')?.value ?? null"
          :average-latency="metric('agent_average_latency')?.value ?? null"
        />
      </div>

      <div class="lower-grid">
        <section class="data-panel" aria-labelledby="recent-runs-title">
          <header class="panel-header">
            <div>
              <h2 id="recent-runs-title">最近运行</h2>
              <p>按开始时间倒序，显示最近五项。</p>
            </div>
            <RouterLink class="text-link" to="/agent/runs">查看全部</RouterLink>
          </header>
          <div class="table-scroll">
            <table>
              <thead><tr><th scope="col">运行</th><th scope="col">模型与范围</th><th scope="col" class="numeric">Token / 耗时</th><th scope="col">状态</th></tr></thead>
              <tbody>
                <tr v-for="run in runs" :key="run.run_id">
                  <td><strong class="mono">{{ run.run_id }}</strong><small>{{ formatDateTime(run.started_at) }}</small></td>
                  <td><strong>{{ run.model_id || '未记录模型' }}</strong><small>Owner {{ run.owner_user_id || '—' }} / 门店 {{ run.store_id || '—' }}</small></td>
                  <td class="numeric"><strong>{{ formatNumber(run.total_tokens) }}</strong><small>{{ formatDuration(run.duration_ms) }}</small></td>
                  <td><span class="status" :class="statusClass(run.terminal_status)"><i />{{ statusLabel(run.terminal_status) }}</span></td>
                </tr>
                <tr v-if="!loading && !runs.length"><td colspan="4" class="empty-cell">所选时间范围内暂无运行记录。</td></tr>
              </tbody>
            </table>
          </div>
        </section>

        <section class="data-panel audit-panel" aria-labelledby="recent-audit-title">
          <header class="panel-header">
            <div><h2 id="recent-audit-title">最近操作审计</h2><p>所有查看与受控动作均由服务端记录。</p></div>
            <RouterLink class="text-link" to="/audit">查看全部</RouterLink>
          </header>
          <ul class="audit-list">
            <li v-for="event in auditEvents" :key="event.event_id">
              <Activity aria-hidden="true" />
              <div><strong>{{ event.action }}</strong><small>{{ event.resource_type || '系统资源' }} · {{ formatDateTime(event.occurred_at) }}</small></div>
              <span class="audit-result" :class="event.result === 'SUCCESS' ? 'audit-result--success' : 'audit-result--warning'">{{ event.result }}</span>
            </li>
            <li v-if="!loading && !auditEvents.length" class="empty-audit">所选时间范围内暂无操作审计。</li>
          </ul>
        </section>
      </div>

      <footer v-if="overview" class="page-footer">
        <span>数据范围：{{ selectedRangeText }}</span>
        <span>生成时间：{{ formatDateTime(overview.generated_at) }}</span>
      </footer>
    </template>
  </section>
</template>

<style scoped>
.overview-workspace { width: 100%; }
.page-header { display: flex; min-height: 72px; align-items: flex-start; justify-content: space-between; gap: 24px; }
.eyebrow { margin: 0; color: var(--admin-muted, #71716d); font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 10px; letter-spacing: .08em; }
h1 { margin: 9px 0 0; font-size: 24px; font-weight: 500; line-height: 1.15; }
.page-description { margin: 8px 0 0; color: var(--admin-muted, #71716d); font-size: 12px; }
.page-actions { display: flex; flex: 0 0 auto; align-items: center; gap: 8px; }
.range-control { display: inline-flex; height: 32px; align-items: center; gap: 7px; border: 1px solid var(--admin-border, #e7e7e4); border-radius: 999px; background: var(--admin-card, #fcfcfb); padding: 0 10px; color: var(--admin-muted, #71716d); }
.range-control :deep(svg), .button :deep(svg) { width: 14px; height: 14px; stroke-width: 1.8; }
.range-control select { border: 0; outline: 0; background: transparent; color: var(--admin-foreground, #1d1d1b); font-size: 12px; }
.button { display: inline-flex; height: 32px; align-items: center; justify-content: center; gap: 7px; border-radius: 999px; padding: 0 12px; cursor: pointer; font-size: 12px; transition: background-color .16s ease-out, color .16s ease-out; }
.button:disabled { cursor: wait; opacity: .65; }
.button--outline { border: 1px solid var(--admin-border, #e7e7e4); background: var(--admin-card, #fcfcfb); color: var(--admin-foreground, #1d1d1b); }
.button--outline:hover:not(:disabled) { background: var(--admin-secondary, #f2f2ef); }
.spinning { animation: spin .8s linear infinite; }

.scope-notice { margin: 18px 0 0; border-bottom: 1px solid var(--admin-border, #e7e7e4); padding: 0 0 10px; color: var(--admin-muted, #71716d); font-size: 11px; }
.scope-notice--warning { color: #9a691f; }
.error-banner { display: flex; align-items: center; gap: 12px; margin-top: 22px; border: 1px solid #e6c2bd; border-radius: 8px; background: #fff9f7; padding: 12px; color: #843e35; font-size: 12px; }
.error-banner span { flex: 1; }

.metric-grid { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 8px; margin-top: 18px; }
.dashboard-grid, .lower-grid { display: grid; grid-template-columns: minmax(0, 3fr) minmax(330px, 2fr); gap: 8px; margin-top: 8px; }
.lower-grid { align-items: stretch; }

.data-panel { min-width: 0; border: 1px solid var(--admin-border, #e7e7e4); border-radius: 8px; background: var(--admin-card, #fcfcfb); padding: 18px; }
.panel-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; }
.panel-header h2 { margin: 0; font-size: 14px; font-weight: 500; }
.panel-header p { margin: 5px 0 0; color: var(--admin-muted, #71716d); font-size: 11px; }
.text-link { color: var(--admin-muted, #71716d); font-size: 11px; text-decoration: none; }
.text-link:hover { color: var(--admin-foreground, #1d1d1b); text-decoration: underline; }
.table-scroll { overflow-x: auto; margin-top: 16px; }
table { width: 100%; min-width: 660px; border-collapse: collapse; table-layout: fixed; }
th { height: 34px; border-bottom: 1px solid var(--admin-border, #e7e7e4); color: var(--admin-muted, #71716d); font-size: 11px; font-weight: 400; text-align: left; }
th:nth-child(1) { width: 30%; } th:nth-child(2) { width: 33%; } th:nth-child(3) { width: 20%; } th:nth-child(4) { width: 17%; }
td { height: 58px; border-bottom: 1px solid var(--admin-border, #e7e7e4); padding: 7px 8px 7px 0; font-size: 11px; vertical-align: middle; }
td strong { display: block; overflow: hidden; font-weight: 500; text-overflow: ellipsis; white-space: nowrap; }
td small { display: block; overflow: hidden; margin-top: 5px; color: var(--admin-muted, #71716d); font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.mono { color: #65659d; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 10px; }
.numeric { padding-right: 10px; text-align: right; font-variant-numeric: tabular-nums; }
.status { display: inline-flex; align-items: center; gap: 5px; border-radius: 999px; padding: 4px 7px; font-size: 10px; white-space: nowrap; }
.status i { width: 5px; height: 5px; border-radius: 50%; background: currentColor; }
.status--success { background: rgba(67, 165, 107, .12); color: #318254; }
.status--attention { background: rgba(200, 148, 73, .13); color: #9a691f; }
.status--danger { background: rgba(188, 97, 85, .12); color: #a24d43; }
.status--muted { background: #f1f1ef; color: #71716d; }
.empty-cell { height: 120px; color: var(--admin-muted, #71716d); text-align: center; }

.audit-list { display: grid; margin: 17px 0 0; padding: 0; list-style: none; }
.audit-list li { display: flex; min-height: 58px; align-items: center; gap: 10px; border-bottom: 1px solid var(--admin-border, #e7e7e4); }
.audit-list li:last-child { border-bottom: 0; }
.audit-list li > :deep(svg) { width: 16px; height: 16px; flex: 0 0 16px; color: #4ba776; stroke-width: 1.7; }
.audit-list div { min-width: 0; flex: 1; }
.audit-list strong, .audit-list small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.audit-list strong { font-size: 11px; font-weight: 500; }
.audit-list small { margin-top: 5px; color: var(--admin-muted, #71716d); font-size: 10px; }
.audit-result { flex: 0 0 auto; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 9px; }
.audit-result--success { color: #318254; } .audit-result--warning { color: #9a691f; }
.audit-list .empty-audit { justify-content: center; height: 120px; color: var(--admin-muted, #71716d); font-size: 11px; }
.page-footer { display: flex; justify-content: space-between; gap: 16px; margin-top: 20px; color: var(--admin-muted, #71716d); font-size: 10px; }
.visually-hidden { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; clip-path: inset(50%); }

@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 1180px) { .metric-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); } .dashboard-grid, .lower-grid { grid-template-columns: 1fr; } }
@media (max-width: 760px) { .page-header { flex-direction: column; gap: 18px; } .page-actions { width: 100%; } .range-control { flex: 1; } .range-control select { width: 100%; } .metric-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .error-banner { align-items: flex-start; flex-direction: column; } .page-footer { align-items: flex-start; flex-direction: column; gap: 6px; } }
@media (prefers-reduced-motion: reduce) { .spinning { animation: none; } }
</style>
