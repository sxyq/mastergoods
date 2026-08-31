<script setup lang="ts">
import type { Component } from 'vue'

defineProps<{
  label: string
  value: string
  detail: string
  icon: Component
  unavailable?: boolean
}>()
</script>

<template>
  <article class="metric-card" :class="{ 'metric-card--unavailable': unavailable }">
    <header>
      <span>{{ label }}</span>
      <component :is="icon" aria-hidden="true" />
    </header>
    <strong class="metric-card__value">{{ value }}</strong>
    <p>{{ detail }}</p>
  </article>
</template>

<style scoped>
.metric-card {
  min-height: 116px;
  border: 1px solid var(--admin-border, #e7e7e4);
  border-radius: 8px;
  background: var(--admin-card, #fcfcfb);
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
  font-size: 12px;
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

.metric-card__value {
  display: block;
  margin-top: 15px;
  font-size: 25px;
  font-weight: 500;
  letter-spacing: 0;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.metric-card p {
  min-height: 16px;
  margin: 9px 0 0;
  font-size: 11px;
}

.metric-card--unavailable .metric-card__value {
  color: var(--admin-muted, #71716d);
}

@media (max-width: 480px) {
  .metric-card {
    min-height: 104px;
    padding: 12px;
  }

  .metric-card__value {
    margin-top: 13px;
    font-size: 21px;
  }
}
</style>
