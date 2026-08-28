<script setup lang="ts">
import { ref } from 'vue'
import {
  Activity,
  Bot,
  ChevronDown,
  Database,
  Eye,
  Github,
  LayoutDashboard,
  MoreHorizontal,
  Server,
  Settings2,
  Store,
  UsersRound,
  X,
} from 'lucide-vue-next'
import type { Component } from 'vue'

withDefaults(defineProps<{
  open?: boolean
  activeId?: string
}>(), {
  open: false,
  activeId: 'overview',
})

const emit = defineEmits<{
  close: []
  navigate: [id: string]
  notice: [message: string]
}>()

interface AdminNavItem {
  id: string
  label: string
  icon: Component
  badge?: string
}

const primaryItems: AdminNavItem[] = [
  { id: 'overview', label: '平台总览', icon: LayoutDashboard },
  { id: 'users', label: '用户与门店', icon: UsersRound },
  { id: 'agent', label: 'Agent 运行', icon: Bot, badge: '12' },
  { id: 'audit', label: '操作审计', icon: Eye },
  { id: 'system', label: '系统状态', icon: Server },
]

const documentationGroups = [
  { label: 'Agent', icon: Bot, items: ['运行记录', '工具调用', '上下文窗口'] },
  { label: '组织', icon: UsersRound, items: ['门店成员', '权限范围'] },
  { label: '系统', icon: Database, items: ['服务健康', '数据保留'] },
]

const openGroups = ref<Record<string, boolean>>({ Agent: true, 组织: false, 系统: false })

function selectItem(item: AdminNavItem) {
  if (item.id === 'overview') {
    emit('navigate', item.id)
    emit('close')
    return
  }
  emit('notice', `${item.label}将在后续阶段接入`)
}

function toggleGroup(label: string) {
  openGroups.value[label] = !openGroups.value[label]
}
</script>

<template>
  <aside class="admin-sidebar" :class="{ 'admin-sidebar--open': open }" aria-label="管理员后台导航">
    <div class="admin-sidebar-brand">
      <button type="button" class="admin-brand-lockup" aria-label="返回平台总览" title="返回平台总览" @click="selectItem(primaryItems[0])">
        <strong>MASTER GOODS</strong>
        <span>ADMIN CONSOLE</span>
      </button>
      <button type="button" class="admin-icon-button admin-sidebar-close" aria-label="关闭导航" title="关闭导航" @click="emit('close')">
        <X aria-hidden="true" />
      </button>
      <button type="button" class="admin-icon-button admin-github-link" aria-label="打开项目主页" title="项目主页" @click="emit('notice', '项目主页入口仅用于视觉预览')">
        <Github aria-hidden="true" />
      </button>
    </div>

    <nav class="admin-sidebar-nav">
      <div class="admin-nav-caption">控制台</div>
      <div class="admin-nav-list">
        <button
          v-for="item in primaryItems"
          :key="item.id"
          type="button"
          class="admin-nav-item"
          :class="{ 'admin-nav-item--active': activeId === item.id }"
          :aria-current="activeId === item.id ? 'page' : undefined"
          @click="selectItem(item)"
        >
          <component :is="item.icon" aria-hidden="true" />
          <span>{{ item.label }}</span>
          <small v-if="item.badge">{{ item.badge }}</small>
        </button>
      </div>

      <div class="admin-nav-caption admin-nav-caption--spaced">监控视图</div>
      <div class="admin-nav-list admin-documentation-list">
        <div v-for="group in documentationGroups" :key="group.label" class="admin-documentation-group">
          <button type="button" class="admin-nav-item admin-documentation-toggle" :aria-expanded="Boolean(openGroups[group.label])" @click="toggleGroup(group.label)">
            <component :is="group.icon" aria-hidden="true" />
            <span>{{ group.label }}</span>
            <ChevronDown aria-hidden="true" :class="{ 'is-collapsed': !openGroups[group.label] }" />
          </button>
          <div v-if="openGroups[group.label]" class="admin-documentation-items">
            <button v-for="item in group.items" :key="item" type="button" @click="emit('notice', `${group.label} / ${item}将在后续阶段接入`)">
              {{ item }}
            </button>
          </div>
        </div>
      </div>
    </nav>

    <div class="admin-sidebar-footer">
      <div class="admin-connection-status"><span class="admin-status-dot admin-status-dot--online" /> 服务状态待接入</div>
      <button type="button" class="admin-account-control" @click="emit('notice', '管理员身份由服务端会话提供')">
        <span class="admin-account-avatar">SA</span>
        <span class="admin-account-copy"><strong>系统管理员</strong><small>SERVER SESSION</small></span>
        <MoreHorizontal class="admin-account-menu" aria-hidden="true" />
      </button>
      <div class="admin-sidebar-footer-links">
        <button type="button" @click="emit('notice', '系统设置将在后续阶段接入')"><Settings2 aria-hidden="true" />系统设置</button>
        <button type="button" @click="emit('notice', '权限范围由服务端会话决定')"><Store aria-hidden="true" />授权范围</button>
      </div>
      <span class="admin-sidebar-mark"><Activity aria-hidden="true" /> 观察、追踪、核验</span>
    </div>
  </aside>
</template>
