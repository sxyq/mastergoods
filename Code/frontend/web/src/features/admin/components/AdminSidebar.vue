<script setup lang="ts">
import { computed, ref } from 'vue'
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
import { useRouter } from 'vue-router'
import { useSession } from '@/app/stores/session'
import { useAdminSession } from '@/app/stores/admin-session'
import type { AdminPermission } from '@/entities/admin/contracts'

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
  permission: AdminPermission
}

const primaryItems: AdminNavItem[] = [
  { id: 'overview', label: '平台总览', icon: LayoutDashboard, permission: 'admin.dashboard.read' },
  { id: 'users', label: '用户与门店', icon: UsersRound, permission: 'admin.user.read' },
  { id: 'agent', label: 'Agent 运行', icon: Bot, permission: 'admin.agent.run.read' },
  { id: 'config', label: 'Agent 配置', icon: Settings2, permission: 'admin.agent.config.read' },
  { id: 'audit', label: '操作审计', icon: Eye, permission: 'admin.audit.read' },
  { id: 'system', label: '系统状态', icon: Server, permission: 'admin.system.read' },
]

const documentationGroups = [
  { label: 'Agent', icon: Bot, items: [{ label: '运行记录', id: 'agent', permission: 'admin.agent.run.read' as AdminPermission }, { label: '工具调用', id: 'agent', permission: 'admin.agent.run.read' as AdminPermission }, { label: '上下文窗口', id: 'agent', permission: 'admin.agent.run.read' as AdminPermission }, { label: '运行配置', id: 'config', permission: 'admin.agent.config.read' as AdminPermission }] },
  { label: '组织', icon: UsersRound, items: [{ label: '门店成员', id: 'users', permission: 'admin.store.read' as AdminPermission }, { label: '权限范围', id: 'users', permission: 'admin.store.read' as AdminPermission }] },
  { label: '系统', icon: Database, items: [{ label: '服务健康', id: 'system', permission: 'admin.system.read' as AdminPermission }, { label: '数据保留', id: 'system', permission: 'admin.system.read' as AdminPermission }] },
]

const openGroups = ref<Record<string, boolean>>({ Agent: true, 组织: false, 系统: false })
const router = useRouter()
const session = useSession()
const adminSession = useAdminSession()
const visiblePrimaryItems = computed(() => primaryItems.filter((item) => adminSession.can(item.permission)))
const visibleDocumentationGroups = computed(() => documentationGroups.map((group) => ({ ...group, items: group.items.filter((item) => adminSession.can(item.permission)) })).filter((group) => group.items.length))
const accountName = computed(() => adminSession.session.value?.adminUserId ? `管理员 ${adminSession.session.value.adminUserId}` : '管理员会话')
const accountRole = computed(() => adminSession.session.value?.role === 'SUPER_ADMIN' ? 'SUPER ADMIN' : adminSession.session.value?.role === 'AUDIT_OBSERVER' ? 'AUDIT OBSERVER' : session.token.value ? 'SERVER SESSION' : '未登录')
const accountAvatar = computed(() => adminSession.session.value?.role === 'SUPER_ADMIN' ? 'SA' : 'AO')

const routeById: Record<string, string> = {
  overview: '/admin/overview',
  users: '/admin/users',
  agent: '/admin/agent/runs',
  audit: '/admin/audit',
  system: '/admin/system',
  config: '/admin/agent/config',
}

function goTo(id: string) {
  const route = routeById[id]
  if (route) void router.push(route)
  emit('navigate', id)
  emit('close')
}

function selectItem(item: AdminNavItem) {
  goTo(item.id)
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
          v-for="item in visiblePrimaryItems"
          :key="item.id"
          type="button"
          class="admin-nav-item"
          :class="{ 'admin-nav-item--active': activeId === item.id }"
          :aria-current="activeId === item.id ? 'page' : undefined"
          @click="selectItem(item)"
        >
          <component :is="item.icon" aria-hidden="true" />
          <span>{{ item.label }}</span>
        </button>
      </div>

      <div class="admin-nav-caption admin-nav-caption--spaced">监控视图</div>
      <div class="admin-nav-list admin-documentation-list">
        <div v-for="group in visibleDocumentationGroups" :key="group.label" class="admin-documentation-group">
          <button type="button" class="admin-nav-item admin-documentation-toggle" :aria-expanded="Boolean(openGroups[group.label])" @click="toggleGroup(group.label)">
            <component :is="group.icon" aria-hidden="true" />
            <span>{{ group.label }}</span>
            <ChevronDown aria-hidden="true" :class="{ 'is-collapsed': !openGroups[group.label] }" />
          </button>
          <div v-if="openGroups[group.label]" class="admin-documentation-items">
            <button v-for="item in group.items" :key="item.label" type="button" @click="goTo(item.id)">
              {{ item.label }}
            </button>
          </div>
        </div>
      </div>
    </nav>

    <div class="admin-sidebar-footer">
      <div class="admin-connection-status"><span class="admin-status-dot admin-status-dot--online" /> 服务状态见系统页</div>
      <button type="button" class="admin-account-control" @click="emit('notice', '管理员身份由服务端会话提供')">
        <span class="admin-account-avatar">{{ accountAvatar }}</span>
        <span class="admin-account-copy"><strong>{{ accountName }}</strong><small>{{ accountRole }}</small></span>
        <MoreHorizontal class="admin-account-menu" aria-hidden="true" />
      </button>
      <div class="admin-sidebar-footer-links">
        <button type="button" @click="goTo('system')"><Settings2 aria-hidden="true" />系统状态</button>
        <button type="button" @click="emit('notice', '权限范围由服务端会话决定')"><Store aria-hidden="true" />授权范围</button>
      </div>
      <span class="admin-sidebar-mark"><Activity aria-hidden="true" /> 观察、追踪、核验</span>
    </div>
  </aside>
</template>
