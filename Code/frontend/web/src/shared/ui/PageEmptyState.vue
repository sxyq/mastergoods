<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    message: string
    actionLabel?: string
  }>(),
  {
    actionLabel: '',
  },
)

defineEmits<{
  action: []
}>()
</script>

<template>
  <section class="page-empty-state">
    <strong>{{ title }}</strong>
    <p>{{ message }}</p>
    <button v-if="actionLabel" type="button" class="ghost-action" @click="$emit('action')">
      {{ actionLabel }}
    </button>
  </section>
</template>
