<script setup lang="ts">
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { roleDescriptions, roleLabels, type StoreRole } from '@/entities/auth/roles'
import { useSession } from '@/app/stores/session'

const router = useRouter()
const session = useSession()

const roleOptions: StoreRole[] = ['OWNER', 'MANAGER', 'SALES', 'PURCHASING', 'WAREHOUSE', 'FINANCE', 'ASSISTANT']

async function logout() {
  await session.logout()
  await router.push('/login')
}
</script>

<template>
  <div class="app-layout">
    <aside class="sidebar">
      <RouterLink to="/" class="brand">
        <span class="brand-mark">智</span>
        <span>
          <strong>智慧记</strong>
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
            ? '真实后端按门店成员权限上下文鉴权，导航与操作入口会随当前账号权限变化。'
            : roleDescriptions[session.role.value] }}
        </p>
      </section>

      <div class="sidebar-footer">
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
