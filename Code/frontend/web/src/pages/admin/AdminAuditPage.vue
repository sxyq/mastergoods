<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ChevronLeft, ChevronRight, Download, Eye, RefreshCw, Search, ShieldAlert } from 'lucide-vue-next'
import AdminLayout from '@/features/admin/components/AdminLayout.vue'
import AdminPanelState from '@/features/admin/components/AdminPanelState.vue'
import { useSession } from '@/app/stores/session'
import { useAdminSession } from '@/app/stores/admin-session'
import { createAdminExport, fetchAdminAuditEvents, fetchAdminExports, type AdminAuditEvent, type AdminExportJob } from '@/shared/api/admin'

const session = useSession()
const adminSession = useAdminSession()
const action = ref('')
const resourceType = ref('')
const page = ref(0)
const size = 20
const items = ref<AdminAuditEvent[]>([])
const total = ref(0)
const hasNext = ref(false)
const loading = ref(false)
const error = ref<unknown>(null)
const exportJobs = ref<AdminExportJob[]>([])
const exportLoading = ref(false)
const exportError = ref<unknown>(null)
const exportNotice = ref('')
const errorMessage = computed(() => error.value instanceof Error ? error.value.message : '管理员审计读取失败')
const exportErrorMessage = computed(() => exportError.value instanceof Error ? exportError.value.message : '导出任务读取失败')

async function load() {
  if (!session.token.value) { error.value = new Error('管理员会话已失效'); return }
  loading.value = true; error.value = null
  try { const result = await fetchAdminAuditEvents(session.token.value, { action: action.value || undefined, resourceType: resourceType.value || undefined, page: page.value, size }); items.value = result.items; total.value = result.total; hasNext.value = result.hasNext }
  catch (cause) { error.value = cause }
  finally { loading.value = false }
}
async function loadExports() {
  if (!session.token.value || !adminSession.can('admin.export')) return
  exportLoading.value = true; exportError.value = null
  try { exportJobs.value = (await fetchAdminExports(session.token.value, { page: 0, size: 10 })).items }
  catch (cause) { exportError.value = cause }
  finally { exportLoading.value = false }
}
async function requestExport() {
  if (!session.token.value || !adminSession.can('admin.export')) return
  const reason = window.prompt('请输入导出原因')?.trim()
  if (!reason) return
  exportLoading.value = true; exportError.value = null; exportNotice.value = ''
  try { await createAdminExport(session.token.value, { exportType: 'audit_events', fields: ['eventId', 'action', 'resourceType', 'resourceId', 'result', 'occurredAt'], reason, idempotencyKey: crypto.randomUUID() }); exportNotice.value = '导出任务已创建'; await loadExports() }
  catch (cause) { exportError.value = cause }
  finally { exportLoading.value = false }
}
function previous() { if (page.value > 0) { page.value -= 1; void load() } }
function next() { if (hasNext.value) { page.value += 1; void load() } }
function date(value?: string) { return value ? new Date(value).toLocaleString('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }) : '-' }
watch([action, resourceType], () => { page.value = 0; void load() })
onMounted(async () => { await adminSession.ensure(session.token.value); await Promise.all([load(), loadExports()]) })
</script>

<template>
  <AdminLayout active-id="audit"><section class="admin-page-v2"><header class="admin-page-v2__header"><div><div class="admin-page-v2__crumb">Admin / <strong>Audit</strong></div><h1>操作审计</h1><p>查询管理员动作、资源和服务端返回的结果摘要。</p></div><button class="admin-button-v2" type="button" :disabled="loading" @click="load"><RefreshCw :class="{ 'is-spinning': loading }" aria-hidden="true" />刷新</button></header><div v-if="error" class="admin-error-v2" role="alert"><ShieldAlert aria-hidden="true" /><span>{{ errorMessage }}</span><button type="button" @click="load">重试</button></div><div class="admin-grid"><article class="admin-card-v2 admin-span-12"><div class="admin-card-v2__header"><div><h2>事件记录</h2><p>正文与敏感字段默认不在审计列表中返回。</p></div><Eye aria-hidden="true" /></div><div class="admin-card-v2__body admin-toolbar"><label class="admin-field"><Search aria-hidden="true" /><input v-model="action" type="search" placeholder="动作，例如 read_run" aria-label="筛选动作" /></label><label class="admin-field"><select v-model="resourceType" aria-label="资源类型"><option value="">全部资源</option><option value="RUN">运行</option><option value="USER">用户</option><option value="STORE">门店</option><option value="CONFIG">配置</option></select></label></div><AdminPanelState v-if="loading" state="loading" title="正在读取审计事件" /><AdminPanelState v-else-if="error" state="error" :message="errorMessage" @retry="load" /><AdminPanelState v-else-if="items.length === 0" state="empty" title="当前范围暂无审计事件" message="服务端没有返回当前筛选条件下的审计记录。" /><div v-else class="admin-table-wrap"><table class="admin-table-v2"><thead><tr><th>时间</th><th>动作</th><th>资源</th><th>操作者</th><th>结果</th><th>摘要</th></tr></thead><tbody><tr v-for="item in items" :key="item.eventId"><td>{{ date(item.occurredAt) }}</td><td><strong>{{ item.action }}</strong></td><td>{{ item.resourceType || '-' }}<small>{{ item.resourceId || '-' }}</small></td><td><code>{{ item.actorAdminUserId || '-' }}</code></td><td><span class="admin-status-v2" :class="`admin-status-v2--${/success|ok|allow/i.test(item.result || '') ? 'ok' : /fail|deny/i.test(item.result || '') ? 'bad' : 'warn'}`">{{ item.result || '未知' }}</span></td><td>{{ item.summary || '无摘要' }}</td></tr></tbody></table></div><footer v-if="!loading && !error && total > 0" class="admin-pagination"><span>第 {{ page + 1 }} 页 · {{ total }} 条</span><button type="button" :disabled="page === 0" aria-label="上一页" @click="previous"><ChevronLeft aria-hidden="true" /></button><button type="button" :disabled="!hasNext" aria-label="下一页" @click="next"><ChevronRight aria-hidden="true" /></button></footer></article><article class="admin-card-v2 admin-span-12"><div class="admin-card-v2__header"><div><h2>脱敏导出</h2><p>仅提交字段白名单，服务端再次校验管理员范围和短期下载权限。</p></div><button v-if="adminSession.can('admin.export')" class="admin-button-v2" type="button" :disabled="exportLoading" @click="requestExport"><Download aria-hidden="true" />创建导出</button><span v-else class="admin-status-v2">当前会话无导出权限</span></div><div class="admin-card-v2__body"><AdminPanelState v-if="exportLoading && !exportJobs.length" state="loading" title="正在读取导出任务" /><AdminPanelState v-else-if="exportError" state="error" :message="exportErrorMessage" @retry="loadExports" /><AdminPanelState v-else-if="!adminSession.can('admin.export')" state="blocked" title="导出受控" message="当前管理员角色没有导出权限，页面不会提交导出请求。" /><AdminPanelState v-else-if="!exportJobs.length" state="empty" title="暂无导出任务" message="当前范围没有已创建的脱敏导出任务。" /><div v-else class="admin-table-wrap"><table class="admin-table-v2"><thead><tr><th>任务 ID</th><th>类型</th><th>状态</th><th>创建时间</th><th>过期时间</th></tr></thead><tbody><tr v-for="job in exportJobs" :key="job.exportId"><td><code>{{ job.exportId }}</code></td><td>{{ job.exportType || '-' }}</td><td>{{ job.status || '未知' }}</td><td>{{ date(job.createdAt) }}</td><td>{{ date(job.expiresAt) }}</td></tr></tbody></table></div><p v-if="exportNotice" role="status">{{ exportNotice }}</p></div></article></div></section></AdminLayout>
</template>
