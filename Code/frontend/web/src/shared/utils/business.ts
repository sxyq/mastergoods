export const SALE_DRAFT = 0
export const SALE_COMPLETED = 1
export const SALE_CANCELLED = 2
export const SALE_CONFIRMED = 3

export const FINANCE_INCOME = 1
export const FINANCE_EXPENSE = 2

export const METHOD_CASH = 1
export const METHOD_WECHAT = 2
export const METHOD_ALIPAY = 3
export const METHOD_BANK = 4
export const METHOD_OTHER = 5

export const SALES_RETURN_DRAFT = 0
export const SALES_RETURN_CONFIRMED = 1
export const SALES_RETURN_COMPLETED = 2
export const SALES_RETURN_CANCELLED = 3

export const PAY_ORDER_DRAFT = 0
export const PAY_ORDER_PAID = 1
export const PAY_ORDER_CANCELLED = 2

export const PURCHASE_RETURN_DRAFT = 0
export const PURCHASE_RETURN_CONFIRMED = 1
export const PURCHASE_RETURN_COMPLETED = 2
export const PURCHASE_RETURN_CANCELLED = 3

export const ACCOUNT_TYPE_CASH = 0
export const ACCOUNT_TYPE_BANK = 1
export const ACCOUNT_TYPE_ALIPAY = 2
export const ACCOUNT_TYPE_WECHAT = 3

export const ACCOUNT_ACTIVE = 1
export const ACCOUNT_DISABLED = 0

export const ACCOUNT_TRANSFER_PENDING = 0
export const ACCOUNT_TRANSFER_COMPLETED = 1
export const ACCOUNT_TRANSFER_CANCELLED = 2

const CURRENCY_FORMATTER = new Intl.NumberFormat('zh-CN', {
  style: 'currency',
  currency: 'CNY',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

const DATE_TIME_FORMATTER = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
})

const DATE_FORMATTER = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
})

const RELATIVE_TIME_FORMATTER = new Intl.DateTimeFormat('zh-CN', {
  hour: '2-digit',
  minute: '2-digit',
})

const RELATIVE_DATE_TIME_FORMATTER = new Intl.DateTimeFormat('zh-CN', {
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
})

const numberFormatters = new Map<number, Intl.NumberFormat>()

export function formatCurrency(value: number | null | undefined) {
  return CURRENCY_FORMATTER.format(value ?? 0)
}

export function formatNumber(value: number | null | undefined, maximumFractionDigits = 2) {
  let formatter = numberFormatters.get(maximumFractionDigits)
  if (!formatter) {
    formatter = new Intl.NumberFormat('zh-CN', {
      maximumFractionDigits,
    })
    numberFormatters.set(maximumFractionDigits, formatter)
  }
  return formatter.format(value ?? 0)
}

export function formatPercent(value: number) {
  return `${(value * 100).toFixed(1)}%`
}

export function formatDateTime(timestamp: number | null | undefined) {
  if (!timestamp) return '--'
  return DATE_TIME_FORMATTER.format(timestamp)
}

export function formatDate(timestamp: number | null | undefined) {
  if (!timestamp) return '--'
  return DATE_FORMATTER.format(timestamp)
}

export function formatRelativeDate(timestamp: number | null | undefined) {
  if (!timestamp) return '--'
  const date = new Date(timestamp)
  const now = new Date()
  const sameDay =
    date.getFullYear() === now.getFullYear()
    && date.getMonth() === now.getMonth()
    && date.getDate() === now.getDate()
  if (sameDay) {
    return RELATIVE_TIME_FORMATTER.format(timestamp)
  }
  return RELATIVE_DATE_TIME_FORMATTER.format(timestamp)
}

export function todayStartAt() {
  const now = new Date()
  now.setHours(0, 0, 0, 0)
  return now.getTime()
}

export function todayEndAt() {
  const now = new Date()
  now.setHours(23, 59, 59, 999)
  return now.getTime()
}

export function weekStartAt(reference = new Date()) {
  const date = new Date(reference)
  const day = date.getDay() || 7
  date.setDate(date.getDate() - day + 1)
  date.setHours(0, 0, 0, 0)
  return date.getTime()
}

export function monthStartAt(reference = new Date()) {
  const date = new Date(reference)
  date.setDate(1)
  date.setHours(0, 0, 0, 0)
  return date.getTime()
}

export function saleOrderStatusLabel(status: number) {
  if (status === SALE_DRAFT) return '草稿'
  if (status === SALE_COMPLETED) return '已完成'
  if (status === SALE_CANCELLED) return '已作废'
  if (status === SALE_CONFIRMED) return '已确认'
  return '未知'
}

export function saleShippingStatus(status: number) {
  if (status === SALE_CANCELLED) return '已作废'
  if (status === SALE_COMPLETED) return '已出库'
  if (status === SALE_CONFIRMED) return '待出库'
  return '待审核'
}

export function salePaymentStatus(totalAmount: number, paidAmount: number, status: number) {
  if (status === SALE_CANCELLED) return '已作废'
  if (paidAmount <= 0) return '未收款'
  if (paidAmount < totalAmount) return '部分收款'
  return status === SALE_COMPLETED ? '已完成' : '已结清'
}

export function saleOrderStatusTokens(totalAmount: number, paidAmount: number, status: number): string[] {
  const tokens: string[] = []
  if (status === SALE_DRAFT) tokens.push('待审核')
  if (status === SALE_CONFIRMED) tokens.push('待出库')
  if (status === SALE_COMPLETED) tokens.push('已完成')
  if (status === SALE_CANCELLED) tokens.push('已作废')
  if (status !== SALE_CANCELLED && paidAmount < totalAmount) tokens.push('待结算')
  return tokens
}

export function salesReturnStatusLabel(status: number) {
  if (status === SALES_RETURN_DRAFT) return '草稿'
  if (status === SALES_RETURN_CONFIRMED) return '已确认'
  if (status === SALES_RETURN_COMPLETED) return '已退款'
  if (status === SALES_RETURN_CANCELLED) return '已取消'
  return '未知'
}

export function salesReturnRefundStatus(totalAmount: number, refundAmount: number, status: number) {
  if (status === SALES_RETURN_CANCELLED) return '已取消'
  if (refundAmount <= 0) return '待退款'
  if (refundAmount < totalAmount) return '部分退款'
  return '已退款'
}

export function salesReturnStatusTokens(totalAmount: number, refundAmount: number, status: number): string[] {
  const tokens: string[] = []
  if (status === SALES_RETURN_CANCELLED) tokens.push('已作废')
  else if (refundAmount >= totalAmount && totalAmount > 0) tokens.push('已退款')
  else if (status === SALES_RETURN_COMPLETED) tokens.push('已完成')
  else if (refundAmount > 0) tokens.push('部分退款')
  else tokens.push('待退款')
  if (status === SALES_RETURN_DRAFT) tokens.push('待审核')
  return tokens
}

export function purchaseReceiptStatus(totalAmount: number, receivedAmount: number, status: number) {
  if (status === SALE_CANCELLED) return '已作废'
  if (receivedAmount >= totalAmount && totalAmount > 0) return '已入库'
  if (receivedAmount > 0) return '部分入库'
  if (status === SALE_DRAFT) return '草稿'
  return '待入库'
}

export function purchasePaymentStatus(totalAmount: number, paidAmount: number, status: number) {
  if (status === SALE_CANCELLED) return '已作废'
  if (paidAmount >= totalAmount && totalAmount > 0) return '已付款'
  if (paidAmount > 0) return '部分付款'
  return status === SALE_DRAFT ? '待审批' : '待付款'
}

export function purchaseOrderStatusLabel(totalAmount: number, paidAmount: number, receivedAmount: number, status: number) {
  const receipt = purchaseReceiptStatus(totalAmount, receivedAmount, status)
  const payment = purchasePaymentStatus(totalAmount, paidAmount, status)
  if (receipt === '已入库' && payment === '已付款') return '已完成'
  return receipt
}

export function purchaseOrderStatusTokens(totalAmount: number, paidAmount: number, receivedAmount: number, status: number): string[] {
  const tokens: string[] = []
  if (status === SALE_DRAFT) {
    tokens.push('草稿')
    tokens.push('待审批')
  }
  if (status === SALE_CANCELLED) tokens.push('已作废')
  if (receivedAmount > 0 && receivedAmount < totalAmount) tokens.push('部分入库')
  if (status !== SALE_CANCELLED && receivedAmount <= 0 && status !== SALE_DRAFT && status !== SALE_COMPLETED) {
    tokens.push('待入库')
  }
  if (status !== SALE_CANCELLED && paidAmount < totalAmount && status !== SALE_DRAFT) {
    tokens.push('待付款')
  }
  if (status === SALE_COMPLETED || (receivedAmount >= totalAmount && paidAmount >= totalAmount && totalAmount > 0)) {
    tokens.push('已完成')
  }
  return tokens
}

export function purchaseReturnStatusLabel(status: number) {
  if (status === PURCHASE_RETURN_DRAFT) return '草稿'
  if (status === PURCHASE_RETURN_CONFIRMED) return '已确认'
  if (status === PURCHASE_RETURN_COMPLETED) return '已退款'
  if (status === PURCHASE_RETURN_CANCELLED) return '已取消'
  return '未知'
}

export function purchaseReturnRefundStatus(totalAmount: number, refundAmount: number, status: number) {
  if (status === PURCHASE_RETURN_CANCELLED) return '已取消'
  if (refundAmount <= 0) return '待退款'
  if (refundAmount < totalAmount) return '部分退款'
  return '已退款'
}

export function payOrderStatusLabel(status: number) {
  if (status === PAY_ORDER_DRAFT) return '草稿'
  if (status === PAY_ORDER_PAID) return '已付款'
  if (status === PAY_ORDER_CANCELLED) return '已取消'
  return '未知'
}

export function purchaseReceiptFlowStatus(status: number) {
  if (status === SALE_COMPLETED) return '已完成'
  if (status === SALE_CANCELLED) return '已作废'
  if (status === SALE_CONFIRMED) return '待确认'
  return '草稿'
}

export function financeTypeLabel(type: number | null | undefined) {
  if (type === FINANCE_INCOME) return '收入'
  if (type === FINANCE_EXPENSE) return '支出'
  return '其他'
}

export function financeMethodLabel(method: number | null | undefined) {
  if (method === METHOD_CASH) return '现金'
  if (method === METHOD_WECHAT) return '微信'
  if (method === METHOD_ALIPAY) return '支付宝'
  if (method === METHOD_BANK) return '银行卡'
  if (method === METHOD_OTHER) return '其他'
  return '--'
}

export function accountTypeLabel(type: number | null | undefined) {
  if (type === ACCOUNT_TYPE_CASH) return '现金'
  if (type === ACCOUNT_TYPE_BANK) return '银行'
  if (type === ACCOUNT_TYPE_ALIPAY) return '支付宝'
  if (type === ACCOUNT_TYPE_WECHAT) return '微信'
  return '--'
}

export function accountStatusLabel(status: number | null | undefined) {
  if (status === ACCOUNT_ACTIVE) return '启用'
  if (status === ACCOUNT_DISABLED) return '停用'
  return '--'
}

export function accountTransferStatusLabel(status: number | null | undefined) {
  if (status === ACCOUNT_TRANSFER_PENDING) return '待处理'
  if (status === ACCOUNT_TRANSFER_COMPLETED) return '已完成'
  if (status === ACCOUNT_TRANSFER_CANCELLED) return '已取消'
  return '--'
}

export function inventoryTrendLabel(stock: number, safeStock: number) {
  if (stock <= 0) return '缺货'
  if (stock < safeStock) return '低库存'
  return '正常'
}

export function productStatusTokens(status: number, updatedAt: number, lowStock: boolean, now = Date.now()): string[] {
  const tokens: string[] = []
  tokens.push(status === 1 ? '启用' : '停用')
  if (lowStock) tokens.push('低库存')
  if (now - updatedAt <= 7 * 24 * 60 * 60 * 1000) tokens.push('最近更新')
  return tokens
}

function normalizeText(value: string | null | undefined) {
  return (value ?? '').trim().toLowerCase()
}

const INVENTORY_SOURCE_LABELS: Record<string, string> = {
  sale_order: '销售出库',
  sales_return: '销售退货入库',
  purchase_order: '采购单',
  purchase_receipt: '采购入库',
  stock_adjust: '库存调整',
  inventory_snapshot: '库存盘点',
  stock_loss: '盘亏调整',
  stock_gain: '盘盈调整',
  manual_use: '手工领用',
  manual_inbound: '补录入库',
}

export function inventorySourceLabel(sourceType: string | null | undefined) {
  return INVENTORY_SOURCE_LABELS[normalizeText(sourceType)] ?? (sourceType?.trim() || '库存变动')
}

export type ReportPeriodKey = 'today' | 'week' | 'month' | 'custom'

export function reportRangeForPeriod(period: ReportPeriodKey, custom?: { startAt: number; endAt: number }) {
  const now = Date.now()
  if (period === 'today') {
    return { startAt: todayStartAt(), endAt: now }
  }
  if (period === 'week') {
    return { startAt: weekStartAt(), endAt: now }
  }
  if (period === 'month') {
    return { startAt: monthStartAt(), endAt: now }
  }
  return {
    startAt: custom?.startAt ?? todayStartAt(),
    endAt: custom?.endAt ?? now,
  }
}

export function salesTrendBucket(period: ReportPeriodKey) {
  return period === 'today' ? 'hour6' : 'day'
}

const RISK_LEVEL_LABELS: Record<string, string> = {
  high: '高风险',
  medium: '中风险',
  low: '低风险',
}

export function riskLevelLabel(level: string | null | undefined) {
  return RISK_LEVEL_LABELS[normalizeText(level)] ?? (level?.trim() || '一般')
}

export function formatDuration(durationMs: number | null | undefined) {
  if (!durationMs || durationMs <= 0) return '--'
  if (durationMs < 1000) return `${durationMs} ms`
  const seconds = durationMs / 1000
  if (seconds < 60) return `${seconds.toFixed(seconds >= 10 ? 1 : 2)} s`
  const minutes = Math.floor(seconds / 60)
  const remainSeconds = Math.round(seconds % 60)
  return `${minutes} 分 ${remainSeconds} 秒`
}
