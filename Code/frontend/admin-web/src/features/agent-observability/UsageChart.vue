<script setup lang="ts">
import { computed, ref } from 'vue'
import { BarChart3 } from 'lucide-vue-next'
import type { AdminUsage } from '@/shared/api/admin'
import { formatDuration, formatNumber, formatShortDate } from '@/shared/utils/format'

const props = defineProps<{ items: AdminUsage[]; modelId: string; loading?: boolean }>()

type MetricField =
  | 'request_count'
  | 'input_tokens'
  | 'output_tokens'
  | 'total_tokens'
  | 'average_duration_ms'
  | 'p95_duration_ms'
  | 'average_time_to_first_token_ms'
  | 'p95_time_to_first_token_ms'

type ChartMode = 'tokens' | 'duration'

const metricFields: MetricField[] = [
  'request_count',
  'input_tokens',
  'output_tokens',
  'total_tokens',
  'average_duration_ms',
  'p95_duration_ms',
  'average_time_to_first_token_ms',
  'p95_time_to_first_token_ms',
]

function usageValue(item: AdminUsage, field: MetricField): number | null {
  const value = item[field]
  return typeof value === 'number' && Number.isFinite(value) && value >= 0 ? value : null
}

function sumMetric(items: AdminUsage[], field: MetricField): number | null {
  const values = items.map((item) => usageValue(item, field)).filter((value): value is number => value !== null)
  return values.length ? values.reduce((sum, value) => sum + value, 0) : null
}

function weightedAverage(items: AdminUsage[], field: MetricField): number | null {
  let weightedTotal = 0
  let weightedCount = 0
  let fallbackTotal = 0
  let fallbackCount = 0
  for (const item of items) {
    const value = usageValue(item, field)
    if (value === null) continue
    const requestCount = usageValue(item, 'request_count')
    if (requestCount !== null && requestCount > 0) {
      weightedTotal += value * requestCount
      weightedCount += requestCount
    } else {
      fallbackTotal += value
      fallbackCount += 1
    }
  }
  if (weightedCount > 0) return Math.round(weightedTotal / weightedCount)
  return fallbackCount > 0 ? Math.round(fallbackTotal / fallbackCount) : null
}

function maximumMetric(items: AdminUsage[], field: MetricField): number | null {
  const values = items.map((item) => usageValue(item, field)).filter((value): value is number => value !== null)
  return values.length ? Math.max(...values) : null
}

const sorted = computed(() => [...props.items]
  .filter((item) => item.bucket_start && metricFields.some((field) => usageValue(item, field) !== null))
  .sort((a, b) => String(a.bucket_start).localeCompare(String(b.bucket_start))))
const distinctModels = computed(() => new Set(sorted.value.map((item) => item.model_id).filter(Boolean)))
const hasMixedModels = computed(() => !props.modelId && distinctModels.value.size > 1)
const summary = computed(() => ({
  requestCount: sumMetric(sorted.value, 'request_count'),
  inputTokens: sumMetric(sorted.value, 'input_tokens'),
  outputTokens: sumMetric(sorted.value, 'output_tokens'),
  totalTokens: sumMetric(sorted.value, 'total_tokens'),
  averageDurationMs: weightedAverage(sorted.value, 'average_duration_ms'),
  p95DurationMs: maximumMetric(sorted.value, 'p95_duration_ms'),
  averageTimeToFirstTokenMs: weightedAverage(sorted.value, 'average_time_to_first_token_ms'),
  p95TimeToFirstTokenMs: maximumMetric(sorted.value, 'p95_time_to_first_token_ms'),
}))
const tokenSources = computed(() => [...new Set(sorted.value
  .map((item) => tokenSourceLabel(item.token_source))
  .filter(Boolean))])
const sourceSummary = computed(() => tokenSources.value.length === 1
  ? tokenSources.value[0]
  : tokenSources.value.length
    ? 'MIXED'
    : 'UNAVAILABLE')
const hasEstimated = computed(() => sorted.value.some((item) => item.estimated || tokenSourceLabel(item.token_source) === 'ESTIMATED'))
const chartMode = ref<ChartMode>('tokens')
const chart = computed(() => {
  const width = 640
  const height = 190
  const left = 38
  const right = 12
  const top = 12
  const bottom = 29
  const availableWidth = width - left - right
  const availableHeight = height - top - bottom
  const field: MetricField = chartMode.value === 'tokens' ? 'total_tokens' : 'p95_duration_ms'
  const metricLabel = chartMode.value === 'tokens' ? '总 Token' : 'P95 总耗时'
  const values = sorted.value.map((item) => usageValue(item, field))
  const availableValues = values.filter((value): value is number => value !== null)
  const max = Math.max(1, ...availableValues)
  const step = availableWidth / Math.max(sorted.value.length, 1)
  const barWidth = Math.max(4, Math.min(22, step * .56))
  return {
    width,
    height,
    left,
    top,
    bottom,
    availableWidth,
    availableHeight,
    max,
    metricLabel,
    hasValues: availableValues.length > 0,
    bars: sorted.value.map((item, index) => {
      const value = values[index]
      const h = value === null ? 0 : value === 0 ? 2 : Math.max(3, value / max * availableHeight)
      return {
        item,
        value,
        key: item.bucket_start ?? item.run_id ?? `bucket-${index}`,
        x: left + step * index + (step - barWidth) / 2,
        y: top + availableHeight - h,
        h,
        w: barWidth,
        label: item.bucket_start ? formatShortDate(item.bucket_start) : '—',
        show: index === 0 || index === sorted.value.length - 1 || index === Math.floor(sorted.value.length / 2),
      }
    }),
  }
})

function tokenSourceLabel(value: string | null | undefined): string {
  return value ? String(value).toUpperCase() : 'UNAVAILABLE'
}

function sourceClass(value: string): string {
  const normalized = value.toUpperCase()
  if (normalized === 'EXACT') return 'source--exact'
  if (normalized === 'ESTIMATED') return 'source--estimated'
  if (normalized === 'UNAVAILABLE') return 'source--unavailable'
  return 'source--mixed'
}

function formatChartValue(value: number | null): string {
  return chartMode.value === 'tokens' ? formatNumber(value) : formatDuration(value)
}
</script>

<template>
  <section class="usage-panel" aria-labelledby="usage-title" :aria-busy="loading || undefined">
    <header>
      <div>
        <h2 id="usage-title">Token / 延迟使用</h2>
        <p>{{ modelId ? `模型：${modelId}` : '按当前模型筛选展示' }}</p>
      </div>
      <BarChart3 aria-hidden="true" />
    </header>

    <template v-if="!loading && !hasMixedModels && sorted.length">
      <div class="summary-row">
        <div class="summary"><strong>{{ formatNumber(summary.totalTokens) }}</strong><span>总 Token</span></div>
        <div class="source-summary" aria-label="Token 数据口径">
          <span class="source" :class="sourceClass(sourceSummary)">token_source={{ sourceSummary }}</span>
          <span>estimated={{ hasEstimated ? 'true' : 'false' }}</span>
        </div>
      </div>

      <div class="metric-grid" aria-label="用量摘要">
        <div class="metric"><span>请求数</span><strong>{{ formatNumber(summary.requestCount) }}</strong><small>当前范围</small></div>
        <div class="metric"><span>输入 Token</span><strong>{{ formatNumber(summary.inputTokens) }}</strong><small>bucket 汇总</small></div>
        <div class="metric"><span>输出 Token</span><strong>{{ formatNumber(summary.outputTokens) }}</strong><small>bucket 汇总</small></div>
        <div class="metric"><span>总 Token</span><strong>{{ formatNumber(summary.totalTokens) }}</strong><small>bucket 汇总</small></div>
        <div class="metric"><span>平均总耗时</span><strong>{{ formatDuration(summary.averageDurationMs) }}</strong><small>按请求数加权</small></div>
        <div class="metric"><span>P95 总耗时</span><strong>{{ formatDuration(summary.p95DurationMs) }}</strong><small>bucket 最大值</small></div>
        <div class="metric"><span>平均首字延迟</span><strong>{{ formatDuration(summary.averageTimeToFirstTokenMs) }}</strong><small>按请求数加权</small></div>
        <div class="metric"><span>P95 首字延迟</span><strong>{{ formatDuration(summary.p95TimeToFirstTokenMs) }}</strong><small>bucket 最大值</small></div>
      </div>
      <p class="metric-note">耗时指标保留服务端 bucket 口径；摘要中的 P95 为当前 bucket P95 的最大值，下表展示每个 bucket 的原始统计字段。</p>

      <div class="chart-heading">
        <div><h3>{{ chart.metricLabel }}趋势</h3><span>服务端 bucket · 直接数值见下表</span></div>
        <div class="chart-switch" role="group" aria-label="图表指标">
          <button type="button" :class="{ active: chartMode === 'tokens' }" @click="chartMode = 'tokens'">Token 总量</button>
          <button type="button" :class="{ active: chartMode === 'duration' }" @click="chartMode = 'duration'">P95 总耗时</button>
        </div>
      </div>
      <svg v-if="chart.hasValues" :viewBox="`0 0 ${chart.width} ${chart.height}`" role="img" :aria-label="`${chart.metricLabel}趋势，共 ${chart.bars.length} 个 bucket`">
        <title>{{ chart.metricLabel }}趋势</title>
        <g v-for="ratio in [0, 1, 2, 3]" :key="ratio">
          <line :x1="chart.left" :x2="chart.left + chart.availableWidth" :y1="chart.top + chart.availableHeight * ratio / 3" :y2="chart.top + chart.availableHeight * ratio / 3" class="grid" />
        </g>
        <g v-for="bar in chart.bars" :key="bar.key">
          <rect :x="bar.x" :y="bar.y" :width="bar.w" :height="bar.h" rx="2" class="bar">
            <title>{{ bar.label }} · {{ chart.metricLabel }} {{ formatChartValue(bar.value) }}</title>
          </rect>
          <text v-if="bar.show" :x="bar.x + bar.w / 2" :y="chart.height - 8" text-anchor="middle">{{ bar.label }}</text>
        </g>
      </svg>
      <div v-else class="chart-empty">当前 bucket 没有可用的{{ chart.metricLabel }}数据。</div>

      <div class="usage-table-wrap">
        <table class="usage-table">
          <caption>按 bucket 的请求、Token、总耗时和首字延迟</caption>
          <thead><tr><th scope="col">Bucket</th><th scope="col" class="numeric">请求数</th><th scope="col" class="numeric">输入 Token</th><th scope="col" class="numeric">输出 Token</th><th scope="col" class="numeric">总 Token</th><th scope="col" class="numeric">平均 / P95 总耗时</th><th scope="col" class="numeric">平均 / P95 首字延迟</th><th scope="col">数据口径</th></tr></thead>
          <tbody>
            <tr v-for="(item, index) in sorted" :key="item.bucket_start ?? item.run_id ?? index">
              <th scope="row"><strong>{{ item.bucket_start ? formatShortDate(item.bucket_start) : '—' }}</strong><small>{{ item.bucket_start || '未提供 bucket' }}</small></th>
              <td class="numeric">{{ formatNumber(item.request_count) }}</td>
              <td class="numeric">{{ formatNumber(item.input_tokens) }}</td>
              <td class="numeric">{{ formatNumber(item.output_tokens) }}</td>
              <td class="numeric">{{ formatNumber(item.total_tokens) }}</td>
              <td class="numeric"><strong>{{ formatDuration(item.average_duration_ms) }}</strong><small>P95 {{ formatDuration(item.p95_duration_ms) }}</small></td>
              <td class="numeric"><strong>{{ formatDuration(item.average_time_to_first_token_ms) }}</strong><small>P95 {{ formatDuration(item.p95_time_to_first_token_ms) }}</small></td>
              <td><span class="source" :class="sourceClass(tokenSourceLabel(item.token_source))">{{ tokenSourceLabel(item.token_source) }}</span><small>estimated={{ item.estimated ? 'true' : 'false' }}</small></td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>

    <div v-else class="empty">
      <BarChart3 aria-hidden="true" />
      <strong>{{ loading ? '正在加载使用量' : hasMixedModels ? '请选择一个模型' : '暂无 Token 使用数据' }}</strong>
      <span v-if="hasMixedModels">不同模型的 Token 统计不能直接合并展示。</span>
      <span v-else-if="!loading">所选时间范围内没有可用 bucket。</span>
    </div>
  </section>
</template>

<style scoped>
.usage-panel { min-height: 304px; border: 1px solid var(--admin-border); border-radius: 8px; background: #fff; padding: 18px; }
.usage-panel header { display: flex; align-items: flex-start; justify-content: space-between; gap: 10px; }
.usage-panel h2 { margin: 0; font-size: 14px; font-weight: 500; }
.usage-panel p { margin: 5px 0 0; color: var(--admin-muted); font-size: 11px; }
.usage-panel header :deep(svg) { width: 16px; height: 16px; color: var(--admin-muted); stroke-width: 1.7; }
.summary-row { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-top: 20px; }
.summary { display: flex; align-items: baseline; gap: 6px; }
.summary strong { font-size: 22px; font-weight: 500; font-variant-numeric: tabular-nums; }
.summary span { color: var(--admin-muted); font-size: 11px; }
.source-summary { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 6px; color: var(--admin-muted); font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 9px; }
.source { display: inline-flex; align-items: center; border-radius: 999px; padding: 4px 6px; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 9px; font-variant-numeric: tabular-nums; white-space: nowrap; }
.source--exact { background: var(--admin-positive-bg); color: var(--admin-positive); }
.source--estimated { background: var(--admin-attention-bg); color: var(--admin-attention); }
.source--unavailable, .source--mixed { background: var(--admin-secondary); color: var(--admin-muted); }
.metric-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 1px; margin-top: 16px; border: 1px solid var(--admin-border); background: var(--admin-border); }
.metric { min-width: 0; background: #fff; padding: 10px; }
.metric span, .metric small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.metric span { color: var(--admin-muted); font-size: 10px; }
.metric strong { display: block; margin-top: 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 14px; font-weight: 500; font-variant-numeric: tabular-nums; }
.metric small { margin-top: 4px; color: var(--admin-muted-light); font-size: 9px; }
.metric-note { line-height: 1.45; }
.chart-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 10px; margin-top: 18px; }
.chart-heading h3 { margin: 0; font-size: 12px; font-weight: 500; }
.chart-heading span { display: block; margin-top: 4px; color: var(--admin-muted); font-size: 10px; }
.chart-switch { display: inline-flex; gap: 2px; border: 1px solid var(--admin-border); border-radius: 999px; padding: 2px; }
.chart-switch button { border: 0; border-radius: 999px; background: transparent; padding: 4px 7px; color: var(--admin-muted); cursor: pointer; font-size: 9px; }
.chart-switch button:hover { color: var(--admin-foreground); }
.chart-switch button.active { background: var(--admin-secondary); color: var(--admin-foreground); }
svg { display: block; width: 100%; height: 175px; margin-top: 6px; overflow: visible; }
.grid { stroke: var(--admin-border); stroke-dasharray: 3 4; }
.bar { fill: #86b8d9; }
.bar:hover { fill: #5f9bc6; }
text { fill: var(--admin-muted); font-family: ui-sans-serif, system-ui, sans-serif; font-size: 10px; }
.chart-empty { display: grid; min-height: 175px; place-content: center; color: var(--admin-muted); font-size: 10px; text-align: center; }
.usage-table-wrap { max-height: 280px; margin-top: 8px; overflow: auto; border-top: 1px solid var(--admin-border); }
.usage-table { width: 100%; min-width: 900px; border-collapse: collapse; }
.usage-table th { height: 32px; border-bottom: 1px solid var(--admin-border); color: var(--admin-muted); font-size: 9px; font-weight: 400; text-align: left; white-space: nowrap; }
.usage-table td, .usage-table tbody th { height: 48px; border-bottom: 1px solid var(--admin-border); padding: 7px 8px 7px 0; font-size: 10px; white-space: nowrap; }
.usage-table tbody th { color: var(--admin-foreground); font-weight: 400; }
.usage-table tbody tr:hover { background: #fafaf8; }
.usage-table strong, .usage-table small { display: block; }
.usage-table strong { font-weight: 500; }
.usage-table small { margin-top: 4px; color: var(--admin-muted); font-size: 9px; }
.numeric { padding-right: 10px !important; text-align: right !important; font-variant-numeric: tabular-nums; }
.empty { display: grid; min-height: 218px; place-content: center; gap: 8px; color: var(--admin-muted); text-align: center; }
.empty :deep(svg) { width: 22px; height: 22px; margin: 0 auto; }
.empty strong { color: var(--admin-foreground); font-size: 12px; font-weight: 500; }
.empty span { font-size: 11px; }
@media (max-width: 720px) {
  .metric-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .chart-heading { align-items: flex-start; flex-direction: column; }
  .chart-switch { align-self: stretch; justify-content: space-between; }
  .chart-switch button { flex: 1; }
}
@media (max-width: 480px) {
  .usage-panel { padding: 14px; }
  .summary-row { align-items: flex-start; flex-direction: column; }
  .source-summary { justify-content: flex-start; }
}
</style>
