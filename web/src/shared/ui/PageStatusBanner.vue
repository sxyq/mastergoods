<script setup lang="ts">
withDefaults(
  defineProps<{
    tone?: 'info' | 'success' | 'warning' | 'error'
    title?: string
    message: string
    actionLabel?: string
  }>(),
  {
    tone: 'info',
    title: '',
    actionLabel: '',
  },
)

defineEmits<{
  action: []
}>()
</script>

<template>
  <section class="page-status-banner" :data-tone="tone">
    <div class="page-status-copy">
      <strong v-if="title">{{ title }}</strong>
      <p>{{ message }}</p>
    </div>
    <button v-if="actionLabel" type="button" class="ghost-action" @click="$emit('action')">
      {{ actionLabel }}
    </button>
  </section>
</template>
