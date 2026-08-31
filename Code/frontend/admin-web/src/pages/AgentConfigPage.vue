<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { Bot, Check, CircleAlert, Plus, RefreshCw, Settings2, Wrench, X } from 'lucide-vue-next'
import { hasAdminPermission } from '@/app/stores/admin-session'
import { getAdminAgentConfig, updateAdminAgentConfig, type AdminAgentConfig } from '@/shared/api/admin'
import { AdminApiError } from '@/shared/api/client'
import { formatDateTime } from '@/shared/utils/format'
import AdminPageHeader from '@/shared/components/AdminPageHeader.vue'
import ConfirmDialog from '@/shared/components/ConfirmDialog.vue'
import StatePanel from '@/shared/components/StatePanel.vue'

const config = ref<AdminAgentConfig | null>(null)
const modelId = ref('')
const enabled = ref(false)
const tools = ref<string[]>([])
const toolInput = ref('')
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const saveError = ref('')
const validationError = ref('')
const confirmOpen = ref(false)
const canManage = computed(() => hasAdminPermission('admin.agent.config.manage'))
const hasChanges = computed(() => !!config.value && (modelId.value.trim() !== (config.value.model_id ?? '') || enabled.value !== config.value.agent_enabled || tools.value.join('\u0000') !== config.value.enabled_tools.join('\u0000')))

function apply(configValue: AdminAgentConfig): void { config.value = configValue; modelId.value = configValue.model_id ?? ''; enabled.value = configValue.agent_enabled; tools.value = [...configValue.enabled_tools]; saveError.value = ''; validationError.value = '' }
async function load(): Promise<void> { loading.value = true; error.value = ''; try { apply(await getAdminAgentConfig()) } catch (reason) { error.value = reason instanceof Error ? reason.message : '无法读取 Agent 配置。' } finally { loading.value = false } }
function validate(): string {
  const model = modelId.value.trim()
  if (model && (model.length > 128 || !/^[A-Za-z0-9._:/-]+$/.test(model))) return '模型 ID 只能包含字母、数字和 . _ : / -，且长度不能超过 128。'
  if (tools.value.length > 200) return '最多可启用 200 个工具。'
  if (tools.value.some((tool) => !tool || tool.length > 128)) return '工具名称不能为空，且长度不能超过 128。'
  return ''
}
function addTool(): void {
  const tool = toolInput.value.trim()
  if (!tool) return
  if (tool.length > 128) { validationError.value = '工具名称长度不能超过 128。'; return }
  if (tools.value.length >= 200) { validationError.value = '最多可启用 200 个工具。'; return }
  if (!tools.value.includes(tool)) tools.value = [...tools.value, tool]
  toolInput.value = ''
  validationError.value = ''
}
function removeTool(tool: string): void { tools.value = tools.value.filter((item) => item !== tool) }
function mutationId(): string { return globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(16).slice(2)}` }
function openConfirmation(): void { validationError.value = validate(); if (!validationError.value) confirmOpen.value = true }
function saveMessage(reason: unknown): string {
  if (reason instanceof AdminApiError) {
    if (reason.status === 409) return '配置已被其他管理员更新。请刷新页面后检查最新版本，再重新提交。'
    if (reason.status === 401 || reason.status === 403) return '当前账号没有更新 Agent 配置的权限。'
    if (reason.status === 400 || reason.status === 422) return reason.message || '提交内容未通过服务端校验。'
  }
  return reason instanceof Error ? reason.message : '配置更新失败。'
}
async function save(): Promise<void> {
  if (!config.value || saving.value) return
  validationError.value = validate()
  if (validationError.value) { confirmOpen.value = false; return }
  saving.value = true
  saveError.value = ''
  try {
    apply(await updateAdminAgentConfig({ model_id: modelId.value.trim() || null, agent_enabled: enabled.value, enabled_tools: tools.value, expected_version: config.value.version, idempotency_key: mutationId(), reason: '管理员后台更新 Agent 配置', confirmed: true }))
    confirmOpen.value = false
  } catch (reason) { saveError.value = saveMessage(reason) } finally { saving.value = false }
}
watch(() => config.value?.version, () => { saveError.value = '' })
onMounted(load)
</script>

<template>
  <section>
    <AdminPageHeader eyebrow="AGENT / CONFIGURATION" title="Agent 配置" description="管理模型标识、启用状态和可调用工具；密钥不会出现在此页面。"><template #actions><button class="outline-button" type="button" :disabled="loading || saving" @click="load"><RefreshCw :class="{ spinning: loading }" aria-hidden="true" />刷新</button><button v-if="canManage" class="primary-button" type="button" :disabled="!hasChanges || saving" @click="openConfirmation"><Check aria-hidden="true" />保存变更</button></template></AdminPageHeader>
    <StatePanel v-if="loading" state="loading" title="正在读取 Agent 配置" detail="正在向服务端请求当前模型、启用状态和工具目录。" />
    <StatePanel v-else-if="error" state="error" :detail="error" @retry="load" />
    <div v-else-if="config" class="config-grid"><section class="config-panel"><header><div><h2>运行配置</h2><p>当前版本 {{ config.version }} · {{ config.effective_state || '未记录生效状态' }}</p></div><Settings2 aria-hidden="true" /></header><label class="field">模型 ID<input v-model="modelId" :disabled="!canManage || saving" maxlength="128" placeholder="例如 provider/model-id" /></label><label class="switch-row"><span><strong>启用 Agent</strong><small>关闭后新的 Agent 请求将由服务端拒绝或跳过。</small></span><input v-model="enabled" type="checkbox" :disabled="!canManage || saving" /></label><p v-if="validationError || saveError" class="save-error" role="alert">{{ validationError || saveError }}</p></section>
      <section class="config-panel"><header><div><h2>工具目录</h2><p>仅显示和提交工具名称，不含工具参数或密钥。</p></div><Wrench aria-hidden="true" /></header><div class="tool-tags"><span v-for="tool in tools" :key="tool">{{ tool }}<button v-if="canManage" type="button" :aria-label="`移除 ${tool}`" :disabled="saving" @click="removeTool(tool)"><X /></button></span><p v-if="!tools.length">暂无启用工具。</p></div><div v-if="canManage" class="add-tool"><input v-model="toolInput" aria-label="添加工具名称" maxlength="128" placeholder="添加工具名称" :disabled="saving" @keyup.enter="addTool" /><button type="button" :disabled="saving || !toolInput.trim()" @click="addTool"><Plus aria-hidden="true" />添加</button></div></section>
      <aside class="history-panel"><Bot aria-hidden="true" /><p>CONFIGURATION STATE</p><h2>{{ config.agent_enabled ? 'Agent 已启用' : 'Agent 已停用' }}</h2><dl><div><dt>最近生效</dt><dd>{{ formatDateTime(config.effective_at) }}</dd></div><div><dt>更新人</dt><dd>{{ config.updated_by || '—' }}</dd></div><div><dt>版本</dt><dd>{{ config.version }}</dd></div></dl><span class="history-note"><CircleAlert aria-hidden="true" />历史变更时间线需要后端提供历史记录接口后再展示。</span></aside>
    </div>
    <ConfirmDialog :open="confirmOpen" :busy="saving" title="确认更新 Agent 配置？" description="服务端将验证版本、权限和工具名，并记录本次变更的原因与结果。" confirm-label="确认更新" @cancel="confirmOpen = false" @confirm="save" />
  </section>
</template>

<style scoped>
.primary-button, .outline-button { display: inline-flex; height: 32px; align-items: center; gap: 7px; border-radius: 999px; padding: 0 12px; cursor: pointer; font-size: 12px; }.primary-button { border: 1px solid var(--admin-foreground); background: var(--admin-foreground); color: #fff; }.primary-button:disabled, .outline-button:disabled { cursor: wait; opacity: .65; }.outline-button { border: 1px solid var(--admin-border); background: #fff; color: var(--admin-foreground); }.outline-button:hover:not(:disabled) { background: var(--admin-secondary); }.primary-button :deep(svg), .outline-button :deep(svg) { width: 14px; height: 14px; stroke-width: 1.7; }.spinning { animation: spin .8s linear infinite; }.config-grid { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) 270px; gap: 8px; margin-top: 22px; }.config-panel, .history-panel { border: 1px solid var(--admin-border); border-radius: 8px; background: #fff; padding: 18px; }.config-panel header { display: flex; align-items: flex-start; justify-content: space-between; gap: 10px; }.config-panel h2, .history-panel h2 { margin: 0; font-size: 14px; font-weight: 500; }.config-panel p { margin: 5px 0 0; color: var(--admin-muted); font-size: 11px; }.config-panel header > :deep(svg) { width: 16px; height: 16px; color: var(--admin-muted); }.field { display: grid; gap: 8px; margin-top: 24px; font-size: 11px; }.field input, .add-tool input { width: 100%; height: 34px; border: 1px solid var(--admin-border); border-radius: 6px; background: #fff; padding: 0 9px; color: var(--admin-foreground); outline: 0; font-size: 11px; }.switch-row { display: flex; align-items: center; justify-content: space-between; gap: 14px; margin-top: 23px; border-top: 1px solid var(--admin-border); padding-top: 16px; }.switch-row span { display: grid; gap: 5px; }.switch-row strong { font-size: 11px; font-weight: 500; }.switch-row small { color: var(--admin-muted); font-size: 10px; line-height: 1.4; }.switch-row input { width: 36px; height: 20px; accent-color: var(--admin-foreground); }.save-error { margin-top: 16px !important; color: var(--admin-danger) !important; }.tool-tags { display: flex; min-height: 92px; flex-wrap: wrap; align-content: flex-start; gap: 7px; margin-top: 22px; }.tool-tags > span { display: inline-flex; max-width: 100%; align-items: center; gap: 5px; border: 1px solid var(--admin-border); border-radius: 999px; background: #fafaf8; padding: 5px 8px; overflow: hidden; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }.tool-tags button { display: grid; width: 14px; height: 14px; place-items: center; border: 0; border-radius: 50%; background: transparent; color: var(--admin-muted); cursor: pointer; }.tool-tags button:hover { background: var(--admin-secondary); color: var(--admin-foreground); }.tool-tags button :deep(svg) { width: 11px; height: 11px; }.tool-tags p { width: 100%; margin: 0; color: var(--admin-muted); font-size: 11px; }.add-tool { display: flex; gap: 7px; margin-top: 16px; }.add-tool button { display: inline-flex; height: 34px; align-items: center; gap: 5px; border: 1px solid var(--admin-border); border-radius: 6px; background: #fff; padding: 0 9px; cursor: pointer; font-size: 10px; }.add-tool button:disabled { cursor: not-allowed; opacity: .5; }.add-tool button :deep(svg) { width: 13px; height: 13px; }.history-panel > :deep(svg) { width: 20px; height: 20px; color: #66669c; }.history-panel > p { margin: 21px 0 0; color: var(--admin-muted); font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 9px; letter-spacing: .08em; }.history-panel h2 { margin-top: 9px; font-size: 16px; }.history-panel dl { display: grid; margin: 20px 0 0; }.history-panel dl div { border-bottom: 1px solid var(--admin-border); padding: 11px 0; }.history-panel dt { color: var(--admin-muted); font-size: 10px; }.history-panel dd { margin: 6px 0 0; font-size: 11px; overflow-wrap: anywhere; }.history-note { display: flex; gap: 6px; margin-top: 15px; color: var(--admin-attention); font-size: 10px; line-height: 1.45; }.history-note :deep(svg) { width: 14px; height: 14px; flex: 0 0 14px; }@keyframes spin { to { transform: rotate(360deg); } }@media (max-width: 1080px) { .config-grid { grid-template-columns: 1fr 1fr; }.history-panel { grid-column: span 2; }}@media (max-width: 630px) { .config-grid { grid-template-columns: 1fr; }.history-panel { grid-column: auto; }}
</style>
