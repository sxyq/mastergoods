import type { Component } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import AppLayout from '@/app/layouts/AppLayout.vue'
import LoginPage from '@/pages/auth/LoginPage.vue'
import ForbiddenPage from '@/pages/ForbiddenPage.vue'
import DashboardPage from '@/pages/dashboard/DashboardPage.vue'
import PartnerArchivePage from '@/pages/archives/PartnerArchivePage.vue'
import ProductArchivePage from '@/pages/archives/ProductArchivePage.vue'
import ProductEditPage from '@/pages/archives/ProductEditPage.vue'
import SalesOrderListPage from '@/pages/documents/SalesOrderListPage.vue'
import DocumentsOverviewPage from '@/pages/documents/DocumentsOverviewPage.vue'
import SalesOrderEditPage from '@/pages/documents/SalesOrderEditPage.vue'
import SalesOrderDetailPage from '@/pages/documents/SalesOrderDetailPage.vue'
import SalesPaymentPage from '@/pages/documents/SalesPaymentPage.vue'
import SalesReturnPage from '@/pages/documents/SalesReturnPage.vue'
import PurchaseOrderListPage from '@/pages/documents/PurchaseOrderListPage.vue'
import PurchaseOrderEditPage from '@/pages/documents/PurchaseOrderEditPage.vue'
import PurchaseOrderDetailPage from '@/pages/documents/PurchaseOrderDetailPage.vue'
import PurchaseReceiptPage from '@/pages/documents/PurchaseReceiptPage.vue'
import PurchaseReturnPage from '@/pages/documents/PurchaseReturnPage.vue'
import PayOrderDetailPage from '@/pages/finance/PayOrderDetailPage.vue'
import InventoryAdjustPage from '@/pages/inventory/InventoryAdjustPage.vue'
import ProductLedgerPage from '@/pages/inventory/ProductLedgerPage.vue'
import InventorySnapshotPage from '@/pages/inventory/InventorySnapshotPage.vue'
import FinanceRecordPage from '@/pages/finance/FinanceRecordPage.vue'
import DailyExpensePage from '@/pages/finance/DailyExpensePage.vue'
import ReportsPage from '@/pages/reports/ReportsPage.vue'
import AgentPage from '@/pages/agent/AgentPage.vue'
import StitchScreenPage from '@/pages/StitchScreenPage.vue'
import PlanningOverviewPage from '@/pages/planning/PlanningOverviewPage.vue'
import RoleAccessPage from '@/pages/settings/RoleAccessPage.vue'
import DatabasePage from '@/pages/settings/DatabasePage.vue'
import SettingsOverviewPage from '@/pages/settings/SettingsOverviewPage.vue'
import { mobileReferenceScreens, pcDesktopScreens } from './stitch-screens'

const screenComponentByRoute: Record<string, Component> = {
  '/dashboard': DashboardPage,
  '/documents': DocumentsOverviewPage,
  '/archives/products': ProductArchivePage,
  '/archives/products/edit': ProductEditPage,
  '/archives/customers': PartnerArchivePage,
  '/archives/suppliers': PartnerArchivePage,
  '/documents/sales': SalesOrderListPage,
  '/documents/sales/edit': SalesOrderEditPage,
  '/documents/sales/detail': SalesOrderDetailPage,
  '/documents/sales/payment': SalesPaymentPage,
  '/documents/sales-returns': SalesReturnPage,
  '/documents/purchases': PurchaseOrderListPage,
  '/documents/purchases/edit': PurchaseOrderEditPage,
  '/documents/purchases/detail': PurchaseOrderDetailPage,
  '/documents/purchase-receipts': PurchaseReceiptPage,
  '/documents/purchase-returns': PurchaseReturnPage,
  '/documents/pay-orders/detail': PayOrderDetailPage,
  '/inventory/adjust': InventoryAdjustPage,
  '/inventory/product-ledger': ProductLedgerPage,
  '/inventory/snapshots': InventorySnapshotPage,
  '/finance/records/detail': FinanceRecordPage,
  '/finance/daily-expense': DailyExpensePage,
  '/reports': ReportsPage,
  '/agent': AgentPage,
  '/planning': PlanningOverviewPage,
  '/settings': SettingsOverviewPage,
}

function resolveScreenComponent(route: string) {
  return screenComponentByRoute[route] ?? StitchScreenPage
}

const stitchRoutes: RouteRecordRaw[] = [...pcDesktopScreens, ...mobileReferenceScreens].map((screen) => ({
  path: screen.route.replace(/^\//, ''),
  name: `${screen.source}-${screen.order}-${screen.id}`,
  component: resolveScreenComponent(screen.route),
  meta: {
    title: screen.title,
    screen,
    permissions: screen.permission,
    permissionMode: screen.permissionMode,
  },
}))

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
    redirect: '/dashboard',
    children: [
      ...stitchRoutes,
      {
        path: 'settings/roles',
        name: 'role-access',
        component: RoleAccessPage,
        meta: { title: '角色权限', permissions: ['users:manage'] },
      },
      {
        path: 'settings/database',
        name: 'database',
        component: DatabasePage,
        meta: { title: '数据库管理', permissions: ['database:manage'] },
      },
    ],
  },
]
