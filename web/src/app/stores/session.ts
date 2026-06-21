import { computed, reactive } from 'vue'
import { canAccess, demoMembers, roleLabels, rolePermissions, type Permission, type StoreMember, type StoreRole } from '@/entities/auth/roles'
import * as authApi from '@/shared/api/client'

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
  userId: Number(localStorage.getItem('zhihuiji.web.userId') ?? 0),
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

export function useSession() {
  const member = computed<StoreMember>(() => {
    if (state.source === 'api') {
      return {
        id: state.userId ? String(state.userId) : 'api-user',
        name: state.nickname,
        role: state.currentRole,
        phone: state.phone,
        storeId: state.storeId,
        storeName: state.storeName,
        status: state.currentMemberStatus,
        title: state.currentTitle || roleLabels[state.currentRole],
      }
    }
    return state.localMembers.find((item) => item.id === state.currentMemberId) ?? state.localMembers[0]
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
  const localMembers = computed<EditableStoreMember[]>(() => state.localMembers.map((item) => ({
    ...item,
    permissions: rolePermissions[item.role],
  })))
  const loading = computed(() => state.loading)
  const error = computed(() => state.error)

  function switchRole(nextRole: StoreRole) {
    if (state.source === 'api') return
    state.currentRole = nextRole
    const nextMember = state.localMembers.find((item) => item.role === nextRole && item.status === 1)
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
    const nextMember = state.localMembers.find((item) => item.id === memberId && item.status === 1)
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

  function hasPermission(required?: Permission[]) {
    if (!required || required.length === 0) return true
    if (state.currentRole === 'OWNER') return true
    if (state.source === 'api') {
      const granted = new Set(permissions.value)
      return required.every((permission) => granted.has(permission))
    }
    return canAccess(state.currentRole, required)
  }

  function hasAnyPermission(required?: Permission[]) {
    if (!required || required.length === 0) return true
    if (state.currentRole === 'OWNER') return true
    const granted = new Set(permissions.value)
    return required.some((permission) => granted.has(permission))
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
      logout()
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

  function logout() {
    logoutInternal()
  }

  function enterDemo(memberId = 'u-owner') {
    const nextMember = state.localMembers.find((item) => item.id === memberId && item.status === 1) ?? state.localMembers[0]
    state.token = ''
    state.refreshToken = ''
    state.userId = 0
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
    const member = state.localMembers.find((item) => item.id === memberId)
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
  state.userId = payload.userId
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
  state.userId = payload.userId || state.userId
  state.source = 'api'
  persist()
}

function applyProfile(profile: authApi.UserProfile) {
  state.userId = profile.id
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
  state.userId = 0
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

function persist() {
  localStorage.setItem('zhihuiji.web.token', state.token)
  localStorage.setItem('zhihuiji.web.refreshToken', state.refreshToken)
  localStorage.setItem('zhihuiji.web.userId', String(state.userId))
  localStorage.setItem('zhihuiji.web.phone', state.phone)
  localStorage.setItem('zhihuiji.web.nickname', state.nickname)
  localStorage.setItem('zhihuiji.web.storeId', state.storeId)
  localStorage.setItem('zhihuiji.web.storeName', state.storeName)
  localStorage.setItem('zhihuiji.web.storeRole', state.currentRole)
  localStorage.setItem('zhihuiji.web.title', state.currentTitle)
  localStorage.setItem('zhihuiji.web.memberStatus', String(state.currentMemberStatus))
  localStorage.setItem('zhihuiji.web.permissions', JSON.stringify(state.permissions))
  localStorage.setItem('zhihuiji.web.source', state.source)
}

function clearPersisted() {
  localStorage.removeItem('zhihuiji.web.token')
  localStorage.removeItem('zhihuiji.web.refreshToken')
  localStorage.removeItem('zhihuiji.web.userId')
  localStorage.removeItem('zhihuiji.web.phone')
  localStorage.removeItem('zhihuiji.web.nickname')
  localStorage.removeItem('zhihuiji.web.storeId')
  localStorage.removeItem('zhihuiji.web.storeName')
  localStorage.removeItem('zhihuiji.web.storeRole')
  localStorage.removeItem('zhihuiji.web.title')
  localStorage.removeItem('zhihuiji.web.memberStatus')
  localStorage.removeItem('zhihuiji.web.permissions')
  localStorage.removeItem('zhihuiji.web.source')
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
  if (!stored) return localStorage.getItem('zhihuiji.web.source') === 'demo' ? rolePermissions[readStoredRole()] : []
  try {
    const parsed = JSON.parse(stored)
    if (Array.isArray(parsed)) return parsed as Permission[]
    return localStorage.getItem('zhihuiji.web.source') === 'demo' ? rolePermissions[readStoredRole()] : []
  } catch {
    return localStorage.getItem('zhihuiji.web.source') === 'demo' ? rolePermissions[readStoredRole()] : []
  }
}

function readStoredMembers(): StoreMember[] {
  const stored = localStorage.getItem('zhihuiji.web.localMembers')
  if (!stored) return [...demoMembers]
  try {
    const parsed = JSON.parse(stored)
    if (!Array.isArray(parsed)) return [...demoMembers]
    const ownerCount = parsed.filter((item) => item?.role === 'OWNER').length
    return ownerCount === 1 ? parsed as StoreMember[] : [...demoMembers]
  } catch {
    return [...demoMembers]
  }
}
