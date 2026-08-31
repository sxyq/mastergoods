<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { AlertTriangle, X } from 'lucide-vue-next'

const props = withDefaults(defineProps<{
  open: boolean
  title: string
  description: string
  confirmLabel?: string
  busy?: boolean
  danger?: boolean
}>(), { confirmLabel: '确认', busy: false, danger: false })

const emit = defineEmits<{ cancel: []; confirm: [] }>()
const dialog = ref<HTMLElement | null>(null)

watch(() => props.open, async (open) => {
  if (open) {
    await nextTick()
    dialog.value?.focus()
  }
})
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="dialog-backdrop" @mousedown.self="!busy && emit('cancel')">
      <section ref="dialog" class="dialog" role="dialog" aria-modal="true" :aria-label="title" tabindex="-1" @keydown.esc="!busy && emit('cancel')">
        <header><span :class="{ 'icon--danger': danger }"><AlertTriangle aria-hidden="true" /></span><button type="button" aria-label="关闭" :disabled="busy" @click="emit('cancel')"><X /></button></header>
        <h2>{{ title }}</h2><p>{{ description }}</p>
        <footer><button type="button" class="button button--quiet" :disabled="busy" @click="emit('cancel')">取消</button><button type="button" class="button" :class="{ 'button--danger': danger }" :disabled="busy" @click="emit('confirm')">{{ busy ? '正在提交' : confirmLabel }}</button></footer>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.dialog-backdrop { position: fixed; inset: 0; z-index: 100; display: grid; place-items: center; background: rgba(24, 24, 22, .24); padding: 20px; }.dialog { width: min(420px, 100%); border: 1px solid var(--admin-border); border-radius: 8px; background: #fff; padding: 20px; box-shadow: 0 16px 40px rgba(24, 24, 20, .14); }.dialog header { display: flex; align-items: center; justify-content: space-between; }.dialog header span { display: grid; width: 30px; height: 30px; place-items: center; border-radius: 6px; background: var(--admin-attention-bg); color: var(--admin-attention); }.dialog header span :deep(svg) { width: 16px; height: 16px; }.icon--danger { background: var(--admin-danger-bg) !important; color: var(--admin-danger) !important; }.dialog header button { display: grid; width: 30px; height: 30px; place-items: center; border: 0; border-radius: 6px; background: transparent; color: var(--admin-muted); cursor: pointer; }.dialog header button:hover { background: var(--admin-secondary); }.dialog header button :deep(svg) { width: 16px; height: 16px; }.dialog h2 { margin: 19px 0 0; font-size: 16px; font-weight: 500; }.dialog p { margin: 10px 0 0; color: var(--admin-muted); font-size: 12px; line-height: 1.55; }.dialog footer { display: flex; justify-content: flex-end; gap: 8px; margin-top: 22px; }.button { height: 32px; border: 1px solid var(--admin-foreground); border-radius: 999px; background: var(--admin-foreground); padding: 0 13px; color: #fff; cursor: pointer; font-size: 12px; }.button--quiet { border-color: var(--admin-border); background: #fff; color: var(--admin-foreground); }.button--danger { border-color: var(--admin-danger); background: var(--admin-danger); }.button:disabled { cursor: wait; opacity: .65; }
</style>
