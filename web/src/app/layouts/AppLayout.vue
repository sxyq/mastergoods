<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { pcDesktopScreens } from '@/app/router/stitch-screens'
import { roleDescriptions, roleLabels, type StoreRole } from '@/entities/auth/roles'
import { useSession } from '@/app/stores/session'

const router = useRouter()
const session = useSession()
const searchQuery = ref('')
const searchInputRef = ref<HTMLInputElement | null>(null)
type IndexedScreen = (typeof pcDesktopScreens)[number] & {
  searchText: string
}

const indexedScreens = pcDesktopScreens.map((screen) => ({
  ...screen,
  searchText: `${screen.title} ${screen.module}`.toLowerCase(),
})) satisfies IndexedScreen[]

const navGroups = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  const groups = new Map<string, IndexedScreen[]>()
  for (const screen of indexedScreens) {
    const allowed = screen.permissionMode === 'any'
      ? session.hasAnyPermission(screen.permission)
      : session.hasPermission(screen.permission)
    if (!allowed) continue
    if (query && !screen.searchText.includes(query)) continue
    const items = groups.get(screen.module)
    if (items) {
      items.push(screen)
    } else {
      groups.set(screen.module, [screen])
    }
  }
  return Array.from(groups.entries()).map(([label, items]) => ({ label, items }))
})

const roleOptions: StoreRole[] = ['OWNER', 'MANAGER', 'SALES', 'PURCHASING', 'WAREHOUSE', 'FINANCE', 'ASSISTANT']

async function logout() {
  await session.logout()
  await router.push('/login')
}

function handleGlobalKeydown(event: KeyboardEvent) {
  if (event.key === '/' && document.activeElement?.tagName !== 'INPUT' && document.activeElement?.tagName !== 'TEXTAREA') {
    event.preventDefault()
    searchInputRef.value?.focus()
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleGlobalKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleGlobalKeydown)
})
</script>

<template>
  <div class="app-layout">
    <aside class="sidebar">
      <RouterLink to="/dashboard" class="brand">
        <span class="brand-mark">智</span>
        <span>
          <strong>智慧记</strong>
          <small>{{ session.member.value.storeName }}</small>
        </span>
      </RouterLink>

      <label class="sidebar-search">
        <span class="material-symbols-outlined">search</span>
        <input
          ref="searchInputRef"
          v-model="searchQuery"
          type="text"
          placeholder="搜索单据..."
        />
        <i>/</i>
      </label>

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
            ? '真实后端按门店成员权限上下文鉴权，导航与操作入口会随当前账号权限变化。'
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
        <p v-if="navGroups.length === 0" class="nav-empty">未找到匹配的页面</p>
      </nav>

      <div class="sidebar-footer">
        <RouterLink to="/settings/database" class="sidebar-footer__link">系统设置</RouterLink>
        <RouterLink to="/settings/roles" class="sidebar-footer__link">角色权限</RouterLink>
        <div class="sidebar-profile">
          <strong>{{ session.member.value.name }}</strong>
          <span>{{ session.source.value === 'api' ? 'API 已连接' : '本地演示' }}</span>
        </div>
        <button v-if="session.isAuthenticated.value" type="button" class="ghost-action sidebar-footer__button" @click="logout">退出登录</button>
        <RouterLink v-else-if="session.source.value === 'demo'" to="/login" class="sidebar-footer__link sidebar-footer__link--button">切回登录</RouterLink>
      </div>
    </aside>

    <main class="workspace">
      <RouterView />
    </main>
  </div>
</template>
