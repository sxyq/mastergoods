<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Bot, CircleAlert, RefreshCw, ShieldAlert } from 'lucide-vue-next'
import AdminLayout from '@/features/admin/components/AdminLayout.vue'
import AdminPanelState from '@/features/admin/components/AdminPanelState.vue'
import { useSession } from '@/app/stores/session'
import { fetchAdminConfig } from '@/shared/api/admin'

const session = useSession()
const loading = ref(false)
const error = ref<unknown>(null)
const config = ref<Record<string, unknown> | null>(null)
const errorMessage = computed(() => error.value instanceof Error ? error.value.message : 'Agent 配置读取失败')
const field = (key: string) => { const value = config.value?.[key]; return value == null ? '-' : Array.isArray(value) ? value.join(', ') : String(value) }
async function load() { if (!session.token.value) { error.value = new Error('管理员会话已失效'); return }; loading.value = true; error.value = null; try { config.value = await fetchAdminConfig(session.token.value) } catch (cause) { error.value = cause } finally { loading.value = false } }
onMounted(load)
</script>
<template>
  <AdminLayout active-id="agent">
    <section class="admin-page-v2"><header class="admin-page-v2__header"><div><div class="admin-page-v2__crumb">Admin / Agent / <strong>Configuration</strong></div><h1>Agent 配置</h1><p>查看当前生效的模型、工具目录和范围配置。页面不展示 Provider 密钥或认证字段。</p></div><button class="admin-button-v2" type="button" :disabled="loading" @click="load"><RefreshCw :class="{ 'is-spinning': loading }" aria-hidden="true" />刷新</button></header><div v-if="error" class="admin-error-v2" role="alert"><ShieldAlert aria-hidden="true" /><span>{{ errorMessage }}</span><button type="button" @click="load">重试</button></div><div class="admin-grid"><article class="admin-card-v2 admin-span-8"><div class="admin-card-v2__header"><div><h2>当前配置</h2><p>数据由管理员配置接口返回。</p></div><Bot aria-hidden="true" /></div><AdminPanelState v-if="loading" state="loading" title="正在读取 Agent 配置" /><AdminPanelState v-else-if="error" state="error" :message="errorMessage" @retry="load" /><AdminPanelState v-else-if="!config" state="empty" title="尚未返回配置" message="服务端没有返回当前配置，不使用本地示例内容。" /><div v-else class="admin-card-v2__body"><dl><dt>模型 ID</dt><dd>{{ field('modelId') }}</dd><dt>工具目录</dt><dd>{{ field('toolCatalog') }}</dd><dt>启用范围</dt><dd>{{ field('enabledScopes') }}</dd><dt>配置版本</dt><dd>{{ field('version') }}</dd><dt>生效状态</dt><dd><span class="admin-status-v2 admin-status-v2--ok">{{ field('status') }}</span></dd></dl><p style="margin-top:22px"><CircleAlert aria-hidden="true" /> 只读查看：配置写入与变更原因将在服务端 PATCH 契约完成后开放。</p></div></article><article class="admin-card-v2 admin-span-4 admin-card-v2--pad"><h2>安全边界</h2><p>展示字段采用明确白名单。Provider key、Cookie、Session token、密码和私钥不会进入页面状态。</p></article></div></section>
  </AdminLayout>
</template>
<style scoped>
dl { display:grid; grid-template-columns:120px 1fr; gap:16px; margin:0; }
dt { color:#a0a0a0; font-size:11px; }
dd { margin:0; font-size:12px; word-break:break-word; }
p svg { width:14px; vertical-align:-3px; }
</style>
