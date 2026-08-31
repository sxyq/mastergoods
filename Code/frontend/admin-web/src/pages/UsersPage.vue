<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { Filter, RefreshCw, Search, Store, UsersRound } from 'lucide-vue-next'
import { hasAdminPermission } from '@/app/stores/admin-session'
import { getAdminStoreMembers, getAdminStores, getAdminUsers, updateAdminMember, updateAdminStore, updateAdminUser, type AdminMember, type AdminStore, type AdminUser } from '@/shared/api/admin'
import { formatDateTime, formatNumber } from '@/shared/utils/format'
import AdminPageHeader from '@/shared/components/AdminPageHeader.vue'
import ConfirmDialog from '@/shared/components/ConfirmDialog.vue'
import StatePanel from '@/shared/components/StatePanel.vue'

type Tab = 'users' | 'stores'
const tab = ref<Tab>('users')
const query = ref('')
const users = ref<AdminUser[]>([])
const stores = ref<AdminStore[]>([])
const userTotal = ref(0)
const storeTotal = ref(0)
const userPage = ref(0)
const storePage = ref(0)
const pageSize = 20
const loading = ref(true)
const error = ref('')
const userError = ref('')
const storeError = ref('')
const selectedUser = ref<AdminUser | null>(null)
const selectedStore = ref<AdminStore | null>(null)
const members = ref<AdminMember[]>([])
const membersLoading = ref(false)
const membersError = ref('')
let membersRequestId = 0
const confirmOpen = ref(false)
const mutationBusy = ref(false)
const mutationError = ref('')
const canManageUser = computed(() => hasAdminPermission('admin.user.manage'))
const canManageStore = computed(() => hasAdminPermission('admin.store.manage'))
const storeName = ref('')
const storeStatus = ref('1')
const editingMemberId = ref<string | null>(null)
const memberRole = ref('')
const memberTitle = ref('')
const memberStatus = ref('1')
const pendingAction = ref<'user' | 'store' | 'member' | null>(null)
const activeTotal = computed(() => tab.value === 'users' ? userTotal.value : storeTotal.value)
const activePage = computed(() => tab.value === 'users' ? userPage.value : storePage.value)
const selected = computed<AdminUser | AdminStore | null>(() => tab.value === 'users' ? selectedUser.value : selectedStore.value)
const activeError = computed(() => tab.value === 'users' ? userError.value : storeError.value)
const maxSize = computed(() => Math.max(userTotal.value, storeTotal.value, 1))
const selectedUserTargetStatus = computed(() => selectedUser.value ? targetStatus(selectedUser.value) : 1)
const confirmationTitle = computed(() => selectedUserTargetStatus.value === 0 ? '确认停用该用户？' : '确认恢复该用户？')
const confirmationLabel = computed(() => selectedUserTargetStatus.value === 0 ? '确认停用' : '确认恢复')
const actionTitle = computed(() => pendingAction.value === 'store' ? '确认保存门店变更？' : pendingAction.value === 'member' ? '确认保存成员变更？' : confirmationTitle.value)
const actionLabel = computed(() => pendingAction.value === 'store' || pendingAction.value === 'member' ? '确认保存' : confirmationLabel.value)

async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  userError.value = ''; storeError.value = ''
  const [usersResult, storesResult] = await Promise.allSettled([
    getAdminUsers({ query: query.value.trim() || undefined, page: userPage.value, size: pageSize }),
    getAdminStores({ page: storePage.value, size: pageSize }),
  ])
  if (usersResult.status === 'fulfilled') { users.value = usersResult.value.items; userTotal.value = usersResult.value.total }
  else { users.value = []; userTotal.value = 0; userError.value = usersResult.reason instanceof Error ? usersResult.reason.message : '无法读取用户信息。' }
  if (storesResult.status === 'fulfilled') { stores.value = storesResult.value.items; storeTotal.value = storesResult.value.total }
  else { stores.value = []; storeTotal.value = 0; storeError.value = storesResult.reason instanceof Error ? storesResult.reason.message : '无法读取门店信息。' }
  if (usersResult.status === 'rejected' && storesResult.status === 'rejected') error.value = '无法读取用户与门店信息。'
  loading.value = false
}

function select(item: AdminUser | AdminStore): void {
  if (tab.value === 'users') selectedUser.value = item as AdminUser
  else {
    const store = item as AdminStore
    selectedStore.value = store
    storeName.value = store.name ?? ''
    storeStatus.value = store.status ?? '1'
    editingMemberId.value = null
    void loadMembers(store)
  }
}

async function loadMembers(store: AdminStore): Promise<void> {
  const requestId = ++membersRequestId
  membersLoading.value = true
  membersError.value = ''
  members.value = []
  try {
    const result = await getAdminStoreMembers(store.store_id, { page: 0, size: pageSize })
    if (requestId !== membersRequestId || selectedStore.value?.store_id !== store.store_id) return
    members.value = result.items
  } catch (reason) {
    if (requestId !== membersRequestId || selectedStore.value?.store_id !== store.store_id) return
    membersError.value = reason instanceof Error ? reason.message : '无法读取成员关系。'
  } finally {
    if (requestId === membersRequestId) membersLoading.value = false
  }
}

function switchTab(next: Tab): void {
  tab.value = next
  if (next === 'stores' && selectedStore.value) void loadMembers(selectedStore.value)
}
function retryMembers(): void {
  if (selectedStore.value) void loadMembers(selectedStore.value)
}
function editMember(member: AdminMember): void {
  editingMemberId.value = member.user_id
  memberRole.value = member.role ?? ''
  memberTitle.value = member.title ?? ''
  memberStatus.value = member.status ?? '1'
}
function statusCode(value: string): number { return value === 'ACTIVE' || value === '1' ? 1 : 0 }
function mutationMessage(reason: unknown, fallback: string): string {
  if (!(reason instanceof Error)) return fallback
  const status = 'status' in reason ? Number((reason as { status?: number }).status) : 0
  if (status === 401) return '会话已过期，请重新登录。'
  if (status === 403) return '当前角色没有执行此操作的权限。'
  if (status === 409) return '数据已被其他操作更新，请刷新后重试。'
  if (status === 422) return reason.message || '请求未通过业务校验。'
  return reason.message || fallback
}
function requestStoreUpdate(): void { if (selectedStore.value && canManageStore.value) { pendingAction.value = 'store'; confirmOpen.value = true } }
function requestMemberUpdate(): void { if (selectedStore.value && editingMemberId.value && canManageStore.value) { pendingAction.value = 'member'; confirmOpen.value = true } }
function requestUserStatus(): void { if (selectedUser.value && canManageUser.value) { pendingAction.value = 'user'; confirmOpen.value = true } }
function cancelMutation(): void { if (!mutationBusy.value) { confirmOpen.value = false; pendingAction.value = null } }
async function changeStore(): Promise<void> {
  if (!selectedStore.value) return
  mutationBusy.value = true; mutationError.value = ''
  try {
    const updated = await updateAdminStore(selectedStore.value.store_id, { name: storeName.value.trim(), status: statusCode(storeStatus.value), owner_user_id: selectedStore.value.owner_user_id, expected_version: selectedStore.value.version, idempotency_key: mutationId(), reason: '管理员后台调整门店信息', confirmed: true })
    selectedStore.value = updated
    stores.value = stores.value.map((item) => item.store_id === updated.store_id ? updated : item)
    storeName.value = updated.name ?? ''; storeStatus.value = updated.status ?? '1'
    confirmOpen.value = false; pendingAction.value = null
  } catch (reason) { mutationError.value = mutationMessage(reason, '门店更新失败。') } finally { mutationBusy.value = false }
}
async function changeMember(): Promise<void> {
  const store = selectedStore.value
  const member = members.value.find((item) => item.user_id === editingMemberId.value)
  if (!store || !member) return
  mutationBusy.value = true; mutationError.value = ''
  try {
    const updated = await updateAdminMember(store.store_id, member.user_id, { role: memberRole.value.trim(), title: memberTitle.value.trim() || null, status: statusCode(memberStatus.value), expected_version: member.version, idempotency_key: mutationId(), reason: '管理员后台调整成员关系', confirmed: true, keep_sessions: false })
    members.value = members.value.map((item) => item.user_id === updated.user_id ? updated : item)
    editingMemberId.value = null; confirmOpen.value = false; pendingAction.value = null
  } catch (reason) { mutationError.value = mutationMessage(reason, '成员关系更新失败。') } finally { mutationBusy.value = false }
}
function confirmMutation(): void {
  if (pendingAction.value === 'store') void changeStore()
  else if (pendingAction.value === 'member') void changeMember()
  else void changeUserStatus()
}
function previousPage(): void { if (activePage.value <= 0) return; if (tab.value === 'users') userPage.value--; else storePage.value--; void load() }
function nextPage(): void { if ((activePage.value + 1) * pageSize >= activeTotal.value) return; if (tab.value === 'users') userPage.value++; else storePage.value++; void load() }
function search(): void { userPage.value = 0; void load() }

function statusLabel(status: string | null): string { return status === '1' || status === 'ACTIVE' ? '启用' : status === '0' || status === 'DISABLED' ? '停用' : status || '未知' }
function targetStatus(user: AdminUser): number { return user.status === '1' || user.status === 'ACTIVE' ? 0 : 1 }
function mutationId(): string { return globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(16).slice(2)}` }

async function changeUserStatus(): Promise<void> {
  if (!selectedUser.value) return
  mutationBusy.value = true
  mutationError.value = ''
  try {
    const updated = await updateAdminUser(selectedUser.value.user_id, {
      status: targetStatus(selectedUser.value),
      expected_version: selectedUser.value.version,
      idempotency_key: mutationId(),
      reason: '管理员后台调整用户状态',
      confirmed: true,
      keep_sessions: false,
    })
    selectedUser.value = updated
    users.value = users.value.map((item) => item.user_id === updated.user_id ? updated : item)
    confirmOpen.value = false
    pendingAction.value = null
  } catch (reason) {
    mutationError.value = mutationMessage(reason, '状态更新失败。')
  } finally { mutationBusy.value = false }
}

onMounted(load)
watch(stores, (items) => {
  if (tab.value === 'stores' && selectedStore.value && !items.some((item) => item.store_id === selectedStore.value?.store_id)) {
    selectedStore.value = null
    ++membersRequestId
    members.value = []
  }
})
</script>

<template>
  <section>
    <AdminPageHeader eyebrow="ORGANIZATION / USERS" title="用户与门店" description="查看授权范围内的用户、门店与成员关系。">
      <template #actions><button class="outline-button" type="button" :disabled="loading" @click="load"><RefreshCw :class="{ spinning: loading }" aria-hidden="true" />刷新</button></template>
    </AdminPageHeader>

    <section class="scale-panel" aria-labelledby="scale-title">
      <header><div><h2 id="scale-title">组织规模</h2><p>用户和门店总数来自各自列表接口的总记录数。</p></div></header>
      <div class="scale-bars">
        <div><span><UsersRound aria-hidden="true" />用户</span><strong>{{ formatNumber(userTotal) }}</strong><i><b :style="{ width: `${Math.max(4, userTotal / maxSize * 100)}%` }" /></i></div>
        <div><span><Store aria-hidden="true" />门店</span><strong>{{ formatNumber(storeTotal) }}</strong><i><b class="store-bar" :style="{ width: `${Math.max(4, storeTotal / maxSize * 100)}%` }" /></i></div>
      </div>
    </section>

    <div class="workspace-grid">
      <section class="table-panel" aria-labelledby="organization-list-title">
        <header class="table-header"><div><h2 id="organization-list-title">{{ tab === 'users' ? '用户列表' : '门店列表' }}</h2><p>{{ formatNumber(activeTotal) }} 条记录</p></div><div class="tabs"><button type="button" :class="{ active: tab === 'users' }" @click="switchTab('users')">用户</button><button type="button" :class="{ active: tab === 'stores' }" @click="switchTab('stores')">门店</button></div></header>
        <div v-if="tab === 'users'" class="toolbar"><label><Search aria-hidden="true" /><input v-model="query" placeholder="搜索昵称或已脱敏手机号" @keyup.enter="search" /></label><button class="outline-button" type="button" @click="search"><Filter aria-hidden="true" />筛选</button></div>
        <StatePanel v-if="loading" state="loading" title="正在读取组织数据" />
        <StatePanel v-else-if="activeError" state="error" :detail="activeError" @retry="load" />
        <div v-else class="table-scroll">
          <table v-if="tab === 'users'"><thead><tr><th scope="col">用户</th><th scope="col">状态</th><th scope="col">更新时间</th></tr></thead><tbody><tr v-for="user in users" :key="user.user_id" :class="{ selected: selectedUser?.user_id === user.user_id }" tabindex="0" :aria-selected="selectedUser?.user_id === user.user_id" @click="select(user)" @keydown.enter.prevent="select(user)" @keydown.space.prevent="select(user)"><td><strong>{{ user.nickname || '未设置昵称' }}</strong><small class="mono">{{ user.user_id }} · {{ user.phone_masked || '—' }}</small></td><td><span class="status" :class="user.status === '1' || user.status === 'ACTIVE' ? 'status--success' : 'status--muted'">{{ statusLabel(user.status) }}</span></td><td>{{ formatDateTime(user.updated_at) }}</td></tr><tr v-if="!users.length"><td colspan="3" class="empty">暂无匹配用户。</td></tr></tbody></table>
          <table v-else><thead><tr><th scope="col">门店</th><th scope="col">成员</th><th scope="col">状态</th></tr></thead><tbody><tr v-for="store in stores" :key="store.store_id" :class="{ selected: selectedStore?.store_id === store.store_id }" tabindex="0" :aria-selected="selectedStore?.store_id === store.store_id" @click="select(store)" @keydown.enter.prevent="select(store)" @keydown.space.prevent="select(store)"><td><strong>{{ store.name || '未命名门店' }}</strong><small class="mono">{{ store.store_id }} · Owner {{ store.owner_user_id }}</small></td><td>{{ formatNumber(store.member_count) }}</td><td><span class="status">{{ statusLabel(store.status) }}</span></td></tr><tr v-if="!stores.length"><td colspan="3" class="empty">暂无门店记录。</td></tr></tbody></table>
        </div>
        <footer class="pagination"><span>第 {{ activePage + 1 }} 页</span><div><button type="button" :disabled="activePage <= 0" @click="previousPage">上一页</button><button type="button" :disabled="(activePage + 1) * pageSize >= activeTotal" @click="nextPage">下一页</button></div></footer>
      </section>

      <aside class="detail-panel" aria-labelledby="detail-title"><template v-if="selected"><header><p>SELECTED RECORD</p><h2 id="detail-title">{{ tab === 'users' ? selectedUser?.nickname || '用户详情' : selectedStore?.name || '门店详情' }}</h2></header><dl v-if="tab === 'users' && selectedUser"><div><dt>用户 ID</dt><dd class="mono">{{ selectedUser.user_id }}</dd></div><div><dt>手机号</dt><dd>{{ selectedUser.phone_masked || '—' }}</dd></div><div><dt>状态</dt><dd>{{ statusLabel(selectedUser.status) }}</dd></div><div><dt>更新时间</dt><dd>{{ formatDateTime(selectedUser.updated_at) }}</dd></div></dl><template v-else-if="selectedStore"><dl><div><dt>门店 ID</dt><dd class="mono">{{ selectedStore.store_id }}</dd></div><div><dt>Owner</dt><dd class="mono">{{ selectedStore.owner_user_id }}</dd></div><div><dt>成员数</dt><dd>{{ formatNumber(selectedStore.member_count) }}</dd></div><div><dt>状态</dt><dd>{{ statusLabel(selectedStore.status) }}</dd></div></dl><section v-if="canManageStore" class="edit-form" aria-label="编辑门店"><label>名称<input v-model="storeName" /></label><label>Owner 用户 ID<output class="readonly-value mono">{{ selectedStore.owner_user_id }}</output></label><label>状态<select v-model="storeStatus"><option value="1">启用</option><option value="0">停用</option></select></label><button class="save-action" type="button" @click="requestStoreUpdate">保存门店</button></section><section class="members-section" aria-labelledby="members-title"><div class="members-heading"><h3 id="members-title">成员关系</h3><span>{{ membersLoading ? '读取中' : `${members.length} 人` }}</span></div><StatePanel v-if="membersLoading" state="loading" title="正在读取成员" /><StatePanel v-else-if="membersError" state="error" :detail="membersError" @retry="retryMembers" /><StatePanel v-else-if="!members.length" state="empty" title="暂无成员关系" /><div v-else class="members-list"><article v-for="member in members" :key="`${member.store_id}-${member.user_id}`" class="member-row"><div><strong>{{ member.nickname || '未设置昵称' }}</strong><small class="mono">{{ member.user_id }} · {{ member.phone_masked || '—' }}</small></div><div class="member-meta"><span>{{ member.role || '未分配角色' }}<em v-if="member.title"> · {{ member.title }}</em></span><span class="status" :class="member.status === '1' || member.status === 'ACTIVE' ? 'status--success' : 'status--muted'">{{ statusLabel(member.status) }}</span><small>有效期：接口未提供</small></div><button v-if="canManageStore && editingMemberId !== member.user_id" class="edit-action" type="button" @click="editMember(member)">编辑</button><form v-if="canManageStore && editingMemberId === member.user_id" class="member-edit-form" @submit.prevent="requestMemberUpdate"><label>角色<input v-model="memberRole" required /></label><label>称谓<input v-model="memberTitle" /></label><label>状态<select v-model="memberStatus"><option value="1">启用</option><option value="0">停用</option></select></label><div><button class="save-action" type="submit">保存成员</button><button class="cancel-action" type="button" @click="editingMemberId = null">取消</button></div></form></article></div></section></template><button v-if="tab === 'users' && canManageUser && selectedUser" class="danger-action" type="button" @click="requestUserStatus">{{ selectedUserTargetStatus === 0 ? '停用用户' : '恢复用户' }}</button><p v-if="mutationError" class="mutation-error" role="alert">{{ mutationError }}</p></template><div v-else class="detail-empty"><UsersRound aria-hidden="true" /><span>选择一条记录以查看详情。</span></div></aside>
    </div>

    <ConfirmDialog :open="confirmOpen" :busy="mutationBusy" :danger="pendingAction === 'user'" :title="actionTitle" description="服务端将重新校验权限并记录本次变更。" :confirm-label="actionLabel" @cancel="cancelMutation" @confirm="confirmMutation" />
  </section>
</template>

<style scoped>
.outline-button { display: inline-flex; height: 32px; align-items: center; gap: 7px; border: 1px solid var(--admin-border); border-radius: 999px; background: #fff; padding: 0 12px; color: var(--admin-foreground); cursor: pointer; font-size: 12px; }.outline-button:hover:not(:disabled) { background: var(--admin-secondary); }.outline-button:disabled { opacity: .65; cursor: wait; }.outline-button :deep(svg) { width: 14px; height: 14px; stroke-width: 1.7; }.spinning { animation: spin .8s linear infinite; }.scale-panel, .table-panel, .detail-panel { border: 1px solid var(--admin-border); border-radius: 8px; background: #fff; }.scale-panel { margin-top: 22px; padding: 18px; }.scale-panel h2, .table-header h2, .detail-panel h2 { margin: 0; font-size: 14px; font-weight: 500; }.scale-panel p, .table-header p { margin: 5px 0 0; color: var(--admin-muted); font-size: 11px; }.scale-bars { display: grid; gap: 14px; margin-top: 22px; }.scale-bars > div { display: grid; grid-template-columns: 115px auto minmax(80px, 1fr); align-items: center; gap: 12px; }.scale-bars span { display: flex; align-items: center; gap: 7px; font-size: 11px; }.scale-bars span :deep(svg) { width: 14px; height: 14px; color: #5f9bc6; }.scale-bars strong { font-size: 14px; font-weight: 500; font-variant-numeric: tabular-nums; }.scale-bars i { display: block; height: 7px; border-radius: 4px; background: var(--admin-secondary); overflow: hidden; }.scale-bars b { display: block; height: 100%; border-radius: inherit; background: #8ab8d7; }.scale-bars .store-bar { background: #68a981; }.workspace-grid { display: grid; grid-template-columns: minmax(0, 1fr) 300px; gap: 8px; margin-top: 8px; }.table-panel { min-width: 0; padding: 18px; }.table-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }.tabs { display: flex; gap: 2px; border: 1px solid var(--admin-border); border-radius: 6px; padding: 2px; }.tabs button { height: 26px; border: 0; border-radius: 4px; background: transparent; padding: 0 8px; color: var(--admin-muted); cursor: pointer; font-size: 10px; }.tabs button.active { background: var(--admin-secondary); color: var(--admin-foreground); }.toolbar { display: flex; gap: 8px; margin-top: 18px; }.toolbar label { display: flex; min-width: 0; height: 32px; flex: 1; align-items: center; gap: 7px; border: 1px solid var(--admin-border); border-radius: 6px; padding: 0 9px; color: var(--admin-muted); }.toolbar label :deep(svg) { width: 14px; height: 14px; }.toolbar input { min-width: 0; width: 100%; border: 0; outline: 0; color: var(--admin-foreground); font-size: 11px; }.table-scroll { overflow-x: auto; margin-top: 14px; }table { width: 100%; min-width: 520px; border-collapse: collapse; }th { height: 34px; border-bottom: 1px solid var(--admin-border); color: var(--admin-muted); font-size: 11px; font-weight: 400; text-align: left; }td { height: 59px; border-bottom: 1px solid var(--admin-border); padding-right: 10px; font-size: 11px; }tbody tr { cursor: pointer; }tbody tr:hover, tbody tr.selected { background: #fafaf8; }td strong, td small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }td strong { font-weight: 500; }td small { margin-top: 5px; color: var(--admin-muted); font-size: 10px; }.mono { color: #65659d; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; }.status { display: inline-flex; border-radius: 999px; background: var(--admin-secondary); padding: 4px 7px; color: var(--admin-muted); font-size: 10px; }.status--success { background: var(--admin-positive-bg); color: var(--admin-positive); }.status--muted { background: var(--admin-secondary); color: var(--admin-muted); }.empty { height: 120px; color: var(--admin-muted); text-align: center; }.pagination { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding-top: 12px; color: var(--admin-muted); font-size: 10px; }.pagination div { display: flex; gap: 6px; }.pagination button { height: 28px; border: 1px solid var(--admin-border); border-radius: 5px; background: #fff; padding: 0 8px; color: var(--admin-foreground); cursor: pointer; font-size: 10px; }.pagination button:disabled { cursor: not-allowed; opacity: .5; }.detail-panel { padding: 18px; }.detail-panel header p { margin: 0; color: var(--admin-muted); font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 9px; letter-spacing: .08em; }.detail-panel h2 { margin-top: 9px; }.detail-panel dl { display: grid; gap: 0; margin: 20px 0 0; }.detail-panel dl div { border-bottom: 1px solid var(--admin-border); padding: 12px 0; }.detail-panel dt { color: var(--admin-muted); font-size: 10px; }.detail-panel dd { margin: 6px 0 0; font-size: 11px; overflow-wrap: anywhere; }.danger-action { width: 100%; height: 32px; margin-top: 20px; border: 1px solid var(--admin-danger); border-radius: 999px; background: #fff; color: var(--admin-danger); cursor: pointer; font-size: 11px; }.danger-action:hover { background: var(--admin-danger-bg); }.mutation-error { margin: 10px 0 0; color: var(--admin-danger); font-size: 10px; }.detail-empty { display: grid; min-height: 230px; place-content: center; gap: 8px; color: var(--admin-muted); text-align: center; font-size: 11px; }.detail-empty :deep(svg) { width: 20px; height: 20px; margin: 0 auto; }@keyframes spin { to { transform: rotate(360deg); } }@media (max-width: 920px) { .workspace-grid { grid-template-columns: 1fr; }.detail-panel { min-height: auto; }}@media (max-width: 600px) { .scale-bars > div { grid-template-columns: 85px auto minmax(55px, 1fr); gap: 8px; }.toolbar { flex-direction: column; }.workspace-grid { margin-top: 8px; } }
.members-section { margin-top: 20px; border-top: 1px solid var(--admin-border); padding-top: 14px; }
.members-heading { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.members-heading h3 { margin: 0; font-size: 12px; font-weight: 500; }
.members-heading span { color: var(--admin-muted); font-size: 10px; }
.members-section :deep(.state-panel) { min-height: 110px; margin-top: 10px; padding: 14px; }
.members-list { display: grid; gap: 0; margin-top: 8px; }
.member-row { display: grid; gap: 8px; border-bottom: 1px solid var(--admin-border); padding: 10px 0; }
.member-row:last-child { border-bottom: 0; }
.member-row strong, .member-row small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.member-row strong { font-size: 11px; font-weight: 500; }
.member-row small { margin-top: 4px; color: var(--admin-muted); font-size: 9px; }
.member-meta { display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: center; gap: 5px 8px; color: var(--admin-muted); font-size: 10px; }
.member-meta > span:first-child { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.member-meta em { font-style: normal; }
.member-meta > small { grid-column: 1 / -1; margin-top: 0; }
.edit-form, .member-edit-form { display: grid; gap: 8px; margin-top: 14px; border: 1px solid var(--admin-border); border-radius: 6px; background: var(--admin-secondary); padding: 10px; }
.edit-form label, .member-edit-form label { display: grid; gap: 4px; color: var(--admin-muted); font-size: 10px; }
.edit-form input, .edit-form select, .member-edit-form input, .member-edit-form select { height: 28px; min-width: 0; border: 1px solid var(--admin-border); border-radius: 4px; background: #fff; padding: 0 7px; color: var(--admin-foreground); font-size: 11px; }
.readonly-value { display: block; min-height: 28px; border: 1px solid var(--admin-border); border-radius: 4px; background: #f7f7f5; padding: 7px; color: var(--admin-muted); overflow-wrap: anywhere; }
.save-action, .cancel-action, .edit-action { height: 28px; border: 1px solid var(--admin-border); border-radius: 999px; background: #fff; padding: 0 10px; color: var(--admin-foreground); cursor: pointer; font-size: 10px; }
.save-action { border-color: var(--admin-foreground); background: var(--admin-foreground); color: #fff; }
.cancel-action { margin-left: 6px; }
.edit-action { justify-self: start; color: var(--admin-muted); }
</style>
