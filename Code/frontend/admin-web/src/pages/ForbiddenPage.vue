<script setup lang="ts">
import { ShieldAlert } from 'lucide-vue-next'
</script>

<template>
  <main class="forbidden-page"><section><ShieldAlert aria-hidden="true" /><p>ACCESS RESTRICTED</p><h1>当前账号没有访问此页面的权限</h1><RouterLink to="/overview">返回平台总览</RouterLink></section></main>
</template>

<style scoped>
.forbidden-page { display: grid; min-height: 50vh; place-items: center; text-align: center; }.forbidden-page section { max-width: 400px; }.forbidden-page :deep(svg) { width: 28px; height: 28px; color: var(--admin-attention); }.forbidden-page p { margin: 16px 0 0; color: var(--admin-muted); font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 10px; letter-spacing: .08em; }.forbidden-page h1 { margin: 10px 0 0; font-size: 18px; font-weight: 500; }.forbidden-page a { display: inline-block; margin-top: 18px; color: var(--admin-muted); font-size: 12px; }.forbidden-page a:hover { color: var(--admin-foreground); }
</style>
