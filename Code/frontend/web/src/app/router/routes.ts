import type { Component } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import AppLayout from '@/app/layouts/AppLayout.vue'
import LoginPage from '@/pages/auth/LoginPage.vue'
import ForbiddenPage from '@/pages/ForbiddenPage.vue'
import DashboardPage from '@/pages/dashboard/DashboardPage.vue'
import PartnerArchivePage from '@/pages/archives/partner/PartnerArchivePage.vue'
import ProductArchivePage from '@/pages/archives/product/ProductArchivePage.vue'
import ProductEditPage from '@/pages/archives/product/ProductEditPage.vue'
import SalesOrderListPage from '@/pages/documents/sales/SalesOrderListPage.vue'
import DocumentsOverviewPage from '@/pages/documents/overview/DocumentsOverviewPage.vue'
import SalesOrderEditPage from '@/pages/documents/sales/SalesOrderEditPage.vue'
import SalesOrderDetailPage from '@/pages/documents/sales/SalesOrderDetailPage.vue'
import SalesPaymentPage from '@/pages/documents/sales/SalesPaymentPage.vue'
import SalesReturnPage from '@/pages/documents/sales/SalesReturnPage.vue'
import PurchaseOrderListPage from '@/pages/documents/purchase/PurchaseOrderListPage.vue'
import PurchaseOrderEditPage from '@/pages/documents/purchase/PurchaseOrderEditPage.vue'
import PurchaseOrderDetailPage from '@/pages/documents/purchase/PurchaseOrderDetailPage.vue'
import PurchaseReceiptPage from '@/pages/documents/purchase/PurchaseReceiptPage.vue'
import PurchaseReturnPage from '@/pages/documents/purchase/PurchaseReturnPage.vue'
import PayOrderDetailPage from '@/pages/finance/payment/PayOrderDetailPage.vue'
import InventoryAdjustPage from '@/pages/inventory/adjustment/InventoryAdjustPage.vue'
import ProductLedgerPage from '@/pages/inventory/ledger/ProductLedgerPage.vue'
import InventorySnapshotPage from '@/pages/inventory/snapshot/InventorySnapshotPage.vue'
import FinanceRecordPage from '@/pages/finance/record/FinanceRecordPage.vue'
import DailyExpensePage from '@/pages/finance/record/DailyExpensePage.vue'
import AccountListPage from '@/pages/finance/account/AccountListPage.vue'
import AccountTransferPage from '@/pages/finance/account/AccountTransferPage.vue'
import ContactListPage from '@/pages/archives/contact/ContactListPage.vue'
import ReportsPage from '@/pages/reports/overview/ReportsPage.vue'
import AgentPage from '@/pages/agent/AgentPage.vue'
import StitchScreenPage from '@/pages/StitchScreenPage.vue'
import PlanningOverviewPage from '@/pages/planning/PlanningOverviewPage.vue'
import RoleAccessPage from '@/pages/settings/RoleAccessPage.vue'
import DatabasePage from '@/pages/settings/DatabasePage.vue'
import SettingsOverviewPage from '@/pages/settings/SettingsOverviewPage.vue'
import { mobileReferenceScreens, pcDesktopScreens } from './stitch-screens'
import { adminRoutes } from './admin-routes'

const screenComponentByRoute: Record<string, Component> = {
  '/dashboard': DashboardPage,
  '/documents': DocumentsOverviewPage,
  '/archives/products': ProductArchivePage,
  '/archives/products/edit': ProductEditPage,
  '/archives/customers': PartnerArchivePage,
  '/archives/suppliers': PartnerArchivePage,
  '/archives/contacts': ContactListPage,
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
  '/finance/accounts': AccountListPage,
  '/finance/transfers': AccountTransferPage,
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
  ...adminRoutes,
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
