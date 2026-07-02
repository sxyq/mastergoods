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
import { entityIdKey } from '@/shared/utils/id'
import {
  financeTypeLabel,
  formatCurrency,
  formatNumber,
  purchasePaymentStatus,
  purchaseReceiptFlowStatus,
  purchaseReceiptStatus,
  saleShippingStatus,
} from '@/shared/utils/business'

const ORDER_DRAFT = 0
const ORDER_COMPLETED = 1
const ORDER_CANCELLED = 2
const ORDER_CONFIRMED = 3
const DAY_MS = 24 * 60 * 60 * 1000

const DATE_FORMATTER = new Intl.DateTimeFormat('zh-CN', {
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
})

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
  const rows: ScreenLiveRow[] = new Array(sorted.length)
  let totalSales = 0
  let unpaidAmount = 0
  let completedCount = 0
  let pendingReview = 0
  let unsettledCount = 0
  for (let index = 0; index < sorted.length; index += 1) {
    const order = sorted[index]
    const shippingLabel = saleShippingStatus(order.status)
    const paymentLabel = salePaymentStatus(order)
    const statusTokens = saleStatusTokens(order)
    totalSales += order.totalAmount
    unpaidAmount += Math.max(order.totalAmount - order.paidAmount, 0)
    if (order.status === ORDER_COMPLETED) completedCount += 1
    if (order.status === ORDER_DRAFT) pendingReview += 1
    if (statusTokens.includes('待结算')) unsettledCount += 1
    rows[index] = {
      cells: [
        order.orderNo,
        order.customerName || '散客',
        formatCurrency(order.totalAmount),
        formatCurrency(order.paidAmount),
        shippingLabel,
        paymentLabel,
        saleActionLabel(order),
      ],
      statusTokens,
    }
  }

  return {
    metrics: [
      { label: '销售单数', value: String(sorted.length), detail: `${completedCount} 单已完成` },
      { label: '销售金额', value: formatCurrency(totalSales), detail: `${pendingReview} 单待审核` },
      { label: '待结算', value: formatCurrency(unpaidAmount), detail: `${unsettledCount} 单` },
    ],
    rows,
    summary: [
      { label: `共 ${sorted.length} 条记录`, value: `待审核 ${pendingReview} 条` },
      { label: '已收金额', value: formatCurrency(totalSales - unpaidAmount) },
      { label: '待结算金额', value: formatCurrency(unpaidAmount) },
    ],
  }
}

function mapPurchaseOrders(orders: PurchaseOrder[]): ScreenLiveData {
  const sorted = [...orders].sort((a, b) => b.createdAt - a.createdAt)
  const rows: ScreenLiveRow[] = new Array(sorted.length)
  let totalAmount = 0
  let unpaid = 0
  let awaitingReceipt = 0
  let draftCount = 0
  let receivedAmount = 0
  let paidAmount = 0
  let awaitingPaymentCount = 0
  for (let index = 0; index < sorted.length; index += 1) {
    const order = sorted[index]
    const receiptLabel = purchaseReceiptStatus(order.totalAmount, order.receivedAmount, order.status)
    const paymentLabel = purchasePaymentStatus(order.totalAmount, order.paidAmount, order.status)
    const statusTokens = purchaseStatusTokens(order)
    totalAmount += order.totalAmount
    unpaid += Math.max(order.totalAmount - order.paidAmount, 0)
    receivedAmount += order.receivedAmount
    paidAmount += order.paidAmount
    if (order.status === ORDER_DRAFT) draftCount += 1
    if (statusTokens.includes('待入库')) awaitingReceipt += 1
    if (statusTokens.includes('待付款')) awaitingPaymentCount += 1
    rows[index] = {
      cells: [
        order.orderNo,
        order.supplierName || '未命名供应商',
        String(order.items.length),
        formatCurrency(order.totalAmount),
        receiptLabel,
        paymentLabel,
        purchaseOwnerLabel(order),
      ],
      statusTokens,
    }
  }

  return {
    metrics: [
      { label: '采购单数', value: String(sorted.length), detail: `${awaitingReceipt} 单待入库` },
      { label: '采购总额', value: formatCurrency(totalAmount), detail: `${draftCount} 单草稿/待审批` },
      { label: '待付款', value: formatCurrency(unpaid), detail: `${awaitingPaymentCount} 单` },
    ],
    rows,
    summary: [
      { label: '已入库金额', value: formatCurrency(receivedAmount) },
      { label: '已付款金额', value: formatCurrency(paidAmount) },
      { label: '待付款金额', value: formatCurrency(unpaid) },
    ],
  }
}

function mapProducts(products: ProductRecord[], lowStockProducts: ProductRecord[]): ScreenLiveData {
  const lowStockIds = new Set<string>()
  for (let index = 0; index < lowStockProducts.length; index += 1) {
    lowStockIds.add(entityIdKey(lowStockProducts[index].id))
  }
  const sorted = [...products].sort((a, b) => b.updatedAt - a.updatedAt)
  const rows: ScreenLiveRow[] = new Array(sorted.length)
  let stockValue = 0
  let activeCount = 0
  let inactiveCount = 0
  for (let index = 0; index < sorted.length; index += 1) {
    const product = sorted[index]
    stockValue += product.stock * product.purchasePrice
    if (product.status === 1) {
      activeCount += 1
    } else {
      inactiveCount += 1
    }
    rows[index] = {
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
    }
  }

  return {
    metrics: [
      { label: '商品档案', value: String(sorted.length), detail: `${activeCount} 个启用` },
      { label: '低库存商品', value: String(lowStockIds.size), detail: '已接真实库存预警' },
      { label: '库存金额', value: formatCurrency(stockValue), detail: '按进货价估算' },
    ],
    rows,
    summary: [
      { label: '启用商品', value: `${activeCount}` },
      { label: '停用商品', value: `${inactiveCount}` },
      { label: '低库存商品', value: `${lowStockIds.size}` },
    ],
  }
}

function mapCustomers(customers: CustomerRecord[]): ScreenLiveData {
  const sorted = [...customers].sort((a, b) => b.updatedAt - a.updatedAt)
  const rows: ScreenLiveRow[] = new Array(sorted.length)
  let balance = 0
  let activeCount = 0
  let debtCount = 0
  let highLevelCount = 0
  const groupNames = new Set<string>()
  for (let index = 0; index < sorted.length; index += 1) {
    const customer = sorted[index]
    balance += customer.balance
    if (customer.status === 1) activeCount += 1
    if (customer.balance > 0) debtCount += 1
    if (customer.level >= 3) highLevelCount += 1
    groupNames.add(customer.groupName || '未分组')
    rows[index] = {
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
    }
  }
  return {
    metrics: [
      { label: '客户档案', value: String(sorted.length), detail: `${activeCount} 个启用` },
      { label: '客户应收', value: formatCurrency(balance), detail: `${debtCount} 个欠款客户` },
      { label: '分组覆盖', value: String(groupNames.size), detail: '已接真实客户分组' },
    ],
    rows,
    summary: [
      { label: '客户总数', value: `${sorted.length}` },
      { label: '应收余额', value: formatCurrency(balance) },
      { label: '高等级客户', value: `${highLevelCount}` },
    ],
  }
}

function mapSuppliers(suppliers: SupplierRecord[]): ScreenLiveData {
  const sorted = [...suppliers].sort((a, b) => b.updatedAt - a.updatedAt)
  const rows: ScreenLiveRow[] = new Array(sorted.length)
  let payable = 0
  let activeCount = 0
  let debtCount = 0
  const groupNames = new Set<string>()
  for (let index = 0; index < sorted.length; index += 1) {
    const supplier = sorted[index]
    payable += supplier.balance
    if (supplier.status === 1) activeCount += 1
    if (supplier.balance > 0) debtCount += 1
    groupNames.add(supplier.groupName || '未分组')
    rows[index] = {
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
    }
  }
  return {
    metrics: [
      { label: '供应商档案', value: String(sorted.length), detail: `${activeCount} 个启用` },
      { label: '供应商应付', value: formatCurrency(payable), detail: `${debtCount} 个待对账` },
      { label: '分组覆盖', value: String(groupNames.size), detail: '已接真实供应商分组' },
    ],
    rows,
    summary: [
      { label: '供应商总数', value: `${sorted.length}` },
      { label: '应付余额', value: formatCurrency(payable) },
      { label: '待对账供应商', value: `${debtCount}` },
    ],
  }
}

function mapPurchaseReceipts(receipts: PurchaseReceipt[]): ScreenLiveData {
  const sorted = [...receipts].sort((a, b) => b.updatedAt - a.updatedAt)
  const rows: ScreenLiveRow[] = new Array(sorted.length)
  let totalAmount = 0
  let confirmed = 0
  let draftCount = 0
  let awaitingConfirmCount = 0
  let unfinishedCount = 0
  for (let index = 0; index < sorted.length; index += 1) {
    const receipt = sorted[index]
    totalAmount += receipt.totalAmount
    if (receipt.status === ORDER_COMPLETED) {
      confirmed += 1
    } else {
      unfinishedCount += 1
    }
    if (receipt.status === ORDER_DRAFT) draftCount += 1
    if (receipt.status === ORDER_CONFIRMED || receipt.status === ORDER_DRAFT) awaitingConfirmCount += 1
    rows[index] = {
      cells: [
        receipt.receiptNo,
        receipt.supplierName || '未命名供应商',
        String(receipt.items.length),
        formatCurrency(receipt.totalAmount),
        purchaseReceiptFlowStatus(receipt.status),
        receipt.notes || '--',
        formatDate(receipt.updatedAt),
      ],
      statusTokens: [purchaseReceiptFlowStatus(receipt.status)],
    }
  }
  return {
    metrics: [
      { label: '入库单数', value: String(sorted.length), detail: `${confirmed} 单已完成` },
      { label: '入库金额', value: formatCurrency(totalAmount), detail: `${draftCount} 单草稿` },
      { label: '待确认', value: `${awaitingConfirmCount}`, detail: '待仓库确认' },
    ],
    rows,
    summary: [
      { label: '已完成入库', value: `${confirmed}` },
      { label: '总入库金额', value: formatCurrency(totalAmount) },
      { label: '草稿/待确认', value: `${unfinishedCount}` },
    ],
  }
}

function mapSalesReturns(returns: SalesReturn[]): ScreenLiveData {
  const sorted = [...returns].sort((a, b) => b.updatedAt - a.updatedAt)
  const rows: ScreenLiveRow[] = new Array(sorted.length)
  let totalAmount = 0
  let refundAmount = 0
  let completedCount = 0
  let pendingRefundCount = 0
  for (let index = 0; index < sorted.length; index += 1) {
    const item = sorted[index]
    totalAmount += item.totalAmount
    refundAmount += item.refundAmount
    if (item.status === ORDER_COMPLETED) completedCount += 1
    if (item.refundAmount < item.totalAmount) pendingRefundCount += 1
    rows[index] = {
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
    }
  }
  return {
    metrics: [
      { label: '退货单数', value: String(sorted.length), detail: `${completedCount} 单已完成` },
      { label: '退货金额', value: formatCurrency(totalAmount), detail: '已接真实销售退货单' },
      { label: '已退款金额', value: formatCurrency(refundAmount), detail: `${pendingRefundCount} 单待退款` },
    ],
    rows,
    summary: [
      { label: '退货金额', value: formatCurrency(totalAmount) },
      { label: '已退款金额', value: formatCurrency(refundAmount) },
      { label: '待退款单数', value: `${pendingRefundCount}` },
    ],
  }
}

function mapInventoryLedger(entries: InventoryLedgerEntry[], keyword: string): ScreenLiveData {
  const normalizedKeyword = keyword.trim().toLowerCase()
  const ordered: InventoryLedgerEntry[] = new Array(entries.length)
  let orderedCount = 0
  let inboundCount = 0
  let outboundCount = 0
  let adjustCount = 0
  for (let index = 0; index < entries.length; index += 1) {
    const entry = entries[index]
    if (normalizedKeyword) {
      const productCode = String(entry.productCode ?? '').toLowerCase()
      const productName = String(entry.productName ?? '').toLowerCase()
      const sourceNo = String(entry.sourceNo ?? '').toLowerCase()
      const sourceType = String(entry.sourceType ?? '').toLowerCase()
      const notes = String(entry.notes ?? '').toLowerCase()
      if (
        !productCode.includes(normalizedKeyword)
        && !productName.includes(normalizedKeyword)
        && !sourceNo.includes(normalizedKeyword)
        && !sourceType.includes(normalizedKeyword)
        && !notes.includes(normalizedKeyword)
      ) continue
    }
    ordered[orderedCount++] = entry
    if (entry.quantityChange > 0) inboundCount += 1
    if (entry.quantityChange < 0) outboundCount += 1
    if (entry.sourceType.toUpperCase().includes('ADJUST')) adjustCount += 1
  }
  const rows: ScreenLiveRow[] = new Array(orderedCount)
  for (let index = 0; index < orderedCount; index += 1) {
    const entry = ordered[index]
    rows[index] = {
      cells: [
        `${entry.productName} / ${entry.productCode}`,
        inventoryDirectionLabel(entry.quantityChange),
        formatNumber(entry.quantityChange),
        entry.sourceNo || `${entry.sourceType}#${entry.sourceId ?? '--'}`,
        formatNumber(entry.quantityAfter),
        formatDate(entry.createdAt),
      ],
      statusTokens: [inventoryDirectionLabel(entry.quantityChange)],
    }
  }
  return {
    metrics: [
      { label: '库存流水', value: String(orderedCount), detail: `${inboundCount} 笔入库` },
      { label: '出库流水', value: `${outboundCount}`, detail: '真实库存变动' },
      { label: '调整流水', value: `${adjustCount}`, detail: '盘点/修正' },
    ],
    rows,
    summary: [
      { label: '最近流水', value: orderedCount > 0 ? formatDate(ordered[0].createdAt) : '--' },
      { label: '正向变动', value: `${inboundCount}` },
      { label: '负向变动', value: `${outboundCount}` },
    ],
  }
}

function mapInventorySnapshots(snapshots: InventorySnapshot[], entries: InventoryLedgerEntry[], keyword: string): ScreenLiveData {
  const normalizedKeyword = keyword.trim().toLowerCase()
  const filtered: InventorySnapshot[] = new Array(snapshots.length)
  let filteredCount = 0
  let uniqueProductCount = 0
  let totalValue = 0
  let todayEntryCount = 0
  const today = Date.now()
  const productIds = new Set<string>()
  for (let index = 0; index < snapshots.length; index += 1) {
    const snapshot = snapshots[index]
    if (normalizedKeyword) {
      const code = String(snapshot.productCode ?? '').toLowerCase()
      const name = String(snapshot.productName ?? '').toLowerCase()
      if (!code.includes(normalizedKeyword) && !name.includes(normalizedKeyword)) {
        continue
      }
    }
    filtered[filteredCount++] = snapshot
    totalValue += snapshot.totalValue || 0
    const productIdKey = entityIdKey(snapshot.productId)
    if (!productIds.has(productIdKey)) {
      productIds.add(productIdKey)
      uniqueProductCount += 1
    }
  }
  const sorted = filtered.slice(0, filteredCount).sort((a, b) => b.snapshotDate - a.snapshotDate)
  const rows: ScreenLiveRow[] = new Array(sorted.length)
  let zeroStockCount = 0
  for (let index = 0; index < sorted.length; index += 1) {
    const snapshot = sorted[index]
    if (snapshot.quantity <= 0) zeroStockCount += 1
    rows[index] = {
      cells: [
        snapshot.productCode,
        snapshot.productName,
        formatNumber(snapshot.quantity),
        formatCurrency(snapshot.unitCost || 0),
        formatCurrency(snapshot.totalValue || 0),
        formatDate(snapshot.snapshotDate),
      ],
      statusTokens: [snapshot.quantity > 0 ? '盘点' : '低库存'],
    }
  }
  for (let index = 0; index < entries.length; index += 1) {
    if (sameDay(entries[index].createdAt, today)) {
      todayEntryCount += 1
    }
  }
  return {
    metrics: [
      { label: '库存快照', value: String(sorted.length), detail: `${uniqueProductCount} 个商品` },
      { label: '库存总值', value: formatCurrency(totalValue), detail: '按快照成本估算' },
      { label: '今日流水', value: `${todayEntryCount}`, detail: '已联真实库存流水' },
    ],
    rows,
    summary: [
      { label: '最近盘点日', value: sorted[0] ? formatDate(sorted[0].snapshotDate) : '--' },
      { label: '库存总值', value: formatCurrency(totalValue) },
      { label: '零库存商品', value: `${zeroStockCount}` },
    ],
  }
}

function mapFinanceRecords(records: FinanceRecord[], accounts: AccountRecord[]): ScreenLiveData {
  const sorted = [...records].sort((a, b) => b.createdAt - a.createdAt)
  const rows: ScreenLiveRow[] = new Array(sorted.length)
  let income = 0
  let incomeCount = 0
  let expenseCount = 0
  for (let index = 0; index < sorted.length; index += 1) {
    const record = sorted[index]
    if (record.amount > 0) {
      income += record.amount
      incomeCount += 1
    } else {
      expenseCount += 1
    }
    rows[index] = {
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
    }
  }
  let accountsBalance = 0
  let enabledAccountCount = 0
  for (let index = 0; index < accounts.length; index += 1) {
    const account = accounts[index]
    accountsBalance += account.balance
    if (account.status === 1) enabledAccountCount += 1
  }
  return {
    metrics: [
      { label: '资金流水', value: String(sorted.length), detail: `${accounts.length} 个账户` },
      { label: '账户余额', value: formatCurrency(accountsBalance), detail: `${enabledAccountCount} 个启用账户` },
      { label: '收入流水', value: formatCurrency(income), detail: '真实资金记录' },
    ],
    rows,
    summary: [
      { label: '账户余额', value: formatCurrency(accountsBalance) },
      { label: '收入笔数', value: `${incomeCount}` },
      { label: '支出笔数', value: `${expenseCount}` },
    ],
  }
}

function mapAgentWorkbench(workbench: AgentWorkbench, tasks: AgentTask[], notifications: AgentNotification[], keyword: string): ScreenLiveData {
  const normalizedKeyword = keyword.trim().toLowerCase()
  const filteredTasks: AgentTask[] = new Array(tasks.length)
  let filteredTaskCount = 0
  for (let index = 0; index < tasks.length; index += 1) {
    const task = tasks[index]
    if (normalizedKeyword) {
      const title = String(task.title ?? '').toLowerCase()
      const taskType = String(task.taskType ?? '').toLowerCase()
      const status = String(task.status ?? '').toLowerCase()
      const inputText = String(task.inputText ?? '').toLowerCase()
      if (
        !title.includes(normalizedKeyword)
        && !taskType.includes(normalizedKeyword)
        && !status.includes(normalizedKeyword)
        && !inputText.includes(normalizedKeyword)
      ) continue
    }
    filteredTasks[filteredTaskCount++] = task
  }
  const sortedTasks = filteredTasks.slice(0, filteredTaskCount).sort((a, b) => b.updatedAt - a.updatedAt)
  const notificationByTaskId = new Map<string, AgentNotification>()
  let unreadCount = 0
  for (let index = 0; index < notifications.length; index += 1) {
    const notification = notifications[index]
    if (!notification.isRead) unreadCount += 1
    if (notification.taskId != null) {
      notificationByTaskId.set(String(notification.taskId), notification)
    }
  }
  const metrics: PageMetric[] = new Array(Math.min(3, workbench.kpiCards.length))
  for (let index = 0; index < metrics.length; index += 1) {
    const card = workbench.kpiCards[index]
    metrics[index] = {
      label: card.label,
      value: card.value,
      detail: card.trendValue || card.trendDirection || '真实 AI 工作台',
    }
  }
  const rows: ScreenLiveRow[] = new Array(sortedTasks.length)
  for (let index = 0; index < sortedTasks.length; index += 1) {
    const task = sortedTasks[index]
    rows[index] = {
      cells: [
        task.title,
        task.taskType,
        task.statusLabel || task.status,
        task.progress == null ? '--' : `${task.progress}%`,
        notificationByTaskId.get(String(task.id))?.title || '等待通知',
        formatDate(task.updatedAt),
      ],
      statusTokens: [task.statusLabel || task.status],
    }
  }
  return {
    metrics,
    rows,
    summary: [
      { label: '快捷提问', value: `${workbench.quickQuestions.length} 条` },
      { label: '待处理通知', value: `${unreadCount}` },
      { label: '待确认草稿', value: `${workbench.pendingDrafts.length}` },
    ],
  }
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
  const tokens: string[] = []
  if (order.status === ORDER_DRAFT) tokens.push('待审核')
  if (order.status === ORDER_CONFIRMED) tokens.push('待出库')
  if (order.status === ORDER_COMPLETED) tokens.push('已完成')
  if (order.status === ORDER_CANCELLED) tokens.push('已作废')
  if (order.status !== ORDER_CANCELLED && order.paidAmount < order.totalAmount) tokens.push('待结算')
  return tokens
}

function purchaseOwnerLabel(order: PurchaseOrder) {
  if (order.status === ORDER_DRAFT) return '采购员工'
  if (order.receivedAmount > 0 && order.receivedAmount < order.totalAmount) return '仓库员工'
  if (order.paidAmount < order.totalAmount) return '财务员工'
  return '店长（总）'
}

function purchaseStatusTokens(order: PurchaseOrder) {
  const tokens: string[] = []
  if (order.status === ORDER_DRAFT) {
    tokens.push('草稿')
    tokens.push('待审批')
  }
  if (order.status === ORDER_CANCELLED) tokens.push('已作废')
  if (order.receivedAmount > 0 && order.receivedAmount < order.totalAmount) tokens.push('部分入库')
  if (order.status !== ORDER_CANCELLED && order.receivedAmount <= 0 && order.status !== ORDER_DRAFT && order.status !== ORDER_COMPLETED) {
    tokens.push('待入库')
  }
  if (order.status !== ORDER_CANCELLED && order.paidAmount < order.totalAmount && order.status !== ORDER_DRAFT) {
    tokens.push('待付款')
  }
  if (order.status === ORDER_COMPLETED || (order.receivedAmount >= order.totalAmount && order.paidAmount >= order.totalAmount && order.totalAmount > 0)) {
    tokens.push('已完成')
  }
  return tokens
}

function productStatusTokens(product: ProductRecord, lowStockIds: Set<string>) {
  const tokens: string[] = []
  tokens.push(product.status === 1 ? '启用' : '停用')
  if (lowStockIds.has(entityIdKey(product.id))) tokens.push('低库存')
  if (Date.now() - product.updatedAt <= 7 * DAY_MS) tokens.push('最近更新')
  return tokens
}

function salesReturnStatusLabel(item: SalesReturn) {
  if (item.status === ORDER_CANCELLED) return '已作废'
  if (item.refundAmount >= item.totalAmount && item.totalAmount > 0) return '已退款'
  if (item.status === ORDER_COMPLETED) return '已完成'
  return item.refundAmount > 0 ? '部分退款' : '待退款'
}

function salesReturnStatusTokens(item: SalesReturn) {
  const tokens: string[] = []
  tokens.push(salesReturnStatusLabel(item))
  if (item.status === ORDER_DRAFT) tokens.push('待审核')
  return tokens
}

function inventoryDirectionLabel(quantityChange: number) {
  if (quantityChange > 0) return '入库'
  if (quantityChange < 0) return '出库'
  return '调整'
}

function financeMethodLabel(method?: number | null) {
  if (method === 1) return '现金'
  if (method === 2) return '银行卡'
  if (method === 3) return '微信/支付宝'
  return '--'
}

function sameDay(left: number, right: number) {
  return new Date(left).toDateString() === new Date(right).toDateString()
}

function formatPercent(value: number) {
  return `${(value * 100).toFixed(1)}%`
}

function formatDate(timestamp: number) {
  return DATE_FORMATTER.format(timestamp)
}
