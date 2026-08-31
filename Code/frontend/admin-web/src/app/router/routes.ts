import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { adminSession, hasAdminPermission, loadAdminSession } from '@/app/stores/admin-session'
import AdminLayout from '@/app/layouts/AdminLayout.vue'
import AgentConfigPage from '@/pages/AgentConfigPage.vue'
import AgentRunsPage from '@/pages/AgentRunsPage.vue'
import AuditPage from '@/pages/AuditPage.vue'
import ForbiddenPage from '@/pages/ForbiddenPage.vue'
import LoginPage from '@/pages/LoginPage.vue'
import OverviewPage from '@/pages/OverviewPage.vue'
import SystemPage from '@/pages/SystemPage.vue'
import UsersPage from '@/pages/UsersPage.vue'

declare module 'vue-router' {
  interface RouteMeta {
    permission?: string
  }
}

const protectedRoutes: RouteRecordRaw[] = [
  { path: 'overview', name: 'overview', component: OverviewPage, meta: { permission: 'admin.dashboard.read' } },
  { path: 'users', name: 'users', component: UsersPage, meta: { permission: 'admin.user.read' } },
  { path: 'agent/runs', name: 'agent-runs', component: AgentRunsPage, meta: { permission: 'admin.agent.run.read' } },
  { path: 'agent/config', name: 'agent-config', component: AgentConfigPage, meta: { permission: 'admin.agent.config.read' } },
  { path: 'audit', name: 'audit', component: AuditPage, meta: { permission: 'admin.audit.read' } },
  { path: 'system', name: 'system', component: SystemPage, meta: { permission: 'admin.system.read' } },
  { path: 'forbidden', name: 'forbidden', component: ForbiddenPage },
]

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/overview' },
  { path: '/login', name: 'login', component: LoginPage },
  { path: '/', component: AdminLayout, children: protectedRoutes },
  { path: '/:pathMatch(.*)*', redirect: '/overview' },
]

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach(async (to) => {
  if (adminSession.status === 'forbidden') {
    return to.name === 'forbidden' ? true : { name: 'forbidden' }
  }
  if (to.name === 'login') {
    const session = await loadAdminSession()
    const sessionStatus: string = adminSession.status
    if (sessionStatus === 'forbidden') return { name: 'forbidden' }
    return session ? { name: 'overview' } : true
  }
  const session = await loadAdminSession()
  if (!session) {
    const sessionStatus: string = adminSession.status
    if (sessionStatus === 'forbidden') return { name: 'forbidden' }
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.permission && !hasAdminPermission(to.meta.permission)) {
    return { name: 'forbidden' }
  }
  return true
})
