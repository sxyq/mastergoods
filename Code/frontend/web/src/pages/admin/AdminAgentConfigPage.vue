<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Bot, Check, CircleAlert, RefreshCw, ShieldAlert } from 'lucide-vue-next'
import AdminLayout from '@/features/admin/components/AdminLayout.vue'
import AdminPanelState from '@/features/admin/components/AdminPanelState.vue'
import { useSession } from '@/app/stores/session'
import { fetchAdminConfig, updateAdminConfig, type AdminConfigPayload } from '@/shared/api/admin'
import { useAdminSession } from '@/app/stores/admin-session'

const session = useSession()
const adminSession = useAdminSession()
const loading = ref(false)
const error = ref<unknown>(null)
const config = ref<AdminConfigPayload | null>(null)
const modelId = ref('')
const agentEnabled = ref(false)
const enabledTools = ref('')
const saving = ref(false)
const notice = ref('')
const errorMessage = computed(() => error.value instanceof Error ? error.value.message : 'Agent 配置读取失败')
async function load() { if (!session.token.value) { error.value = new Error('管理员会话已失效'); return }; loading.value = true; error.value = null; try { config.value = await fetchAdminConfig(session.token.value); modelId.value = config.value.modelId || ''; agentEnabled.value = config.value.agentEnabled; enabledTools.value = (config.value.enabledTools || []).join(', ') } catch (cause) { error.value = cause } finally { loading.value = false } }
async function save() {
  if (!session.token.value || !config.value || !adminSession.can('admin.agent.config.manage')) return
  const reason = window.prompt('请输入配置变更原因')?.trim()
  if (!reason || !window.confirm('确认提交模型与工具配置变更？')) return
  saving.value = true; notice.value = ''; error.value = null
  try {
    config.value = await updateAdminConfig(session.token.value, { modelId: modelId.value.trim() || undefined, agentEnabled: agentEnabled.value, enabledTools: enabledTools.value.split(',').map(item => item.trim()).filter(Boolean), expectedVersion: config.value.version, idempotencyKey: crypto.randomUUID(), reason, confirmed: true })
    modelId.value = config.value.modelId || ''; agentEnabled.value = config.value.agentEnabled; enabledTools.value = (config.value.enabledTools || []).join(', '); notice.value = '配置已生效'
  } catch (cause) { error.value = cause }
  finally { saving.value = false }
}
onMounted(async () => { await adminSession.ensure(session.token.value); await load() })
</script>
<template>
  <AdminLayout active-id="config">
    <section class="admin-page-v2"><header class="admin-page-v2__header"><div><div class="admin-page-v2__crumb">Admin / Agent / <strong>Configuration</strong></div><h1>Agent 配置</h1><p>查看并受控修改模型、工具白名单和运行开关。页面不展示 Provider 密钥或认证字段。</p></div><div class="admin-page-v2__actions"><button v-if="adminSession.can('admin.agent.config.manage')" class="admin-button-v2 admin-button-v2--dark" type="button" :disabled="loading || saving || !config" @click="save"><Check aria-hidden="true" />保存变更</button><button class="admin-button-v2" type="button" :disabled="loading || saving" @click="load"><RefreshCw :class="{ 'is-spinning': loading }" aria-hidden="true" />刷新</button></div></header><div v-if="error" class="admin-error-v2" role="alert"><ShieldAlert aria-hidden="true" /><span>{{ errorMessage }}</span><button type="button" @click="load">重试</button></div><div class="admin-grid"><article class="admin-card-v2 admin-span-8"><div class="admin-card-v2__header"><div><h2>当前配置</h2><p>配置版本由服务端控制，保存前会检查并发版本。</p></div><Bot aria-hidden="true" /></div><AdminPanelState v-if="loading" state="loading" title="正在读取 Agent 配置" /><AdminPanelState v-else-if="error" state="error" :message="errorMessage" @retry="load" /><AdminPanelState v-else-if="!config" state="empty" title="尚未返回配置" message="服务端没有返回当前配置，不使用本地示例内容。" /><div v-else class="admin-card-v2__body"><dl><dt>模型 ID</dt><dd><input v-if="adminSession.can('admin.agent.config.manage')" v-model="modelId" class="admin-inline-input" aria-label="模型 ID" /><span v-else>{{ config.modelId || '-' }}</span></dd><dt>Agent 开关</dt><dd><label v-if="adminSession.can('admin.agent.config.manage')" class="admin-toggle"><input v-model="agentEnabled" type="checkbox" /><span>{{ agentEnabled ? '已启用' : '已停用' }}</span></label><span v-else class="admin-status-v2" :class="config.agentEnabled ? 'admin-status-v2--ok' : 'admin-status-v2--warn'">{{ config.agentEnabled ? '已启用' : '已停用' }}</span></dd><dt>启用工具</dt><dd><input v-if="adminSession.can('admin.agent.config.manage')" v-model="enabledTools" class="admin-inline-input" aria-label="启用工具" placeholder="以逗号分隔工具名" /><span v-else>{{ (config.enabledTools || []).join(', ') || '-' }}</span></dd><dt>配置版本</dt><dd>{{ config.version }}</dd><dt>生效状态</dt><dd>{{ config.effectiveState || '-' }}<small v-if="config.effectiveAt">{{ config.effectiveAt }}</small></dd></dl><p v-if="notice" class="admin-success-note" role="status"><Check aria-hidden="true" /> {{ notice }}</p><p style="margin-top:22px"><CircleAlert aria-hidden="true" /> 高风险配置变更会写入管理员审计；Provider key、Cookie、Session token、密码和私钥不会进入页面状态。</p></div></article><article class="admin-card-v2 admin-span-4 admin-card-v2--pad"><h2>安全边界</h2><p>观察员只能查看配置元数据；只有具备管理权限的会话可以提交模型与工具变更。</p></article></div></section>
  </AdminLayout>
</template>
<style scoped>
dl { display:grid; grid-template-columns:120px 1fr; gap:16px; margin:0; }
dt { color:#a0a0a0; font-size:11px; }
dd { margin:0; font-size:12px; word-break:break-word; }
p svg { width:14px; vertical-align:-3px; }
.admin-inline-input { width:100%; max-width:420px; border:1px solid #e8e8e5; border-radius:6px; padding:7px 9px; font-size:12px; }
.admin-toggle { display:inline-flex; align-items:center; gap:8px; }
.admin-success-note { color:#327a4b; }
</style>
