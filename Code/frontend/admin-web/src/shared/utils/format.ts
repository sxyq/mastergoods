const numberFormatter = new Intl.NumberFormat('zh-CN')

export function formatNumber(value: number | null | undefined): string {
  return value === null || value === undefined ? '—' : numberFormatter.format(value)
}

export function formatPercent(value: number | null | undefined, digits = 1): string {
  return value === null || value === undefined ? '—' : `${value.toFixed(digits)}%`
}

export function formatDuration(value: number | null | undefined): string {
  if (value === null || value === undefined) return '—'
  if (value < 1_000) return `${Math.round(value)} ms`
  if (value < 60_000) return `${(value / 1_000).toFixed(1)} s`
  return `${Math.floor(value / 60_000)} 分 ${(Math.floor(value / 1_000) % 60)} 秒`
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date)
}

export function formatShortDate(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return new Intl.DateTimeFormat('zh-CN', { month: 'numeric', day: 'numeric' }).format(date)
}
