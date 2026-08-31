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
.page-header { display: flex; min-height: 64px; align-items: flex-start; justify-content: space-between; gap: 20px; }
.eyebrow { margin: 0; color: var(--admin-muted); font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 10px; letter-spacing: .08em; }
h1 { margin: 8px 0 0; font-size: 24px; font-weight: 500; line-height: 1.2; }
.description { margin: 8px 0 0; color: var(--admin-muted); font-size: 12px; }
.actions { display: flex; flex: 0 0 auto; flex-wrap: wrap; align-items: center; justify-content: flex-end; gap: 8px; }
@media (max-width: 700px) { .page-header { flex-direction: column; gap: 16px; }.actions { width: 100%; justify-content: flex-start; } }
</style>
