<template>
  <section class="rewrite-shell">
    <div class="rewrite-shell__card">
      <span class="rewrite-shell__mark">智</span>
      <h1>业务系统重构中</h1>
      <p>旧业务页面已清场，新领域页面正在重写。登录态与权限仍可正常使用。</p>
    </div>
  </section>
</template>

<style scoped>
.rewrite-shell {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100%;
  padding: 48px 24px;
}

.rewrite-shell__card {
  max-width: 480px;
  padding: 40px 48px;
  text-align: center;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 24px rgb(15 23 42 / 6%);
}

.rewrite-shell__mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: #1d4ed8;
  color: #fff;
  font-size: 24px;
  font-weight: 700;
}

.rewrite-shell__card h1 {
  margin: 16px 0 8px;
  font-size: 22px;
  color: #0f172a;
}

.rewrite-shell__card p {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: #64748b;
}
</style>
