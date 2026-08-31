<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { AlertTriangle, LoaderCircle, X } from 'lucide-vue-next'

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
let previousFocus: HTMLElement | null = null

watch(() => props.open, async (open) => {
  if (open) {
    previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null
    await nextTick()
    dialog.value?.focus()
  } else {
    previousFocus?.focus()
    previousFocus = null
  }
})

function keepFocus(event: KeyboardEvent): void {
  if (event.key !== 'Tab' || !dialog.value) return
  const controls = [...dialog.value.querySelectorAll<HTMLElement>('button:not(:disabled), [href], input:not(:disabled), select:not(:disabled), textarea:not(:disabled), [tabindex]:not([tabindex="-1"])')]
  if (!controls.length) {
    event.preventDefault()
    return
  }
  const first = controls[0]
  const last = controls[controls.length - 1]
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first.focus()
  }
}

onBeforeUnmount(() => previousFocus?.focus())
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="dialog-backdrop" @mousedown.self="!busy && emit('cancel')">
      <section ref="dialog" class="dialog" role="dialog" aria-modal="true" :aria-labelledby="'confirm-dialog-title'" :aria-describedby="'confirm-dialog-description'" tabindex="-1" @keydown.esc="!busy && emit('cancel')" @keydown="keepFocus">
        <header><span :class="{ 'icon--danger': danger }"><AlertTriangle aria-hidden="true" /></span><button type="button" aria-label="关闭" :disabled="busy" @click="emit('cancel')"><X /></button></header>
        <h2 id="confirm-dialog-title">{{ title }}</h2><p id="confirm-dialog-description">{{ description }}</p>
        <footer><button type="button" class="button button--quiet" :disabled="busy" @click="emit('cancel')">取消</button><button type="button" class="button" :class="{ 'button--danger': danger }" :disabled="busy" @click="emit('confirm')"><LoaderCircle v-if="busy" class="spinner" aria-hidden="true" />{{ busy ? '正在提交' : confirmLabel }}</button></footer>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.dialog-backdrop { position: fixed; inset: 0; z-index: 100; display: grid; place-items: center; background: rgba(24, 24, 22, .28); padding: 20px; }.dialog { width: min(420px, 100%); border: 1px solid var(--admin-border); border-radius: 8px; background: #fff; padding: 20px; box-shadow: 0 18px 48px rgba(24, 24, 20, .16); }.dialog header { display: flex; align-items: center; justify-content: space-between; }.dialog header span { display: grid; width: 30px; height: 30px; place-items: center; border-radius: 6px; background: var(--admin-attention-bg); color: var(--admin-attention); }.dialog header span :deep(svg) { width: 16px; height: 16px; }.icon--danger { background: var(--admin-danger-bg) !important; color: var(--admin-danger) !important; }.dialog header button { display: grid; width: 30px; height: 30px; place-items: center; border: 0; border-radius: 6px; background: transparent; color: var(--admin-muted); cursor: pointer; }.dialog header button:hover:not(:disabled) { background: var(--admin-secondary); color: var(--admin-foreground); }.dialog header button :deep(svg) { width: 16px; height: 16px; }.dialog h2 { margin: 19px 0 0; font-size: 16px; font-weight: 550; }.dialog p { margin: 10px 0 0; color: var(--admin-muted); font-size: 12px; line-height: 1.6; }.dialog footer { display: flex; justify-content: flex-end; gap: 8px; margin-top: 22px; }.button { display: inline-flex; height: 32px; align-items: center; gap: 6px; border: 1px solid var(--admin-foreground); border-radius: 999px; background: var(--admin-foreground); padding: 0 13px; color: #fff; cursor: pointer; font-size: 12px; }.button:hover:not(:disabled) { background: #343436; }.button--quiet { border-color: var(--admin-border); background: #fff; color: var(--admin-foreground); }.button--quiet:hover:not(:disabled) { border-color: var(--admin-border-strong); background: var(--admin-secondary); }.button--danger { border-color: var(--admin-danger); background: var(--admin-danger); }.button:disabled { cursor: wait; opacity: .58; }.button :deep(svg) { width: 14px; height: 14px; }.spinner { animation: spin .8s linear infinite; } @keyframes spin { to { transform: rotate(360deg); } } @media (max-width: 480px) { .dialog-backdrop { align-items: end; padding: 12px; }.dialog { padding: 18px; }.dialog footer { display: grid; grid-template-columns: 1fr 1fr; }.button { justify-content: center; } } @media (prefers-reduced-motion: reduce) { .spinner { animation: none; } }
</style>
