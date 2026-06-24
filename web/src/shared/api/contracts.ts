import type { Permission } from '@/entities/auth/roles'

export type ApiMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'

export interface ApiContract {
  method: ApiMethod
  path: string
  purpose: string
  permission: Permission
  tables: string[]
}

export const endpointCatalog = {
  auth: [
    api('POST', '/v1/auth/login', '账号登录，返回 accessToken、refreshToken 和当前用户信息', 'dashboard:view', ['users', 'sessions']),
    api('GET', '/v1/auth/users/me', '读取当前登录用户资料；后续扩展 store、role、permissions', 'dashboard:view', ['users', 'stores', 'store_memberships']),
  ],
  dashboard: [
    api('GET', '/v1/admin/summary', '经营首页汇总指标、待办和系统状态', 'dashboard:view', ['products', 'sale_orders', 'purchase_orders', 'finance_records']),
    api('GET', '/v1/reports/sales-summary', '销售趋势、收款和利润概览', 'reports:view', ['sale_orders', 'sale_order_items', 'payments']),
    api('GET', '/v1/reports/cashflow-summary', '现金流、账户余额和日常支出概览', 'finance:view', ['finance_records', 'accounts', 'cash_change_records']),
  ],
  products: [
    api('GET', '/v2/products', '商品列表、库存、价格、分类和供应商筛选', 'archives:view', ['products', 'product_categories', 'product_units', 'inventory_snapshots']),
    api('POST', '/v2/products', '新增商品档案', 'archives:write', ['products']),
    api('PUT', '/v2/products/{id}', '编辑商品资料、价格和库存预警线', 'archives:write', ['products']),
    api('GET', '/v2/product-categories', '商品分类选项', 'archives:view', ['product_categories']),
    api('GET', '/v2/product-units', '商品单位选项', 'archives:view', ['product_units']),
  ],
  partners: [
    api('GET', '/v2/customers', '客户档案列表与欠款查询', 'archives:view', ['customers', 'customer_groups', 'customer_contacts']),
    api('GET', '/v2/suppliers', '供应商档案列表、应付款和联系人', 'archives:view', ['suppliers', 'supplier_groups', 'supplier_contacts']),
    api('POST', '/v2/customers', '新增客户档案', 'archives:write', ['customers']),
    api('POST', '/v2/suppliers', '新增供应商档案', 'archives:write', ['suppliers']),
  ],
  sales: [
    api('GET', '/v2/sale-orders', '销售单列表、状态、金额、商品和收款筛选', 'sales:view', ['sale_orders', 'sale_order_items', 'payments']),
    api('POST', '/v2/sale-orders', '新建销售单草稿', 'sales:write', ['sale_orders', 'sale_order_items']),
    api('GET', '/v2/sale-orders/{id}', '销售单详情、商品明细和收款记录', 'sales:view', ['sale_orders', 'sale_order_items', 'payments']),
    api('PUT', '/v2/sale-orders/{id}', '编辑销售单草稿', 'sales:write', ['sale_orders', 'sale_order_items']),
    api('PUT', '/v2/sale-orders/{id}/confirm', '确认销售单并写入库存/资金影响', 'sales:write', ['sale_orders', 'inventory_ledger', 'bill_fund_links']),
    api('POST', '/v2/sale-orders/{id}/payments', '销售收款', 'finance:write', ['payments', 'finance_records', 'bill_fund_links']),
    api('GET', '/v2/sales-returns', '销售退货列表与退货详情', 'sales:view', ['sales_returns', 'sales_return_items']),
    api('POST', '/v2/sales-returns', '创建销售退货单', 'sales:write', ['sales_returns', 'sales_return_items', 'inventory_ledger']),
    api('GET', '/v2/sales-returns/{id}', '读取销售退货单详情', 'sales:view', ['sales_returns', 'sales_return_items']),
    api('GET', '/v2/sales-returns/by-order/{orderId}', '按来源销售单查询退货单', 'sales:view', ['sales_returns']),
    api('PUT', '/v2/sales-returns/{id}/draft', '更新销售退货草稿备注', 'sales:write', ['sales_returns']),
    api('PUT', '/v2/sales-returns/{id}/confirm', '确认销售退货并回写库存/客户余额', 'sales:write', ['sales_returns', 'inventory_ledger']),
    api('POST', '/v2/sales-returns/{id}/refunds', '登记销售退货退款', 'finance:write', ['payments', 'sales_returns']),
    api('PUT', '/v2/sales-returns/{id}/cancel', '取消销售退货单', 'sales:write', ['sales_returns', 'inventory_ledger']),
  ],
  purchase: [
    api('GET', '/v2/purchase-orders', '采购单列表、状态和供应商筛选', 'purchase:view', ['purchase_orders', 'purchase_order_items']),
    api('POST', '/v2/purchase-orders', '新建采购单', 'purchase:write', ['purchase_orders', 'purchase_order_items']),
    api('GET', '/v2/purchase-orders/{id}', '采购单详情和商品明细', 'purchase:view', ['purchase_orders', 'purchase_order_items']),
    api('PUT', '/v2/purchase-orders/{id}', '编辑采购单', 'purchase:write', ['purchase_orders', 'purchase_order_items']),
    api('GET', '/v2/purchase-receipts', '采购入库单列表', 'inventory:view', ['purchase_receipts', 'purchase_receipt_items']),
    api('POST', '/v2/purchase-receipts', '采购入库并更新库存流水', 'inventory:write', ['purchase_receipts', 'purchase_receipt_items', 'inventory_ledger']),
    api('GET', '/v2/purchase-returns', '采购退货列表与详情', 'purchase:view', ['purchase_returns', 'purchase_return_items', 'purchase_return_refunds']),
    api('POST', '/v2/purchase-returns', '创建采购退货单', 'purchase:write', ['purchase_returns', 'purchase_return_items']),
    api('GET', '/v2/purchase-returns/{id}', '读取采购退货单详情', 'purchase:view', ['purchase_returns', 'purchase_return_items', 'purchase_return_refunds']),
    api('GET', '/v2/purchase-returns/by-order/{orderId}', '按来源采购单查询采购退货单', 'purchase:view', ['purchase_returns']),
    api('PUT', '/v2/purchase-returns/{id}/draft', '更新采购退货草稿备注', 'purchase:write', ['purchase_returns']),
    api('PUT', '/v2/purchase-returns/{id}/confirm', '确认采购退货并回写库存、采购入库与供应商余额', 'purchase:write', ['purchase_returns', 'inventory_ledger', 'suppliers']),
    api('POST', '/v2/purchase-returns/{id}/refunds', '登记采购退货退款', 'finance:write', ['purchase_return_refunds', 'purchase_returns']),
    api('PUT', '/v2/purchase-returns/{id}/cancel', '取消采购退货单', 'purchase:write', ['purchase_returns', 'inventory_ledger', 'suppliers']),
  ],
  inventory: [
    api('GET', '/v2/inventory/snapshots', '库存盘点、当前库存和低库存预警', 'inventory:view', ['inventory_snapshots', 'products']),
    api('GET', '/v2/inventory/ledger/by-source', '商品库存流水和来源单据追踪', 'inventory:view', ['inventory_ledger']),
    api('POST', '/v2/inventory/adjustments', '库存调整、盘盈盘亏和备注', 'inventory:write', ['inventory_adjustments', 'inventory_ledger']),
  ],
  finance: [
    api('GET', '/v2/accounts', '资金账户列表和余额', 'finance:view', ['accounts']),
    api('POST', '/v2/accounts', '新增资金账户', 'finance:write', ['accounts']),
    api('GET', '/v1/finance-records', '资金流水、日常支出和收付款记录', 'finance:view', ['finance_records']),
    api('GET', '/v2/cash-change-records', '找零记录列表、按单据或账户筛选', 'finance:view', ['cash_change_records', 'accounts']),
    api('POST', '/v2/cash-change-records', '创建找零记录并回写关联账户余额', 'finance:write', ['cash_change_records', 'accounts']),
    api('GET', '/v2/pay-orders', '付款单列表与详情', 'finance:view', ['pay_orders']),
    api('POST', '/v2/pay-orders', '创建付款单', 'finance:write', ['pay_orders', 'finance_records']),
    api('GET', '/v2/pay-orders/{id}', '读取付款单详情', 'finance:view', ['pay_orders']),
    api('PUT', '/v2/pay-orders/{id}/status', '更新付款单状态并触发账户/供应商联动', 'finance:write', ['pay_orders', 'accounts', 'suppliers']),
    api('POST', '/v2/bill-fund-links', '业务单据与资金流水关联', 'finance:write', ['bill_fund_links']),
  ],
  reports: [
    api('GET', '/v1/reports/sales-summary', '经营概览中的销售汇总', 'reports:view', ['sale_orders', 'payments']),
    api('GET', '/v1/reports/sales-trend', '销售趋势图和订单趋势', 'reports:view', ['sale_orders']),
    api('GET', '/v1/reports/profit-summary', '经营利润汇总', 'reports:view', ['sale_order_items', 'products']),
    api('GET', '/v1/reports/refund-records', '退款记录明细', 'reports:view', ['payments']),
    api('GET', '/v1/reports/stock-out-records', '销售出库记录', 'reports:view', ['sale_order_items']),
    api('GET', '/v1/reports/top-products', '热销商品排行', 'reports:view', ['sale_order_items']),
    api('GET', '/v1/reports/profit-by-products', '商品利润排行', 'reports:view', ['sale_order_items']),
    api('GET', '/v1/reports/profit-by-customers', '客户利润排行', 'reports:view', ['sale_order_items', 'customers']),
    api('GET', '/v1/reports/inventory-flow', '库存流向分析', 'reports:view', ['inventory_ledger']),
    api('GET', '/v1/reports/customer-sales', '客户销售汇总', 'reports:view', ['sale_orders', 'customers']),
    api('GET', '/v1/reports/top-receivable-customers', '应收客户排行', 'reports:view', ['customers']),
    api('GET', '/v1/reports/low-stock-products', '低库存预警', 'reports:view', ['products']),
    api('GET', '/v1/reports/reconciliation-summary', '往来对账汇总', 'reports:view', ['customers', 'suppliers', 'finance_records']),
    api('GET', '/v1/reports/cashflow-summary', '现金流汇总', 'reports:view', ['finance_records']),
  ],
  agent: [
    api('GET', '/v2/agent/conversations', 'AI 助手会话列表', 'agent:view', ['agent_conversations', 'agent_messages']),
    api('POST', '/v2/agent/conversations', '创建 AI 会话', 'agent:write', ['agent_conversations']),
    api('PUT', '/v2/agent/conversations/{id}', '更新 AI 会话标题或状态', 'agent:write', ['agent_conversations']),
    api('DELETE', '/v2/agent/conversations/{id}', '删除 AI 会话', 'agent:write', ['agent_conversations', 'agent_messages']),
    api('GET', '/v2/agent/conversations/{conversationId}/messages', '读取会话消息', 'agent:view', ['agent_messages']),
    api('POST', '/v2/agent/conversations/{conversationId}/messages', '手动写入会话消息', 'agent:write', ['agent_messages']),
    api('GET', '/v2/agent/drafts', '读取 AI 草稿列表', 'agent:view', ['agent_drafts']),
    api('POST', '/v2/agent/drafts', '创建 AI 草稿', 'agent:write', ['agent_drafts']),
    api('PUT', '/v2/agent/drafts/{id}', '更新 AI 草稿', 'agent:write', ['agent_drafts']),
    api('DELETE', '/v2/agent/drafts/{id}', '删除 AI 草稿', 'agent:write', ['agent_drafts']),
    api('GET', '/v2/agent/workbench', '读取 AI 工作台摘要、KPI 和快捷问题', 'agent:view', ['agent_conversations', 'agent_tasks', 'agent_notifications']),
    api('GET', '/v2/agent/tasks', '读取 AI 任务列表', 'agent:view', ['agent_tasks']),
    api('GET', '/v2/agent/notifications', '读取 AI 通知列表', 'agent:view', ['agent_notifications']),
    api('POST', '/v2/agent/notifications/{id}/read', '标记 AI 通知已读', 'agent:write', ['agent_notifications']),
    api('POST', '/v2/agent/chat', '基于店铺数据的 AI 问答', 'agent:write', ['agent_messages', 'agent_run_audits']),
    api('POST', '/v2/agent/chat/stream', '流式 AI 问答与运行轨迹', 'agent:write', ['agent_messages', 'agent_run_audits']),
    api('POST', '/v2/agent/runs/{runId}/cancel', '取消 AI 运行', 'agent:write', ['agent_run_audits']),
    api('GET', '/v2/agent/runs/{runId}/audit', 'AI 运行审计和损失指标', 'agent:view', ['agent_run_audits']),
  ],
  sync: [
    api('GET', '/v2/sync/health', '同步、数据库连接和导入健康检查', 'database:manage', ['sync_cursors', 'import_jobs']),
    api('POST', '/v2/import-jobs', '旧 SQLite 数据导入任务', 'database:manage', ['import_jobs']),
    api('POST', '/v1/admin/migration/import-legacy', '管理员触发旧库导入', 'database:manage', ['import_jobs']),
  ],
  rbac: [
    api('GET', '/v1/admin/users', '兼容旧的真实登录账号管理入口；当前门店成员主入口已切到 /v2/stores/current/members', 'users:manage', ['users', 'sessions']),
    api('POST', '/v1/admin/users', '兼容旧的真实登录账号创建入口', 'users:manage', ['users']),
    api('PUT', '/v1/admin/users/{id}', '兼容旧的昵称、密码、启停状态与会话保留策略更新入口', 'users:manage', ['users', 'sessions']),
    api('GET', '/v2/stores/current', '读取当前门店、当前成员身份、角色和权限', 'users:manage', ['stores', 'store_memberships']),
    api('GET', '/v2/stores/current/members', '读取店铺成员、角色、岗位、权限与会话数量', 'users:manage', ['stores', 'store_memberships', 'users']),
    api('POST', '/v2/stores/current/members', '创建员工账号并绑定门店角色', 'users:manage', ['stores', 'store_memberships', 'users']),
    api('PUT', '/v2/stores/current/members/{id}', '调整员工昵称、角色、状态、密码与会话保留策略', 'users:manage', ['stores', 'store_memberships', 'users']),
  ],
}

export const routeContractKeys: Record<string, (keyof typeof endpointCatalog)[]> = {
  '/dashboard': ['dashboard', 'rbac'],
  '/planning': ['dashboard', 'products', 'partners', 'sales', 'purchase', 'inventory', 'finance', 'agent', 'sync', 'rbac'],
  '/archives/products': ['products', 'inventory'],
  '/archives/products/edit': ['products'],
  '/archives/suppliers': ['partners', 'finance'],
  '/archives/customers': ['partners', 'sales'],
  '/documents/sales': ['sales'],
  '/documents/sales/edit': ['sales', 'products', 'partners'],
  '/documents/sales/detail': ['sales', 'finance', 'inventory'],
  '/documents/sales/payment': ['sales', 'finance'],
  '/documents/sales-returns': ['sales', 'inventory'],
  '/documents/purchases': ['purchase', 'partners'],
  '/documents/purchases/edit': ['purchase', 'products', 'partners'],
  '/documents/purchases/detail': ['purchase', 'finance', 'inventory'],
  '/documents/purchase-receipts': ['purchase', 'inventory'],
  '/documents/purchase-returns': ['purchase', 'inventory'],
  '/documents/purchase-returns/interactive': ['purchase', 'inventory'],
  '/documents/pay-orders/detail': ['finance', 'purchase'],
  '/documents': ['sales', 'purchase'],
  '/inventory/adjust': ['inventory'],
  '/inventory/product-ledger': ['inventory'],
  '/inventory/product-ledger-light': ['inventory'],
  '/inventory/snapshots': ['inventory'],
  '/finance/records/detail': ['finance'],
  '/finance/records/detail-aurora': ['finance'],
  '/finance/daily-expense': ['finance'],
  '/reports': ['reports', 'finance', 'sales', 'purchase'],
  '/reports/light': ['reports', 'finance', 'sales', 'purchase'],
  '/agent': ['agent'],
  '/agent/thinking': ['agent'],
  '/agent/deep-thinking': ['agent'],
  '/settings': ['sync', 'rbac'],
  '/settings/roles': ['rbac'],
  '/settings/database': ['sync'],
}

const routeContractCache = new Map<string, ApiContract[]>(
  Object.entries(routeContractKeys).map(([route, keys]) => {
    const contracts: ApiContract[] = []
    for (const key of keys) {
      contracts.push(...endpointCatalog[key])
    }
    return [route, contracts] as const
  }),
)

export function contractsForRoute(route: string): ApiContract[] {
  const normalizedRoute = route.startsWith('/references/mobile/')
    ? route.replace('/references/mobile', '')
    : route
  return routeContractCache.get(normalizedRoute)
    ?? routeContractCache.get(route)
    ?? endpointCatalog.dashboard
}

function api(method: ApiMethod, path: string, purpose: string, permission: Permission, tables: string[]): ApiContract {
  return { method, path, purpose, permission, tables }
}
