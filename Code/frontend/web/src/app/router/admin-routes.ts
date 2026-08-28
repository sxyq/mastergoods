import type { RouteRecordRaw } from 'vue-router'
import AdminOverviewPage from '@/pages/admin/AdminOverviewPage.vue'
import AdminUsersPage from '@/pages/admin/AdminUsersPage.vue'
import AdminAgentRunsPage from '@/pages/admin/AdminAgentRunsPage.vue'
import AdminAgentConfigPage from '@/pages/admin/AdminAgentConfigPage.vue'
import AdminAuditPage from '@/pages/admin/AdminAuditPage.vue'
import AdminSystemPage from '@/pages/admin/AdminSystemPage.vue'

/**
 * Administrator routes use the regular router guard for session presence and
 * each page performs its own server-derived permission check before querying.
 */
export const adminRoutes: RouteRecordRaw[] = [
  {
    path: '/admin/overview',
    name: 'admin-overview',
    component: AdminOverviewPage,
    meta: { title: '平台总览', adminOnly: true },
  },
  {
    path: '/admin/users',
    name: 'admin-users',
    component: AdminUsersPage,
    meta: { title: '用户与门店', adminOnly: true },
  },
  {
    path: '/admin/agent/runs',
    name: 'admin-agent-runs',
    component: AdminAgentRunsPage,
    meta: { title: 'Agent 运行', adminOnly: true },
  },
  {
    path: '/admin/agent/config',
    name: 'admin-agent-config',
    component: AdminAgentConfigPage,
    meta: { title: 'Agent 配置', adminOnly: true },
  },
  {
    path: '/admin/audit',
    name: 'admin-audit',
    component: AdminAuditPage,
    meta: { title: '操作审计', adminOnly: true },
  },
  {
    path: '/admin/system',
    name: 'admin-system',
    component: AdminSystemPage,
    meta: { title: '系统状态', adminOnly: true },
  },
]
