<script setup lang="ts">
import { Menu, Settings2 } from 'lucide-vue-next'
import { ref } from 'vue'
import AdminSidebar from './AdminSidebar.vue'

withDefaults(defineProps<{
  activeId?: string
}>(), {
  activeId: 'overview',
})

const emit = defineEmits<{
  navigate: [id: string]
  notice: [message: string]
}>()

const sidebarOpen = ref(false)

function closeSidebar() {
  sidebarOpen.value = false
}
</script>

<template>
  <div class="admin-shell">
    <AdminSidebar
      :open="sidebarOpen"
      :active-id="activeId"
      @close="closeSidebar"
      @navigate="emit('navigate', $event)"
      @notice="emit('notice', $event)"
    />
    <button v-if="sidebarOpen" type="button" class="admin-sidebar-backdrop" aria-label="关闭导航" @click="closeSidebar" />

    <div class="admin-main-column">
      <header class="admin-mobile-header">
        <button type="button" class="admin-icon-button" aria-label="打开导航" title="打开导航" @click="sidebarOpen = true">
          <Menu aria-hidden="true" />
        </button>
        <strong>MASTER GOODS</strong>
        <button type="button" class="admin-icon-button" aria-label="打开系统设置" title="打开系统设置" @click="emit('notice', '系统设置将在后续阶段接入')">
          <Settings2 aria-hidden="true" />
        </button>
      </header>
      <main class="admin-content">
        <slot />
      </main>
    </div>
  </div>
</template>
