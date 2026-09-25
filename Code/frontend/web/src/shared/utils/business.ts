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

export function todayStartAt() {
  const now = new Date()
  now.setHours(0, 0, 0, 0)
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
