<script setup lang="ts">
import { AlertTriangle, CircleAlert, CircleCheck, Clock3, Eye, LoaderCircle, ShieldAlert } from 'lucide-vue-next'
import type { Component } from 'vue'

export type AdminStatus = 'completed' | 'review' | 'attention' | 'failed' | 'running' | 'blocked' | 'unavailable'

const props = withDefaults(defineProps<{
  status: AdminStatus
  label?: string
}>(), {
  label: '',
})

const statusConfig: Record<AdminStatus, { label: string; tone: string; icon: Component }> = {
  completed: { label: '已完成', tone: 'success', icon: CircleCheck },
  review: { label: '待确认', tone: 'review', icon: Eye },
  attention: { label: '需关注', tone: 'attention', icon: CircleAlert },
  failed: { label: '失败', tone: 'failed', icon: AlertTriangle },
  running: { label: '进行中', tone: 'running', icon: LoaderCircle },
  blocked: { label: '受控拒绝', tone: 'blocked', icon: ShieldAlert },
  unavailable: { label: '未接入', tone: 'unavailable', icon: Clock3 },
}

const config = () => statusConfig[props.status]
</script>

<template>
  <span class="admin-status-badge" :class="`admin-status-badge--${config().tone}`">
    <component :is="config().icon" aria-hidden="true" />
    <span>{{ label || config().label }}</span>
  </span>
</template>
