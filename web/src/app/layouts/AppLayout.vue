<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { pcDesktopScreens } from '@/app/router/stitch-screens'
import { roleDescriptions, roleLabels, type StoreRole } from '@/entities/auth/roles'
import { useSession } from '@/app/stores/session'

const route = useRoute()
const router = useRouter()
const session = useSession()

const navGroups = computed(() => {
  const groups = new Map<string, typeof pcDesktopScreens>()
  pcDesktopScreens.forEach((screen) => {
    if (!session.hasPermission(screen.permission)) return
    const items = groups.get(screen.module) ?? []
    items.push(screen)
    groups.set(screen.module, items)
  })
  return Array.from(groups.entries()).map(([label, items]) => ({ label, items }))
})

const roleOptions: StoreRole[] = ['OWNER', 'MANAGER', 'SALES', 'PURCHASING', 'WAREHOUSE', 'FINANCE', 'ASSISTANT']

async function logout() {
  session.logout()
  await router.push('/login')
}
</script>

<template>
  <div class="app-layout">
    <aside class="sidebar">
      <RouterLink to="/dashboard" class="brand">
        <span class="brand-mark">智</span>
        <span>
          <strong>智慧记 Web</strong>
          <small>{{ session.member.value.storeName }}</small>
        </span>
      </RouterLink>

      <section class="role-panel">
        <label for="role">当前角色</label>
        <select
          id="role"
          :value="session.role.value"
          :disabled="session.source.value === 'api'"
          @change="session.switchRole(($event.target as HTMLSelectElement).value as StoreRole)"
        >
          <option v-for="role in roleOptions" :key="role" :value="role">{{ roleLabels[role] }}</option>
        </select>
        <p>
          {{ session.source.value === 'api'
            ? '真实后端当前按 owner_user_id 作用域鉴权，API 登录后统一视为店长（总）全量数据权限。'
            : roleDescriptions[session.role.value] }}
        </p>
      </section>

      <nav class="nav">
        <section v-for="group in navGroups" :key="group.label">
          <p>{{ group.label }}</p>
          <RouterLink v-for="item in group.items" :key="item.id" :to="item.route" class="nav-item">
            <span>{{ item.order }}</span>
            {{ item.title }}
          </RouterLink>
        </section>
      </nav>
    </aside>

    <main class="workspace">
      <header class="topbar">
        <div>
          <p class="eyebrow">Stitch MCP / 智慧记 Web 智效系统</p>
          <h1>{{ String(route.meta.title ?? '经营首页') }}</h1>
        </div>
        <div class="topbar-actions">
          <span class="session-source">{{ session.source.value === 'api' ? 'API 已连接' : '本地演示' }}</span>
          <RouterLink to="/settings/roles">角色权限</RouterLink>
          <RouterLink to="/settings/database">数据库</RouterLink>
          <span class="user-chip">{{ session.roleLabel.value }}</span>
          <button v-if="session.isAuthenticated.value" type="button" class="ghost-action" @click="logout">退出</button>
          <RouterLink v-else-if="session.source.value === 'demo'" to="/login">切回登录</RouterLink>
        </div>
      </header>
      <RouterView />
    </main>
  </div>
</template>
