<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useSession } from '@/app/stores/session'

const router = useRouter()
const session = useSession()
const form = reactive({
  phone: '13800000001',
  password: '123456',
})

async function submit() {
  const ok = await session.login(form.phone, form.password)
  if (ok) {
    await router.push('/dashboard')
  }
}

async function enterDemo() {
  session.enterDemo()
  await router.push('/dashboard')
}
</script>

<template>
  <main class="login-page">
    <section class="login-card">
      <div class="brand-row">
        <span class="brand-mark">智</span>
        <div>
          <h1>智慧记 Web</h1>
          <p>PC 经营管理后台</p>
        </div>
      </div>
      <form class="login-form" @submit.prevent="submit">
        <label>
          手机号
          <input v-model="form.phone" autocomplete="username" />
        </label>
        <label>
          密码
          <input v-model="form.password" type="password" autocomplete="current-password" />
        </label>
        <p v-if="session.error.value" class="form-error">{{ session.error.value }}</p>
        <button type="submit" class="primary-action" :disabled="session.loading.value">
          {{ session.loading.value ? '登录中' : '进入系统' }}
        </button>
        <p class="login-hint">可尝试现有后端账号登录；也可直接进入本地演示，演示店长（总）与多员工权限。</p>
        <button type="button" class="secondary-action" @click="enterDemo">使用本地演示权限</button>
      </form>
    </section>
  </main>
</template>
