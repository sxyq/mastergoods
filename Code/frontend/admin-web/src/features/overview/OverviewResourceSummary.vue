<script setup lang="ts">
import { computed } from 'vue'
import { Activity, Gauge, Wrench } from 'lucide-vue-next'
import { formatDuration, formatNumber, formatPercent } from '@/shared/utils/format'

const props = defineProps<{
  successRate: number | null
  activeRuns: number | null
  toolCalls: number | null
  totalRuns: number | null
  averageLatency: number | null
  loading?: boolean
}>()

const ringStyle = computed(() => {
  const value = Math.max(0, Math.min(100, props.successRate ?? 0))
  return { '--ring-progress': `${value}%` }
})

const successRateLabel = computed(() => props.successRate === null ? '暂无' : formatPercent(props.successRate))
const successRateDescription = computed(() => props.successRate === null ? '终态完成率暂无样本' : `终态完成率 ${formatPercent(props.successRate)}`)

const callsPerRun = computed(() => {
  if (!props.totalRuns || props.totalRuns <= 0 || props.toolCalls === null) return null
  return props.toolCalls / props.totalRuns
})
</script>

<template>
  <section class="resource-panel" aria-labelledby="resource-title" :aria-busy="loading || undefined">
    <header class="panel-heading">
      <div>
        <h2 id="resource-title">运行资源</h2>
        <p>终态完成率与当前调用负载</p>
      </div>
      <Gauge aria-hidden="true" />
    </header>

    <div v-if="loading" class="resource-body resource-body--loading" role="status" aria-label="正在加载运行资源">
      <div class="ring-skeleton" aria-hidden="true" />
      <div class="resource-list-skeleton" aria-hidden="true">
        <div v-for="index in 4" :key="index"><i /><span /></div>
      </div>
    </div>

    <div v-else class="resource-body">
      <div class="success-ring" :class="{ 'success-ring--unavailable': successRate === null }" :style="ringStyle" :aria-label="successRateDescription">
        <div>
          <strong>{{ successRateLabel }}</strong>
          <span>{{ successRate === null ? '暂无终态样本' : '终态完成率' }}</span>
        </div>
      </div>

      <dl class="resource-list">
        <div>
          <dt><Activity aria-hidden="true" />进行中运行</dt>
          <dd>{{ formatNumber(activeRuns) }}</dd>
          <small>当前查询范围</small>
        </div>
        <div>
          <dt><Wrench aria-hidden="true" />工具调用</dt>
          <dd>{{ formatNumber(toolCalls) }}</dd>
          <small>所选时间范围内</small>
        </div>
        <div>
          <dt><Gauge aria-hidden="true" />平均每次调用</dt>
          <dd>{{ callsPerRun === null ? '—' : callsPerRun.toFixed(1) }}</dd>
          <small>工具调用 / Agent 运行</small>
        </div>
        <div>
          <dt><Activity aria-hidden="true" />平均运行耗时</dt>
          <dd>{{ formatDuration(averageLatency) }}</dd>
          <small>已完成样本</small>
        </div>
      </dl>
    </div>
  </section>
</template>

<style scoped>
.resource-panel {
  min-height: 352px;
  border: 1px solid var(--admin-border, #e7e7e4);
  border-radius: 8px;
  background: #fafaf8;
  padding: 18px;
}

.panel-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; }
.panel-heading h2 { margin: 0; font-size: 14px; font-weight: 500; }
.panel-heading p { margin: 5px 0 0; color: var(--admin-muted, #71716d); font-size: 11px; }
.panel-heading > :deep(svg) { width: 16px; height: 16px; color: var(--admin-muted, #71716d); stroke-width: 1.7; }

.resource-body {
  display: grid;
  min-height: 260px;
  grid-template-columns: minmax(134px, .9fr) minmax(150px, 1.1fr);
  align-items: center;
  gap: 18px;
  margin-top: 22px;
}

.success-ring {
  display: grid;
  width: 156px;
  height: 156px;
  max-width: 100%;
  place-items: center;
  margin: 0 auto;
  border-radius: 50%;
  background: conic-gradient(#4ba776 0 var(--ring-progress), #dce7ee var(--ring-progress) 100%);
}

.success-ring--unavailable { background: #dce7ee; }

.success-ring::before { width: 119px; height: 119px; border-radius: 50%; background: #fafaf8; content: ''; grid-area: 1 / 1; }
.success-ring > div { z-index: 1; display: grid; place-items: center; grid-area: 1 / 1; }
.success-ring strong { font-size: 24px; font-weight: 500; font-variant-numeric: tabular-nums; }
.success-ring span { margin-top: 4px; color: var(--admin-muted, #71716d); font-size: 10px; }

.resource-list { display: grid; margin: 0; }
.resource-list > div { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 3px 10px; border-bottom: 1px solid var(--admin-border, #e7e7e4); padding: 10px 0; }
.resource-list > div:last-child { border-bottom: 0; }
.resource-list dt { display: flex; min-width: 0; align-items: center; gap: 7px; overflow: hidden; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.resource-list dt :deep(svg) { width: 14px; height: 14px; flex: 0 0 14px; color: #5f9bc6; stroke-width: 1.7; }
.resource-list dd { grid-column: 2; grid-row: 1 / span 2; align-self: center; margin: 0; font-size: 15px; font-weight: 500; font-variant-numeric: tabular-nums; }
.resource-list small { color: var(--admin-muted, #71716d); font-size: 10px; }

.ring-skeleton { width: 156px; height: 156px; margin: 0 auto; border: 18px solid #dfe8e2; border-radius: 50%; animation: skeleton-pulse 1.4s ease-in-out infinite; }
.resource-list-skeleton { display: grid; align-self: stretch; grid-template-rows: repeat(4, 1fr); }
.resource-list-skeleton > div { display: grid; grid-template-columns: 1fr 42px; align-content: center; gap: 7px 12px; border-bottom: 1px solid var(--admin-border, #e7e7e4); }
.resource-list-skeleton > div:last-child { border-bottom: 0; }
.resource-list-skeleton i, .resource-list-skeleton span { display: block; height: 9px; border-radius: 3px; background: #e9e9e6; animation: skeleton-pulse 1.4s ease-in-out infinite; }
.resource-list-skeleton i { width: 72%; }
.resource-list-skeleton span { width: 42px; grid-column: 2; grid-row: 1; }

@keyframes skeleton-pulse { 50% { opacity: .52; } }

@media (max-width: 480px) {
  .resource-body { grid-template-columns: 1fr; gap: 18px; }
  .success-ring { width: 132px; height: 132px; }
  .success-ring::before { width: 101px; height: 101px; }
}

@media (prefers-reduced-motion: reduce) { .ring-skeleton, .resource-list-skeleton i, .resource-list-skeleton span { animation: none; } }
</style>
