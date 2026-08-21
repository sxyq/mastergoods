<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useSession } from '@/app/stores/session'

const router = useRouter()
const session = useSession()

async function reauthenticate() {
  await session.logout()
  await router.replace('/login')
}
</script>

<template>
  <section class="access-denied">
    <h2>403 无权访问</h2>
    <p>当前身份没有访问该页面的权限。请切换到允许的演示角色，或使用真实 owner 账号重新登录。</p>
    <button type="button" class="primary-button" @click="reauthenticate">重新登录</button>
  </section>
</template>
