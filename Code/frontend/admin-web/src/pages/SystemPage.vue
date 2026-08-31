<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Activity, Check, CircleAlert, Database, RefreshCw, Server, Settings2 } from 'lucide-vue-next'
import { hasAdminPermission } from '@/app/stores/admin-session'
import { isAdminApiError } from '@/shared/api/client'
import { getAdminHealth, getAdminRetentionPolicy, updateAdminRetentionPolicy, type AdminHealth, type AdminRetentionPolicy } from '@/shared/api/admin'
import { formatDateTime } from '@/shared/utils/format'
import AdminPageHeader from '@/shared/components/AdminPageHeader.vue'
import ConfirmDialog from '@/shared/components/ConfirmDialog.vue'
import StatePanel from '@/shared/components/StatePanel.vue'

const health = ref<AdminHealth | null>(null)
const policy = ref<AdminRetentionPolicy | null>(null)
const auditDays = ref(0); const messageDays = ref(0); const toolResultDays = ref(0); const metricsDays = ref(0); const contentMode = ref('')
const loading = ref(true); const healthLoading = ref(true); const retentionLoading = ref(true)
const healthError = ref(''); const retentionError = ref(''); const saving = ref(false); const saveError = ref(''); const confirmOpen = ref(false)
const canManageRetention = computed(() => hasAdminPermission('admin.system.retention.manage'))
const healthLabel = computed(() => {
  const status = health.value?.status
  if (!status || status === 'UNKNOWN') return '未知'
  if (status === 'UP' || status === 'HEALTHY' || status === 'OK') return '运行正常'
  if (status === 'WARN' || status === 'DEGRADED') return '需要关注'
  return status
})
const hasChanges = computed(() => !!policy.value && (auditDays.value !== policy.value.audit_days || messageDays.value !== policy.value.message_days || toolResultDays.value !== policy.value.tool_result_days || metricsDays.value !== policy.value.metrics_days || contentMode.value !== policy.value.content_mode))
const errorEntries = computed(() => health.value?.errors ?? [])
const errorChart = computed(() => {
  const width = 420
  const top = 16
  const rowHeight = 52
  const railX = 14
  const height = Math.max(74, top + Math.max(errorEntries.value.length, 1) * rowHeight)
  return {
    width,
    height,
    railX,
    rows: errorEntries.value.map((entry, index) => ({ ...entry, y: top + index * rowHeight })),
  }
})
const errorChartDescription = computed(() => {
  const count = errorEntries.value.length
  return count ? `当前健康检查返回 ${count} 条错误摘要；时间线按服务端返回顺序展示，完整字段见下方数据表。` : '当前健康检查没有返回错误摘要。'
})
function applyPolicy(value: AdminRetentionPolicy): void { policy.value = value; auditDays.value = value.audit_days; messageDays.value = value.message_days; toolResultDays.value = value.tool_result_days; metricsDays.value = value.metrics_days; contentMode.value = value.content_mode; saveError.value = '' }
function compact(value: string, limit = 34): string { return value.length > limit ? `${Array.from(value).slice(0, limit - 1).join('')}…` : value }
async function load(): Promise<void> {
  loading.value = true; healthLoading.value = true; retentionLoading.value = true; healthError.value = ''; retentionError.value = ''
  const [healthResult, retentionResult] = await Promise.allSettled([getAdminHealth(), getAdminRetentionPolicy()])
  if (healthResult.status === 'fulfilled') health.value = healthResult.value
  else healthError.value = healthResult.reason instanceof Error ? healthResult.reason.message : '无法读取系统健康状态。'
  if (retentionResult.status === 'fulfilled') applyPolicy(retentionResult.value)
  else retentionError.value = retentionResult.reason instanceof Error ? retentionResult.reason.message : '无法读取数据保留策略。'
  healthLoading.value = false; retentionLoading.value = false; loading.value = false
}
function identity(): string { return globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(16).slice(2)}` }
function validRetentionDays(): boolean { return [auditDays.value, messageDays.value, toolResultDays.value, metricsDays.value].every((value) => Number.isInteger(value) && value > 0) }
async function save(): Promise<void> {
  if (!policy.value || saving.value || !validRetentionDays()) {
    if (!validRetentionDays()) saveError.value = '保留天数必须是大于 0 的整数。'
    return
  }
  saving.value = true; saveError.value = ''
  try {
    applyPolicy(await updateAdminRetentionPolicy({ audit_days: auditDays.value, message_days: messageDays.value, tool_result_days: toolResultDays.value, metrics_days: metricsDays.value, content_mode: contentMode.value, expected_version: policy.value.version, idempotency_key: identity(), reason: '管理员后台更新数据保留策略', confirmed: true }))
    confirmOpen.value = false
  } catch (reason) {
    const message = reason instanceof Error ? reason.message : '保留策略更新失败。'
    const status = isAdminApiError(reason) ? reason.status : undefined
    saveError.value = status === 409 || /409|conflict|版本/i.test(message) ? '保留策略版本已变化，请刷新后重新编辑。' : status === 401 || status === 403 || /401|403|unauthori|forbidden|权限/i.test(message) ? '当前会话没有更新保留策略的权限。' : message
  } finally { saving.value = false }
}
function statusClass(status: string): string { return status === 'UP' || status === 'HEALTHY' || status === 'OK' ? 'good' : status === 'WARN' || status === 'DEGRADED' ? 'warn' : 'bad' }
onMounted(load)
</script>

<template>
  <section>
    <AdminPageHeader eyebrow="SYSTEM / HEALTH" title="系统状态" description="查看服务健康、最近错误摘要和数据保留策略。">
      <template #actions>
        <button class="outline-button" type="button" :disabled="loading" @click="load">
          <RefreshCw :class="{ spinning: loading }" aria-hidden="true" />刷新
        </button>
        <button v-if="canManageRetention" class="primary-button" type="button" :disabled="!hasChanges || saving" @click="confirmOpen = true">
          <Check aria-hidden="true" />保存策略
        </button>
      </template>
    </AdminPageHeader>

    <section class="health-banner" :class="`health--${statusClass(health?.status || '')}`" aria-label="系统健康状态" :aria-busy="healthLoading || undefined">
      <StatePanel v-if="healthLoading" state="loading" title="正在检查系统健康" detail="正在读取当前健康检查响应。" />
      <StatePanel v-else-if="healthError" state="error" :detail="healthError" @retry="load" />
      <template v-else-if="health">
        <div><Activity aria-hidden="true" /><span><strong id="health-status-title">{{ healthLabel }}</strong><small>版本 {{ health.version || '—' }} · 检查于 {{ formatDateTime(health.generated_at) }}</small></span></div>
        <Server aria-hidden="true" />
      </template>
      <StatePanel v-else state="empty" title="暂无健康状态" detail="当前响应没有可展示的系统健康信息。" />
    </section>

    <div class="system-grid">
      <section class="panel components-panel" aria-labelledby="components-title" :aria-busy="healthLoading || undefined">
        <header><div><h2 id="components-title">服务组件</h2><p>来自当前健康检查响应。</p></div><Server aria-hidden="true" /></header>
        <StatePanel v-if="healthLoading" state="loading" title="正在读取组件状态" />
        <StatePanel v-else-if="healthError" state="error" title="组件状态暂不可用" :detail="healthError" @retry="load" />
        <div v-else-if="health?.components?.length" class="component-list">
          <div v-for="component in health.components" :key="component.service_name">
            <span class="component-status" role="img" :aria-label="`${component.service_name} 状态 ${component.status}`" :class="`component-status--${statusClass(component.status)}`" />
            <div><strong>{{ component.service_name }}</strong><small>{{ component.error_summary || component.version || '无错误摘要' }}</small></div>
            <span class="component-meta">{{ component.status }}<template v-if="component.queue_depth !== null"> · 队列 {{ component.queue_depth }}</template></span>
          </div>
        </div>
        <StatePanel v-else state="empty" title="暂无组件状态" detail="当前健康响应没有返回组件列表。" />
      </section>

      <section class="panel errors-panel" aria-labelledby="errors-title" :aria-busy="healthLoading || undefined">
        <header><div><h2 id="errors-title">最近错误摘要</h2><p>当前健康响应中的错误，不表示完整历史。</p></div><CircleAlert aria-hidden="true" /></header>
        <StatePanel v-if="healthLoading" state="loading" title="正在读取错误时间线" />
        <StatePanel v-else-if="healthError" state="error" title="错误时间线暂不可用" :detail="healthError" @retry="load" />
        <StatePanel v-else-if="!errorEntries.length" state="empty" title="当前没有错误摘要" detail="当前健康检查没有返回错误记录。" />
        <template v-else>
          <p id="error-timeline-description" class="chart-summary">{{ errorChartDescription }}</p>
          <div class="error-chart-scroll">
            <svg class="error-timeline-chart" :viewBox="`0 0 ${errorChart.width} ${errorChart.height}`" role="img" aria-labelledby="errors-title error-timeline-description">
              <title>系统错误时间线</title>
              <desc>{{ errorChartDescription }}</desc>
              <line class="error-rail" :x1="errorChart.railX" :x2="errorChart.railX" y1="16" :y2="errorChart.height - 10" />
              <g v-for="entry in errorChart.rows" :key="`${entry.component}-${entry.occurred_at}-${entry.category}`" class="error-point">
                <title>{{ `${entry.component} · ${entry.category}：${entry.summary}` }}</title>
                <circle :cx="errorChart.railX" :cy="entry.y" r="4" />
                <text class="error-label" x="32" :y="entry.y - 2">{{ compact(`${entry.component} · ${entry.category}`) }}</text>
                <text class="error-date" x="32" :y="entry.y + 14">{{ formatDateTime(entry.occurred_at) }}</text>
              </g>
            </svg>
          </div>
          <table class="visually-hidden">
            <caption>系统错误时间线数据</caption>
            <thead><tr><th scope="col">组件</th><th scope="col">类别</th><th scope="col">摘要</th><th scope="col">发生时间</th></tr></thead>
            <tbody><tr v-for="entry in errorEntries" :key="`${entry.component}-${entry.occurred_at}-${entry.category}-table`"><th scope="row">{{ entry.component }}</th><td>{{ entry.category }}</td><td>{{ entry.summary }}</td><td>{{ formatDateTime(entry.occurred_at) }}</td></tr></tbody>
          </table>
        </template>
      </section>

      <section class="panel retention-panel" aria-labelledby="retention-title" :aria-busy="retentionLoading || undefined">
        <header><div><h2 id="retention-title">数据保留</h2><p>当前版本 {{ policy?.version ?? '—' }} · {{ formatDateTime(policy?.effective_at) }}</p></div><Database aria-hidden="true" /></header>
        <StatePanel v-if="retentionLoading" state="loading" title="正在读取保留策略" />
        <StatePanel v-else-if="retentionError" state="error" :detail="retentionError" @retry="load" />
        <StatePanel v-else-if="!policy" state="empty" title="暂无保留策略" detail="当前响应没有可编辑的数据保留策略。" />
        <template v-else>
          <div class="retention-fields">
            <label>审计记录<input v-model.number="auditDays" type="number" min="1" step="1" :disabled="!canManageRetention || saving" /><small>天</small></label>
            <label>消息摘要<input v-model.number="messageDays" type="number" min="1" step="1" :disabled="!canManageRetention || saving" /><small>天</small></label>
            <label>工具结果<input v-model.number="toolResultDays" type="number" min="1" step="1" :disabled="!canManageRetention || saving" /><small>天</small></label>
            <label>统计数据<input v-model.number="metricsDays" type="number" min="1" step="1" :disabled="!canManageRetention || saving" /><small>天</small></label>
          </div>
          <label class="content-mode">内容策略<select v-model="contentMode" :disabled="!canManageRetention || saving"><option value="REDACTED">REDACTED</option><option value="METADATA_ONLY">METADATA_ONLY</option><option value="FULL_ALLOWED">FULL_ALLOWED</option></select></label>
          <p v-if="saveError" class="save-error" role="alert">{{ saveError }}</p>
          <p class="policy-note"><Settings2 aria-hidden="true" />变更由服务端验证版本、权限和确认信息后才会生效。</p>
        </template>
      </section>
    </div>
    <ConfirmDialog :open="confirmOpen" :busy="saving" danger title="确认更新数据保留策略？" description="策略会影响未来清理批次的保留范围。服务端将记录变更原因、版本和执行结果。" confirm-label="确认更新" @cancel="confirmOpen = false" @confirm="save" />
  </section>
</template>

<style scoped>
.primary-button, .outline-button { display: inline-flex; height: 32px; align-items: center; gap: 7px; border-radius: 999px; padding: 0 12px; cursor: pointer; font-size: 12px; }.primary-button { border: 1px solid var(--admin-foreground); background: var(--admin-foreground); color: #fff; }.outline-button { border: 1px solid var(--admin-border); background: #fff; color: var(--admin-foreground); }.outline-button:hover:not(:disabled) { background: var(--admin-secondary); }.primary-button:disabled, .outline-button:disabled { cursor: wait; opacity: .65; }.primary-button :deep(svg), .outline-button :deep(svg) { width: 14px; height: 14px; stroke-width: 1.7; }.spinning { animation: spin .8s linear infinite; }.health-banner { display: flex; align-items: center; justify-content: space-between; gap: 14px; margin-top: 22px; border: 1px solid var(--admin-border); border-radius: 8px; background: #fff; padding: 16px 18px; }.health-banner > div { display: flex; align-items: center; gap: 10px; }.health-banner > div > :deep(svg), .health-banner > :deep(svg) { width: 19px; height: 19px; }.health-banner span { display: grid; gap: 4px; }.health-banner strong { font-size: 13px; font-weight: 500; }.health-banner small { color: var(--admin-muted); font-size: 10px; }.health--good > div > :deep(svg) { color: var(--admin-positive); }.health--warn > div > :deep(svg) { color: var(--admin-attention); }.health--bad > div > :deep(svg) { color: var(--admin-danger); }.system-grid { display: grid; grid-template-columns: minmax(0, 1.1fr) minmax(0, .9fr); gap: 8px; margin-top: 8px; }.panel { border: 1px solid var(--admin-border); border-radius: 8px; background: #fff; padding: 18px; }.panel header { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }.panel h2 { margin: 0; font-size: 14px; font-weight: 500; }.panel header p { margin: 5px 0 0; color: var(--admin-muted); font-size: 11px; }.panel header > :deep(svg) { width: 16px; height: 16px; color: var(--admin-muted); }.component-list { display: grid; margin-top: 20px; }.component-list > div { display: grid; grid-template-columns: 10px minmax(0, 1fr) auto; align-items: center; gap: 9px; border-bottom: 1px solid var(--admin-border); padding: 12px 0; }.component-list > div:last-child { border-bottom: 0; }.component-status { width: 7px; height: 7px; border-radius: 50%; }.component-status--good { background: var(--admin-positive); }.component-status--warn { background: var(--admin-attention); }.component-status--bad { background: var(--admin-danger); }.component-list strong, .component-list small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.component-list strong { font-size: 11px; font-weight: 500; }.component-list small { margin-top: 4px; color: var(--admin-muted); font-size: 10px; }.component-meta { color: var(--admin-muted); font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 9px; text-align: right; }.empty-row { display: block !important; padding: 35px 0 !important; color: var(--admin-muted); text-align: center; font-size: 11px; }.errors-panel ol { display: grid; margin: 21px 0 0; padding: 0; list-style: none; }.errors-panel li { display: grid; grid-template-columns: 8px minmax(0, 1fr); gap: 9px; border-left: 1px solid var(--admin-border); padding: 0 0 16px 11px; }.errors-panel li span { width: 7px; height: 7px; margin-left: -16px; border-radius: 50%; background: var(--admin-attention); }.errors-panel strong, .errors-panel small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.errors-panel strong { font-size: 10px; font-weight: 500; }.errors-panel small { margin-top: 5px; color: var(--admin-muted); font-size: 9px; }.errors-panel .empty-error { display: block; border: 0; padding: 34px 0; color: var(--admin-muted); text-align: center; font-size: 11px; }.retention-panel { grid-column: span 2; }.retention-fields { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 8px; margin-top: 22px; }.retention-fields label { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 5px; border: 1px solid var(--admin-border); border-radius: 6px; padding: 9px; color: var(--admin-muted); font-size: 10px; }.retention-fields input { grid-column: 1; width: 100%; border: 0; background: transparent; padding: 2px 0 0; color: var(--admin-foreground); font-size: 17px; font-variant-numeric: tabular-nums; outline: 0; }.retention-fields small { align-self: end; font-size: 9px; }.content-mode { display: grid; gap: 7px; margin-top: 16px; color: var(--admin-muted); font-size: 10px; }.content-mode select { height: 32px; border: 1px solid var(--admin-border); border-radius: 6px; background: #fff; padding: 0 8px; color: var(--admin-foreground); font-size: 11px; }.save-error { margin: 12px 0 0; color: var(--admin-danger); font-size: 10px; }.policy-note { display: flex; gap: 6px; margin: 15px 0 0; color: var(--admin-muted); font-size: 10px; line-height: 1.45; }.policy-note :deep(svg) { width: 14px; height: 14px; flex: 0 0 14px; }@keyframes spin { to { transform: rotate(360deg); } }@media (max-width: 900px) { .system-grid { grid-template-columns: 1fr; }.retention-panel { grid-column: auto; }.retention-fields { grid-template-columns: repeat(2, minmax(0, 1fr)); }}@media (max-width: 460px) { .retention-fields { grid-template-columns: 1fr; }}
</style>

<style scoped>
.health-banner > :deep(.state-panel), .panel > :deep(.state-panel) { min-height: 150px; margin: 0; }
.chart-summary { margin: 18px 0 0; color: var(--admin-muted); font-size: 10px; line-height: 1.5; }
.error-chart-scroll { overflow-x: auto; margin-top: 4px; }
.error-timeline-chart { display: block; width: 100%; min-width: 360px; height: auto; overflow: visible; }
.error-rail { stroke: var(--admin-border); stroke-width: 1; }
.error-point circle { fill: var(--admin-attention); stroke: #fff; stroke-width: 2; }
.error-label, .error-date { fill: var(--admin-foreground); font-family: ui-sans-serif, system-ui, sans-serif; font-size: 11px; }
.error-date { fill: var(--admin-muted); font-size: 10px; }
.retention-fields input:invalid { color: var(--admin-danger); }
button:focus-visible, input:focus-visible, select:focus-visible { outline: 2px solid #5f9bc6; outline-offset: 2px; }
.visually-hidden { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); margin: -1px; padding: 0; border: 0; white-space: nowrap; clip-path: inset(50%); }
@media (prefers-reduced-motion: reduce) { .spinning { animation: none; } }
</style>
