import type { RouteRecordRaw } from 'vue-router'
import AppLayout from '@/app/layouts/AppLayout.vue'
import LoginPage from '@/pages/auth/LoginPage.vue'
import ForbiddenPage from '@/pages/ForbiddenPage.vue'
import RewriteShellPage from '@/pages/RewriteShellPage.vue'

export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: LoginPage,
    meta: { title: '登录' },
  },
  {
    path: '/403',
    name: 'forbidden',
    component: ForbiddenPage,
    meta: { title: '403 无权访问' },
  },
  {
    path: '/',
    component: AppLayout,
    children: [
      {
        path: '',
        name: 'rewrite-shell',
        component: RewriteShellPage,
        meta: { title: '业务系统重构中' },
      },
    ],
  },
]
