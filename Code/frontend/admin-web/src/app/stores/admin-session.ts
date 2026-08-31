import { computed, reactive, readonly } from 'vue'
import { AdminApiError, apiRequest, clearAccessToken, getAccessToken, isAdminApiError, setAdminAuthTokens, setAdminSessionFailureHandler } from '@/shared/api/client'
import { getAdminSession, type AdminSession } from '@/shared/api/admin'

type SessionStatus = 'unknown' | 'loading' | 'authenticated' | 'unauthenticated' | 'forbidden'

type AuthResponse = {
  user_id: string
  token: string
  refresh_token: string
  expires_in: number
}

const state = reactive<{
  status: SessionStatus
  session: AdminSession | null
  error: string
}>({
  status: 'unknown',
  session: null,
  error: '',
})

let inFlight: Promise<AdminSession | null> | null = null
let loginRedirectInFlight = false

setAdminSessionFailureHandler((error) => {
  state.session = null
  state.status = 'unauthenticated'
  state.error = error.message || '管理员会话已失效，请重新登录。'
  void redirectToLogin()
})

export const adminSession = readonly(state)
export const isAdminAuthenticated = computed(() => state.status === 'authenticated')

export function hasAdminPermission(permission: string): boolean {
  return state.session?.permissions.includes(permission) ?? false
}

async function redirectToLogin(): Promise<void> {
  if (loginRedirectInFlight) return
  loginRedirectInFlight = true
  try {
    const { router } = await import('@/app/router/routes')
    const currentRoute = router.currentRoute.value
    if (currentRoute.name === 'login') return
    await router.replace({ name: 'login', query: { redirect: currentRoute.fullPath } })
  } catch {
    // 路由守卫会在下一次导航时再次按当前会话状态处理。
  } finally {
    loginRedirectInFlight = false
  }
}

export async function loadAdminSession(force = false): Promise<AdminSession | null> {
  if (!force && state.status === 'authenticated' && state.session) return state.session
  if (inFlight) return inFlight
  if (!getAccessToken()) {
    state.status = 'unauthenticated'
    state.session = null
    return null
  }

  state.status = 'loading'
  state.error = ''
  inFlight = getAdminSession()
    .then((session) => {
      state.status = 'authenticated'
      state.session = session
      return session
    })
    .catch((error: unknown) => {
      state.session = null
      state.error = error instanceof Error ? error.message : '无法恢复管理员会话。'
      state.status = isAdminApiError(error, 403) ? 'forbidden' : 'unauthenticated'
      clearAccessToken()
      return null
    })
    .finally(() => { inFlight = null })
  return inFlight
}

export async function loginAsAdmin(account: string, password: string): Promise<AdminSession> {
  state.status = 'loading'
  state.error = ''
  try {
    const result = await apiRequest<AuthResponse>('/v2/auth/login', {
      method: 'POST',
      body: JSON.stringify({ phone: account, password }),
    })
    setAdminAuthTokens(result.token, result.refresh_token)
    const session = await loadAdminSession(true)
    if (!session) {
      const sessionStatus: string = state.status
      if (sessionStatus === 'forbidden') {
        throw new AdminApiError('当前账号没有访问管理员后台的权限。', 403, 403)
      }
      throw new Error(state.error || '当前账号没有管理员权限。')
    }
    return session
  } catch (error) {
    clearAccessToken()
    const forbidden = (state.status as SessionStatus) === 'forbidden' || isAdminApiError(error, 403)
    state.status = forbidden ? 'forbidden' : 'unauthenticated'
    state.session = null
    state.error = error instanceof Error ? error.message : '登录失败，请稍后重试。'
    throw error
  }
}

export async function logoutAdmin(): Promise<void> {
  try {
    if (getAccessToken()) await apiRequest<null>('/v2/auth/logout', { method: 'POST' })
  } catch {
    // 本地会话仍需清除，服务端退出失败不会阻止用户离开后台。
  } finally {
    clearAccessToken()
    state.status = 'unauthenticated'
    state.session = null
    state.error = ''
  }
}
