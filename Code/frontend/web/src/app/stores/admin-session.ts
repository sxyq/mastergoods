import { computed, reactive } from 'vue'
import { fetchAdminSession, type AdminSessionContract } from '@/shared/api/admin'
import type { AdminPermission } from '@/entities/admin/contracts'

const state = reactive<{
  token: string
  session: AdminSessionContract | null
  loading: boolean
  error: unknown
  forbidden: boolean
}>({ token: '', session: null, loading: false, error: null, forbidden: false })

export function useAdminSession() {
  async function ensure(token: string) {
    if (!token) {
      state.session = null
      state.forbidden = true
      state.error = new Error('管理员会话已失效')
      return false
    }
    if (state.session && state.token === token) return true
    state.token = token
    state.loading = true
    state.error = null
    state.forbidden = false
    try {
      state.session = await fetchAdminSession(token)
      return true
    } catch (cause) {
      state.session = null
      state.error = cause
      state.forbidden = isForbidden(cause)
      return false
    } finally {
      state.loading = false
    }
  }

  function can(permission: AdminPermission) {
    return state.session?.permissions.includes(permission) ?? false
  }

  function reset() {
    state.token = ''
    state.session = null
    state.error = null
    state.forbidden = false
  }

  return {
    session: computed(() => state.session),
    loading: computed(() => state.loading),
    error: computed(() => state.error),
    forbidden: computed(() => state.forbidden),
    ensure,
    can,
    reset,
  }
}

function isForbidden(value: unknown) {
  return typeof value === 'object' && value !== null && 'status' in value && (value as { status?: number }).status === 403
}
