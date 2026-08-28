<script setup lang="ts">
import { adminApiPaths } from '@/shared/api/admin'

const plannedSections = [
  { label: '组织管理', path: adminApiPaths.users },
  { label: 'Agent 观测', path: adminApiPaths.agentRuns },
  { label: '审计与系统', path: adminApiPaths.auditEvents },
]
</script>

<template>
  <section class="page admin-page-skeleton">
    <header class="page-header">
      <div>
        <p class="page-eyebrow">ADMIN CONSOLE</p>
        <h1>管理员后台</h1>
        <p class="page-subtitle">管理员 session 与 API 接入完成后启用。</p>
      </div>
    </header>

    <div class="panel admin-page-skeleton__panel">
      <div>
        <span class="status-dot status-dot--muted" aria-hidden="true" />
        <strong>正式模块骨架</strong>
      </div>
      <p>当前仅提供类型、路径和页面边界，不展示模拟统计，也不改变普通门店权限。</p>
      <ul>
        <li v-for="section in plannedSections" :key="section.path">
          <span>{{ section.label }}</span>
          <code>{{ section.path }}</code>
        </li>
      </ul>
    </div>
  </section>
</template>
