<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { BarChart3, Download, FileDown, Filter, RefreshCw, Search, ShieldCheck } from 'lucide-vue-next'
import { hasAdminPermission } from '@/app/stores/admin-session'
import { createAdminExport, downloadAdminExport, getAdminAuditEvents, getAdminExportJobs, type AdminAuditEvent, type AdminExportJob } from '@/shared/api/admin'
import { formatDateTime, formatNumber } from '@/shared/utils/format'
import AdminPageHeader from '@/shared/components/AdminPageHeader.vue'
import ConfirmDialog from '@/shared/components/ConfirmDialog.vue'
import StatePanel from '@/shared/components/StatePanel.vue'

const eventId = ref('')
const action = ref('')
const result = ref('')
const rangeDays = ref(30)
const page = ref(0)
const pageSize = 25
const events = ref<AdminAuditEvent[]>([])
const total = ref(0)
const loading = ref(true)
const error = ref('')
const selected = ref<AdminAuditEvent | null>(null)
const exportOpen = ref(false)
const exportBusy = ref(false)
const exportMessage = ref('')
const jobs = ref<AdminExportJob[]>([])
const canExport = computed(() => hasAdminPermission('admin.export'))
const hasNext = computed(() => (page.value + 1) * pageSize < total.value)
const actionCounts = computed(() => {
  const entries = new Map<string, number>()
  events.value.forEach((event) => {
    const name = event.action || '未标记操作'
    entries.set(name, (entries.get(name) ?? 0) + 1)
  })
  return [...entries.entries()].sort((a, b) => b[1] - a[1]).slice(0, 4)
})
const maxActionCount = computed(() => Math.max(1, ...actionCounts.value.map(([, count]) => count)))
const actionChart = computed(() => {
  const width = 520
  const top = 14
  const rowHeight = 40
  const barX = 184
  const valueX = width - 12
  const barWidth = valueX - barX - 42
  const height = Math.max(96, top + Math.max(actionCounts.value.length, 1) * rowHeight + 8)
  const rows = actionCounts.value.map(([name, count], index) => ({
    name,
    count,
    label: name.length > 28 ? `${Array.from(name).slice(0, 27).join('')}…` : name,
    y: top + index * rowHeight,
    barWidth: Math.max(4, count / maxActionCount.value * barWidth),
  }))
  return { width, height, barX, valueX, barWidth, rows }
})
const range = computed(() => { const to = new Date(); const from = new Date(to.getTime() - rangeDays.value * 86_400_000); return { from, to } })

async function load(): Promise<void> {
  loading.value = true; error.value = ''
  const request = getAdminAuditEvents({ eventId: eventId.value.trim() || undefined, action: action.value.trim() || undefined, result: result.value || undefined, from: range.value.from, to: range.value.to, page: page.value, size: pageSize })
  const [eventResult, jobResult] = await Promise.allSettled([request, canExport.value ? getAdminExportJobs({ page: 0, size: 5 }) : Promise.resolve(null)])
  if (eventResult.status === 'fulfilled') {
    events.value = eventResult.value.items
    total.value = eventResult.value.total
    if (selected.value && !events.value.some((event) => event.event_id === selected.value?.event_id)) selected.value = null
  }
  else error.value = eventResult.reason instanceof Error ? eventResult.reason.message : '无法读取审计事件。'
  if (jobResult.status === 'fulfilled' && jobResult.value) jobs.value = jobResult.value.items
  loading.value = false
}

function applyFilters(): void { page.value = 0; void load() }
function selectEvent(event: AdminAuditEvent): void { selected.value = event }
function identity(): string { return globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(16).slice(2)}` }
async function createExport(): Promise<void> {
  exportBusy.value = true; exportMessage.value = ''
  try {
    const job = await createAdminExport({ export_type: 'audit_events', fields: ['eventId', 'action', 'resourceType', 'resourceId', 'result', 'occurredAt', 'actorAdminUserId', 'ownerUserId', 'storeId'], from: range.value.from.toISOString(), to: range.value.to.toISOString(), reason: '管理员后台导出审计事件', idempotency_key: identity() })
    jobs.value = [job, ...jobs.value.filter((item) => item.export_id !== job.export_id)].slice(0, 5)
    exportMessage.value = `导出任务 ${job.status}`
    exportOpen.value = false
  } catch (reason) { exportMessage.value = reason instanceof Error ? reason.message : '创建导出任务失败。' } finally { exportBusy.value = false }
}
async function download(job: AdminExportJob): Promise<void> {
  try {
    const blob = await downloadAdminExport(job.export_id)
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url; anchor.download = `admin-audit-${job.export_id}.csv`; anchor.click()
    URL.revokeObjectURL(url)
    exportMessage.value = '导出文件已开始下载。'
  } catch (reason) { exportMessage.value = reason instanceof Error ? reason.message : '下载失败。' }
}
onMounted(load)
</script>

<template>
  <section>
    <AdminPageHeader eyebrow="SECURITY / AUDIT" title="操作审计" description="检索管理员登录、查看、权限和受控操作的服务端审计记录。">
      <template #actions>
        <button v-if="canExport" class="primary-button" type="button" @click="exportOpen = true">
          <FileDown aria-hidden="true" />创建导出
        </button>
        <button class="outline-button" type="button" :disabled="loading" @click="load">
          <RefreshCw :class="{ spinning: loading }" aria-hidden="true" />刷新
        </button>
      </template>
    </AdminPageHeader>
    <p v-if="exportMessage" class="export-message" role="status">{{ exportMessage }}</p>
    <div class="audit-grid">
      <section>
        <div class="filter-panel">
          <label aria-label="事件 ID">
            <Search aria-hidden="true" />
            <input v-model="eventId" placeholder="事件 ID" @keyup.enter="applyFilters" />
          </label>
          <label aria-label="操作类型">
            <Filter aria-hidden="true" />
            <input v-model="action" placeholder="操作类型" @keyup.enter="applyFilters" />
          </label>
          <select v-model="result" aria-label="结果">
            <option value="">全部结果</option>
            <option value="SUCCESS">成功</option>
            <option value="DENIED">拒绝</option>
            <option value="FAILED">失败</option>
          </select>
          <button class="outline-button" type="button" @click="applyFilters">筛选</button>
        </div>

        <section class="table-panel" aria-labelledby="audit-events-title" :aria-busy="loading || undefined">
          <header>
            <div>
              <h2 id="audit-events-title">审计事件</h2>
              <p>{{ formatNumber(total) }} 条记录 · 当前时间范围 {{ rangeDays }} 天</p>
            </div>
          </header>
          <StatePanel v-if="loading" state="loading" title="正在读取审计事件" detail="正在向服务端请求当前筛选范围。" />
          <StatePanel v-else-if="error" state="error" :detail="error" @retry="load" />
          <StatePanel v-else-if="!events.length" state="empty" title="暂无匹配审计事件" detail="可以调整事件 ID、操作类型或结果筛选条件。" />
          <div v-else class="table-scroll">
            <table>
              <thead>
                <tr><th>操作</th><th>资源</th><th>操作者</th><th>结果</th><th>时间</th></tr>
              </thead>
              <tbody>
                <tr
                  v-for="event in events"
                  :key="event.event_id"
                  :class="{ selected: selected?.event_id === event.event_id }"
                  :tabindex="0"
                  :aria-selected="selected?.event_id === event.event_id"
                  @click="selectEvent(event)"
                  @keydown.enter.prevent="selectEvent(event)"
                  @keydown.space.prevent="selectEvent(event)"
                >
                  <td><strong>{{ event.action || '未标记操作' }}</strong><small class="mono">{{ event.event_id }}</small></td>
                  <td><strong>{{ event.resource_type || '系统资源' }}</strong><small>{{ event.resource_id || '—' }}</small></td>
                  <td>{{ event.actor_admin_user_id || '匿名拒绝事件' }}</td>
                  <td><span class="result" :class="event.result === 'SUCCESS' ? 'result--success' : 'result--warning'">{{ event.result }}</span></td>
                  <td>{{ formatDateTime(event.occurred_at) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <footer v-if="!loading && !error && events.length">
            <span>第 {{ page + 1 }} 页</span>
            <div>
              <button type="button" :disabled="page <= 0" @click="page--; load()">上一页</button>
              <button type="button" :disabled="!hasNext" @click="page++; load()">下一页</button>
            </div>
          </footer>
        </section>

        <section class="distribution-panel" aria-labelledby="distribution-title" :aria-busy="loading || undefined">
          <header>
            <div>
              <h2 id="distribution-title">当前页动作分布</h2>
              <p>按当前加载页面的审计事件计数，最多展示四种动作。</p>
            </div>
            <BarChart3 aria-hidden="true" />
          </header>
          <StatePanel v-if="loading" state="loading" title="正在计算动作分布" />
          <StatePanel v-else-if="error" state="error" title="动作分布暂不可用" detail="审计事件读取失败后，无法计算当前页分布。" @retry="load" />
          <StatePanel v-else-if="!actionCounts.length" state="empty" title="暂无动作分布" detail="当前页没有可统计的审计动作。" />
          <template v-else>
            <p id="action-distribution-description" class="chart-summary">当前页共 {{ formatNumber(events.length) }} 条事件；条形长度表示动作出现次数，具体数值始终显示在条形右侧。</p>
            <div class="distribution-chart-wrap">
              <svg
                class="distribution-chart"
                :viewBox="`0 0 ${actionChart.width} ${actionChart.height}`"
                role="img"
                aria-labelledby="distribution-title action-distribution-description"
              >
                <title>当前页审计动作分布</title>
                <desc>显示当前审计页面中出现频率最高的动作及其事件数量。</desc>
                <g v-for="row in actionChart.rows" :key="row.name" class="distribution-row">
                  <title>{{ `${row.name}：${formatNumber(row.count)} 条` }}</title>
                  <text class="distribution-label" x="12" :y="row.y + 7">{{ row.label }}</text>
                  <rect class="distribution-track" :x="actionChart.barX" :y="row.y" :width="actionChart.barWidth" height="12" rx="6" />
                  <rect class="distribution-bar" :x="actionChart.barX" :y="row.y" :width="row.barWidth" height="12" rx="6" />
                  <text class="distribution-value" :x="actionChart.valueX" :y="row.y + 9" text-anchor="end">{{ formatNumber(row.count) }}</text>
                </g>
              </svg>
            </div>
            <table class="visually-hidden">
              <caption>当前页动作分布数据</caption>
              <thead><tr><th scope="col">操作</th><th scope="col">事件数</th></tr></thead>
              <tbody><tr v-for="[name, count] in actionCounts" :key="name"><th scope="row">{{ name }}</th><td>{{ count }}</td></tr></tbody>
            </table>
          </template>
        </section>
      </section>

      <aside class="audit-detail" aria-labelledby="audit-detail-title">
        <template v-if="selected">
          <header><p>EVENT DETAIL</p><h2 id="audit-detail-title">{{ selected.action || '未标记操作' }}</h2></header>
          <dl>
            <div><dt>事件 ID</dt><dd class="mono">{{ selected.event_id }}</dd></div>
            <div><dt>操作者</dt><dd>{{ selected.actor_admin_user_id || '匿名拒绝事件' }}</dd></div>
            <div><dt>角色</dt><dd>{{ selected.role || '—' }}</dd></div>
            <div><dt>范围</dt><dd>Owner {{ selected.owner_user_id || '—' }} / 门店 {{ selected.store_id || '—' }}</dd></div>
            <div><dt>IP</dt><dd>{{ selected.source_ip || '—' }}</dd></div>
            <div><dt>原因</dt><dd>{{ selected.reason || selected.summary || '—' }}</dd></div>
          </dl>
        </template>
        <div v-else class="detail-empty" aria-live="polite"><ShieldCheck aria-hidden="true" /><span id="audit-detail-title">选择审计事件以查看摘要。</span></div>
        <section v-if="canExport && jobs.length" class="export-jobs" aria-labelledby="export-jobs-title">
          <h3 id="export-jobs-title">最近导出</h3>
          <ul>
            <li v-for="job in jobs" :key="job.export_id">
              <div><strong>{{ job.status }}</strong><small>{{ formatDateTime(job.created_at) }}</small></div>
              <button v-if="job.download_url" type="button" aria-label="下载导出文件" @click="download(job)"><Download aria-hidden="true" /></button>
            </li>
          </ul>
        </section>
      </aside>
    </div>
    <ConfirmDialog :open="exportOpen" :busy="exportBusy" title="创建审计导出？" description="导出仅包含字段白名单和当前时间范围内已授权的数据。服务端会生成可过期的异步导出任务并写入审计。" confirm-label="创建任务" @cancel="exportOpen = false" @confirm="createExport" />
  </section>
</template>

<style scoped>
.primary-button, .outline-button { display: inline-flex; height: 32px; align-items: center; gap: 7px; border-radius: 999px; padding: 0 12px; cursor: pointer; font-size: 12px; }.primary-button { border: 1px solid var(--admin-foreground); background: var(--admin-foreground); color: #fff; }.outline-button { border: 1px solid var(--admin-border); background: #fff; color: var(--admin-foreground); }.outline-button:hover:not(:disabled) { background: var(--admin-secondary); }.outline-button:disabled { cursor: wait; opacity: .65; }.primary-button :deep(svg), .outline-button :deep(svg) { width: 14px; height: 14px; stroke-width: 1.7; }.spinning { animation: spin .8s linear infinite; }.export-message { margin: 17px 0 0; border-bottom: 1px solid var(--admin-border); padding-bottom: 10px; color: var(--admin-muted); font-size: 11px; }.audit-grid { display: grid; grid-template-columns: minmax(0, 1fr) 300px; gap: 8px; margin-top: 22px; }.filter-panel { display: grid; grid-template-columns: 1fr 1fr 120px auto; gap: 8px; }.filter-panel label { display: flex; min-width: 0; height: 32px; align-items: center; gap: 7px; border: 1px solid var(--admin-border); border-radius: 6px; background: #fff; padding: 0 9px; color: var(--admin-muted); }.filter-panel label :deep(svg) { width: 14px; height: 14px; }.filter-panel input, .filter-panel select { width: 100%; min-width: 0; border: 1px solid var(--admin-border); border-radius: 6px; background: #fff; padding: 0 9px; color: var(--admin-foreground); outline: 0; font-size: 11px; }.filter-panel label input { border: 0; padding: 0; }.table-panel, .distribution-panel, .audit-detail { border: 1px solid var(--admin-border); border-radius: 8px; background: #fff; }.table-panel { margin-top: 8px; padding: 18px; }.table-panel header, .distribution-panel header { display: flex; align-items: flex-start; justify-content: space-between; gap: 10px; }.table-panel h2, .distribution-panel h2, .audit-detail h2 { margin: 0; font-size: 14px; font-weight: 500; }.table-panel p, .distribution-panel p { margin: 5px 0 0; color: var(--admin-muted); font-size: 11px; }.table-scroll { overflow-x: auto; margin-top: 15px; }table { width: 100%; min-width: 780px; border-collapse: collapse; }th { height: 34px; border-bottom: 1px solid var(--admin-border); color: var(--admin-muted); font-size: 11px; font-weight: 400; text-align: left; }td { height: 60px; border-bottom: 1px solid var(--admin-border); padding-right: 9px; font-size: 11px; }tbody tr { cursor: pointer; }tbody tr:hover, tbody tr.selected { background: #fafaf8; }td strong, td small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }td strong { font-weight: 500; }td small { margin-top: 5px; color: var(--admin-muted); font-size: 10px; }.mono { color: #65659d; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 10px; }.result { display: inline-flex; border-radius: 999px; padding: 4px 7px; font-size: 10px; }.result--success { background: var(--admin-positive-bg); color: var(--admin-positive); }.result--warning { background: var(--admin-attention-bg); color: var(--admin-attention); }.empty { height: 120px; color: var(--admin-muted); text-align: center; }.table-panel footer { display: flex; align-items: center; justify-content: space-between; padding-top: 12px; color: var(--admin-muted); font-size: 10px; }.table-panel footer div { display: flex; gap: 6px; }.table-panel footer button { height: 28px; border: 1px solid var(--admin-border); border-radius: 5px; background: #fff; padding: 0 8px; cursor: pointer; font-size: 10px; }.table-panel footer button:disabled { cursor: not-allowed; opacity: .5; }.distribution-panel { margin-top: 8px; padding: 18px; }.distribution-panel header > :deep(svg) { width: 16px; height: 16px; color: var(--admin-muted); stroke-width: 1.7; }.distribution { display: grid; gap: 14px; margin-top: 21px; }.distribution > div { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 7px 10px; }.distribution span { overflow: hidden; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }.distribution strong { font-size: 11px; font-weight: 500; }.distribution i { grid-column: span 2; display: block; height: 6px; border-radius: 4px; background: var(--admin-secondary); overflow: hidden; }.distribution b { display: block; height: 100%; border-radius: inherit; background: #8ab8d7; }.distribution-empty { display: grid; min-height: 160px; place-content: center; gap: 8px; color: var(--admin-muted); text-align: center; font-size: 10px; }.distribution-empty :deep(svg) { width: 18px; height: 18px; margin: 0 auto; }.audit-detail { align-self: start; padding: 18px; }.audit-detail header p { margin: 0; color: var(--admin-muted); font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 9px; letter-spacing: .08em; }.audit-detail h2 { margin-top: 8px; overflow-wrap: anywhere; }.audit-detail dl { display: grid; margin: 20px 0 0; }.audit-detail dl div { border-bottom: 1px solid var(--admin-border); padding: 11px 0; }.audit-detail dt { color: var(--admin-muted); font-size: 10px; }.audit-detail dd { margin: 6px 0 0; font-size: 11px; line-height: 1.45; overflow-wrap: anywhere; }.detail-empty { display: grid; min-height: 235px; place-content: center; gap: 8px; color: var(--admin-muted); text-align: center; font-size: 11px; }.detail-empty :deep(svg) { width: 21px; height: 21px; margin: 0 auto; }.export-jobs { margin-top: 20px; border-top: 1px solid var(--admin-border); padding-top: 16px; }.export-jobs h3 { margin: 0; font-size: 11px; font-weight: 500; }.export-jobs ul { display: grid; gap: 0; margin: 10px 0 0; padding: 0; list-style: none; }.export-jobs li { display: flex; align-items: center; justify-content: space-between; gap: 8px; border-bottom: 1px solid var(--admin-border); padding: 9px 0; }.export-jobs strong, .export-jobs small { display: block; }.export-jobs strong { font-size: 10px; font-weight: 500; }.export-jobs small { margin-top: 3px; color: var(--admin-muted); font-size: 9px; }.export-jobs button { display: grid; width: 27px; height: 27px; place-items: center; border: 0; border-radius: 5px; background: transparent; color: var(--admin-muted); cursor: pointer; }.export-jobs button:hover { background: var(--admin-secondary); }.export-jobs button :deep(svg) { width: 14px; height: 14px; }@keyframes spin { to { transform: rotate(360deg); } }@media (max-width: 1020px) { .audit-grid { grid-template-columns: 1fr; }.audit-detail { min-height: auto; }}@media (max-width: 680px) { .filter-panel { grid-template-columns: 1fr 1fr; }.filter-panel > :last-child { grid-column: span 2; }}@media (max-width: 430px) { .filter-panel { grid-template-columns: 1fr; }.filter-panel > :last-child { grid-column: auto; }}
</style>

<style scoped>
.chart-summary { margin: 18px 0 0; color: var(--admin-muted); font-size: 10px; line-height: 1.5; }
.distribution-chart-wrap { overflow-x: auto; margin-top: 3px; }
.distribution-chart { display: block; width: 100%; min-width: 320px; height: auto; overflow: visible; }
.distribution-label, .distribution-value { fill: var(--admin-foreground); font-family: ui-sans-serif, system-ui, sans-serif; font-size: 11px; }
.distribution-label { fill: var(--admin-muted); }
.distribution-value { font-variant-numeric: tabular-nums; font-weight: 500; }
.distribution-track { fill: var(--admin-secondary); }
.distribution-bar { fill: #8ab8d7; }
.distribution-row:hover .distribution-bar, .distribution-row:focus-within .distribution-bar { fill: #5f9bc6; }
tbody tr:focus-visible, button:focus-visible, input:focus-visible, select:focus-visible { outline: 2px solid #5f9bc6; outline-offset: 2px; }
.visually-hidden { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); margin: -1px; padding: 0; border: 0; white-space: nowrap; clip-path: inset(50%); }
@media (prefers-reduced-motion: reduce) { .spinning { animation: none; } }
</style>
