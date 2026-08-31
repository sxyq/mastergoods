<script setup lang="ts">
import { AlertCircle, Inbox, LoaderCircle, ShieldAlert } from 'lucide-vue-next'

withDefaults(defineProps<{
  state: 'loading' | 'empty' | 'error' | 'forbidden'
  title?: string
  detail?: string
}>(), { title: '', detail: '' })

const emit = defineEmits<{ retry: [] }>()
</script>

<template>
  <section class="state-panel" :class="`state-panel--${state}`" :role="state === 'error' || state === 'forbidden' ? 'alert' : undefined">
    <LoaderCircle v-if="state === 'loading'" class="spinner" aria-label="正在加载" />
    <Inbox v-else-if="state === 'empty'" aria-hidden="true" />
    <ShieldAlert v-else-if="state === 'forbidden'" aria-hidden="true" />
    <AlertCircle v-else aria-hidden="true" />
    <strong>{{ title || (state === 'loading' ? '正在加载' : state === 'empty' ? '暂无数据' : state === 'forbidden' ? '无权访问' : '页面暂不可用') }}</strong>
    <p v-if="detail">{{ detail }}</p>
    <button v-if="state === 'error'" type="button" @click="emit('retry')">重试</button>
  </section>
</template>

<style scoped>
.state-panel { display: grid; min-height: 190px; place-content: center; gap: 8px; border: 1px solid var(--admin-border); border-radius: 8px; background: var(--admin-card); padding: 24px; color: var(--admin-muted); text-align: center; }
.state-panel :deep(svg) { width: 22px; height: 22px; margin: 0 auto 3px; stroke-width: 1.7; }.state-panel strong { color: var(--admin-foreground); font-size: 12px; font-weight: 500; }.state-panel p { max-width: 380px; margin: 0; font-size: 11px; line-height: 1.55; }.state-panel button { justify-self: center; height: 30px; border: 1px solid var(--admin-border); border-radius: 999px; background: #fff; padding: 0 12px; color: var(--admin-foreground); cursor: pointer; font-size: 11px; }.state-panel--error { color: var(--admin-danger); }.state-panel--forbidden { color: var(--admin-attention); }.spinner { animation: spin .8s linear infinite; } @keyframes spin { to { transform: rotate(360deg); } } @media (prefers-reduced-motion: reduce) { .spinner { animation: none; } }
</style>
