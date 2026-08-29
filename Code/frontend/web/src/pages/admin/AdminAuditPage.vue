<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { ChevronLeft, ChevronRight, Clock3, Download, Eye, RefreshCw, Search, ShieldAlert, X } from 'lucide-vue-next'
import AdminLayout from '@/features/admin/components/AdminLayout.vue'
import AdminPanelState from '@/features/admin/components/AdminPanelState.vue'
import AdminScopeFilter from '@/features/admin/components/AdminScopeFilter.vue'
import { useSession } from '@/app/stores/session'
import { useAdminSession } from '@/app/stores/admin-session'
import { createAdminExport, downloadAdminExport, fetchAdminAuditEvents, fetchAdminExports, type AdminAuditEvent, type AdminExportJob } from '@/shared/api/admin'

const session = useSession()
const adminSession = useAdminSession()
const action = ref('')
const resourceType = ref('')
const result = ref('')
const eventId = ref('')
const from = ref('')
const to = ref('')
const ownerUserId = ref('')
const storeId = ref('')
const page = ref(0)
const size = 20
const items = ref<AdminAuditEvent[]>([])
const total = ref(0)
const hasNext = ref(false)
const loading = ref(false)
const error = ref<unknown>(null)
const selected = ref<AdminAuditEvent | null>(null)
const exportJobs = ref<AdminExportJob[]>([])
const exportLoading = ref(false)
const exportError = ref<unknown>(null)
const exportNotice = ref('')
const downloadingExportId = ref<string | null>(null)
const errorMessage = computed(() => error.value instanceof Error ? error.value.message : '管理员审计读取失败')
const exportErrorMessage = computed(() => exportError.value instanceof Error ? exportError.value.message : '导出任务读取失败')
const detailOpen = computed(() => selected.value !== null)

function date(value?: string | null) { return value ? new Date(value).toLocaleString('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }) : '-' }
function iso(value: string) { return value ? new Date(value).toISOString() : undefined }
function auditTone(value?: string | null) { return /success|ok|allow/i.test(value ?? '') ? 'ok' : /fail|deny|expired/i.test(value ?? '') ? 'bad' : 'warn' }
function auditLabel(value?: string | null) { return /success|ok|allow/i.test(value ?? '') ? '成功' : /fail|deny/i.test(value ?? '') ? '失败/拒绝' : value || '未知' }
function resetFilters() { eventId.value = ''; action.value = ''; resourceType.value = ''; result.value = ''; from.value = ''; to.value = ''; ownerUserId.value = ''; storeId.value = ''; page.value = 0; void load() }

async function load() {
  if (!session.token.value) { error.value = new Error('管理员会话已失效'); return }
  loading.value = true; error.value = null
  try {
    const response = await fetchAdminAuditEvents(session.token.value, { eventId: eventId.value.trim() || undefined, action: action.value.trim() || undefined, resourceType: resourceType.value || undefined, result: result.value || undefined, from: iso(from.value), to: iso(to.value), ownerUserId: ownerUserId.value || undefined, storeId: storeId.value || undefined, page: page.value, size })
    items.value = response.items; total.value = response.total; hasNext.value = response.hasNext
  } catch (cause) { error.value = cause; items.value = [] }
  finally { loading.value = false }
}

async function loadExports() {
  if (!session.token.value || !adminSession.can('admin.export')) return
  exportLoading.value = true; exportError.value = null
  try { exportJobs.value = (await fetchAdminExports(session.token.value, { page: 0, size: 10 })).items }
  catch (cause) { exportError.value = cause; exportJobs.value = [] }
  finally { exportLoading.value = false }
}

async function requestExport() {
  if (!session.token.value || !adminSession.can('admin.export') || exportLoading.value) return
  const reason = window.prompt('请输入导出原因')?.trim()
  if (!reason) return
  exportLoading.value = true; exportError.value = null; exportNotice.value = ''
  try {
    await createAdminExport(session.token.value, { exportType: 'audit_events', fields: ['eventId', 'action', 'resourceType', 'resourceId', 'result', 'occurredAt', 'actorAdminUserId', 'ownerUserId', 'storeId'], from: iso(from.value), to: iso(to.value), ownerUserId: ownerUserId.value || undefined, storeId: storeId.value || undefined, reason, idempotencyKey: crypto.randomUUID() })
    exportNotice.value = '导出任务已创建'; await loadExports()
  } catch (cause) { exportError.value = cause }
  finally { exportLoading.value = false }
}

async function download(job: AdminExportJob) {
  if (!session.token.value || !adminSession.can('admin.export') || job.status !== 'READY') return
  if (job.expiresAt && Date.parse(job.expiresAt) <= Date.now()) { exportError.value = new Error('导出任务已过期'); await loadExports(); return }
  downloadingExportId.value = job.exportId; exportError.value = null; exportNotice.value = ''
  try {
    const blob = await downloadAdminExport(session.token.value, job.exportId)
    const url = URL.createObjectURL(blob); const link = document.createElement('a'); link.href = url; link.download = `admin-export-${job.exportId}.csv`; document.body.appendChild(link); link.click(); link.remove(); URL.revokeObjectURL(url)
    exportNotice.value = '导出文件已下载'; await loadExports()
  } catch (cause) { exportError.value = cause }
  finally { downloadingExportId.value = null }
}

function previous() { if (page.value > 0) { page.value -= 1; void load() } }
function next() { if (hasNext.value) { page.value += 1; void load() } }
function closeDetail() { selected.value = null }
function handleEscape(event: KeyboardEvent) { if (event.key === 'Escape') closeDetail() }

watch([eventId, action, resourceType, result, from, to, ownerUserId, storeId], () => { page.value = 0; void load() })
onMounted(async () => { window.addEventListener('keydown', handleEscape); if (await adminSession.ensure(session.token.value)) await Promise.all([load(), loadExports()]) })
onUnmounted(() => window.removeEventListener('keydown', handleEscape))
</script>

<template>
  <AdminLayout active-id="audit"><section class="admin-page-v2"><header class="admin-page-v2__header"><div><div class="admin-page-v2__crumb">Admin / <strong>Audit</strong></div><h1>操作审计</h1><p>查询管理员动作、资源、结果、时间和访问来源。正文与敏感字段默认不返回。</p></div><button class="admin-button-v2" type="button" :disabled="loading" @click="load"><RefreshCw :class="{ 'is-spinning': loading }" aria-hidden="true" />刷新</button></header><div v-if="error" class="admin-error-v2" role="alert"><ShieldAlert aria-hidden="true" /><span>{{ errorMessage }}</span><button type="button" @click="load">重试</button></div><div class="admin-grid"><article class="admin-card-v2 admin-span-12"><div class="admin-card-v2__header"><div><h2>事件记录</h2><p>查询本身也会写入审计；服务端负责范围过滤和字段脱敏。</p></div><Eye aria-hidden="true" /></div><div class="admin-card-v2__body admin-audit-filters"><div class="admin-toolbar"><label class="admin-field"><Search aria-hidden="true" /><input v-model="eventId" type="search" placeholder="事件 ID（精确）" aria-label="按事件 ID 精确筛选" /></label><label class="admin-field"><Search aria-hidden="true" /><input v-model="action" type="search" placeholder="动作，例如 admin.user.update" aria-label="筛选动作" /></label><label class="admin-field"><select v-model="resourceType" aria-label="资源类型"><option value="">全部资源</option><option value="RUN">运行</option><option value="USER">用户</option><option value="STORE">门店</option><option value="CONFIG">配置</option><option value="EXPORT">导出</option><option value="SYSTEM">系统</option></select></label><label class="admin-field"><select v-model="result" aria-label="结果筛选"><option value="">全部结果</option><option value="SUCCESS">成功</option><option value="FAILED">失败</option><option value="DENIED">拒绝</option><option value="EXPIRED">过期</option></select></label></div><div class="admin-toolbar"><label class="admin-datetime"><Clock3 aria-hidden="true" /><span>开始</span><input v-model="from" type="datetime-local" aria-label="开始时间" /></label><label class="admin-datetime"><Clock3 aria-hidden="true" /><span>结束</span><input v-model="to" type="datetime-local" aria-label="结束时间" /></label><AdminScopeFilter v-model:owner-user-id="ownerUserId" v-model:store-id="storeId" :owner-user-ids="adminSession.session.value?.ownerUserIds" :store-ids="adminSession.session.value?.storeIds" /><button class="admin-button-v2" type="button" @click="resetFilters">清除筛选</button></div></div><AdminPanelState v-if="loading" state="loading" title="正在读取审计事件" /><AdminPanelState v-else-if="error" state="error" :message="errorMessage" @retry="load" /><AdminPanelState v-else-if="items.length === 0" state="empty" title="当前范围暂无审计事件" message="服务端没有返回当前筛选条件下的审计记录。" /><div v-else class="admin-table-wrap"><table class="admin-table-v2"><caption class="sr-only">管理员审计事件</caption><thead><tr><th>时间</th><th>动作</th><th>资源</th><th>操作者 / 角色</th><th>Owner / 门店</th><th>结果</th><th>摘要</th></tr></thead><tbody><tr v-for="item in items" :key="item.eventId" tabindex="0" @click="selected = item" @keydown.enter="selected = item"><td>{{ date(item.occurredAt) }}</td><td><strong>{{ item.action }}</strong></td><td>{{ item.resourceType || '-' }}<small><code>{{ item.resourceId || '-' }}</code></small></td><td><code>{{ item.actorAdminUserId || '-' }}</code><small>{{ item.role || '角色未提供' }}</small></td><td><code>{{ item.ownerUserId || '-' }}</code><small>{{ item.storeId || '-' }}</small></td><td><span class="admin-status-v2" :class="`admin-status-v2--${auditTone(item.result)}`">{{ auditLabel(item.result) }}</span></td><td>{{ item.summary || item.reason || '无摘要' }}</td></tr></tbody></table></div><footer v-if="!loading && !error && total > 0" class="admin-pagination"><span>第 {{ page + 1 }} 页 · {{ total }} 条</span><button type="button" :disabled="page === 0" aria-label="上一页" @click="previous"><ChevronLeft aria-hidden="true" /></button><button type="button" :disabled="!hasNext" aria-label="下一页" @click="next"><ChevronRight aria-hidden="true" /></button></footer></article>
      <article class="admin-card-v2 admin-span-12"><div class="admin-card-v2__header"><div><h2>脱敏导出</h2><p>仅提交字段白名单，服务端再次校验管理员范围和短期下载权限。</p></div><button v-if="adminSession.can('admin.export')" class="admin-button-v2" type="button" :disabled="exportLoading" @click="requestExport"><Download aria-hidden="true" />创建导出</button><span v-else class="admin-status-v2">当前会话无导出权限</span></div><div class="admin-card-v2__body"><AdminPanelState v-if="exportLoading && !exportJobs.length" state="loading" title="正在读取导出任务" /><AdminPanelState v-else-if="exportError" state="error" :message="exportErrorMessage" @retry="loadExports" /><AdminPanelState v-else-if="!adminSession.can('admin.export')" state="blocked" title="导出受控" message="当前管理员角色没有导出权限，页面不会提交导出请求。" /><AdminPanelState v-else-if="!exportJobs.length" state="empty" title="暂无导出任务" message="当前范围没有已创建的脱敏导出任务。" /><div v-else class="admin-table-wrap"><table class="admin-table-v2"><caption class="sr-only">脱敏导出任务</caption><thead><tr><th>任务 ID</th><th>类型</th><th>状态</th><th>字段</th><th>创建/完成</th><th>过期时间</th><th>下载次数</th><th>操作</th></tr></thead><tbody><tr v-for="job in exportJobs" :key="job.exportId"><td><code>{{ job.exportId }}</code></td><td>{{ job.exportType || '-' }}</td><td><span class="admin-status-v2" :class="`admin-status-v2--${job.status === 'READY' ? 'ok' : /fail|expired/i.test(job.status || '') ? 'bad' : 'warn'}`">{{ job.status || '未知' }}</span><small v-if="job.errorSummary">{{ job.errorSummary }}</small></td><td>{{ job.fields?.join(', ') || '-' }}<small v-if="job.contentRedacted">内容已脱敏</small></td><td>{{ date(job.createdAt) }}<small>{{ date(job.completedAt) }}</small></td><td>{{ date(job.expiresAt) }}</td><td>{{ job.downloadCount ?? 0 }}</td><td><button v-if="job.status === 'READY'" class="admin-button-v2 admin-button-v2--compact" type="button" :disabled="downloadingExportId === job.exportId" :aria-label="`下载导出 ${job.exportId}`" @click="download(job)"><Download aria-hidden="true" />{{ downloadingExportId === job.exportId ? '下载中' : '下载' }}</button><span v-else class="admin-card-v2__meta">不可下载</span></td></tr></tbody></table></div><p v-if="exportNotice" class="admin-success-note" role="status">{{ exportNotice }}</p></div></article></div></section>
    <button v-if="detailOpen" type="button" class="admin-detail-scrim" aria-label="关闭审计详情" @click="closeDetail" /><aside v-if="detailOpen" class="admin-detail-drawer admin-audit-drawer" aria-label="审计事件详情"><header class="admin-detail-drawer__head"><div><div class="admin-page-v2__crumb">AUDIT EVENT</div><h2>审计详情</h2><p><code>{{ selected?.eventId || '-' }}</code></p></div><button class="admin-detail-drawer__close" type="button" aria-label="关闭审计详情" title="关闭审计详情" @click="closeDetail"><X aria-hidden="true" /></button></header><div v-if="selected" class="admin-detail-drawer__body"><div class="admin-audit-detail-result"><span class="admin-status-v2" :class="`admin-status-v2--${auditTone(selected.result)}`">{{ auditLabel(selected.result) }}</span><span>{{ date(selected.occurredAt) }}</span></div><dl><dt>动作</dt><dd><code>{{ selected.action }}</code></dd><dt>资源</dt><dd>{{ selected.resourceType || '-' }} / <code>{{ selected.resourceId || '-' }}</code></dd><dt>操作者</dt><dd><code>{{ selected.actorAdminUserId || '-' }}</code> · {{ selected.role || '角色未提供' }}</dd><dt>Owner / 门店</dt><dd><code>{{ selected.ownerUserId || '-' }}</code> / <code>{{ selected.storeId || '-' }}</code></dd><dt>来源 IP</dt><dd>{{ selected.sourceIp || '服务端未提供' }}</dd><dt>请求 ID</dt><dd><code>{{ selected.requestId || '服务端未提供' }}</code></dd><dt>User-Agent</dt><dd>{{ selected.userAgentSummary || '服务端未提供' }}</dd><dt>原因</dt><dd>{{ selected.reason || '未提供原因' }}</dd><dt>摘要</dt><dd>{{ selected.summary || '无摘要' }}</dd></dl><p class="admin-detail-note"><Eye aria-hidden="true" /> 审计详情仅展示服务端脱敏字段，不包含密码、Cookie、Session Token、Provider key 或私钥。</p></div></aside>
  </AdminLayout>
</template>

<style scoped>
.admin-audit-filters { display: grid; gap: 10px; }
.admin-datetime { display: flex; min-height: 34px; align-items: center; gap: 7px; border: 1px solid #e8e8e5; border-radius: 7px; background: #fff; padding: 0 10px; color: #737373; font-size: 11px; }
.admin-datetime svg { width: 15px; color: #a0a0a0; }
.admin-datetime input { border: 0; outline: 0; color: #454545; font-size: 11px; }
.admin-success-note { color: #327a4b !important; }
.admin-audit-drawer { width: min(540px, 100vw); }
.admin-audit-detail-result { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 24px; color: #888; font-size: 11px; }
.admin-audit-drawer dl { display: grid; grid-template-columns: 112px 1fr; gap: 15px 16px; margin: 0; }
.admin-audit-drawer dt { color: #a0a0a0; font-size: 11px; }
.admin-audit-drawer dd { margin: 0; color: #424242; font-size: 12px; word-break: break-word; }
.admin-detail-note { display: flex; align-items: flex-start; gap: 8px; margin-top: 26px; border-top: 1px solid #f0f0ed; padding-top: 16px; color: #737373; font-size: 11px; line-height: 1.6; }
.admin-detail-note svg { width: 15px; flex: 0 0 auto; color: #a0a0a0; }
.sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; }
@media (max-width: 620px) { .admin-audit-drawer dl { grid-template-columns: 92px 1fr; }.admin-datetime { width: 100%; }.admin-datetime input { min-width: 0; flex: 1; } }
</style>
