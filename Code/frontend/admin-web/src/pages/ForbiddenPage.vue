<script setup lang="ts">
import { ShieldAlert } from 'lucide-vue-next'
</script>

<template>
  <main class="forbidden-page"><section><span class="icon-wrap"><ShieldAlert aria-hidden="true" /></span><p>ACCESS RESTRICTED</p><h1>当前账号没有访问此页面的权限</h1><span class="detail">权限由管理员角色和服务端授权范围共同决定。</span><RouterLink to="/login">返回管理员登录</RouterLink></section></main>
</template>

<style scoped>
.forbidden-page { display: grid; min-height: calc(100vh - 152px); place-items: center; text-align: center; }.forbidden-page section { width: min(420px, 100%); border: 1px solid var(--admin-border); border-radius: 8px; background: #fff; padding: 34px 28px; box-shadow: var(--admin-shadow); }.icon-wrap { display: grid; width: 38px; height: 38px; margin: 0 auto; place-items: center; border: 1px solid #efdfc7; border-radius: 8px; background: var(--admin-attention-bg); color: var(--admin-attention); }.forbidden-page :deep(svg) { width: 19px; height: 19px; stroke-width: 1.7; }.forbidden-page p { margin: 18px 0 0; color: var(--admin-muted); font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 9px; letter-spacing: .12em; }.forbidden-page h1 { margin: 10px 0 0; font-size: 18px; font-weight: 520; line-height: 1.45; }.detail { display: block; margin-top: 9px; color: var(--admin-muted); font-size: 11px; line-height: 1.6; }.forbidden-page a { display: inline-flex; height: 32px; align-items: center; margin-top: 22px; border: 1px solid var(--admin-border); border-radius: 999px; padding: 0 13px; color: var(--admin-foreground); font-size: 11px; text-decoration: none; }.forbidden-page a:hover { border-color: var(--admin-border-strong); background: var(--admin-secondary); }
</style>
