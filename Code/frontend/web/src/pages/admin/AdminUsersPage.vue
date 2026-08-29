<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { Building2, Check, ChevronLeft, ChevronRight, RefreshCw, Search, ShieldAlert, UserRound, UsersRound, X } from 'lucide-vue-next'
import AdminLayout from '@/features/admin/components/AdminLayout.vue'
import AdminPanelState from '@/features/admin/components/AdminPanelState.vue'
import AdminScopeFilter from '@/features/admin/components/AdminScopeFilter.vue'
import AdminStatusBadge from '@/features/admin/components/AdminStatusBadge.vue'
import { useSession } from '@/app/stores/session'
import { useAdminSession } from '@/app/stores/admin-session'
import {
  fetchAdminStore,
  fetchAdminStoreMembers,
  fetchAdminStores,
  fetchAdminUser,
  fetchAdminUsers,
  updateAdminStore,
  updateAdminStoreMember,
  updateAdminUser,
  type AdminMemberSummary,
  type AdminStoreSummary,
  type AdminUserPayload,
} from '@/shared/api/admin'

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

const detailMode = ref<'user' | 'store' | null>(null)
const selectedUser = ref<AdminUserPayload | null>(null)
const userDetail = ref<AdminUserPayload | null>(null)
const selectedStore = ref<AdminStoreSummary | null>(null)
const storeDetail = ref<AdminStoreSummary | null>(null)
const detailLoading = ref(false)
const detailError = ref<unknown>(null)
const members = ref<AdminMemberSummary[]>([])
const memberPage = ref(0)
const memberTotal = ref(0)
const memberHasNext = ref(false)
const memberLoading = ref(false)
const memberError = ref<unknown>(null)
const memberSavingId = ref<string | null>(null)
let listSerial = 0

const errorMessage = computed(() => error.value instanceof Error ? error.value.message : '管理员组织数据读取失败')
const mutationErrorMessage = computed(() => mutationError.value instanceof Error ? mutationError.value.message : '状态更新失败')
const detailErrorMessage = computed(() => detailError.value instanceof Error ? detailError.value.message : '详情读取失败')
const memberErrorMessage = computed(() => memberError.value instanceof Error ? memberError.value.message : '成员关系读取失败')
const detailOpen = computed(() => detailMode.value !== null)
const canReadUsers = computed(() => adminSession.can('admin.user.read'))
const canReadStores = computed(() => adminSession.can('admin.store.read'))
const activeCanRead = computed(() => tab.value === 'users' ? canReadUsers.value : canReadStores.value)

function date(value?: string | null) {
  return value ? new Date(value).toLocaleString('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }) : '-'
}

function statusTone(status?: string | null) {
  return /active|enabled|正常|启用/i.test(status ?? '') ? 'ok' : /disabled|inactive|停用/i.test(status ?? '') ? 'bad' : 'warn'
}

function statusType(status?: string | null) {
  return statusTone(status) === 'ok' ? 'completed' : statusTone(status) === 'bad' ? 'failed' : 'running'
}

function statusCode(status?: string | null) {
  return statusTone(status) === 'ok' ? 1 : 0
}

function statusLabel(status?: string | null) {
  return statusTone(status) === 'ok' ? '已启用' : statusTone(status) === 'bad' ? '已停用' : status || '未知'
}

async function load() {
  const serial = ++listSerial
  if (!session.token.value) { if (serial === listSerial) error.value = new Error('管理员会话已失效'); return }
  if (!activeCanRead.value) {
    if (serial === listSerial) { error.value = null; users.value = []; stores.value = []; total.value = 0; hasNext.value = false }
    return
  }
  loading.value = true
  error.value = null
  try {
    if (tab.value === 'users') {
      const result = await fetchAdminUsers(session.token.value, { query: search.value.trim() || undefined, ownerUserId: ownerUserId.value || undefined, storeId: storeId.value || undefined, page: page.value, size })
      if (serial !== listSerial) return
      users.value = result.items; total.value = result.total; hasNext.value = result.hasNext
    } else {
      const result = await fetchAdminStores(session.token.value, { ownerUserId: ownerUserId.value || undefined, storeId: storeId.value || undefined, page: page.value, size })
      if (serial !== listSerial) return
      stores.value = result.items; total.value = result.total; hasNext.value = result.hasNext
    }
  } catch (cause) {
    if (serial !== listSerial) return
    error.value = cause
  } finally {
    if (serial === listSerial) loading.value = false
  }
}

function switchTab(next: 'users' | 'stores') { tab.value = next; page.value = 0; void load() }
function applyScope() { page.value = 0; void load() }
function previous() { if (page.value > 0) { page.value -= 1; void load() } }
function next() { if (hasNext.value) { page.value += 1; void load() } }

async function toggleUser(item: AdminUserPayload) {
  if (!session.token.value || !adminSession.can('admin.user.manage')) return
  const nextStatus = statusCode(item.status) === 1 ? 0 : 1
  const actionLabel = nextStatus === 1 ? '恢复' : '停用'
  const reason = window.prompt(`请输入${actionLabel}用户的原因`)?.trim()
  if (!reason || !window.confirm(`确认${actionLabel}用户“${item.nickname || item.userId}”？`)) return
  savingId.value = item.userId; mutationError.value = null; mutationNotice.value = ''
  try {
    const updated = await updateAdminUser(session.token.value, item.userId, { status: nextStatus, keepSessions: nextStatus === 1, expectedVersion: item.version ?? 0, idempotencyKey: crypto.randomUUID(), reason, confirmed: true, ownerUserId: ownerUserId.value || undefined, storeId: storeId.value || undefined })
    const index = users.value.findIndex((entry) => entry.userId === item.userId)
    if (index >= 0) users.value[index] = updated
    if (userDetail.value?.userId === item.userId) userDetail.value = updated
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
    if (storeDetail.value?.storeId === item.storeId) storeDetail.value = updated
    if (selectedStore.value?.storeId === item.storeId) selectedStore.value = updated
    mutationNotice.value = `${actionLabel}操作已完成`
  } catch (cause) { mutationError.value = cause; void load() }
  finally { savingId.value = null }
}

async function openUser(item: AdminUserPayload) {
  if (!canReadUsers.value) return
  closeDetail()
  detailMode.value = 'user'; selectedUser.value = item; userDetail.value = item; detailLoading.value = true; detailError.value = null
  if (!session.token.value) { detailError.value = new Error('管理员会话已失效'); detailLoading.value = false; return }
  try { userDetail.value = await fetchAdminUser(session.token.value, item.userId, { ownerUserId: ownerUserId.value || undefined, storeId: storeId.value || undefined }) }
  catch (cause) { detailError.value = cause }
  finally { detailLoading.value = false }
}

async function openStore(item: AdminStoreSummary) {
  if (!canReadStores.value) return
  closeDetail()
  detailMode.value = 'store'; selectedStore.value = item; storeDetail.value = item; detailLoading.value = true; detailError.value = null; members.value = []; memberPage.value = 0; memberTotal.value = 0; memberHasNext.value = false; memberError.value = null
  if (!session.token.value) { detailError.value = new Error('管理员会话已失效'); detailLoading.value = false; return }
  try {
    const [detail, memberPageResult] = await Promise.all([
      fetchAdminStore(session.token.value, item.storeId, { ownerUserId: item.ownerUserId }),
      fetchAdminStoreMembers(session.token.value, item.storeId, { ownerUserId: item.ownerUserId, page: 0, size: 20 }),
    ])
    if (detailMode.value !== 'store' || selectedStore.value?.storeId !== item.storeId) return
    storeDetail.value = detail; members.value = memberPageResult.items; memberTotal.value = memberPageResult.total; memberHasNext.value = memberPageResult.hasNext
  } catch (cause) { detailError.value = cause }
  finally { detailLoading.value = false }
}

async function loadMembers() {
  if (!session.token.value || !selectedStore.value || !canReadStores.value) return
  memberLoading.value = true; memberError.value = null
  try {
    const result = await fetchAdminStoreMembers(session.token.value, selectedStore.value.storeId, { ownerUserId: selectedStore.value.ownerUserId, page: memberPage.value, size: 20 })
    if (detailMode.value !== 'store') return
    members.value = result.items; memberTotal.value = result.total; memberHasNext.value = result.hasNext
  } catch (cause) { memberError.value = cause }
  finally { memberLoading.value = false }
}

async function toggleMember(item: AdminMemberSummary) {
  if (!session.token.value || !selectedStore.value || !adminSession.can('admin.store.manage')) return
  const nextStatus = statusCode(item.status) === 1 ? 0 : 1
  const actionLabel = nextStatus === 1 ? '恢复' : '停用'
  const reason = window.prompt(`请输入${actionLabel}成员的原因`)?.trim()
  if (!reason || !window.confirm(`确认${actionLabel}成员“${item.nickname || item.userId}”？`)) return
  memberSavingId.value = item.userId; memberError.value = null
  try {
    const updated = await updateAdminStoreMember(session.token.value, selectedStore.value.storeId, item.userId, { status: nextStatus, keepSessions: nextStatus === 1, expectedVersion: item.version, idempotencyKey: crypto.randomUUID(), reason, confirmed: true })
    const index = members.value.findIndex((entry) => entry.userId === item.userId)
    if (index >= 0) members.value[index] = updated
    mutationNotice.value = `${actionLabel}成员操作已完成`
  } catch (cause) { memberError.value = cause; void loadMembers() }
  finally { memberSavingId.value = null }
}

function closeDetail() {
  detailMode.value = null; selectedUser.value = null; userDetail.value = null; selectedStore.value = null; storeDetail.value = null; members.value = []; detailError.value = null; memberError.value = null
}

function handleEscape(event: KeyboardEvent) { if (event.key === 'Escape' && detailOpen.value) closeDetail() }

watch(search, () => { page.value = 0; void load() })
onMounted(async () => { window.addEventListener('keydown', handleEscape); if (await adminSession.ensure(session.token.value)) await load() })
onUnmounted(() => window.removeEventListener('keydown', handleEscape))
</script>

<template>
  <AdminLayout active-id="users">
    <section class="admin-page-v2">
      <header class="admin-page-v2__header"><div><div class="admin-page-v2__crumb">Admin / Organization / <strong>Directory</strong></div><h1>用户与门店</h1><p>查看授权范围内的用户、门店、店长和店员关系。</p></div><div class="admin-page-v2__actions"><button class="admin-button-v2" type="button" :disabled="loading" @click="load"><RefreshCw :class="{ 'is-spinning': loading }" aria-hidden="true" />刷新</button></div></header>
      <div v-if="error" class="admin-error-v2" role="alert"><ShieldAlert aria-hidden="true" /><span>{{ errorMessage }}</span><button type="button" @click="load">重试</button></div>
      <div v-if="mutationError" class="admin-error-v2" role="alert"><ShieldAlert aria-hidden="true" /><span>{{ mutationErrorMessage }}</span><button type="button" @click="load">刷新</button></div>
      <p v-if="mutationNotice" class="admin-success-note" role="status"><Check aria-hidden="true" /> {{ mutationNotice }}</p>
      <div class="admin-grid"><article class="admin-card-v2 admin-span-12">
        <div class="admin-card-v2__header"><div class="admin-toolbar"><button class="admin-button-v2" :class="{ 'admin-button-v2--dark': tab === 'users' }" type="button" @click="switchTab('users')"><UsersRound aria-hidden="true" />用户</button><button class="admin-button-v2" :class="{ 'admin-button-v2--dark': tab === 'stores' }" type="button" @click="switchTab('stores')"><Building2 aria-hidden="true" />门店</button></div><span class="admin-card-v2__meta">第 {{ page + 1 }} 页 · {{ total }} 条</span></div>
        <div class="admin-card-v2__body admin-toolbar"><label v-if="tab === 'users'" class="admin-field"><Search aria-hidden="true" /><input v-model="search" type="search" placeholder="搜索昵称或脱敏手机号" aria-label="搜索用户" /></label><span v-else class="admin-card-v2__meta">门店列表按当前管理员授权范围返回。</span><AdminScopeFilter v-model:owner-user-id="ownerUserId" v-model:store-id="storeId" :owner-user-ids="adminSession.session.value?.ownerUserIds" :store-ids="adminSession.session.value?.storeIds" /><button class="admin-button-v2" type="button" @click="applyScope">应用范围</button></div>
        <AdminPanelState v-if="!activeCanRead" state="blocked" title="组织数据受控" :message="tab === 'users' ? '当前管理员会话没有用户读取权限，页面不会请求或展示用户数据。' : '当前管理员会话没有门店读取权限，页面不会请求或展示门店数据。'" /><AdminPanelState v-else-if="loading" state="loading" title="正在读取组织数据" /><AdminPanelState v-else-if="error" state="error" :message="errorMessage" @retry="load" /><AdminPanelState v-else-if="(tab === 'users' ? users : stores).length === 0" state="empty" title="当前范围暂无记录" message="服务端没有返回可见的组织数据，请调整授权范围后重试。" />
        <div v-else class="admin-table-wrap">
          <table v-if="tab === 'users'" class="admin-table-v2"><caption class="sr-only">用户列表</caption><thead><tr><th>用户</th><th>手机号</th><th>状态</th><th>版本</th><th>创建时间</th><th>更新时间</th><th v-if="adminSession.can('admin.user.manage')">操作</th></tr></thead><tbody><tr v-for="item in users" :key="item.userId" tabindex="0" @click="openUser(item)" @keydown.enter="openUser(item)"><td><strong>{{ item.nickname || '未命名用户' }}</strong><small><code>{{ item.userId }}</code></small></td><td>{{ item.phoneMasked || '已脱敏' }}</td><td><AdminStatusBadge :status="statusType(item.status)" :label="statusLabel(item.status)" /></td><td><code>v{{ item.version ?? 0 }}</code></td><td>{{ date(item.createdAt) }}</td><td>{{ date(item.updatedAt) }}</td><td v-if="adminSession.can('admin.user.manage')"><button class="admin-button-v2 admin-button-v2--compact" type="button" :disabled="savingId === item.userId" @click.stop="toggleUser(item)">{{ savingId === item.userId ? '处理中' : statusCode(item.status) === 1 ? '停用' : '恢复' }}</button></td></tr></tbody></table>
          <table v-else class="admin-table-v2"><caption class="sr-only">门店列表</caption><thead><tr><th>门店</th><th>Owner ID</th><th>状态</th><th>版本</th><th>成员</th><th>创建时间</th><th v-if="adminSession.can('admin.store.manage')">操作</th></tr></thead><tbody><tr v-for="item in stores" :key="item.storeId" tabindex="0" @click="openStore(item)" @keydown.enter="openStore(item)"><td><strong>{{ item.name || '未命名门店' }}</strong><small><code>{{ item.storeId }}</code></small></td><td><code>{{ item.ownerUserId }}</code></td><td><AdminStatusBadge :status="statusType(item.status)" :label="statusLabel(item.status)" /></td><td><code>v{{ item.version ?? 0 }}</code></td><td>{{ item.memberCount }}</td><td>{{ date(item.createdAt) }}</td><td v-if="adminSession.can('admin.store.manage')"><button class="admin-button-v2 admin-button-v2--compact" type="button" :disabled="savingId === item.storeId" @click.stop="toggleStore(item)">{{ savingId === item.storeId ? '处理中' : statusCode(item.status) === 1 ? '停用' : '恢复' }}</button></td></tr></tbody></table>
        </div>
        <footer v-if="!loading && !error && total > 0" class="admin-pagination"><span>显示第 {{ page * size + 1 }} - {{ page * size + (tab === 'users' ? users.length : stores.length) }} 条</span><button type="button" :disabled="page === 0" aria-label="上一页" @click="previous"><ChevronLeft aria-hidden="true" /></button><button type="button" :disabled="!hasNext" aria-label="下一页" @click="next"><ChevronRight aria-hidden="true" /></button></footer>
      </article></div>
    </section>

    <button v-if="detailOpen" type="button" class="admin-detail-scrim" aria-label="关闭详情" @click="closeDetail" />
    <aside v-if="detailOpen" class="admin-detail-drawer admin-organization-drawer" :aria-label="detailMode === 'store' ? '门店详情' : '用户详情'">
      <header class="admin-detail-drawer__head"><div><div class="admin-page-v2__crumb">ORGANIZATION / {{ detailMode === 'store' ? 'STORE' : 'USER' }}</div><h2>{{ detailMode === 'store' ? storeDetail?.name || '门店详情' : userDetail?.nickname || '用户详情' }}</h2><p><code>{{ detailMode === 'store' ? storeDetail?.storeId : userDetail?.userId }}</code></p></div><button class="admin-detail-drawer__close" type="button" aria-label="关闭详情" title="关闭详情" @click="closeDetail"><X aria-hidden="true" /></button></header>
      <div class="admin-detail-drawer__body">
        <AdminPanelState v-if="detailLoading" state="loading" title="正在读取详情" /><AdminPanelState v-else-if="detailError" state="error" :message="detailErrorMessage" @retry="detailMode === 'store' && selectedStore ? openStore(selectedStore) : selectedUser && openUser(selectedUser)" />
        <template v-else-if="detailMode === 'user' && userDetail"><div class="admin-detail-summary"><AdminStatusBadge :status="statusType(userDetail.status)" :label="statusLabel(userDetail.status)" /><dl><dt>用户 ID</dt><dd><code>{{ userDetail.userId }}</code></dd><dt>手机号</dt><dd>{{ userDetail.phoneMasked || '已脱敏' }}</dd><dt>创建时间</dt><dd>{{ date(userDetail.createdAt) }}</dd><dt>更新时间</dt><dd>{{ date(userDetail.updatedAt) }}</dd><dt>版本</dt><dd><code>v{{ userDetail.version ?? 0 }}</code></dd><dt>最近登录</dt><dd>服务端未在当前摘要接口提供</dd><dt>成员归属</dt><dd>请从门店详情查看授权关系</dd></dl></div><div class="admin-detail-note"><UserRound aria-hidden="true" /><span>用户详情只显示服务端返回的脱敏摘要；密码、Cookie、Session Token 和密钥不会进入页面。</span></div></template>
        <template v-else-if="detailMode === 'store' && storeDetail"><div class="admin-detail-summary"><div class="admin-detail-summary__top"><AdminStatusBadge :status="statusType(storeDetail.status)" :label="statusLabel(storeDetail.status)" /><button v-if="adminSession.can('admin.store.manage')" class="admin-button-v2 admin-button-v2--compact" type="button" :disabled="savingId === storeDetail.storeId" @click="toggleStore(storeDetail)">{{ savingId === storeDetail.storeId ? '处理中' : statusCode(storeDetail.status) === 1 ? '停用门店' : '恢复门店' }}</button></div><dl><dt>门店 ID</dt><dd><code>{{ storeDetail.storeId }}</code></dd><dt>Owner ID</dt><dd><code>{{ storeDetail.ownerUserId }}</code></dd><dt>创建时间</dt><dd>{{ date(storeDetail.createdAt) }}</dd><dt>更新时间</dt><dd>{{ date(storeDetail.updatedAt) }}</dd><dt>成员数</dt><dd>{{ storeDetail.memberCount }}</dd><dt>版本</dt><dd><code>v{{ storeDetail.version ?? 0 }}</code></dd></dl></div><section class="admin-members-section"><div class="admin-members-section__header"><div><h3>成员关系</h3><p>店长和店员状态由门店成员关系服务返回。</p></div><span class="admin-card-v2__meta">{{ memberTotal }} 人</span></div><AdminPanelState v-if="memberLoading" state="loading" title="正在读取成员" /><AdminPanelState v-else-if="memberError" state="error" :message="memberErrorMessage" @retry="loadMembers" /><AdminPanelState v-else-if="!members.length" state="empty" title="暂无成员" message="服务端没有返回当前门店的成员关系。" /><div v-else class="admin-member-list"><div v-for="member in members" :key="member.userId" class="admin-member-row"><div class="admin-member-row__identity"><strong>{{ member.nickname || '未命名成员' }}</strong><small><code>{{ member.userId }}</code> · {{ member.phoneMasked || '已脱敏' }}</small></div><div class="admin-member-row__role"><span>{{ member.role || '未提供角色' }}</span><small>{{ member.title || '-' }}</small></div><AdminStatusBadge :status="statusType(member.status)" :label="statusLabel(member.status)" /><button v-if="adminSession.can('admin.store.manage')" class="admin-button-v2 admin-button-v2--compact" type="button" :disabled="memberSavingId === member.userId" @click="toggleMember(member)">{{ memberSavingId === member.userId ? '处理中' : statusCode(member.status) === 1 ? '停用' : '恢复' }}</button></div></div><footer v-if="memberTotal > 0" class="admin-pagination admin-pagination--drawer"><span>第 {{ memberPage + 1 }} 页</span><button type="button" :disabled="memberPage === 0 || memberLoading" aria-label="上一页" @click="memberPage -= 1; loadMembers()"><ChevronLeft aria-hidden="true" /></button><button type="button" :disabled="!memberHasNext || memberLoading" aria-label="下一页" @click="memberPage += 1; loadMembers()"><ChevronRight aria-hidden="true" /></button></footer></section></template>
      </div>
    </aside>
  </AdminLayout>
</template>

<style scoped>
.admin-success-note { max-width: 1400px; margin: 0 auto 14px; color: #327a4b; font-size: 12px; }
.admin-success-note svg { width: 14px; height: 14px; margin-right: 5px; vertical-align: -3px; }
.admin-table-v2 tbody tr { cursor: pointer; }
.admin-organization-drawer { width: min(620px, 100vw); }
.admin-detail-summary { display: grid; gap: 20px; }
.admin-detail-summary__top { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.admin-detail-summary dl { display: grid; grid-template-columns: 112px 1fr; gap: 14px 16px; margin: 0; }
.admin-detail-summary dt { color: #a0a0a0; font-size: 11px; }
.admin-detail-summary dd { margin: 0; color: #424242; font-size: 12px; word-break: break-word; }
.admin-detail-note { display: flex; align-items: flex-start; gap: 8px; border-top: 1px solid #f0f0ed; padding-top: 16px; color: #737373; font-size: 11px; line-height: 1.6; }
.admin-detail-note svg { width: 15px; flex: 0 0 auto; color: #a0a0a0; }
.admin-members-section { margin-top: 26px; border-top: 1px solid #e8e8e5; padding-top: 20px; }
.admin-members-section__header { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 14px; }
.admin-members-section h3 { margin: 0; font-size: 14px; }
.admin-members-section p { margin-top: 5px; }
.admin-member-list { display: grid; border: 1px solid #e8e8e5; border-radius: 7px; }
.admin-member-row { display: grid; grid-template-columns: minmax(0, 1.4fr) 90px auto auto; align-items: center; gap: 10px; border-bottom: 1px solid #f0f0ed; padding: 12px; }
.admin-member-row:last-child { border-bottom: 0; }
.admin-member-row__identity, .admin-member-row__role { min-width: 0; }
.admin-member-row__identity strong, .admin-member-row__identity small, .admin-member-row__role span, .admin-member-row__role small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.admin-member-row__identity strong { color: #1d1d1f; font-size: 11px; }
.admin-member-row__identity small, .admin-member-row__role small { margin-top: 4px; color: #8d8d89; font-size: 10px; }
.admin-member-row__role span { color: #555; font-size: 11px; }
.admin-pagination--drawer { padding: 12px 0 0; border-top: 0; }
.sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; }
@media (max-width: 620px) { .admin-member-row { grid-template-columns: minmax(0, 1fr) auto; }.admin-member-row__role { grid-column: 1; }.admin-member-row .admin-status-badge { grid-column: 2; grid-row: 1; }.admin-member-row > button { grid-column: 2; grid-row: 2; } }
</style>
