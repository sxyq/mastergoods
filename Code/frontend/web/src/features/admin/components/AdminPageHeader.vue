<script setup lang="ts">
import { ChevronDown, Clock3, RefreshCw } from 'lucide-vue-next'

withDefaults(defineProps<{
  title: string
  breadcrumb?: string
  range?: string
  refreshing?: boolean
}>(), {
  breadcrumb: '管理后台',
  range: '近 30 天',
  refreshing: false,
})

const emit = defineEmits<{
  'update:range': [value: string]
  refresh: []
}>()

const ranges = ['今天', '近 7 天', '近 30 天', '近 90 天']
</script>

<template>
  <header class="admin-page-header">
    <div class="admin-breadcrumb-block">
      <div class="admin-breadcrumbs" aria-label="当前位置">
        <span>{{ breadcrumb }}</span>
        <span aria-hidden="true">/</span>
        <strong>{{ title }}</strong>
      </div>
      <h1>{{ title }}</h1>
    </div>
    <div class="admin-page-actions">
      <label class="admin-period-selector">
        <Clock3 aria-hidden="true" />
        <select :value="range" aria-label="统计时间范围" @change="emit('update:range', ($event.target as HTMLSelectElement).value)">
          <option v-for="item in ranges" :key="item" :value="item">{{ item }}</option>
        </select>
        <ChevronDown aria-hidden="true" />
      </label>
      <button type="button" class="admin-button admin-button--secondary" :disabled="refreshing" @click="emit('refresh')">
        <RefreshCw aria-hidden="true" :class="{ 'is-spinning': refreshing }" />
        刷新
      </button>
    </div>
  </header>
</template>
