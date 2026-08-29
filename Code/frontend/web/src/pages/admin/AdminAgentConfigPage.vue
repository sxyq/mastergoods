<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Bot, Check, CircleAlert, History, RefreshCw, ShieldAlert, Wrench } from 'lucide-vue-next'
import AdminLayout from '@/features/admin/components/AdminLayout.vue'
import AdminPanelState from '@/features/admin/components/AdminPanelState.vue'
import AdminStatusBadge from '@/features/admin/components/AdminStatusBadge.vue'
import { useSession } from '@/app/stores/session'
import { fetchAdminAuditEvents, fetchAdminConfig, updateAdminConfig, type AdminAuditEvent, type AdminConfigPayload } from '@/shared/api/admin'
import { useAdminSession } from '@/app/stores/admin-session'

const session = useSession()
const adminSession = useAdminSession()
const loading = ref(false)
const error = ref<unknown>(null)
const config = ref<AdminConfigPayload | null>(null)
const modelId = ref('')
const agentEnabled = ref(false)
const enabledTools = ref<string[]>([])
const toolDraft = ref('')
const saving = ref(false)
const notice = ref('')
const history = ref<AdminAuditEvent[]>([])
const historyLoading = ref(false)
const historyError = ref<unknown>(null)
const errorMessage = computed(() => error.value instanceof Error ? error.value.message : 'Agent 配置读取失败')
const historyErrorMessage = computed(() => historyError.value instanceof Error ? historyError.value.message : '配置变更历史读取失败')
const canManage = computed(() => adminSession.can('admin.agent.config.manage'))

async function load() {
  if (!session.token.value) { error.value = new Error('管理员会话已失效'); return }
  loading.value = true; error.value = null
  try {
    config.value = await fetchAdminConfig(session.token.value)
    modelId.value = config.value.modelId || ''
    agentEnabled.value = config.value.agentEnabled
    enabledTools.value = [...(config.value.enabledTools || [])]
  } catch (cause) { error.value = cause; config.value = null }
  finally { loading.value = false }
}

async function loadHistory() {
  if (!session.token.value || !adminSession.can('admin.agent.config.read')) return
  historyLoading.value = true; historyError.value = null
  try { history.value = (await fetchAdminAuditEvents(session.token.value, { action: 'admin.agent.config.update', page: 0, size: 10 })).items }
  catch (cause) { historyError.value = cause; history.value = [] }
  finally { historyLoading.value = false }
}

function addTool() {
  const value = toolDraft.value.trim()
  if (!value || enabledTools.value.includes(value)) return
  enabledTools.value = [...enabledTools.value, value]
  toolDraft.value = ''
}

function removeTool(value: string) { enabledTools.value = enabledTools.value.filter((tool) => tool !== value) }

async function save() {
  if (!session.token.value || !config.value || !canManage.value || saving.value) return
  const reason = window.prompt('请输入配置变更原因')?.trim()
  if (!reason || !window.confirm('确认提交模型、工具和运行开关变更？')) return
  saving.value = true; notice.value = ''; error.value = null
  try {
    config.value = await updateAdminConfig(session.token.value, { modelId: modelId.value.trim() || undefined, agentEnabled: agentEnabled.value, enabledTools: enabledTools.value, expectedVersion: config.value.version, idempotencyKey: crypto.randomUUID(), reason, confirmed: true })
    modelId.value = config.value.modelId || ''; agentEnabled.value = config.value.agentEnabled; enabledTools.value = [...(config.value.enabledTools || [])]; notice.value = `配置 v${config.value.version} 已保存，状态：${config.value.effectiveState || '未提供'}`
    await loadHistory()
  } catch (cause) { error.value = cause; await load() }
  finally { saving.value = false }
}

function date(value?: string) { return value ? new Date(value).toLocaleString('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }) : '-' }
function auditTone(result?: string | null) { return /success|ok|allow/i.test(result ?? '') ? 'ok' : /fail|deny/i.test(result ?? '') ? 'bad' : 'warn' }
function auditLabel(result?: string | null) { return /success|ok|allow/i.test(result ?? '') ? '成功' : /fail|deny/i.test(result ?? '') ? '失败' : result || '未知' }

onMounted(async () => { if (await adminSession.ensure(session.token.value)) await Promise.all([load(), loadHistory()]) })
</script>

<template>
  <AdminLayout active-id="config">
    <section class="admin-page-v2"><header class="admin-page-v2__header"><div><div class="admin-page-v2__crumb">Admin / Agent / <strong>Configuration</strong></div><h1>Agent 配置</h1><p>查看并受控修改模型、工具白名单和运行开关。页面不展示 Provider 密钥或认证字段。</p></div><div class="admin-page-v2__actions"><button v-if="canManage" class="admin-button-v2 admin-button-v2--dark" type="button" :disabled="loading || saving || !config" @click="save"><Check aria-hidden="true" />保存变更</button><button class="admin-button-v2" type="button" :disabled="loading || saving" @click="load"><RefreshCw :class="{ 'is-spinning': loading }" aria-hidden="true" />刷新</button></div></header>
      <div v-if="error" class="admin-error-v2" role="alert"><ShieldAlert aria-hidden="true" /><span>{{ errorMessage }}</span><button type="button" @click="load">重试</button></div>
      <p v-if="notice" class="admin-success-note" role="status"><Check aria-hidden="true" /> {{ notice }}</p>
      <div class="admin-grid"><article class="admin-card-v2 admin-span-7"><div class="admin-card-v2__header"><div><h2>当前配置</h2><p>配置版本由服务端控制，保存前会检查并发版本。</p></div><Bot aria-hidden="true" /></div><AdminPanelState v-if="loading" state="loading" title="正在读取 Agent 配置" /><AdminPanelState v-else-if="error" state="error" :message="errorMessage" @retry="load" /><AdminPanelState v-else-if="!config" state="empty" title="尚未返回配置" message="服务端没有返回当前配置，不使用本地示例内容。" /><div v-else class="admin-card-v2__body"><dl><dt>模型 ID</dt><dd><input v-if="canManage" v-model="modelId" class="admin-inline-input" aria-label="模型 ID" /><span v-else>{{ config.modelId || '-' }}</span></dd><dt>Agent 开关</dt><dd><label v-if="canManage" class="admin-toggle"><input v-model="agentEnabled" type="checkbox" /><span>{{ agentEnabled ? '已启用' : '已停用' }}</span></label><AdminStatusBadge v-else :status="config.agentEnabled ? 'completed' : 'attention'" :label="config.agentEnabled ? '已启用' : '已停用'" /></dd><dt>配置版本</dt><dd><code>v{{ config.version }}</code></dd><dt>生效状态</dt><dd>{{ config.effectiveState || '-' }}<small v-if="config.effectiveAt">{{ date(config.effectiveAt) }}</small></dd><dt>最后更新</dt><dd><code>{{ config.updatedBy || '服务端未提供' }}</code></dd></dl><div class="admin-tools-editor"><div class="admin-tools-editor__heading"><div><h3><Wrench aria-hidden="true" />工具目录与启用状态</h3><p>工具名必须由服务端 ToolRegistry 校验，输入未知工具会被拒绝。</p></div><span class="admin-card-v2__meta">{{ enabledTools.length }} 个启用</span></div><div class="admin-tool-chips"><span v-for="tool in enabledTools" :key="tool" class="admin-tool-chip"><code>{{ tool }}</code><button v-if="canManage" type="button" :aria-label="`移除工具 ${tool}`" title="移除工具" @click="removeTool(tool)">&times;</button></span><span v-if="!enabledTools.length" class="admin-card-v2__meta">服务端没有返回启用工具。</span></div><div v-if="canManage" class="admin-tool-add"><input v-model="toolDraft" class="admin-inline-input" aria-label="工具名称" placeholder="输入已注册工具名" @keydown.enter.prevent="addTool" /><button class="admin-button-v2 admin-button-v2--compact" type="button" @click="addTool"><Wrench aria-hidden="true" />添加</button></div></div><p><CircleAlert aria-hidden="true" /> Provider key、Cookie、Session token、密码和私钥不会进入页面状态；配置变更会生成审计事件。</p></div></article><article class="admin-card-v2 admin-span-5 admin-card-v2--pad"><div class="admin-section-kicker"><ShieldAlert aria-hidden="true" />权限边界</div><h2>{{ canManage ? '当前会话可提交配置' : '当前会话只读' }}</h2><p>观察员可以查看模型、开关和工具元数据，但不能提交变更。服务端会再次校验角色、范围、版本和工具白名单。</p><dl class="admin-boundary-list"><dt>读取权限</dt><dd>{{ adminSession.can('admin.agent.config.read') ? '已授予' : '未授予' }}</dd><dt>写入权限</dt><dd>{{ canManage ? '已授予' : '未授予' }}</dd><dt>内容策略</dt><dd>仅元数据，不包含认证信息</dd></dl></article></div>
      <div class="admin-grid"><article class="admin-card-v2 admin-span-12"><div class="admin-card-v2__header"><div><h2>配置变更历史</h2><p>按 `admin.agent.config.update` 审计事件展示原因、版本和结果。</p></div><History aria-hidden="true" /></div><AdminPanelState v-if="historyLoading" state="loading" title="正在读取配置历史" /><AdminPanelState v-else-if="historyError" state="error" :message="historyErrorMessage" @retry="loadHistory" /><AdminPanelState v-else-if="!history.length" state="empty" title="暂无配置变更" message="服务端没有返回配置变更审计记录。" /><div v-else class="admin-history-list"><div v-for="item in history" :key="item.eventId"><div><strong>{{ date(item.occurredAt) }}</strong><span class="admin-status-v2" :class="`admin-status-v2--${auditTone(item.result)}`">{{ auditLabel(item.result) }}</span></div><p><code>{{ item.eventId || '-' }}</code> · 操作者 <code>{{ item.actorAdminUserId || '-' }}</code> · {{ item.summary || '无摘要' }}</p><small>{{ item.reason || '未提供原因' }}{{ item.requestId ? ` · request ${item.requestId}` : '' }}</small></div></div></article></div></section>
  </AdminLayout>
</template>

<style scoped>
dl { display: grid; grid-template-columns: 112px 1fr; gap: 15px 16px; margin: 0; }
dt { color: #a0a0a0; font-size: 11px; }
dd { margin: 0; color: #424242; font-size: 12px; word-break: break-word; }
dd small { display: block; margin-top: 4px; color: #8d8d89; font-size: 10px; }
p svg { width: 14px; vertical-align: -3px; }
.admin-inline-input { width: 100%; max-width: 420px; border: 1px solid #e8e8e5; border-radius: 6px; padding: 7px 9px; font-size: 12px; }
.admin-toggle { display: inline-flex; align-items: center; gap: 8px; }
.admin-success-note { max-width: 1400px; margin: 0 auto 14px; color: #327a4b; font-size: 12px; }
.admin-success-note svg { width: 14px; margin-right: 5px; vertical-align: -3px; }
.admin-tools-editor { margin-top: 24px; border-top: 1px solid #f0f0ed; padding-top: 18px; }
.admin-tools-editor__heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.admin-tools-editor h3 { display: flex; align-items: center; gap: 7px; margin: 0; font-size: 13px; }
.admin-tools-editor h3 svg { width: 15px; color: #8063a8; }
.admin-tools-editor p { margin-top: 5px; font-size: 11px; }
.admin-tool-chips { display: flex; flex-wrap: wrap; gap: 7px; margin-top: 14px; }
.admin-tool-chip { display: inline-flex; align-items: center; gap: 7px; border: 1px solid #dddcd8; border-radius: 999px; background: #fbfbfa; padding: 5px 8px 5px 10px; color: #555; font-size: 10px; }
.admin-tool-chip button { width: 16px; height: 16px; border: 0; border-radius: 50%; background: #ededeb; color: #777; font-size: 13px; line-height: 14px; cursor: pointer; }
.admin-tool-add { display: flex; gap: 8px; margin-top: 12px; }
.admin-tool-add .admin-inline-input { max-width: 260px; }
.admin-section-kicker { display: flex; align-items: center; gap: 7px; color: #8d8d89; font-size: 10px; letter-spacing: .08em; text-transform: uppercase; }
.admin-section-kicker svg { width: 15px; }
.admin-boundary-list { margin-top: 22px; }
.admin-history-list { display: grid; }
.admin-history-list > div { border-bottom: 1px solid #f0f0ed; padding: 14px 20px; }
.admin-history-list > div:last-child { border-bottom: 0; }
.admin-history-list > div > div { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.admin-history-list strong { color: #1d1d1f; font-size: 11px; }
.admin-history-list p, .admin-history-list small { display: block; margin-top: 6px; color: #777; font-size: 10px; line-height: 1.5; }
@media (max-width: 620px) { dl { grid-template-columns: 94px 1fr; }.admin-tool-add { align-items: stretch; flex-direction: column; }.admin-tool-add .admin-inline-input { max-width: none; } }
</style>
