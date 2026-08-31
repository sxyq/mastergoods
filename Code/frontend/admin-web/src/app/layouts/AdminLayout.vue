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
          <strong>MASTER GOODS</strong>
          <small>ADMIN CONSOLE</small>
        </RouterLink>
        <button class="icon-button sidebar-close" type="button" aria-label="关闭导航" @click="closeMobile"><X /></button>
      </div>

      <nav class="sidebar-nav">
        <p class="nav-caption">控制台</p>
        <div class="nav-list">
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
        </div>

        <p v-if="visibleAgentNavigation.length || visibleSystemNavigation.length" class="nav-caption nav-caption--spaced">监控视图</p>

        <section v-if="visibleAgentNavigation.length" class="nav-group nav-group--first">
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
        <span class="account-avatar" aria-hidden="true">SA</span>
        <div class="account-copy"><span class="service-status"><i />服务正常</span><strong>{{ adminSession.session?.admin_user_id || '系统管理员' }}</strong><small>{{ displayRole }}</small></div>
        <button class="icon-button" type="button" aria-label="退出管理员后台" @click="signOut"><LogOut /></button>
      </div>
    </aside>

    <div class="shell-main">
      <header class="mobile-header">
        <button class="icon-button" type="button" aria-label="打开导航" @click="mobileOpen = true"><Menu /></button>
        <RouterLink class="mobile-brand" to="/overview">MASTER GOODS</RouterLink>
        <RouterLink v-if="hasAdminPermission('admin.system.read')" class="icon-button" to="/system" aria-label="打开系统状态"><Activity aria-hidden="true" /></RouterLink>
        <span v-else class="mobile-spacer" aria-hidden="true" />
      </header>
      <main class="page-container"><RouterView /></main>
    </div>
  </div>
</template>

<style scoped>
.admin-app-shell { min-height: 100vh; background: var(--admin-background); }
.sidebar { position: fixed; inset: 0 auto 0 0; z-index: 30; display: flex; width: 288px; flex-direction: column; border-right: 1px solid var(--admin-border); background: #fff; padding: 24px 16px 16px; }
.brand-row { display: flex; min-height: 34px; align-items: flex-start; justify-content: space-between; padding: 0 10px; }
.brand { display: grid; gap: 3px; color: var(--admin-foreground); text-decoration: none; }
.brand strong { font-size: 14px; font-weight: 650; letter-spacing: .045em; line-height: 1; }
.brand small { color: var(--admin-muted-light); font-size: 8px; font-weight: 500; letter-spacing: .17em; line-height: 1; }
.mobile-brand { color: var(--admin-foreground); font-size: 12px; font-weight: 650; letter-spacing: .045em; text-decoration: none; }
.icon-button { display: inline-grid; width: 30px; height: 30px; place-items: center; border: 0; border-radius: 6px; background: transparent; color: var(--admin-muted); cursor: pointer; }
.icon-button:hover { background: var(--admin-secondary); color: var(--admin-foreground); }
.icon-button :deep(svg) { width: 16px; height: 16px; stroke-width: 1.7; }
.sidebar-close { display: none; }
.sidebar-nav { min-height: 0; flex: 1; margin-top: 30px; overflow-y: auto; padding-right: 2px; }
.nav-caption { margin: 0 10px 7px; color: var(--admin-foreground); font-size: 11px; font-weight: 600; }
.nav-caption--spaced { margin-top: 32px; }
.nav-list { display: grid; gap: 2px; }
.nav-link, .nav-group-title { display: flex; width: 100%; min-height: 32px; align-items: center; gap: 9px; border: 0; border-radius: 6px; background: transparent; padding: 7px 10px; color: var(--admin-muted); font-size: 12px; font-weight: 400; line-height: 1.35; text-align: left; text-decoration: none; transition: background-color .16s ease-out, color .16s ease-out; }
.nav-link:hover, .nav-group-title:hover { background: var(--admin-secondary); color: var(--admin-foreground); }
.nav-link--active { background: var(--admin-secondary); color: var(--admin-foreground); }
.nav-link :deep(svg), .nav-group-title :deep(svg) { width: 16px; height: 16px; flex: 0 0 16px; stroke-width: 1.7; }
.nav-group { margin-top: 12px; }
.nav-group--first { margin-top: 0; }
.nav-group-title { justify-content: space-between; color: var(--admin-foreground); cursor: pointer; }
.nav-group-title > span { display: flex; align-items: center; gap: 9px; }
.nav-group-title > :deep(svg) { width: 14px; height: 14px; transition: transform .16s ease-out; }
.chevron--closed { transform: rotate(-90deg); }
.nav-group-items { display: grid; gap: 2px; margin-top: 4px; }
.nav-link--child { padding-left: 18px; }
.nav-link--child :deep(svg) { width: 15px; height: 15px; }
.sidebar-account { display: flex; min-height: 74px; align-items: flex-end; gap: 10px; border-top: 1px solid var(--admin-border); padding: 14px 6px 0; }
.account-avatar { display: grid; width: 30px; height: 30px; flex: 0 0 30px; place-items: center; border: 1px solid var(--admin-border); border-radius: 6px; background: var(--admin-secondary); color: var(--admin-muted); font-size: 9px; }
.account-copy { min-width: 0; flex: 1; }
.service-status { display: flex; align-items: center; gap: 6px; margin-bottom: 10px; color: var(--admin-muted); font-size: 10px; }
.service-status i { width: 6px; height: 6px; border-radius: 50%; background: var(--admin-positive); }
.account-copy strong, .account-copy small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.account-copy strong { font-size: 11px; font-weight: 550; }.account-copy small { margin-top: 3px; color: var(--admin-muted-light); font-size: 9px; letter-spacing: .04em; }
.shell-main { min-height: 100vh; padding-left: 288px; }
.page-container { width: 100%; max-width: 1360px; margin: 0 auto; padding: 76px 32px 40px; }
.mobile-header, .sidebar-scrim { display: none; }

@media (max-width: 1180px) { .page-container { padding-inline: 24px; } }
@media (max-width: 760px) {
  .sidebar { left: -288px; width: 288px; transition: transform .18s ease-out; transform: translateX(0); }.sidebar--open { transform: translateX(288px); }.sidebar-close { display: inline-grid; }
  .sidebar-scrim { position: fixed; inset: 0; z-index: 20; display: block; width: 100%; border: 0; background: rgba(20, 20, 18, .24); }
  .shell-main { padding-left: 0; }.mobile-header { position: sticky; top: 0; z-index: 10; display: flex; height: 48px; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--admin-border); background: rgba(253, 253, 252, .97); padding: 0 14px; }
  .mobile-spacer { width: 30px; }
  .page-container { padding: 30px 12px 24px; }
}
</style>
