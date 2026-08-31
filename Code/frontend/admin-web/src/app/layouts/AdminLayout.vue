<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Activity, Bot, ChevronDown, Eye, LayoutDashboard, LogOut, Menu, Server, Settings2, ShieldCheck, UsersRound, X } from 'lucide-vue-next'
import { adminSession, hasAdminPermission, logoutAdmin } from '@/app/stores/admin-session'

type NavigationItem = {
  label: string
  to: string
  permission: string
  icon: typeof LayoutDashboard
}

const router = useRouter()
const route = useRoute()
const mobileOpen = ref(false)
const agentOpen = ref(true)
const systemOpen = ref(true)

const navigation: NavigationItem[] = [
  { label: '平台总览', to: '/overview', permission: 'admin.dashboard.read', icon: LayoutDashboard },
  { label: '用户与门店', to: '/users', permission: 'admin.user.read', icon: UsersRound },
]

const agentNavigation: NavigationItem[] = [
  { label: '运行观测', to: '/agent/runs', permission: 'admin.agent.run.read', icon: Bot },
  { label: 'Agent 配置', to: '/agent/config', permission: 'admin.agent.config.read', icon: Settings2 },
]

const systemNavigation: NavigationItem[] = [
  { label: '操作审计', to: '/audit', permission: 'admin.audit.read', icon: Eye },
  { label: '系统状态', to: '/system', permission: 'admin.system.read', icon: Server },
]

const visible = (items: NavigationItem[]) => items.filter((item) => hasAdminPermission(item.permission))
const visibleNavigation = computed(() => visible(navigation))
const visibleAgentNavigation = computed(() => visible(agentNavigation))
const visibleSystemNavigation = computed(() => visible(systemNavigation))
const displayRole = computed(() => adminSession.session?.role === 'SUPER_ADMIN' ? '超级管理员' : '审计观察员')
const currentPath = computed(() => route.path)

function closeMobile(): void {
  mobileOpen.value = false
}

async function signOut(): Promise<void> {
  await logoutAdmin()
  closeMobile()
  await router.replace('/login')
}
</script>

<template>
  <div class="admin-app-shell">
    <button v-if="mobileOpen" class="sidebar-scrim" type="button" aria-label="关闭导航" @click="closeMobile" />
    <aside class="sidebar" :class="{ 'sidebar--open': mobileOpen }" aria-label="管理员导航">
      <div class="brand-row">
        <RouterLink class="brand" to="/overview" @click="closeMobile">
          <span>智慧记</span>
          <small>ADMIN</small>
        </RouterLink>
        <button class="icon-button sidebar-close" type="button" aria-label="关闭导航" @click="closeMobile"><X /></button>
      </div>

      <nav class="sidebar-nav">
        <RouterLink
          v-for="item in visibleNavigation"
          :key="item.to"
          :to="item.to"
          class="nav-link"
          :class="{ 'nav-link--active': currentPath === item.to }"
          @click="closeMobile"
        >
          <component :is="item.icon" aria-hidden="true" />
          <span>{{ item.label }}</span>
        </RouterLink>

        <section v-if="visibleAgentNavigation.length" class="nav-group">
          <button class="nav-group-title" type="button" :aria-expanded="agentOpen" @click="agentOpen = !agentOpen">
            <span><Bot aria-hidden="true" />Agent</span><ChevronDown :class="{ 'chevron--closed': !agentOpen }" aria-hidden="true" />
          </button>
          <div v-show="agentOpen" class="nav-group-items">
            <RouterLink v-for="item in visibleAgentNavigation" :key="item.to" :to="item.to" class="nav-link nav-link--child" :class="{ 'nav-link--active': currentPath === item.to }" @click="closeMobile">
              <component :is="item.icon" aria-hidden="true" /><span>{{ item.label }}</span>
            </RouterLink>
          </div>
        </section>

        <section v-if="visibleSystemNavigation.length" class="nav-group">
          <button class="nav-group-title" type="button" :aria-expanded="systemOpen" @click="systemOpen = !systemOpen">
            <span><ShieldCheck aria-hidden="true" />管理与安全</span><ChevronDown :class="{ 'chevron--closed': !systemOpen }" aria-hidden="true" />
          </button>
          <div v-show="systemOpen" class="nav-group-items">
            <RouterLink v-for="item in visibleSystemNavigation" :key="item.to" :to="item.to" class="nav-link nav-link--child" :class="{ 'nav-link--active': currentPath === item.to }" @click="closeMobile">
              <component :is="item.icon" aria-hidden="true" /><span>{{ item.label }}</span>
            </RouterLink>
          </div>
        </section>
      </nav>

      <div class="sidebar-account">
        <div class="account-copy"><strong>{{ adminSession.session?.admin_user_id || '管理员' }}</strong><small>{{ displayRole }}</small></div>
        <button class="icon-button" type="button" aria-label="退出管理员后台" @click="signOut"><LogOut /></button>
      </div>
    </aside>

    <div class="shell-main">
      <header class="mobile-header">
        <button class="icon-button" type="button" aria-label="打开导航" @click="mobileOpen = true"><Menu /></button>
        <RouterLink class="mobile-brand" to="/overview">智慧记 <small>ADMIN</small></RouterLink>
        <span class="mobile-status"><Activity aria-hidden="true" /> 已连接</span>
      </header>
      <main class="page-container"><RouterView /></main>
    </div>
  </div>
</template>

<style scoped>
.admin-app-shell { min-height: 100vh; background: var(--admin-background); }
.sidebar { position: fixed; inset: 0 auto 0 0; z-index: 30; display: flex; width: 288px; flex-direction: column; border-right: 1px solid var(--admin-border); background: #fff; padding: 24px 16px 16px; }
.brand-row { display: flex; height: 28px; align-items: center; justify-content: space-between; padding: 0 10px; }
.brand, .mobile-brand { display: flex; align-items: baseline; gap: 8px; color: var(--admin-foreground); font-size: 16px; font-weight: 600; text-decoration: none; }
.brand small, .mobile-brand small { color: var(--admin-muted); font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 9px; font-weight: 500; letter-spacing: .08em; }
.icon-button { display: inline-grid; width: 30px; height: 30px; place-items: center; border: 0; border-radius: 6px; background: transparent; color: var(--admin-muted); cursor: pointer; }
.icon-button:hover { background: var(--admin-secondary); color: var(--admin-foreground); }
.icon-button :deep(svg) { width: 16px; height: 16px; stroke-width: 1.7; }
.sidebar-close { display: none; }
.sidebar-nav { min-height: 0; flex: 1; margin-top: 30px; overflow-y: auto; padding-right: 2px; }
.nav-link, .nav-group-title { display: flex; width: 100%; height: 32px; align-items: center; gap: 9px; border: 0; border-radius: 6px; background: transparent; padding: 0 10px; color: var(--admin-muted); font-size: 12px; font-weight: 400; text-align: left; text-decoration: none; transition: background-color .16s ease-out, color .16s ease-out; }
.nav-link:hover, .nav-group-title:hover { background: var(--admin-secondary); color: var(--admin-foreground); }
.nav-link--active { background: var(--admin-secondary); color: var(--admin-foreground); }
.nav-link :deep(svg), .nav-group-title :deep(svg) { width: 16px; height: 16px; flex: 0 0 16px; stroke-width: 1.7; }
.nav-group { margin-top: 24px; }
.nav-group-title { justify-content: space-between; color: var(--admin-foreground); cursor: pointer; }
.nav-group-title > span { display: flex; align-items: center; gap: 9px; }
.nav-group-title > :deep(svg) { width: 14px; height: 14px; transition: transform .16s ease-out; }
.chevron--closed { transform: rotate(-90deg); }
.nav-group-items { display: grid; gap: 2px; margin-top: 4px; }
.nav-link--child { padding-left: 18px; }
.nav-link--child :deep(svg) { width: 15px; height: 15px; }
.sidebar-account { display: flex; min-height: 56px; align-items: center; gap: 10px; border-top: 1px solid var(--admin-border); padding: 14px 10px 0; }
.account-copy { min-width: 0; flex: 1; }
.account-copy strong, .account-copy small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.account-copy strong { font-size: 11px; font-weight: 500; }.account-copy small { margin-top: 4px; color: var(--admin-muted); font-size: 10px; }
.shell-main { min-height: 100vh; padding-left: 288px; }
.page-container { width: 100%; max-width: 1280px; margin: 0 auto; padding: 76px 32px 40px; }
.mobile-header, .sidebar-scrim { display: none; }

@media (max-width: 1180px) { .sidebar { width: 248px; }.shell-main { padding-left: 248px; }.page-container { padding-inline: 24px; } }
@media (max-width: 760px) {
  .sidebar { left: -288px; width: 288px; transition: left .18s ease-out; }.sidebar--open { left: 0; }.sidebar-close { display: inline-grid; }
  .sidebar-scrim { position: fixed; inset: 0; z-index: 20; display: block; width: 100%; border: 0; background: rgba(20, 20, 18, .2); }
  .shell-main { padding-left: 0; }.mobile-header { position: sticky; top: 0; z-index: 10; display: flex; height: 48px; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--admin-border); background: rgba(252, 252, 251, .96); padding: 0 14px; }
  .mobile-brand { font-size: 13px; }.mobile-status { display: inline-flex; align-items: center; gap: 5px; color: var(--admin-positive); font-size: 10px; }.mobile-status :deep(svg) { width: 12px; height: 12px; }
  .page-container { padding: 30px 16px 24px; }
}
</style>
