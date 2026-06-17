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

export function formatCurrency(value: number | null | undefined) {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value ?? 0)
}

export function formatNumber(value: number | null | undefined, maximumFractionDigits = 2) {
  return new Intl.NumberFormat('zh-CN', {
    maximumFractionDigits,
  }).format(value ?? 0)
}

export function formatDateTime(timestamp: number | null | undefined) {
  if (!timestamp) return '--'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(timestamp)
}

export function formatDate(timestamp: number | null | undefined) {
  if (!timestamp) return '--'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(timestamp)
}

export function formatRelativeDate(timestamp: number | null | undefined) {
  if (!timestamp) return '--'
  const date = new Date(timestamp)
  const now = new Date()
  const sameDay = date.toDateString() === now.toDateString()
  if (sameDay) {
    return new Intl.DateTimeFormat('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
    }).format(timestamp)
  }
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(timestamp)
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

export function inventoryTrendLabel(stock: number, safeStock: number) {
  if (stock <= 0) return '缺货'
  if (stock < safeStock) return '低库存'
  return '正常'
}

export function inventorySourceLabel(sourceType: string | null | undefined) {
  const normalized = (sourceType ?? '').trim().toLowerCase()
  if (normalized === 'sale_order') return '销售出库'
  if (normalized === 'sales_return') return '销售退货入库'
  if (normalized === 'purchase_order') return '采购单'
  if (normalized === 'purchase_receipt') return '采购入库'
  if (normalized === 'stock_adjust') return '库存调整'
  if (normalized === 'inventory_snapshot') return '库存盘点'
  if (normalized === 'stock_loss') return '盘亏调整'
  if (normalized === 'stock_gain') return '盘盈调整'
  if (normalized === 'manual_use') return '手工领用'
  if (normalized === 'manual_inbound') return '补录入库'
  return sourceType?.trim() || '库存变动'
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

export function riskLevelLabel(level: string | null | undefined) {
  const normalized = (level ?? '').trim().toLowerCase()
  if (normalized === 'high') return '高风险'
  if (normalized === 'medium') return '中风险'
  if (normalized === 'low') return '低风险'
  return level?.trim() || '一般'
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
