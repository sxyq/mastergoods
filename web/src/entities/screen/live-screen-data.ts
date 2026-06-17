import {
  fetchAccounts,
  fetchAgentNotifications,
  fetchAgentTasks,
  fetchAgentWorkbench,
  fetchCashflowSummary,
  fetchCustomers,
  fetchFinanceRecords,
  fetchInventoryLedger,
  fetchInventorySnapshots,
  fetchLowStockProducts,
  fetchLowStockReport,
  fetchProducts,
  fetchPurchaseReceipts,
  fetchProfitSummary,
  fetchPurchaseOrders,
  fetchSaleOrders,
  fetchSalesReturns,
  fetchSalesSummary,
  fetchSuppliers,
  type AccountRecord,
  type AgentNotification,
  type AgentTask,
  type AgentWorkbench,
  type CustomerRecord,
  type FinanceRecord,
  type InventoryLedgerEntry,
  type InventorySnapshot,
  type ProductRecord,
  type PurchaseReceipt,
  type PurchaseOrder,
  type SaleOrder,
  type SalesReturn,
  type SupplierRecord,
} from '@/shared/api/client'
import type { PageMetric, PageSummaryItem } from './page-models'

const ORDER_DRAFT = 0
const ORDER_COMPLETED = 1
const ORDER_CANCELLED = 2
const ORDER_CONFIRMED = 3

export interface ScreenLiveRow {
  cells: string[]
  statusTokens: string[]
}

export interface ScreenLiveData {
  metrics: PageMetric[]
  rows: ScreenLiveRow[]
  summary: PageSummaryItem[]
}

export async function loadLiveScreenData(route: string, token: string, keyword = ''): Promise<ScreenLiveData | null> {
  if (route === '/documents/sales-returns') {
    const returns = await fetchSalesReturns(token, { keyword, page: 0, size: 100 })
    return mapSalesReturns(returns)
  }

  if (route.startsWith('/documents/sales')) {
    const orders = await fetchSaleOrders(token, { keyword, page: 0, size: 100 })
    return mapSalesOrders(orders)
  }

  if (route === '/documents/purchase-receipts') {
    const receipts = await fetchPurchaseReceipts(token, { keyword, page: 0, size: 100 })
    return mapPurchaseReceipts(receipts)
  }

  if (route.startsWith('/documents/purchases')) {
    const orders = await fetchPurchaseOrders(token, { keyword, page: 0, size: 100 })
    return mapPurchaseOrders(orders)
  }

  if (route.startsWith('/archives/products')) {
    const [products, lowStockProducts] = await Promise.all([
      fetchProducts(token, { keyword, page: 0, size: 100 }),
      fetchLowStockProducts(token, 20),
    ])
    return mapProducts(products, lowStockProducts)
  }

  if (route === '/archives/customers') {
    const customers = await fetchCustomers(token, { keyword, page: 0, size: 100 })
    return mapCustomers(customers)
  }

  if (route === '/archives/suppliers') {
    const suppliers = await fetchSuppliers(token, { keyword, page: 0, size: 100 })
    return mapSuppliers(suppliers)
  }

  if (route.startsWith('/inventory/product-ledger')) {
    const entries = await fetchInventoryLedger(token)
    return mapInventoryLedger(entries, keyword)
  }

  if (route.startsWith('/inventory/adjust') || route.startsWith('/inventory/snapshots')) {
    const [snapshots, entries] = await Promise.all([
      fetchInventorySnapshots(token),
      fetchInventoryLedger(token),
    ])
    return mapInventorySnapshots(snapshots, entries, keyword)
  }

  if (route.startsWith('/finance/') || route.startsWith('/documents/pay-orders')) {
    const [records, accounts] = await Promise.all([
      fetchFinanceRecords(token, { keyword, page: 0, size: 100 }),
      fetchAccounts(token),
    ])
    return mapFinanceRecords(records, accounts)
  }

  if (route.startsWith('/agent')) {
    const [workbench, tasks, notifications] = await Promise.all([
      fetchAgentWorkbench(token),
      fetchAgentTasks(token),
      fetchAgentNotifications(token),
    ])
    return mapAgentWorkbench(workbench, tasks, notifications, keyword)
  }

  if (route.startsWith('/reports')) {
    const now = Date.now()
    const thirtyDaysAgo = now - 30 * 24 * 60 * 60 * 1000
    const [sales, profit, cashflow, lowStock] = await Promise.all([
      fetchSalesSummary(token, thirtyDaysAgo, now),
      fetchProfitSummary(token, thirtyDaysAgo, now),
      fetchCashflowSummary(token, thirtyDaysAgo, now),
      fetchLowStockReport(token, 10),
    ])
    return {
      metrics: [
        { label: '本月销售', value: formatCurrency(sales.totalSalesAmount), detail: `${sales.totalOrderCount} 单` },
        { label: '预估毛利', value: formatCurrency(profit.estimatedProfitAmount), detail: formatPercent(profit.estimatedProfitRate) },
        { label: '净现金流', value: formatCurrency(cashflow.netCashFlow), detail: `${cashflow.totalRecordCount} 笔` },
      ],
      rows: [
        {
          cells: ['销售汇总', '销售', `${formatCurrency(sales.totalSalesAmount)} / ${sales.totalOrderCount} 单`, 'reports:view', formatDate(now)],
          statusTokens: ['销售'],
        },
        {
          cells: ['利润汇总', '销售/采购', `${formatCurrency(profit.estimatedProfitAmount)} / ${formatPercent(profit.estimatedProfitRate)}`, 'reports:view', formatDate(now)],
          statusTokens: ['利润'],
        },
        {
          cells: ['现金流', '财务', formatCurrency(cashflow.netCashFlow), 'finance:view', formatDate(now)],
          statusTokens: ['现金流'],
        },
        {
          cells: ['低库存商品', '库存/采购', `${lowStock.length} 项风险`, 'inventory:view', formatDate(now)],
          statusTokens: ['库存'],
        },
      ],
      summary: [
        { label: '未收款', value: formatCurrency(sales.totalUnpaidAmount) },
        { label: '退款金额', value: formatCurrency(sales.totalRefundAmount) },
        { label: '低库存商品', value: `${lowStock.length} 项` },
      ],
    }
  }

  return null
}

function mapSalesOrders(orders: SaleOrder[]): ScreenLiveData {
  const sorted = [...orders].sort((a, b) => b.createdAt - a.createdAt)
  const rows = sorted.map((order) => {
    const shippingLabel = saleShippingStatus(order)
    const paymentLabel = salePaymentStatus(order)
    return {
      cells: [
        order.orderNo,
        order.customerName || '散客',
        formatCurrency(order.totalAmount),
        formatCurrency(order.paidAmount),
        shippingLabel,
        paymentLabel,
        saleActionLabel(order),
      ],
      statusTokens: saleStatusTokens(order),
    }
  })

  const totalSales = sorted.reduce((sum, order) => sum + order.totalAmount, 0)
  const unpaidAmount = sorted.reduce((sum, order) => sum + Math.max(order.totalAmount - order.paidAmount, 0), 0)
  const completedCount = sorted.filter((order) => order.status === ORDER_COMPLETED).length
  const pendingReview = sorted.filter((order) => order.status === ORDER_DRAFT).length

  return {
    metrics: [
      { label: '销售单数', value: String(sorted.length), detail: `${completedCount} 单已完成` },
      { label: '销售金额', value: formatCurrency(totalSales), detail: `${pendingReview} 单待审核` },
      { label: '待结算', value: formatCurrency(unpaidAmount), detail: `${sorted.filter((order) => saleStatusTokens(order).includes('待结算')).length} 单` },
    ],
    rows,
    summary: [
      { label: `共 ${sorted.length} 条记录`, value: `待审核 ${pendingReview} 条` },
      { label: '已收金额', value: formatCurrency(sorted.reduce((sum, order) => sum + order.paidAmount, 0)) },
      { label: '待结算金额', value: formatCurrency(unpaidAmount) },
    ],
  }
}

function mapPurchaseOrders(orders: PurchaseOrder[]): ScreenLiveData {
  const sorted = [...orders].sort((a, b) => b.createdAt - a.createdAt)
  const rows = sorted.map((order) => {
    const receiptLabel = purchaseReceiptStatus(order)
    const paymentLabel = purchasePaymentStatus(order)
    return {
      cells: [
        order.orderNo,
        order.supplierName || '未命名供应商',
        String(order.items.length),
        formatCurrency(order.totalAmount),
        receiptLabel,
        paymentLabel,
        purchaseOwnerLabel(order),
      ],
      statusTokens: purchaseStatusTokens(order),
    }
  })

  const totalAmount = sorted.reduce((sum, order) => sum + order.totalAmount, 0)
  const unpaid = sorted.reduce((sum, order) => sum + Math.max(order.totalAmount - order.paidAmount, 0), 0)
  const awaitingReceipt = sorted.filter((order) => purchaseStatusTokens(order).includes('待入库')).length

  return {
    metrics: [
      { label: '采购单数', value: String(sorted.length), detail: `${awaitingReceipt} 单待入库` },
      { label: '采购总额', value: formatCurrency(totalAmount), detail: `${sorted.filter((order) => order.status === ORDER_DRAFT).length} 单草稿/待审批` },
      { label: '待付款', value: formatCurrency(unpaid), detail: `${sorted.filter((order) => purchaseStatusTokens(order).includes('待付款')).length} 单` },
    ],
    rows,
    summary: [
      { label: '已入库金额', value: formatCurrency(sorted.reduce((sum, order) => sum + order.receivedAmount, 0)) },
      { label: '已付款金额', value: formatCurrency(sorted.reduce((sum, order) => sum + order.paidAmount, 0)) },
      { label: '待付款金额', value: formatCurrency(unpaid) },
    ],
  }
}

function mapProducts(products: ProductRecord[], lowStockProducts: ProductRecord[]): ScreenLiveData {
  const lowStockIds = new Set(lowStockProducts.map((item) => item.id))
  const sorted = [...products].sort((a, b) => b.updatedAt - a.updatedAt)
  const rows = sorted.map((product) => ({
    cells: [
      product.defaultSupplier ? '有供应商' : '待资料',
      product.code,
      product.name,
      product.categoryName || '--',
      product.unitName || '--',
      formatCurrency(product.salePrice),
      formatCurrency(product.purchasePrice),
      formatNumber(product.stock),
      product.status === 1 ? '详情 / 流水 / 启用' : '详情 / 流水 / 停用',
    ],
    statusTokens: productStatusTokens(product, lowStockIds),
  }))

  const stockValue = sorted.reduce((sum, product) => sum + product.stock * product.purchasePrice, 0)
  const activeCount = sorted.filter((product) => product.status === 1).length

  return {
    metrics: [
      { label: '商品档案', value: String(sorted.length), detail: `${activeCount} 个启用` },
      { label: '低库存商品', value: String(lowStockIds.size), detail: '已接真实库存预警' },
      { label: '库存金额', value: formatCurrency(stockValue), detail: '按进货价估算' },
    ],
    rows,
    summary: [
      { label: '启用商品', value: `${activeCount}` },
      { label: '停用商品', value: `${sorted.filter((product) => product.status !== 1).length}` },
      { label: '低库存商品', value: `${lowStockIds.size}` },
    ],
  }
}

function mapCustomers(customers: CustomerRecord[]): ScreenLiveData {
  const sorted = [...customers].sort((a, b) => b.updatedAt - a.updatedAt)
  const balance = sorted.reduce((sum, customer) => sum + customer.balance, 0)
  return {
    metrics: [
      { label: '客户档案', value: String(sorted.length), detail: `${sorted.filter((item) => item.status === 1).length} 个启用` },
      { label: '客户应收', value: formatCurrency(balance), detail: `${sorted.filter((item) => (item.balance || 0) > 0).length} 个欠款客户` },
      { label: '分组覆盖', value: String(new Set(sorted.map((item) => item.groupName || '未分组')).size), detail: '已接真实客户分组' },
    ],
    rows: sorted.map((customer) => ({
      cells: [
        customer.name,
        customer.phone,
        customer.groupName || '未分组',
        customer.primaryContactName || '--',
        formatCurrency(customer.balance),
        customer.status === 1 ? '启用' : '停用',
        formatDate(customer.updatedAt),
      ],
      statusTokens: [
        customer.status === 1 ? '启用' : '停用',
        customer.balance > 0 ? '待跟进' : '全部',
      ],
    })),
    summary: [
      { label: '客户总数', value: `${sorted.length}` },
      { label: '应收余额', value: formatCurrency(balance) },
      { label: '高等级客户', value: `${sorted.filter((item) => item.level >= 3).length}` },
    ],
  }
}

function mapSuppliers(suppliers: SupplierRecord[]): ScreenLiveData {
  const sorted = [...suppliers].sort((a, b) => b.updatedAt - a.updatedAt)
  const payable = sorted.reduce((sum, supplier) => sum + supplier.balance, 0)
  return {
    metrics: [
      { label: '供应商档案', value: String(sorted.length), detail: `${sorted.filter((item) => item.status === 1).length} 个启用` },
      { label: '供应商应付', value: formatCurrency(payable), detail: `${sorted.filter((item) => (item.balance || 0) > 0).length} 个待对账` },
      { label: '分组覆盖', value: String(new Set(sorted.map((item) => item.groupName || '未分组')).size), detail: '已接真实供应商分组' },
    ],
    rows: sorted.map((supplier) => ({
      cells: [
        supplier.name,
        supplier.phone,
        supplier.groupName || '未分组',
        supplier.primaryContactName || '--',
        formatCurrency(supplier.balance),
        supplier.status === 1 ? '启用' : '停用',
        formatDate(supplier.updatedAt),
      ],
      statusTokens: [
        supplier.status === 1 ? '启用' : '停用',
        supplier.balance > 0 ? '待跟进' : '全部',
      ],
    })),
    summary: [
      { label: '供应商总数', value: `${sorted.length}` },
      { label: '应付余额', value: formatCurrency(payable) },
      { label: '待对账供应商', value: `${sorted.filter((item) => (item.balance || 0) > 0).length}` },
    ],
  }
}

function mapPurchaseReceipts(receipts: PurchaseReceipt[]): ScreenLiveData {
  const sorted = [...receipts].sort((a, b) => b.updatedAt - a.updatedAt)
  const totalAmount = sorted.reduce((sum, receipt) => sum + receipt.totalAmount, 0)
  const confirmed = sorted.filter((receipt) => receipt.status === ORDER_COMPLETED).length
  return {
    metrics: [
      { label: '入库单数', value: String(sorted.length), detail: `${confirmed} 单已完成` },
      { label: '入库金额', value: formatCurrency(totalAmount), detail: `${sorted.filter((item) => item.status === ORDER_DRAFT).length} 单草稿` },
      { label: '待确认', value: `${sorted.filter((item) => item.status === ORDER_CONFIRMED || item.status === ORDER_DRAFT).length}`, detail: '待仓库确认' },
    ],
    rows: sorted.map((receipt) => ({
      cells: [
        receipt.receiptNo,
        receipt.supplierName || '未命名供应商',
        String(receipt.items.length),
        formatCurrency(receipt.totalAmount),
        receiptStatusLabel(receipt.status),
        receipt.notes || '--',
        formatDate(receipt.updatedAt),
      ],
      statusTokens: [receiptStatusLabel(receipt.status)],
    })),
    summary: [
      { label: '已完成入库', value: `${confirmed}` },
      { label: '总入库金额', value: formatCurrency(totalAmount) },
      { label: '草稿/待确认', value: `${sorted.filter((item) => item.status !== ORDER_COMPLETED).length}` },
    ],
  }
}

function mapSalesReturns(returns: SalesReturn[]): ScreenLiveData {
  const sorted = [...returns].sort((a, b) => b.updatedAt - a.updatedAt)
  const totalAmount = sorted.reduce((sum, item) => sum + item.totalAmount, 0)
  const refundAmount = sorted.reduce((sum, item) => sum + item.refundAmount, 0)
  return {
    metrics: [
      { label: '退货单数', value: String(sorted.length), detail: `${sorted.filter((item) => item.status === ORDER_COMPLETED).length} 单已完成` },
      { label: '退货金额', value: formatCurrency(totalAmount), detail: '已接真实销售退货单' },
      { label: '已退款金额', value: formatCurrency(refundAmount), detail: `${sorted.filter((item) => item.refundAmount < item.totalAmount).length} 单待退款` },
    ],
    rows: sorted.map((item) => ({
      cells: [
        item.returnNo,
        item.customerName || '散客',
        String(item.items.length),
        formatCurrency(item.totalAmount),
        formatCurrency(item.refundAmount),
        salesReturnStatusLabel(item),
        formatDate(item.updatedAt),
      ],
      statusTokens: salesReturnStatusTokens(item),
    })),
    summary: [
      { label: '退货金额', value: formatCurrency(totalAmount) },
      { label: '已退款金额', value: formatCurrency(refundAmount) },
      { label: '待退款单数', value: `${sorted.filter((item) => item.refundAmount < item.totalAmount).length}` },
    ],
  }
}

function mapInventoryLedger(entries: InventoryLedgerEntry[], keyword: string): ScreenLiveData {
  const filtered = filterByKeyword(entries, keyword, (entry) => [
    entry.productCode,
    entry.productName,
    entry.sourceNo,
    entry.sourceType,
    entry.notes,
  ])
  const sorted = [...filtered].sort((a, b) => b.createdAt - a.createdAt)
  return {
    metrics: [
      { label: '库存流水', value: String(sorted.length), detail: `${sorted.filter((item) => item.quantityChange > 0).length} 笔入库` },
      { label: '出库流水', value: `${sorted.filter((item) => item.quantityChange < 0).length}`, detail: '真实库存变动' },
      { label: '调整流水', value: `${sorted.filter((item) => item.sourceType.toUpperCase().includes('ADJUST')).length}`, detail: '盘点/修正' },
    ],
    rows: sorted.map((entry) => ({
      cells: [
        `${entry.productName} / ${entry.productCode}`,
        inventoryDirectionLabel(entry.quantityChange),
        formatNumber(entry.quantityChange),
        entry.sourceNo || `${entry.sourceType}#${entry.sourceId ?? '--'}`,
        formatNumber(entry.quantityAfter),
        formatDate(entry.createdAt),
      ],
      statusTokens: [inventoryDirectionLabel(entry.quantityChange)],
    })),
    summary: [
      { label: '最近流水', value: sorted[0] ? formatDate(sorted[0].createdAt) : '--' },
      { label: '正向变动', value: `${sorted.filter((item) => item.quantityChange > 0).length}` },
      { label: '负向变动', value: `${sorted.filter((item) => item.quantityChange < 0).length}` },
    ],
  }
}

function mapInventorySnapshots(snapshots: InventorySnapshot[], entries: InventoryLedgerEntry[], keyword: string): ScreenLiveData {
  const filtered = filterByKeyword(snapshots, keyword, (snapshot) => [
    snapshot.productCode,
    snapshot.productName,
  ])
  const sorted = [...filtered].sort((a, b) => b.snapshotDate - a.snapshotDate)
  const totalValue = sorted.reduce((sum, snapshot) => sum + (snapshot.totalValue || 0), 0)
  return {
    metrics: [
      { label: '库存快照', value: String(sorted.length), detail: `${new Set(sorted.map((item) => item.productId)).size} 个商品` },
      { label: '库存总值', value: formatCurrency(totalValue), detail: '按快照成本估算' },
      { label: '今日流水', value: `${entries.filter((item) => sameDay(item.createdAt, Date.now())).length}`, detail: '已联真实库存流水' },
    ],
    rows: sorted.map((snapshot) => ({
      cells: [
        snapshot.productCode,
        snapshot.productName,
        formatNumber(snapshot.quantity),
        formatCurrency(snapshot.unitCost || 0),
        formatCurrency(snapshot.totalValue || 0),
        formatDate(snapshot.snapshotDate),
      ],
      statusTokens: [snapshot.quantity > 0 ? '盘点' : '低库存'],
    })),
    summary: [
      { label: '最近盘点日', value: sorted[0] ? formatDate(sorted[0].snapshotDate) : '--' },
      { label: '库存总值', value: formatCurrency(totalValue) },
      { label: '零库存商品', value: `${sorted.filter((item) => item.quantity <= 0).length}` },
    ],
  }
}

function mapFinanceRecords(records: FinanceRecord[], accounts: AccountRecord[]): ScreenLiveData {
  const sorted = [...records].sort((a, b) => b.createdAt - a.createdAt)
  const income = sorted.filter((item) => item.amount > 0).reduce((sum, item) => sum + item.amount, 0)
  const accountsBalance = accounts.reduce((sum, account) => sum + account.balance, 0)
  return {
    metrics: [
      { label: '资金流水', value: String(sorted.length), detail: `${accounts.length} 个账户` },
      { label: '账户余额', value: formatCurrency(accountsBalance), detail: `${accounts.filter((item) => item.status === 1).length} 个启用账户` },
      { label: '收入流水', value: formatCurrency(income), detail: '真实资金记录' },
    ],
    rows: sorted.map((record) => ({
      cells: [
        record.recordNo,
        financeTypeLabel(record.type),
        record.category || '--',
        record.partnerName || '--',
        formatCurrency(record.amount),
        financeMethodLabel(record.method),
        formatDate(record.createdAt),
      ],
      statusTokens: [financeTypeLabel(record.type)],
    })),
    summary: [
      { label: '账户余额', value: formatCurrency(accountsBalance) },
      { label: '收入笔数', value: `${sorted.filter((item) => item.type === 1).length}` },
      { label: '支出笔数', value: `${sorted.filter((item) => item.type !== 1).length}` },
    ],
  }
}

function mapAgentWorkbench(workbench: AgentWorkbench, tasks: AgentTask[], notifications: AgentNotification[], keyword: string): ScreenLiveData {
  const filteredTasks = filterByKeyword(tasks, keyword, (task) => [task.title, task.taskType, task.status, task.inputText])
  const sortedTasks = [...filteredTasks].sort((a, b) => b.updatedAt - a.updatedAt)
  return {
    metrics: workbench.kpiCards.slice(0, 3).map((card) => ({
      label: card.label,
      value: card.value,
      detail: card.trendValue || card.trendDirection || '真实 AI 工作台',
    })),
    rows: sortedTasks.map((task) => ({
      cells: [
        task.title,
        task.taskType,
        task.statusLabel || task.status,
        task.progress == null ? '--' : `${task.progress}%`,
        notifications.find((notification) => notification.taskId === task.id)?.title || '等待通知',
        formatDate(task.updatedAt),
      ],
      statusTokens: [task.statusLabel || task.status],
    })),
    summary: [
      { label: '快捷提问', value: `${workbench.quickQuestions.length} 条` },
      { label: '待处理通知', value: `${notifications.filter((item) => !item.isRead).length}` },
      { label: '待确认草稿', value: `${workbench.pendingDrafts.length}` },
    ],
  }
}

function saleShippingStatus(order: SaleOrder) {
  if (order.status === ORDER_CANCELLED) return '已作废'
  if (order.status === ORDER_COMPLETED) return '已出库'
  if (order.status === ORDER_CONFIRMED) return '待出库'
  return '待审核'
}

function salePaymentStatus(order: SaleOrder) {
  if (order.status === ORDER_CANCELLED) return '已作废'
  if (order.paidAmount <= 0) return '未收款'
  if (order.paidAmount < order.totalAmount) return '待结算'
  return order.status === ORDER_COMPLETED ? '已完成' : '已结清'
}

function saleActionLabel(order: SaleOrder) {
  if (order.status === ORDER_DRAFT) return '审核 / 编辑'
  if (order.status === ORDER_CANCELLED) return '查看 / 作废'
  if (order.paidAmount < order.totalAmount) return '收款 / 查看'
  return '查看 / 打印'
}

function saleStatusTokens(order: SaleOrder) {
  const tokens = new Set<string>()
  if (order.status === ORDER_DRAFT) tokens.add('待审核')
  if (order.status === ORDER_CONFIRMED) tokens.add('待出库')
  if (order.status === ORDER_COMPLETED) tokens.add('已完成')
  if (order.status === ORDER_CANCELLED) tokens.add('已作废')
  if (order.status !== ORDER_CANCELLED && order.paidAmount < order.totalAmount) tokens.add('待结算')
  return Array.from(tokens)
}

function purchaseReceiptStatus(order: PurchaseOrder) {
  if (order.status === ORDER_CANCELLED) return '已作废'
  if (order.receivedAmount >= order.totalAmount && order.totalAmount > 0) return '已入库'
  if (order.receivedAmount > 0) return '部分入库'
  if (order.status === ORDER_DRAFT) return '草稿'
  return '待入库'
}

function purchasePaymentStatus(order: PurchaseOrder) {
  if (order.status === ORDER_CANCELLED) return '已作废'
  if (order.paidAmount >= order.totalAmount && order.totalAmount > 0) return '已付款'
  if (order.paidAmount > 0) return '部分付款'
  return order.status === ORDER_DRAFT ? '待审批' : '待付款'
}

function purchaseOwnerLabel(order: PurchaseOrder) {
  if (order.status === ORDER_DRAFT) return '采购员工'
  if (order.receivedAmount > 0 && order.receivedAmount < order.totalAmount) return '仓库员工'
  if (order.paidAmount < order.totalAmount) return '财务员工'
  return '店长（总）'
}

function purchaseStatusTokens(order: PurchaseOrder) {
  const tokens = new Set<string>()
  if (order.status === ORDER_DRAFT) {
    tokens.add('草稿')
    tokens.add('待审批')
  }
  if (order.status === ORDER_CANCELLED) tokens.add('已作废')
  if (order.receivedAmount > 0 && order.receivedAmount < order.totalAmount) tokens.add('部分入库')
  if (order.status !== ORDER_CANCELLED && order.receivedAmount <= 0 && order.status !== ORDER_DRAFT && order.status !== ORDER_COMPLETED) {
    tokens.add('待入库')
  }
  if (order.status !== ORDER_CANCELLED && order.paidAmount < order.totalAmount && order.status !== ORDER_DRAFT) {
    tokens.add('待付款')
  }
  if (order.status === ORDER_COMPLETED || (order.receivedAmount >= order.totalAmount && order.paidAmount >= order.totalAmount && order.totalAmount > 0)) {
    tokens.add('已完成')
  }
  return Array.from(tokens)
}

function productStatusTokens(product: ProductRecord, lowStockIds: Set<number>) {
  const tokens = new Set<string>()
  tokens.add(product.status === 1 ? '启用' : '停用')
  if (lowStockIds.has(product.id)) tokens.add('低库存')
  if (Date.now() - product.updatedAt <= 7 * 24 * 60 * 60 * 1000) tokens.add('最近更新')
  return Array.from(tokens)
}

function receiptStatusLabel(status: number) {
  if (status === ORDER_COMPLETED) return '已完成'
  if (status === ORDER_CANCELLED) return '已作废'
  if (status === ORDER_CONFIRMED) return '待确认'
  return '草稿'
}

function salesReturnStatusLabel(item: SalesReturn) {
  if (item.status === ORDER_CANCELLED) return '已作废'
  if (item.refundAmount >= item.totalAmount && item.totalAmount > 0) return '已退款'
  if (item.status === ORDER_COMPLETED) return '已完成'
  return item.refundAmount > 0 ? '部分退款' : '待退款'
}

function salesReturnStatusTokens(item: SalesReturn) {
  const tokens = new Set<string>()
  tokens.add(salesReturnStatusLabel(item))
  if (item.status === ORDER_DRAFT) tokens.add('待审核')
  return Array.from(tokens)
}

function inventoryDirectionLabel(quantityChange: number) {
  if (quantityChange > 0) return '入库'
  if (quantityChange < 0) return '出库'
  return '调整'
}

function financeTypeLabel(type?: number | null) {
  if (type === 1) return '收入'
  if (type === 2) return '支出'
  return '其他'
}

function financeMethodLabel(method?: number | null) {
  if (method === 1) return '现金'
  if (method === 2) return '银行卡'
  if (method === 3) return '微信/支付宝'
  return '--'
}

function filterByKeyword<T>(items: T[], keyword: string, pickFields: (item: T) => Array<string | number | null | undefined>) {
  const normalizedKeyword = keyword.trim().toLowerCase()
  if (!normalizedKeyword) return items
  return items.filter((item) => pickFields(item).some((field) => String(field || '').toLowerCase().includes(normalizedKeyword)))
}

function sameDay(left: number, right: number) {
  return new Date(left).toDateString() === new Date(right).toDateString()
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value || 0)
}

function formatPercent(value: number) {
  return `${(value * 100).toFixed(1)}%`
}

function formatDate(timestamp: number) {
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(timestamp)
}

function formatNumber(value: number) {
  return new Intl.NumberFormat('zh-CN', {
    maximumFractionDigits: 2,
  }).format(value || 0)
}
