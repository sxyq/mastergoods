<script setup lang="ts">
defineProps<{
  eyebrow?: string
  title: string
  description: string
}>()
</script>

<template>
  <header class="page-header">
    <div>
      <p v-if="eyebrow" class="eyebrow">{{ eyebrow }}</p>
      <h1>{{ title }}</h1>
      <p class="description">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="actions"><slot name="actions" /></div>
  </header>
</template>

<style scoped>
.page-header { display: flex; min-height: 72px; align-items: flex-end; justify-content: space-between; gap: 24px; border-bottom: 1px solid var(--admin-border); padding-bottom: 18px; }
.eyebrow { margin: 0; color: var(--admin-muted-light); font-size: 10px; letter-spacing: .025em; }
h1 { margin: 9px 0 0; font-size: 24px; font-weight: 520; line-height: 1.2; letter-spacing: -.015em; }
.description { margin: 8px 0 0; color: var(--admin-muted); font-size: 11px; line-height: 1.5; }
.actions { display: flex; flex: 0 0 auto; flex-wrap: wrap; align-items: center; justify-content: flex-end; gap: 8px; }
@media (max-width: 700px) { .page-header { min-height: 0; flex-direction: column; align-items: stretch; gap: 16px; padding-bottom: 16px; }.actions { width: 100%; justify-content: flex-start; }h1 { font-size: 22px; } }
</style>
