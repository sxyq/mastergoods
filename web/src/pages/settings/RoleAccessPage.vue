<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { roleDescriptions, roleLabels, rolePermissions, type StoreRole } from '@/entities/auth/roles'
import { createAdminUser, fetchAdminUsers, updateAdminUser, type AdminUser } from '@/shared/api/client'
import { useSession } from '@/app/stores/session'

interface AdminUserDraft {
  nickname: string
  password: string
  keepSessions: boolean
  role: StoreRole
  title: string
}

const roles = Object.keys(roleLabels) as StoreRole[]
const employeeRoles = roles.filter((role) => role !== 'OWNER') as Exclude<StoreRole, 'OWNER'>[]
const session = useSession()
const error = ref('')
const success = ref('')
const isApiSource = computed(() => session.source.value === 'api' && Boolean(session.token.value))
const canManageDemo = computed(() => !isApiSource.value && session.hasPermission(['users:manage']))
const canManageApi = computed(() => isApiSource.value && session.hasPermission(['users:manage']))
const displayMembers = computed(() => session.localMembers.value)
const enabledEmployees = computed(() => displayMembers.value.filter((member) => member.role !== 'OWNER' && member.status === 1).length)
const disabledEmployees = computed(() => displayMembers.value.filter((member) => member.status === 0).length)
const apiUsers = ref<AdminUser[]>([])
const apiLoading = ref(false)
const apiSearch = ref('')
const apiDrafts = reactive<Record<number, AdminUserDraft>>({})
const apiEnabledUsers = computed(() => apiUsers.value.filter((user) => user.status === 1).length)
const apiDisabledUsers = computed(() => apiUsers.value.filter((user) => user.status !== 1).length)
const apiActiveSessions = computed(() => apiUsers.value.reduce((sum, user) => sum + user.activeSessions, 0))
const apiBoundUsers = computed(() => apiUsers.value.filter((user) => Boolean(session.getApiRoleBinding(user.id)) || user.id === session.userId.value).length)
const apiUnboundUsers = computed(() => Math.max(apiUsers.value.length - apiBoundUsers.value, 0))
const demoForm = reactive({
  phone: '13800000008',
  name: '新员工',
  title: '门店员工',
  role: 'SALES' as Exclude<StoreRole, 'OWNER'>,
  status: 1 as 0 | 1,
})
const apiForm = reactive({
  phone: '13800000008',
  nickname: '新店员账号',
  password: '123456',
  status: 1,
  role: 'SALES' as Exclude<StoreRole, 'OWNER'>,
  title: '销售员工',
})

watch(
  [isApiSource, () => session.token.value],
  async ([nextIsApi]) => {
    resetFeedback()
    if (!nextIsApi) {
      apiUsers.value = []
      return
    }
    await loadApiUsers()
  },
  { immediate: true },
)

async function loadApiUsers() {
  if (!session.token.value) return
  apiLoading.value = true
  resetFeedback()
  try {
    const users = await fetchAdminUsers(session.token.value, {
      keyword: apiSearch.value.trim() || undefined,
      page: 0,
      size: 200,
    })
    apiUsers.value = [...users].sort((a, b) => b.updatedAt - a.updatedAt)
    syncApiDrafts()
  } catch (loadError) {
    error.value = loadError instanceof Error ? loadError.message : '真实店员账号列表加载失败'
  } finally {
    apiLoading.value = false
  }
}

async function submitApiUser() {
  if (!session.token.value) return
  apiLoading.value = true
  resetFeedback()
  try {
    const created = await createAdminUser(session.token.value, {
      phone: apiForm.phone.trim(),
      nickname: apiForm.nickname.trim(),
      password: apiForm.password.trim(),
      status: apiForm.status,
    })
    session.updateApiRoleBinding(created.id, {
      role: apiForm.role,
      title: apiForm.title.trim() || roleLabels[apiForm.role],
      status: apiForm.status === 1 ? 1 : 0,
    })
    success.value = `真实店员账号「${apiForm.nickname}」已创建`
    apiForm.phone = nextApiPhone()
    apiForm.nickname = '新店员账号'
    apiForm.password = '123456'
    apiForm.status = 1
    apiForm.role = 'SALES'
    apiForm.title = '销售员工'
    await loadApiUsers()
  } catch (submitError) {
    error.value = submitError instanceof Error ? submitError.message : '真实店员账号创建失败'
  } finally {
    apiLoading.value = false
  }
}

async function saveApiUser(userId: number) {
  if (!session.token.value) return
  const user = apiUsers.value.find((item) => item.id === userId)
  const draft = apiDrafts[userId]
  if (!user || !draft) return
  apiLoading.value = true
  resetFeedback()
  try {
    await updateAdminUser(session.token.value, userId, {
      nickname: draft.nickname.trim() || user.nickname,
      password: draft.password.trim() || undefined,
      keepSessions: draft.keepSessions,
      status: user.status,
    })
    if (!isCurrentApiUser(user)) {
      session.updateApiRoleBinding(userId, {
        role: draft.role,
        title: draft.title.trim() || roleLabels[draft.role],
        status: user.status === 1 ? 1 : 0,
      })
    }
    draft.password = ''
    draft.keepSessions = false
    success.value = `店员账号「${draft.nickname.trim() || user.nickname}」已更新`
    await loadApiUsers()
  } catch (submitError) {
    error.value = submitError instanceof Error ? submitError.message : '真实店员账号更新失败'
  } finally {
    apiLoading.value = false
  }
}

async function toggleApiUserStatus(user: AdminUser) {
  if (!session.token.value) return
  const draft = apiDrafts[user.id]
  const nextStatus = user.status === 1 ? 0 : 1
  apiLoading.value = true
  resetFeedback()
  try {
    await updateAdminUser(session.token.value, user.id, {
      nickname: draft?.nickname.trim() || user.nickname,
      status: nextStatus,
    })
    if (!isCurrentApiUser(user)) {
      session.updateApiRoleBinding(user.id, {
        role: draft?.role ?? apiRoleForUser(user).role,
        title: draft?.title.trim() || apiRoleForUser(user).title,
        status: nextStatus,
      })
    }
    success.value = `账号「${user.nickname}」已${user.status === 1 ? '停用' : '启用'}`
    await loadApiUsers()
  } catch (submitError) {
    error.value = submitError instanceof Error ? submitError.message : '账号状态更新失败'
  } finally {
    apiLoading.value = false
  }
}

function submitMember() {
  resetFeedback()
  if (!canManageDemo.value) {
    error.value = '当前角色不能创建或调整员工账号'
    return
  }
  const ok = session.createLocalMember({ ...demoForm })
  if (ok) {
    success.value = `员工「${demoForm.name}」已加入 ${session.member.value.storeName}`
    demoForm.phone = nextDemoPhone()
    demoForm.name = '新员工'
    demoForm.title = '门店员工'
  }
}

function changeMemberRole(memberId: string, role: Exclude<StoreRole, 'OWNER'>) {
  resetFeedback()
  const ok = session.updateLocalMember(memberId, { role, title: roleLabels[role] })
  if (ok) {
    success.value = '员工角色与权限已更新'
    return
  }
  error.value = '店长（总）不能被降级或停用'
}

function toggleMemberStatus(memberId: string, status: 0 | 1) {
  resetFeedback()
  const ok = session.updateLocalMember(memberId, { status: status === 1 ? 0 : 1 })
  if (ok) {
    success.value = '员工状态已更新'
    return
  }
  error.value = '店长（总）必须保持启用'
}

function switchToMember(memberId: string) {
  session.switchMember(memberId)
}

function nextDemoPhone() {
  const maxSuffix = Math.max(...displayMembers.value.map((member) => Number(member.phone.slice(-2))).filter(Number.isFinite), 7)
  return `138000000${String(maxSuffix + 1).padStart(2, '0')}`
}

function nextApiPhone() {
  const maxSuffix = Math.max(...apiUsers.value.map((user) => Number(user.phone.slice(-2))).filter(Number.isFinite), 7)
  return `138000000${String(maxSuffix + 1).padStart(2, '0')}`
}

function syncApiDrafts() {
  const activeIds = new Set(apiUsers.value.map((user) => user.id))
  Object.keys(apiDrafts).forEach((id) => {
    if (!activeIds.has(Number(id))) delete apiDrafts[Number(id)]
  })
  apiUsers.value.forEach((user) => {
    const binding = apiRoleForUser(user)
    apiDrafts[user.id] = {
      nickname: user.nickname,
      password: '',
      keepSessions: apiDrafts[user.id]?.keepSessions ?? false,
      role: binding.role,
      title: binding.title,
    }
  })
}

function apiRoleForUser(user: AdminUser) {
  if (isCurrentApiUser(user)) {
    return { role: 'OWNER' as StoreRole, title: roleLabels.OWNER, status: 1 as 0 | 1 }
  }
  const binding = session.getApiRoleBinding(user.id)
  return {
    role: binding?.role ?? 'ASSISTANT',
    title: binding?.title ?? roleLabels.ASSISTANT,
    status: binding?.status ?? (user.status === 1 ? 1 : 0),
  }
}

function isCurrentApiUser(user: AdminUser) {
  return user.id === session.userId.value
}

function resetFeedback() {
  error.value = ''
  success.value = ''
}

function formatDateTime(timestamp?: number | null) {
  if (!timestamp) return '--'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(timestamp)
}
</script>

<template>
  <section class="panel">
    <p class="eyebrow">Staff & Access</p>
    <h2>店员与权限</h2>
    <p class="muted">
      {{ isApiSource
        ? '当前已切到真实后端模式：本页已接上真实店员账号管理，底层对应 `/v1/admin/users`。账号启停与密码走后端，PC 角色先在 Web 侧绑定，用来控制菜单、页面和按钮权限；后端仍缺 store/member 级角色接口。'
        : '当前 Web 提供完整 PC 端角色规划：一个店铺固定一个店长（总），多个员工按职责拥有不同菜单、页面和操作权限。' }}
    </p>

    <div class="rbac-summary">
      <template v-if="isApiSource">
        <article>
          <span>真实店员账号</span>
          <strong>{{ apiUsers.length }}</strong>
          <small>来自 `/v1/admin/users`</small>
        </article>
        <article>
          <span>启用账号</span>
          <strong>{{ apiEnabledUsers }}</strong>
          <small>可登录真实后端管理端</small>
        </article>
        <article>
          <span>停用账号</span>
          <strong>{{ apiDisabledUsers }}</strong>
          <small>已禁用登录</small>
        </article>
        <article>
          <span>活跃会话</span>
          <strong>{{ apiActiveSessions }}</strong>
          <small>正在生效的登录会话</small>
        </article>
        <article>
          <span>已绑定 PC 角色</span>
          <strong>{{ apiBoundUsers }}</strong>
          <small>含当前店长（总）账号</small>
        </article>
        <article>
          <span>未绑定账号</span>
          <strong>{{ apiUnboundUsers }}</strong>
          <small>默认按只读助理展示</small>
        </article>
      </template>
      <template v-else>
        <article>
          <span>店长（总）</span>
          <strong>1</strong>
          <small>唯一超级账号，不能停用或降级</small>
        </article>
        <article>
          <span>启用员工</span>
          <strong>{{ enabledEmployees }}</strong>
          <small>销售、采购、库存、财务、助理等</small>
        </article>
        <article>
          <span>停用账号</span>
          <strong>{{ disabledEmployees }}</strong>
          <small>不可切换进入，也不显示业务入口</small>
        </article>
        <article>
          <span>当前身份</span>
          <strong>{{ roleLabels[session.role.value] }}</strong>
          <small>{{ session.member.value.name }} / {{ session.member.value.phone }}</small>
        </article>
      </template>
    </div>

    <div v-if="error" class="form-error">{{ error }}</div>
    <div v-if="success" class="form-success">{{ success }}</div>

    <template v-if="isApiSource">
      <section class="member-management">
        <form class="member-form" @submit.prevent="submitApiUser">
          <label>
            手机号
            <input v-model="apiForm.phone" autocomplete="off" :disabled="apiLoading || !canManageApi" />
          </label>
          <label>
            店员昵称
            <input v-model="apiForm.nickname" autocomplete="off" :disabled="apiLoading || !canManageApi" />
          </label>
          <label>
            初始密码
            <input v-model="apiForm.password" autocomplete="new-password" :disabled="apiLoading || !canManageApi" />
          </label>
          <label>
            状态
            <select v-model="apiForm.status" :disabled="apiLoading || !canManageApi">
              <option :value="1">启用</option>
              <option :value="0">停用</option>
            </select>
          </label>
          <label>
            PC 角色
            <select v-model="apiForm.role" :disabled="apiLoading || !canManageApi">
              <option v-for="role in employeeRoles" :key="role" :value="role">{{ roleLabels[role] }}</option>
            </select>
          </label>
          <label>
            岗位
            <input v-model="apiForm.title" autocomplete="off" :disabled="apiLoading || !canManageApi" />
          </label>
          <button type="submit" :disabled="apiLoading || !canManageApi">{{ apiLoading ? '处理中' : '创建店员账号' }}</button>
        </form>

        <div class="member-toolbar">
          <label class="member-search">
            <span>搜索店员账号</span>
            <input v-model="apiSearch" autocomplete="off" placeholder="手机号 / 昵称" @keyup.enter="loadApiUsers" />
          </label>
          <button type="button" class="ghost-action" :disabled="apiLoading" @click="loadApiUsers">
            {{ apiLoading ? '刷新中' : '刷新列表' }}
          </button>
        </div>
      </section>

      <div class="table-shell member-table">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>手机号</th>
              <th>昵称</th>
              <th>状态</th>
              <th>PC 角色</th>
              <th>岗位</th>
              <th>活跃会话</th>
              <th>最近更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in apiUsers" :key="`api-${user.id}`">
              <td>{{ user.id }}</td>
              <td>{{ user.phone }}</td>
              <td>
                <input
                  v-model="apiDrafts[user.id].nickname"
                  class="table-input"
                  autocomplete="off"
                  :disabled="apiLoading || !canManageApi"
                />
              </td>
              <td>
                <span :class="['status-pill', user.status === 1 ? 'on' : 'off']">
                  {{ user.status === 1 ? '启用' : '停用' }}
                </span>
              </td>
              <td>
                <strong v-if="isCurrentApiUser(user)">{{ roleLabels.OWNER }}</strong>
                <select
                  v-else
                  v-model="apiDrafts[user.id].role"
                  class="role-select"
                  :disabled="apiLoading || !canManageApi"
                >
                  <option v-for="role in employeeRoles" :key="role" :value="role">{{ roleLabels[role] }}</option>
                </select>
              </td>
              <td>
                <input
                  v-if="!isCurrentApiUser(user)"
                  v-model="apiDrafts[user.id].title"
                  class="table-input"
                  autocomplete="off"
                  :disabled="apiLoading || !canManageApi"
                />
                <span v-else>{{ roleLabels.OWNER }}</span>
              </td>
              <td>{{ user.activeSessions }}</td>
              <td>{{ formatDateTime(user.updatedAt) }}</td>
              <td class="member-actions">
                <input
                  v-model="apiDrafts[user.id].password"
                  class="table-input"
                  autocomplete="new-password"
                  placeholder="留空不改密码"
                  :disabled="apiLoading || !canManageApi"
                />
                <label class="inline-check">
                  <input v-model="apiDrafts[user.id].keepSessions" type="checkbox" :disabled="apiLoading || !canManageApi" />
                  <span>保留现有会话</span>
                </label>
                <div class="member-action-buttons">
                  <button type="button" class="ghost-action" :disabled="apiLoading || !canManageApi" @click="saveApiUser(user.id)">保存</button>
                  <button type="button" class="ghost-action" :disabled="apiLoading || !canManageApi" @click="toggleApiUserStatus(user)">
                    {{ user.status === 1 ? '停用' : '启用' }}
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="apiUsers.length === 0">
              <td colspan="9" class="empty-cell">当前没有符合条件的真实店员账号</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="form-success">
        当前这张表已经把真实账号和 PC 角色权限联动起来；角色绑定保存在 Web 本地，用于当前 PC 管理端权限体验。真正跨设备、不可篡改的员工权限还需要后端补 store_memberships / permissions 接口。
      </div>
    </template>

    <template v-else>
      <div class="member-management">
        <form class="member-form" @submit.prevent="submitMember">
          <label>
            手机号
            <input v-model="demoForm.phone" autocomplete="off" />
          </label>
          <label>
            姓名
            <input v-model="demoForm.name" autocomplete="off" />
          </label>
          <label>
            岗位
            <input v-model="demoForm.title" autocomplete="off" />
          </label>
          <label>
            角色
            <select v-model="demoForm.role">
              <option v-for="role in employeeRoles" :key="role" :value="role">{{ roleLabels[role] }}</option>
            </select>
          </label>
          <button type="submit" :disabled="!canManageDemo">创建员工</button>
        </form>
      </div>

      <div class="member-strip">
        <article v-for="member in displayMembers" :key="member.id" :class="{ selected: member.id === session.member.value.id }">
          <span>{{ roleLabels[member.role] }}</span>
          <strong>{{ member.name }}</strong>
          <small>{{ member.title }} / {{ member.phone }}</small>
          <small :class="['status-pill', member.status === 1 ? 'on' : 'off']">
            {{ member.status === 1 ? '启用' : '停用' }}
          </small>
        </article>
      </div>

      <div class="table-shell member-table">
        <table>
          <thead>
            <tr>
              <th>成员</th>
              <th>手机号</th>
              <th>角色</th>
              <th>权限数</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="member in displayMembers" :key="`row-${member.id}`">
              <td>
                <strong>{{ member.name }}</strong>
                <small>{{ member.title }}</small>
              </td>
              <td>{{ member.phone }}</td>
              <td>
                <select
                  v-if="member.role !== 'OWNER'"
                  class="role-select"
                  :value="member.role"
                  :disabled="!canManageDemo"
                  @change="changeMemberRole(member.id, ($event.target as HTMLSelectElement).value as Exclude<StoreRole, 'OWNER'>)"
                >
                  <option v-for="role in employeeRoles" :key="role" :value="role">{{ roleLabels[role] }}</option>
                </select>
                <strong v-else>{{ roleLabels.OWNER }}</strong>
              </td>
              <td>{{ member.permissions.length }}</td>
              <td>{{ member.status === 1 ? '启用' : '停用' }}</td>
              <td>
                <button
                  v-if="member.status === 1"
                  type="button"
                  class="ghost-action"
                  @click="switchToMember(member.id)"
                >
                  切换
                </button>
                <button
                  type="button"
                  class="ghost-action"
                  :disabled="member.role === 'OWNER' || !canManageDemo"
                  @click="toggleMemberStatus(member.id, member.status)"
                >
                  {{ member.status === 1 ? '停用' : '启用' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>

    <div class="role-grid">
      <article v-for="role in roles" :key="role" class="role-card">
        <h3>{{ roleLabels[role] }}</h3>
        <p>{{ roleDescriptions[role] }}</p>
        <div class="permission-list">
          <span v-for="permission in rolePermissions[role]" :key="permission">{{ permission }}</span>
        </div>
      </article>
    </div>
  </section>
</template>
