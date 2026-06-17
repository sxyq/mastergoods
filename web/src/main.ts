import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import { routes } from './app/router/routes'
import { useSession } from './app/stores/session'
import './style.css'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach((to) => {
  const session = useSession()
  if (to.path === '/login') {
    return session.isAuthenticated.value ? '/dashboard' : true
  }
  if (!session.hasAppSession.value) return '/login'
  const permissions = to.meta.permissions as Parameters<typeof session.hasPermission>[0]
  const permissionMode = to.meta.permissionMode as 'all' | 'any' | undefined
  const allowed = permissionMode === 'any'
    ? session.hasAnyPermission(permissions)
    : session.hasPermission(permissions)
  if (!allowed) return '/403'
  return true
})

router.afterEach((to) => {
  document.title = `${String(to.meta.title ?? '经营首页')} - 智慧记 Web 管理端`
})

if (typeof window !== 'undefined') {
  window.addEventListener('zhihuiji:web:api-auth', async (event) => {
    const { status } = (event as CustomEvent<{ status?: number }>).detail ?? {}
    if (status === 401 && router.currentRoute.value.path !== '/login') {
      await router.push('/login')
      return
    }
    if (status === 403 && router.currentRoute.value.path !== '/403') {
      await router.push('/403')
    }
  })
}

createApp(App).use(router).mount('#app')
