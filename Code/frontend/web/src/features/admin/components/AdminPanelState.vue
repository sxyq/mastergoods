<script setup lang="ts">
import { AlertCircle, Database, LoaderCircle, RefreshCw, ShieldAlert } from 'lucide-vue-next'

const props = withDefaults(defineProps<{
  state: 'loading' | 'empty' | 'error' | 'blocked' | 'unavailable'
  title?: string
  message?: string
  requestId?: string
}>(), {
  title: '',
  message: '',
  requestId: '',
})

defineEmits<{
  retry: []
}>()

const stateCopy = {
  loading: { title: '正在读取面板', message: '等待管理员接口返回当前范围的数据。', icon: LoaderCircle },
  empty: { title: '当前范围暂无数据', message: '调整时间或授权范围后重新查询。', icon: Database },
  error: { title: '面板暂时不可用', message: '当前面板读取失败，其他面板仍可继续查看。', icon: AlertCircle },
  blocked: { title: '当前范围不可见', message: '服务端未授予该资源的读取范围。', icon: ShieldAlert },
  unavailable: { title: '正式接口尚未接入', message: '这里仅保留界面位置，不代表目标能力已经完成。', icon: Database },
} as const

const copy = () => stateCopy[props.state]
</script>

<template>
  <div class="admin-panel-state" :class="`admin-panel-state--${state}`" role="status">
    <component :is="copy().icon" class="admin-panel-state__icon" :class="{ 'is-spinning': state === 'loading' }" aria-hidden="true" />
    <strong>{{ title || copy().title }}</strong>
    <p>{{ message || copy().message }}</p>
    <code v-if="requestId">request: {{ requestId }}</code>
    <button v-if="state === 'error'" type="button" class="admin-button admin-button--secondary" @click="$emit('retry')">
      <RefreshCw aria-hidden="true" />
      重试面板
    </button>
  </div>
</template>
