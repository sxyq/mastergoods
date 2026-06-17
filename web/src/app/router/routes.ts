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

function resolveScreenComponent(route: string) {
  if (route === '/dashboard') return DashboardPage
  if (route === '/documents') return DocumentsOverviewPage
  if (route === '/archives/products') return ProductArchivePage
  if (route === '/archives/products/edit') return ProductEditPage
  if (route === '/archives/customers') return PartnerArchivePage
  if (route === '/archives/suppliers') return PartnerArchivePage
  if (route === '/documents/sales') return SalesOrderListPage
  if (route === '/documents/sales/edit') return SalesOrderEditPage
  if (route === '/documents/sales/detail') return SalesOrderDetailPage
  if (route === '/documents/sales/payment') return SalesPaymentPage
  if (route === '/documents/sales-returns') return SalesReturnPage
  if (route === '/documents/purchases') return PurchaseOrderListPage
  if (route === '/documents/purchases/edit') return PurchaseOrderEditPage
  if (route === '/documents/purchases/detail') return PurchaseOrderDetailPage
  if (route === '/documents/purchase-receipts') return PurchaseReceiptPage
  if (route === '/documents/purchase-returns') return PurchaseReturnPage
  if (route === '/documents/pay-orders/detail') return PayOrderDetailPage
  if (route === '/inventory/adjust') return InventoryAdjustPage
  if (route === '/inventory/product-ledger') return ProductLedgerPage
  if (route === '/inventory/snapshots') return InventorySnapshotPage
  if (route === '/finance/records/detail') return FinanceRecordPage
  if (route === '/finance/daily-expense') return DailyExpensePage
  if (route === '/reports') return ReportsPage
  if (route === '/agent') return AgentPage
  if (route === '/planning') return PlanningOverviewPage
  if (route === '/settings') return SettingsOverviewPage
  return StitchScreenPage
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
