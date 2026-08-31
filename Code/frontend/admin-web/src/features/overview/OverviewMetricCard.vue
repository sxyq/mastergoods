<script setup lang="ts">
import type { Component } from 'vue'

defineProps<{
  label: string
  value: string
  detail: string
  icon: Component
  unavailable?: boolean
  loading?: boolean
}>()
</script>

<template>
  <article class="metric-card" :class="{ 'metric-card--unavailable': unavailable }" :aria-busy="loading || undefined">
    <header>
      <span>{{ label }}</span>
      <component :is="icon" aria-hidden="true" />
    </header>
    <template v-if="loading">
      <span class="metric-card__skeleton metric-card__skeleton--value" aria-hidden="true" />
      <span class="metric-card__skeleton metric-card__skeleton--detail" aria-hidden="true" />
      <span class="visually-hidden">{{ label }}正在加载</span>
    </template>
    <template v-else>
      <strong class="metric-card__value">{{ value }}</strong>
      <p>{{ detail }}</p>
    </template>
  </article>
</template>

<style scoped>
.metric-card {
  min-height: 112px;
  border: 1px solid var(--admin-border, #e7e7e4);
  border-radius: 8px;
  background: #fafaf8;
  padding: 15px;
}

.metric-card header {
  display: flex;
  min-height: 18px;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.metric-card header span,
.metric-card p {
  overflow: hidden;
  color: var(--admin-muted, #71716d);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metric-card header :deep(svg) {
  width: 16px;
  height: 16px;
  flex: 0 0 16px;
  color: var(--admin-icon, #7b7b76);
  stroke-width: 1.7;
}

.metric-card:nth-child(1) header :deep(svg),
.metric-card:nth-child(5) header :deep(svg) { color: #4a8cc9; }
.metric-card:nth-child(2) header :deep(svg) { color: #43a56b; }
.metric-card:nth-child(3) header :deep(svg) { color: #9073d0; }
.metric-card:nth-child(4) header :deep(svg) { color: #c89449; }

.metric-card__value {
  display: block;
  margin-top: 13px;
  font-size: 23px;
  font-weight: 500;
  letter-spacing: -.03em;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.metric-card p {
  min-height: 16px;
  margin: 8px 0 0;
  font-size: 10px;
}

.metric-card--unavailable .metric-card__value {
  color: var(--admin-muted, #71716d);
}

.metric-card__skeleton {
  display: block;
  border-radius: 4px;
  background: #ececea;
  animation: skeleton-pulse 1.4s ease-in-out infinite;
}

.metric-card__skeleton--value { width: 58%; height: 23px; margin-top: 13px; }
.metric-card__skeleton--detail { width: 72%; height: 10px; margin-top: 8px; }
.visually-hidden { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; clip-path: inset(50%); }

@keyframes skeleton-pulse { 50% { opacity: .52; } }

@media (max-width: 480px) {
  .metric-card {
    min-height: 104px;
    padding: 12px;
  }

  .metric-card__value {
    margin-top: 11px;
    font-size: 20px;
  }

  .metric-card__skeleton--value { height: 20px; margin-top: 11px; }
}

@media (prefers-reduced-motion: reduce) { .metric-card__skeleton { animation: none; } }
</style>
