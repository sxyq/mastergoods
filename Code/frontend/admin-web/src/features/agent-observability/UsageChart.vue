<script setup lang="ts">
import { computed } from 'vue'
import { BarChart3 } from 'lucide-vue-next'
import type { AdminUsage } from '@/shared/api/admin'
import { formatNumber, formatShortDate } from '@/shared/utils/format'

const props = defineProps<{ items: AdminUsage[]; modelId: string; loading?: boolean }>()
const sorted = computed(() => [...props.items]
  .filter((item) => item.bucket_start && Number.isFinite(item.total_tokens) && (item.total_tokens ?? 0) >= 0)
  .sort((a, b) => String(a.bucket_start).localeCompare(String(b.bucket_start))))
const distinctModels = computed(() => new Set(sorted.value.map((item) => item.model_id).filter(Boolean)))
const hasMixedModels = computed(() => !props.modelId && distinctModels.value.size > 1)
const chart = computed(() => {
  const width = 640; const height = 190; const left = 38; const right = 12; const top = 12; const bottom = 29
  const availableWidth = width - left - right; const availableHeight = height - top - bottom
  const max = Math.max(1, ...sorted.value.map((item) => item.total_tokens ?? 0)); const step = availableWidth / Math.max(sorted.value.length, 1); const barWidth = Math.max(4, Math.min(22, step * .56))
  return { width, height, left, top, bottom, availableWidth, availableHeight, max, bars: sorted.value.map((item, index) => { const value = item.total_tokens ?? 0; const h = value === 0 ? 2 : Math.max(3, value / max * availableHeight); return { item, key: item.bucket_start ?? item.run_id ?? `bucket-${index}`, x: left + step * index + (step - barWidth) / 2, y: top + availableHeight - h, h, w: barWidth, label: item.bucket_start ? formatShortDate(item.bucket_start) : '—', show: index === 0 || index === sorted.value.length - 1 || index === Math.floor(sorted.value.length / 2) } }) }
})
const total = computed(() => sorted.value.reduce((sum, item) => sum + (item.total_tokens ?? 0), 0))
</script>

<template>
  <section class="usage-panel" aria-labelledby="usage-title" :aria-busy="loading || undefined">
    <header><div><h2 id="usage-title">Token 使用趋势</h2><p>{{ modelId ? `模型：${modelId}` : '按当前模型筛选展示' }}</p></div><BarChart3 aria-hidden="true" /></header>
    <template v-if="!loading && !hasMixedModels && sorted.length"><div class="summary"><strong>{{ formatNumber(total) }}</strong><span>总 Token</span></div><svg :viewBox="`0 0 ${chart.width} ${chart.height}`" role="img" :aria-label="`${modelId || '当前模型'}的 Token 使用趋势，共 ${formatNumber(total)} Token`"><title>Token 使用趋势</title><g v-for="ratio in [0, 1, 2, 3]" :key="ratio"><line :x1="chart.left" :x2="chart.left + chart.availableWidth" :y1="chart.top + chart.availableHeight * ratio / 3" :y2="chart.top + chart.availableHeight * ratio / 3" class="grid" /></g><g v-for="bar in chart.bars" :key="bar.key"><rect :x="bar.x" :y="bar.y" :width="bar.w" :height="bar.h" rx="2" class="bar" /><text v-if="bar.show" :x="bar.x + bar.w / 2" :y="chart.height - 8" text-anchor="middle">{{ bar.label }}</text></g></svg><table class="visually-hidden"><caption>Token 使用趋势数据</caption><tbody><tr v-for="(item, index) in sorted" :key="item.bucket_start ?? item.run_id ?? index"><th scope="row">{{ item.bucket_start }}</th><td>{{ item.total_tokens }}</td></tr></tbody></table></template>
    <div v-else class="empty"><BarChart3 aria-hidden="true" /><strong>{{ loading ? '正在加载使用量' : hasMixedModels ? '请选择一个模型' : '暂无 Token 使用数据' }}</strong><span v-if="hasMixedModels">不同模型的 Token 统计不能直接合并展示。</span><span v-else-if="!loading">所选时间范围内没有可用 bucket。</span></div>
  </section>
</template>

<style scoped>
.usage-panel { min-height: 304px; border: 1px solid var(--admin-border); border-radius: 8px; background: #fff; padding: 18px; }.usage-panel header { display: flex; align-items: flex-start; justify-content: space-between; gap: 10px; }.usage-panel h2 { margin: 0; font-size: 14px; font-weight: 500; }.usage-panel p { margin: 5px 0 0; color: var(--admin-muted); font-size: 11px; }.usage-panel header :deep(svg) { width: 16px; height: 16px; color: var(--admin-muted); stroke-width: 1.7; }.summary { display: flex; align-items: baseline; gap: 6px; margin-top: 20px; }.summary strong { font-size: 22px; font-weight: 500; font-variant-numeric: tabular-nums; }.summary span { color: var(--admin-muted); font-size: 11px; }svg { display: block; width: 100%; height: 175px; margin-top: 6px; overflow: visible; }.grid { stroke: var(--admin-border); stroke-dasharray: 3 4; }.bar { fill: #86b8d9; }.bar:hover { fill: #5f9bc6; }text { fill: var(--admin-muted); font-family: ui-sans-serif, system-ui, sans-serif; font-size: 10px; }.empty { display: grid; min-height: 218px; place-content: center; gap: 8px; color: var(--admin-muted); text-align: center; }.empty :deep(svg) { width: 22px; height: 22px; margin: 0 auto; }.empty strong { color: var(--admin-foreground); font-size: 12px; font-weight: 500; }.empty span { font-size: 11px; }.visually-hidden { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; clip-path: inset(50%); }
</style>
