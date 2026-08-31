<script setup lang="ts">
import { computed } from 'vue'
import { BarChart3 } from 'lucide-vue-next'
import { formatNumber, formatShortDate } from '@/shared/utils/format'

type TrendPoint = { at: string; value: number }

const props = defineProps<{
  points: TrendPoint[]
  from?: string
  to?: string
  loading?: boolean
}>()

const chart = computed(() => {
  const width = 680
  const height = 216
  const top = 14
  const right = 12
  const bottom = 30
  const left = 40
  const availableWidth = width - left - right
  const availableHeight = height - top - bottom
  const maxValue = Math.max(1, ...props.points.map((point) => point.value))
  const step = availableWidth / Math.max(props.points.length, 1)
  const barWidth = Math.max(4, Math.min(24, step * 0.56))
  const grid = [0, 1, 2, 3].map((index) => {
    const value = Math.round(maxValue * (3 - index) / 3)
    return { value, y: top + availableHeight * index / 3 }
  })
  const bars = props.points.map((point, index) => {
    const barHeight = point.value === 0 ? 2 : Math.max(3, point.value / maxValue * availableHeight)
    return {
      ...point,
      x: left + step * index + (step - barWidth) / 2,
      y: top + availableHeight - barHeight,
      width: barWidth,
      height: barHeight,
      label: formatShortDate(point.at),
      showLabel: index === 0 || index === props.points.length - 1 || index === Math.floor(props.points.length / 2),
    }
  })
  return { width, height, left, top, bottom, availableWidth, availableHeight, maxValue, grid, bars }
})

const total = computed(() => props.points.reduce((sum, point) => sum + point.value, 0))
const rangeLabel = computed(() => props.from && props.to ? `${formatShortDate(props.from)} 至 ${formatShortDate(props.to)}` : '所选时间范围')
</script>

<template>
  <section class="trend-panel" aria-labelledby="overview-trend-title" :aria-busy="loading || undefined">
    <header class="panel-heading">
      <div>
        <h2 id="overview-trend-title">Agent 运行趋势</h2>
        <p>{{ rangeLabel }}，按日汇总</p>
      </div>
      <BarChart3 aria-hidden="true" />
    </header>

    <template v-if="!loading && points.length">
      <div class="trend-summary">
        <strong>{{ formatNumber(total) }}</strong>
        <span>次运行</span>
      </div>
      <svg
        class="trend-chart"
        :viewBox="`0 0 ${chart.width} ${chart.height}`"
        role="img"
        :aria-label="`${rangeLabel}内共有${formatNumber(total)}次 Agent 运行，最高单日${formatNumber(chart.maxValue)}次。`"
      >
        <title>{{ `${rangeLabel} Agent 运行趋势` }}</title>
        <g v-for="line in chart.grid" :key="line.y">
          <line :x1="chart.left" :x2="chart.left + chart.availableWidth" :y1="line.y" :y2="line.y" class="trend-grid" />
          <text :x="chart.left - 9" :y="line.y + 4" class="trend-axis" text-anchor="end">{{ line.value }}</text>
        </g>
        <g v-for="bar in chart.bars" :key="bar.at">
          <rect :x="bar.x" :y="bar.y" :width="bar.width" :height="bar.height" rx="2" class="trend-bar" />
          <text v-if="bar.showLabel" :x="bar.x + bar.width / 2" :y="chart.height - 8" class="trend-axis" text-anchor="middle">{{ bar.label }}</text>
        </g>
      </svg>
      <table class="visually-hidden">
        <caption>Agent 运行趋势数据</caption>
        <thead><tr><th scope="col">日期</th><th scope="col">运行次数</th></tr></thead>
        <tbody><tr v-for="point in points" :key="point.at"><td>{{ formatShortDate(point.at) }}</td><td>{{ point.value }}</td></tr></tbody>
      </table>
    </template>

    <div v-else class="chart-empty">
      <span v-if="loading" class="loading-mark" aria-label="正在加载" />
      <template v-else>
        <BarChart3 aria-hidden="true" />
        <strong>暂无运行趋势</strong>
        <span>所选时间范围内还没有可展示的运行记录。</span>
      </template>
    </div>
  </section>
</template>

<style scoped>
.trend-panel {
  min-height: 352px;
  border: 1px solid var(--admin-border, #e7e7e4);
  border-radius: 8px;
  background: var(--admin-card, #fcfcfb);
  padding: 18px;
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.panel-heading h2 {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
}

.panel-heading p {
  margin: 5px 0 0;
  color: var(--admin-muted, #71716d);
  font-size: 11px;
}

.panel-heading > :deep(svg) {
  width: 16px;
  height: 16px;
  color: var(--admin-muted, #71716d);
  stroke-width: 1.7;
}

.trend-summary {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-top: 23px;
}

.trend-summary strong {
  font-size: 23px;
  font-weight: 500;
  font-variant-numeric: tabular-nums;
}

.trend-summary span {
  color: var(--admin-muted, #71716d);
  font-size: 12px;
}

.trend-chart {
  display: block;
  width: 100%;
  height: 212px;
  margin-top: 9px;
  overflow: visible;
}

.trend-grid {
  stroke: var(--admin-border, #e7e7e4);
  stroke-dasharray: 3 4;
}

.trend-axis {
  fill: var(--admin-muted, #71716d);
  font-family: ui-sans-serif, system-ui, sans-serif;
  font-size: 10px;
}

.trend-bar {
  fill: #87b7d8;
}

.trend-bar:hover {
  fill: #5f9bc6;
}

.chart-empty {
  display: grid;
  min-height: 250px;
  place-content: center;
  gap: 8px;
  color: var(--admin-muted, #71716d);
  text-align: center;
}

.chart-empty :deep(svg) {
  width: 22px;
  height: 22px;
  margin: 0 auto 3px;
  color: var(--admin-icon, #8c8c87);
}

.chart-empty strong { color: var(--admin-foreground, #1d1d1b); font-size: 12px; font-weight: 500; }
.chart-empty span { font-size: 11px; }

.loading-mark {
  width: 20px;
  height: 20px;
  border: 2px solid var(--admin-border, #e7e7e4);
  border-top-color: var(--admin-foreground, #1d1d1b);
  border-radius: 50%;
  animation: spin .8s linear infinite;
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
  clip-path: inset(50%);
}

@keyframes spin { to { transform: rotate(360deg); } }

@media (prefers-reduced-motion: reduce) { .loading-mark { animation: none; } }
</style>
