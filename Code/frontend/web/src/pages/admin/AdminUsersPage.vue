<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { Building2, Check, ChevronLeft, ChevronRight, RefreshCw, Search, ShieldAlert, UsersRound } from 'lucide-vue-next'
import AdminLayout from '@/features/admin/components/AdminLayout.vue'
import AdminPanelState from '@/features/admin/components/AdminPanelState.vue'
import AdminScopeFilter from '@/features/admin/components/AdminScopeFilter.vue'
import { useSession } from '@/app/stores/session'
import { useAdminSession } from '@/app/stores/admin-session'
import { fetchAdminStores, fetchAdminUsers, updateAdminStore, updateAdminUser, type AdminStoreSummary, type AdminUserPayload } from '@/shared/api/admin'

const session = useSession()
const adminSession = useAdminSession()
const tab = ref<'users' | 'stores'>('users')
const search = ref('')
const page = ref(0)
const size = 20
const loading = ref(false)
const error = ref<unknown>(null)
const users = ref<AdminUserPayload[]>([])
const stores = ref<AdminStoreSummary[]>([])
const total = ref(0)
const hasNext = ref(false)
const ownerUserId = ref('')
const storeId = ref('')
const savingId = ref<string | null>(null)
const mutationError = ref<unknown>(null)
const mutationNotice = ref('')

const errorMessage = computed(() => error.value instanceof Error ? error.value.message : '管理员组织数据读取失败')
const mutationErrorMessage = computed(() => mutationError.value instanceof Error ? mutationError.value.message : '状态更新失败')
async function load() {
  if (!session.token.value) { error.value = new Error('管理员会话已失效'); return }
  loading.value = true; error.value = null
  try {
    if (tab.value === 'users') {
      const result = await fetchAdminUsers(session.token.value, { query: search.value.trim() || undefined, ownerUserId: ownerUserId.value || undefined, storeId: storeId.value || undefined, page: page.value, size })
      users.value = result.items; total.value = result.total; hasNext.value = result.hasNext
    } else {
      const result = await fetchAdminStores(session.token.value, { ownerUserId: ownerUserId.value || undefined, storeId: storeId.value || undefined, page: page.value, size })
      stores.value = result.items; total.value = result.total; hasNext.value = result.hasNext
    }
  } catch (cause) { error.value = cause }
  finally { loading.value = false }
}

function switchTab(next: 'users' | 'stores') { tab.value = next; page.value = 0; void load() }
function applyScope() { page.value = 0; void load() }
function previous() { if (page.value > 0) { page.value -= 1; void load() } }
function next() { if (hasNext.value) { page.value += 1; void load() } }
function statusTone(status: string) { return /active|enabled|正常|启用/i.test(status) ? 'ok' : /disabled|inactive|停用/i.test(status) ? 'bad' : 'warn' }
function statusCode(status: string) { return /active|enabled|正常|启用/i.test(status) ? 1 : 0 }
function date(value?: string) { return value ? new Date(value).toLocaleString('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }) : '-' }

async function toggleUser(item: AdminUserPayload) {
  if (!session.token.value || !adminSession.can('admin.user.manage')) return
  const nextStatus = statusCode(item.status) === 1 ? 0 : 1
  const actionLabel = nextStatus === 1 ? '恢复' : '停用'
  const reason = window.prompt(`请输入${actionLabel}用户的原因`)?.trim()
  if (!reason || !window.confirm(`确认${actionLabel}用户“${item.nickname || item.userId}”？`)) return
  savingId.value = item.userId; mutationError.value = null; mutationNotice.value = ''
  try {
    const updated = await updateAdminUser(session.token.value, item.userId, { status: nextStatus, keepSessions: nextStatus === 1, expectedVersion: item.version ?? 0, idempotencyKey: crypto.randomUUID(), reason, confirmed: true })
    const index = users.value.findIndex((entry) => entry.userId === item.userId)
    if (index >= 0) users.value[index] = updated
    mutationNotice.value = `${actionLabel}操作已完成`
  } catch (cause) { mutationError.value = cause; void load() }
  finally { savingId.value = null }
}

async function toggleStore(item: AdminStoreSummary) {
  if (!session.token.value || !adminSession.can('admin.store.manage')) return
  const nextStatus = statusCode(item.status) === 1 ? 0 : 1
  const actionLabel = nextStatus === 1 ? '恢复' : '停用'
  const reason = window.prompt(`请输入${actionLabel}门店的原因`)?.trim()
  if (!reason || !window.confirm(`确认${actionLabel}门店“${item.name || item.storeId}”？`)) return
  savingId.value = item.storeId; mutationError.value = null; mutationNotice.value = ''
  try {
    const updated = await updateAdminStore(session.token.value, item.storeId, { status: nextStatus, expectedVersion: item.version ?? 0, idempotencyKey: crypto.randomUUID(), reason, confirmed: true, ownerUserId: item.ownerUserId })
    const index = stores.value.findIndex((entry) => entry.storeId === item.storeId)
    if (index >= 0) stores.value[index] = updated
    mutationNotice.value = `${actionLabel}操作已完成`
  } catch (cause) { mutationError.value = cause; void load() }
  finally { savingId.value = null }
}

watch(search, () => { page.value = 0; void load() })
onMounted(async () => { await adminSession.ensure(session.token.value); await load() })
</script>

<template>
  <AdminLayout active-id="users">
    <section class="admin-page-v2">
      <header class="admin-page-v2__header">
        <div><div class="admin-page-v2__crumb">Admin / Organization / <strong>Directory</strong></div><h1>用户与门店</h1><p>查看授权范围内的用户、门店和成员关系。</p></div>
        <div class="admin-page-v2__actions"><button class="admin-button-v2" type="button" :disabled="loading" @click="load"><RefreshCw :class="{ 'is-spinning': loading }" aria-hidden="true" />刷新</button></div>
      </header>
      <div v-if="error" class="admin-error-v2" role="alert"><ShieldAlert aria-hidden="true" /><span>{{ errorMessage }}</span><button type="button" @click="load">重试</button></div>
      <div v-if="mutationError" class="admin-error-v2" role="alert"><ShieldAlert aria-hidden="true" /><span>{{ mutationErrorMessage }}</span><button type="button" @click="load">刷新</button></div>
      <p v-if="mutationNotice" class="admin-success-note" role="status"><Check aria-hidden="true" /> {{ mutationNotice }}</p>
      <div class="admin-grid">
        <article class="admin-card-v2 admin-span-12">
          <div class="admin-card-v2__header"><div class="admin-toolbar"><button class="admin-button-v2" :class="{ 'admin-button-v2--dark': tab === 'users' }" type="button" @click="switchTab('users')"><UsersRound aria-hidden="true" />用户</button><button class="admin-button-v2" :class="{ 'admin-button-v2--dark': tab === 'stores' }" type="button" @click="switchTab('stores')"><Building2 aria-hidden="true" />门店</button></div><span class="admin-card-v2__meta">第 {{ page + 1 }} 页 · {{ total }} 条</span></div>
          <div class="admin-card-v2__body admin-toolbar"><label v-if="tab === 'users'" class="admin-field"><Search aria-hidden="true" /><input v-model="search" type="search" placeholder="搜索昵称或脱敏手机号" aria-label="搜索用户" /></label><span v-else class="admin-card-v2__meta">门店列表按当前管理员授权范围返回。</span><AdminScopeFilter v-model:owner-user-id="ownerUserId" v-model:store-id="storeId" :owner-user-ids="adminSession.session.value?.ownerUserIds" :store-ids="adminSession.session.value?.storeIds" /><button class="admin-button-v2" type="button" @click="applyScope">应用范围</button></div>
          <AdminPanelState v-if="loading" state="loading" title="正在读取组织数据" />
          <AdminPanelState v-else-if="error" state="error" :message="errorMessage" @retry="load" />
          <AdminPanelState v-else-if="(tab === 'users' ? users : stores).length === 0" state="empty" title="当前范围暂无记录" message="服务端没有返回可见的组织数据，请调整授权范围后重试。" />
          <div v-else class="admin-table-wrap">
            <table v-if="tab === 'users'" class="admin-table-v2"><thead><tr><th>用户</th><th>手机号</th><th>状态</th><th>版本</th><th>创建时间</th><th>更新时间</th><th v-if="adminSession.can('admin.user.manage')">操作</th></tr></thead><tbody><tr v-for="item in users" :key="item.userId"><td><strong>{{ item.nickname || '未命名用户' }}</strong><small><code>{{ item.userId }}</code></small></td><td>{{ item.phoneMasked || '已脱敏' }}</td><td><span class="admin-status-v2" :class="`admin-status-v2--${statusTone(item.status)}`">{{ item.status || '未知' }}</span></td><td><code>v{{ item.version ?? 0 }}</code></td><td>{{ date(item.createdAt) }}</td><td>{{ date(item.updatedAt) }}</td><td v-if="adminSession.can('admin.user.manage')"><button class="admin-button-v2 admin-button-v2--compact" type="button" :disabled="savingId === item.userId" @click="toggleUser(item)">{{ savingId === item.userId ? '处理中' : statusCode(item.status) === 1 ? '停用' : '恢复' }}</button></td></tr></tbody></table>
            <table v-else class="admin-table-v2"><thead><tr><th>门店</th><th>Owner ID</th><th>状态</th><th>版本</th><th>成员</th><th>创建时间</th><th v-if="adminSession.can('admin.store.manage')">操作</th></tr></thead><tbody><tr v-for="item in stores" :key="item.storeId"><td><strong>{{ item.name || '未命名门店' }}</strong><small><code>{{ item.storeId }}</code></small></td><td><code>{{ item.ownerUserId }}</code></td><td><span class="admin-status-v2" :class="`admin-status-v2--${statusTone(item.status)}`">{{ item.status || '未知' }}</span></td><td><code>v{{ item.version ?? 0 }}</code></td><td>{{ item.memberCount }}</td><td>{{ date(item.createdAt) }}</td><td v-if="adminSession.can('admin.store.manage')"><button class="admin-button-v2 admin-button-v2--compact" type="button" :disabled="savingId === item.storeId" @click="toggleStore(item)">{{ savingId === item.storeId ? '处理中' : statusCode(item.status) === 1 ? '停用' : '恢复' }}</button></td></tr></tbody></table>
          </div>
          <footer v-if="!loading && !error && total > 0" class="admin-pagination"><span>显示第 {{ page * size + 1 }} - {{ page * size + (tab === 'users' ? users.length : stores.length) }} 条</span><button type="button" :disabled="page === 0" aria-label="上一页" @click="previous"><ChevronLeft aria-hidden="true" /></button><button type="button" :disabled="!hasNext" aria-label="下一页" @click="next"><ChevronRight aria-hidden="true" /></button></footer>
        </article>
      </div>
    </section>
  </AdminLayout>
</template>

<style scoped>
.admin-success-note { margin: 0 auto 14px; max-width: 1400px; color: #327a4b; font-size: 12px; }
.admin-success-note svg { width: 14px; height: 14px; vertical-align: -3px; margin-right: 5px; }
</style>
