<template>
  <section class="panel">
    <p class="eyebrow">Planning</p>
    <h2>产品规划</h2>
    <div class="list">
      <article>
        <strong>真实门店成员 / RBAC</strong>
        <span>已开始后端化，仍继续收敛权限和页面联动。</span>
      </article>
      <article>
        <strong>采购退货</strong>
        <span>后端与 PC 已可用，移动端继续补齐完整操作流。</span>
      </article>
      <article>
        <strong>经营报表 / AI 助手</strong>
        <span>保留真实接口联调和验收，持续补证据与细节。</span>
      </article>
    </div>
  </section>
</template>

<style scoped>
.list {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

article {
  padding: 16px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.04);
}

strong { display: block; margin-bottom: 6px; font-size: 16px; }
span { color: rgba(255, 255, 255, 0.72); font-size: 13px; line-height: 1.5; }
</style>
