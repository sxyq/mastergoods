<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, Eye, EyeOff, KeyRound, ShieldCheck } from 'lucide-vue-next'
import { loginAsAdmin } from '@/app/stores/admin-session'

const router = useRouter()
const route = useRoute()
const account = ref('')
const password = ref('')
const showPassword = ref(false)
const submitting = ref(false)
const error = ref('')
const redirect = computed(() => typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/') ? route.query.redirect : '/overview')

async function submit(): Promise<void> {
  if (!account.value.trim() || !password.value) {
    error.value = '请输入账号和密码。'
    return
  }
  submitting.value = true
  error.value = ''
  try {
    await loginAsAdmin(account.value.trim(), password.value)
    password.value = ''
    await router.replace(redirect.value)
  } catch (reason) {
    password.value = ''
    error.value = reason instanceof Error ? reason.message : '登录失败，请稍后重试。'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="login-shell">
    <section class="login-panel" aria-labelledby="login-title">
      <header class="login-brand"><span class="brand-mark"><ShieldCheck aria-hidden="true" /></span><div><strong>智慧记</strong><small>ADMIN CONSOLE</small></div></header>
      <div class="login-copy"><p>SUPER ADMIN</p><h1 id="login-title">管理员登录</h1><span>使用已授予管理员角色的统一账号。</span></div>
      <form @submit.prevent="submit">
        <label>账号<input v-model="account" name="account" autocomplete="username" :disabled="submitting" /></label>
        <label>密码<span class="password-field"><input v-model="password" name="password" :type="showPassword ? 'text' : 'password'" autocomplete="current-password" :disabled="submitting" /><button type="button" :aria-label="showPassword ? '隐藏密码' : '显示密码'" :disabled="submitting" @click="showPassword = !showPassword"><EyeOff v-if="showPassword" /><Eye v-else /></button></span></label>
        <p v-if="error" class="form-error" role="alert">{{ error }}</p>
        <button class="submit-button" type="submit" :disabled="submitting"><KeyRound aria-hidden="true" />{{ submitting ? '正在验证' : '登录后台' }}<ArrowRight aria-hidden="true" /></button>
      </form>
    </section>
  </main>
</template>

<style scoped>
.login-shell { display: grid; min-height: 100vh; place-items: center; background: var(--admin-background); padding: 24px; }.login-panel { width: min(400px, 100%); border: 1px solid var(--admin-border); border-radius: 8px; background: #fff; padding: 28px; box-shadow: 0 12px 30px rgba(26, 26, 23, .05); }.login-brand { display: flex; align-items: center; gap: 10px; }.brand-mark { display: grid; width: 34px; height: 34px; place-items: center; border: 1px solid var(--admin-border); border-radius: 7px; color: var(--admin-foreground); }.brand-mark :deep(svg) { width: 18px; height: 18px; stroke-width: 1.7; }.login-brand strong, .login-brand small { display: block; }.login-brand strong { font-size: 14px; font-weight: 600; }.login-brand small { margin-top: 3px; color: var(--admin-muted); font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 9px; letter-spacing: .08em; }.login-copy { margin-top: 38px; }.login-copy p { margin: 0; color: var(--admin-muted); font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 10px; letter-spacing: .08em; }.login-copy h1 { margin: 9px 0 0; font-size: 24px; font-weight: 500; }.login-copy span { display: block; margin-top: 9px; color: var(--admin-muted); font-size: 12px; }form { display: grid; gap: 18px; margin-top: 28px; }label { display: grid; gap: 7px; color: var(--admin-foreground); font-size: 12px; }input { width: 100%; height: 38px; border: 1px solid var(--admin-border); border-radius: 6px; background: #fff; padding: 0 10px; color: var(--admin-foreground); outline: 0; }.password-field { position: relative; }.password-field input { padding-right: 42px; }.password-field button { position: absolute; inset: 0 4px 0 auto; display: grid; width: 32px; place-items: center; border: 0; background: transparent; color: var(--admin-muted); cursor: pointer; }.password-field button :deep(svg) { width: 16px; height: 16px; }.form-error { margin: -6px 0 0; color: var(--admin-danger); font-size: 11px; }.submit-button { display: flex; height: 38px; align-items: center; justify-content: center; gap: 8px; border: 1px solid var(--admin-foreground); border-radius: 999px; background: var(--admin-foreground); color: #fff; cursor: pointer; font-size: 12px; }.submit-button :deep(svg) { width: 15px; height: 15px; stroke-width: 1.7; }.submit-button :deep(svg):last-child { margin-left: 2px; }.submit-button:disabled { cursor: wait; opacity: .65; }
</style>
