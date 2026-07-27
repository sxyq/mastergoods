import { computed, reactive } from 'vue'
import { canAccess, demoMembers, roleLabels, rolePermissions, type Permission, type StoreMember, type StoreRole } from '@/entities/auth/roles'
import * as authApi from '@/shared/api/client'
import { readQueryId } from '@/shared/utils/id'

export type SessionSource = 'api' | 'demo' | ''

export interface EditableStoreMember extends StoreMember {
  permissions: Permission[]
}

export interface MemberDraft {
  phone: string
  name: string
  title: string
  role: Exclude<StoreRole, 'OWNER'>
  status: 0 | 1
}

const state = reactive({
  currentRole: readStoredRole(),
  currentMemberId: 'u-owner',
  localMembers: readStoredMembers(),
  currentTitle: localStorage.getItem('zhihuiji.web.title') ?? roleLabels.OWNER,
  currentMemberStatus: Number(localStorage.getItem('zhihuiji.web.memberStatus') ?? 1) as 0 | 1,
  token: localStorage.getItem('zhihuiji.web.token') ?? '',
  refreshToken: localStorage.getItem('zhihuiji.web.refreshToken') ?? '',
  userId: readStoredEntityId('zhihuiji.web.userId'),
  phone: localStorage.getItem('zhihuiji.web.phone') ?? '13800000001',
  nickname: localStorage.getItem('zhihuiji.web.nickname') ?? '老板',
  storeId: localStorage.getItem('zhihuiji.web.storeId') ?? 'store-main',
  storeName: localStorage.getItem('zhihuiji.web.storeName') ?? '智慧记总店',
  permissions: readStoredPermissions(),
  source: (localStorage.getItem('zhihuiji.web.source') ?? '') as SessionSource,
  loading: false,
  error: '',
})

authApi.configureAuthRuntime({
  getRefreshToken: () => state.refreshToken,
  onAuthRefreshed: (payload) => applyRefreshedAuthPayload(payload),
  onAuthExpired: () => logoutInternal(),
})

const localMemberIndex = computed(() => {
  const map = new Map<string, StoreMember>()
  for (const item of state.localMembers) {
    map.set(item.id, item)
  }
  return map
})
const activeMemberRoleIndex = computed(() => {
  const map = new Map<StoreRole, StoreMember>()
  for (const item of state.localMembers) {
    if (item.status === 1 && !map.has(item.role)) {
      map.set(item.role, item)
    }
  }
  return map
})
const member = computed<StoreMember>(() => {
  if (state.source === 'api') {
    return {
      id: state.userId || 'api-user',
      name: state.nickname,
      role: state.currentRole,
      phone: state.phone,
      storeId: state.storeId,
      storeName: state.storeName,
      status: state.currentMemberStatus,
      title: state.currentTitle || roleLabels[state.currentRole],
    }
  }
  return localMemberIndex.value.get(state.currentMemberId) ?? state.localMembers[0]
})
const role = computed(() => state.currentRole)
const roleLabel = computed(() => roleLabels[state.currentRole])
const isAuthenticated = computed(() => Boolean(state.token))
const hasAppSession = computed(() => state.source === 'demo' || Boolean(state.token))
const source = computed(() => state.source)
const token = computed(() => state.token)
const userId = computed(() => state.userId)
const permissions = computed(() => {
  if (state.currentRole === 'OWNER') return rolePermissions.OWNER
  if (state.permissions.length > 0) return state.permissions
  if (state.source === 'demo') return rolePermissions[state.currentRole]
  return []
})
const permissionSet = computed(() => new Set(permissions.value))
const localMembers = computed<EditableStoreMember[]>(() => {
  const items = state.localMembers
  const result = new Array<EditableStoreMember>(items.length)
  for (let index = 0; index < items.length; index++) {
    const item = items[index]
    result[index] = {
      ...item,
      permissions: rolePermissions[item.role],
    }
  }
  return result
})
const loading = computed(() => state.loading)
const error = computed(() => state.error)

export function useSession() {
  function switchRole(nextRole: StoreRole) {
    if (state.source === 'api') return
    state.currentRole = nextRole
    const nextMember = activeMemberRoleIndex.value.get(nextRole)
    if (nextMember) {
      state.currentMemberId = nextMember.id
      state.phone = nextMember.phone
      state.nickname = nextMember.name
      state.storeId = nextMember.storeId
      state.storeName = nextMember.storeName
    }
    if (state.source === 'demo') {
      state.permissions = rolePermissions[nextRole]
      persist()
    }
  }

  function switchMember(memberId: string) {
    if (state.source === 'api') return
    const nextMember = localMemberIndex.value.get(memberId)
    if (!nextMember) return
    state.currentMemberId = nextMember.id
    state.currentRole = nextMember.role
    state.permissions = rolePermissions[nextMember.role]
    state.phone = nextMember.phone
    state.nickname = nextMember.name
    state.storeId = nextMember.storeId
    state.storeName = nextMember.storeName
    state.source = 'demo'
    persist()
  }

  function hasPermission(required?: readonly Permission[]) {
    if (!required || required.length === 0) return true
    if (state.currentRole === 'OWNER') return true
    if (state.source === 'api') {
      return required.every((permission) => permissionSet.value.has(permission))
    }
    return canAccess(state.currentRole, required)
  }

  function hasAnyPermission(required?: readonly Permission[]) {
    if (!required || required.length === 0) return true
    if (state.currentRole === 'OWNER') return true
    return required.some((permission) => permissionSet.value.has(permission))
  }

  async function login(phone: string, password: string) {
    state.loading = true
    state.error = ''
    try {
      const result = await authApi.login(phone, password)
      applyAuthPayload(result, phone)
      const profile = await authApi.fetchCurrentUser(result.token)
      applyProfile(profile)
      await refreshStoreContext()
      return true
    } catch (error) {
      state.error = error instanceof Error ? error.message : '登录失败'
      return false
    } finally {
      state.loading = false
    }
  }

  async function refreshProfile() {
    if (!state.token) return false
    state.loading = true
    state.error = ''
    try {
      const profile = await authApi.fetchCurrentUser(state.token)
      applyProfile(profile)
      await refreshStoreContext()
      return true
    } catch (error) {
      state.error = error instanceof Error ? error.message : '会话已失效'
      await logout()
      return false
    } finally {
      state.loading = false
    }
  }

  async function refreshStoreContext() {
    if (!state.token) return false
    try {
      const store = await authApi.fetchCurrentStore(state.token)
      applyStoreContext(store)
      return true
    } catch (error) {
      state.error = error instanceof Error ? error.message : '门店权限上下文加载失败'
      return false
    }
  }

  async function logout() {
    if (state.token && state.source === 'api') {
      try {
        await authApi.logout(state.token)
      } catch {
        // 即使后端调用失败也继续清本地状态
      }
    }
    logoutInternal()
  }

  function enterDemo(memberId = 'u-owner') {
    const nextMember = localMemberIndex.value.get(memberId) ?? state.localMembers[0]
    state.token = ''
    state.refreshToken = ''
    state.userId = ''
    state.currentMemberId = nextMember.id
    state.currentRole = nextMember.role
    state.permissions = rolePermissions[nextMember.role]
    state.phone = nextMember.phone
    state.nickname = nextMember.name
    state.storeId = nextMember.storeId
    state.storeName = nextMember.storeName
    state.source = 'demo'
    persist()
  }

  function createLocalMember(draft: MemberDraft) {
    if (state.source === 'api') {
      state.error = '真实门店成员已改由后端维护'
      return false
    }
    if (!canAccess(state.currentRole, ['users:manage'])) {
      state.error = '当前角色不能管理员工权限'
      return false
    }
    const member: StoreMember = {
      id: `u-local-${Date.now()}`,
      name: draft.name.trim() || '新员工',
      role: draft.role,
      phone: draft.phone.trim(),
      storeId: state.storeId || 'store-main',
      storeName: state.storeName || '智慧记总店',
      status: draft.status,
      title: draft.title.trim() || roleLabels[draft.role],
    }
    state.localMembers.push(member)
    persistMembers()
    return true
  }

  function updateLocalMember(memberId: string, patch: Partial<Omit<StoreMember, 'id' | 'storeId' | 'storeName'>>) {
    if (state.source === 'api') {
      state.error = '真实门店成员已改由后端维护'
      return false
    }
    if (!canAccess(state.currentRole, ['users:manage'])) {
      state.error = '当前角色不能管理员工权限'
      return false
    }
    const member = localMemberIndex.value.get(memberId)
    if (!member || member.role === 'OWNER') return false
    Object.assign(member, patch)
    if (member.id === state.currentMemberId) {
      state.currentRole = member.role
      state.permissions = rolePermissions[member.role]
    }
    persistMembers()
    persist()
    return true
  }

  return {
    member,
    role,
    roleLabel,
    isAuthenticated,
    hasAppSession,
    source,
    token,
    userId,
    permissions,
    localMembers,
    loading,
    error,
    switchRole,
    switchMember,
    hasPermission,
    hasAnyPermission,
    login,
    refreshProfile,
    refreshStoreContext,
    logout,
    enterDemo,
    createLocalMember,
    updateLocalMember,
  }
}

function applyAuthPayload(payload: authApi.AuthPayload, phone: string) {
  state.token = payload.token
  state.refreshToken = payload.refreshToken
  state.userId = String(payload.userId)
  state.phone = phone
  state.nickname = phone
  state.storeId = `owner-${payload.userId}`
  state.storeName = '当前 owner 数据域'
  state.source = 'api'
  persist()
}

function applyRefreshedAuthPayload(payload: authApi.AuthPayload) {
  state.token = payload.token
  state.refreshToken = payload.refreshToken
  state.userId = payload.userId ? String(payload.userId) : state.userId
  state.source = 'api'
  persist()
}

function applyProfile(profile: authApi.UserProfile) {
  state.userId = String(profile.id)
  state.phone = profile.phone
  state.nickname = profile.nickname
  state.source = 'api'
  persist()
}

function applyStoreContext(profile: authApi.CurrentStoreProfile) {
  state.storeId = String(profile.storeId)
  state.storeName = profile.storeName
  state.currentRole = profile.role
  state.currentTitle = profile.title
  state.currentMemberStatus = profile.status === 0 ? 0 : 1
  state.permissions = profile.permissions
  state.currentMemberId = `api-${profile.currentUserId}`
  state.source = 'api'
  persist()
}

function logoutInternal() {
  state.token = ''
  state.refreshToken = ''
  state.userId = ''
  state.source = ''
  state.currentRole = 'OWNER'
  state.currentMemberId = 'u-owner'
  state.permissions = []
  state.phone = ''
  state.nickname = ''
  state.storeId = 'store-main'
  state.storeName = '智慧记总店'
  state.currentTitle = roleLabels.OWNER
  state.currentMemberStatus = 1
  clearPersisted()
}

const PERSISTED_FIELDS: ReadonlyArray<{ key: string; read: () => string }> = [
  { key: 'zhihuiji.web.token', read: () => state.token },
  { key: 'zhihuiji.web.refreshToken', read: () => state.refreshToken },
  { key: 'zhihuiji.web.userId', read: () => String(state.userId) },
  { key: 'zhihuiji.web.phone', read: () => state.phone },
  { key: 'zhihuiji.web.nickname', read: () => state.nickname },
  { key: 'zhihuiji.web.storeId', read: () => state.storeId },
  { key: 'zhihuiji.web.storeName', read: () => state.storeName },
  { key: 'zhihuiji.web.storeRole', read: () => state.currentRole },
  { key: 'zhihuiji.web.title', read: () => state.currentTitle },
  { key: 'zhihuiji.web.memberStatus', read: () => String(state.currentMemberStatus) },
  { key: 'zhihuiji.web.permissions', read: () => JSON.stringify(state.permissions) },
  { key: 'zhihuiji.web.source', read: () => state.source },
]

function persist() {
  for (const { key, read } of PERSISTED_FIELDS) localStorage.setItem(key, read())
}

function clearPersisted() {
  for (const { key } of PERSISTED_FIELDS) localStorage.removeItem(key)
  localStorage.removeItem('zhihuiji.web.apiRoleBindings')
}

function persistMembers() {
  localStorage.setItem('zhihuiji.web.localMembers', JSON.stringify(state.localMembers))
}

function readStoredRole(): StoreRole {
  const stored = localStorage.getItem('zhihuiji.web.storeRole') as StoreRole | null
  return stored && stored in rolePermissions ? stored : 'OWNER'
}

function readStoredPermissions(): Permission[] {
  const stored = localStorage.getItem('zhihuiji.web.permissions')
  const isDemoSource = localStorage.getItem('zhihuiji.web.source') === 'demo'
  if (!stored) return isDemoSource ? rolePermissions[readStoredRole()] : []
  try {
    const parsed = JSON.parse(stored)
    if (Array.isArray(parsed)) return parsed as Permission[]
    return isDemoSource ? rolePermissions[readStoredRole()] : []
  } catch {
    return isDemoSource ? rolePermissions[readStoredRole()] : []
  }
}

function readStoredEntityId(key: string) {
  const stored = localStorage.getItem(key)
  if (!stored) return ''
  return readQueryId(stored) ?? ''
}

function readStoredMembers(): StoreMember[] {
  const stored = localStorage.getItem('zhihuiji.web.localMembers')
  if (!stored) return [...demoMembers]
  try {
    const parsed = JSON.parse(stored)
    if (!Array.isArray(parsed)) return [...demoMembers]
    let ownerCount = 0
    for (const item of parsed) {
      if (item?.role === 'OWNER') {
        ownerCount++
      }
    }
    return ownerCount === 1 ? parsed as StoreMember[] : [...demoMembers]
  } catch {
    return [...demoMembers]
  }
}
