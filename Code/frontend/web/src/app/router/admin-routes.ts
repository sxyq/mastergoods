import type { RouteRecordRaw } from 'vue-router'
import AdminOverviewPage from '@/pages/admin/AdminOverviewPage.vue'

/**
 * Administrator routes are kept separate until the server-derived admin
 * session and authorization guard land in the I1 batch.
 */
export const adminRoutes: RouteRecordRaw[] = [
  {
    path: '/admin/overview',
    name: 'admin-overview',
    component: AdminOverviewPage,
    meta: { title: '管理员后台', adminOnly: true },
  },
]
