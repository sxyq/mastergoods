<script setup lang="ts">
import { computed } from 'vue'
import { useSession } from '@/app/stores/session'
import type { Permission } from '@/entities/auth/roles'

const session = useSession()

const settingCards = computed(() => [
  {
    title: '店员与权限',
    desc: '维护一个店长（总）和多个员工账号，查看角色矩阵，管理启停与岗位权限。',
    route: '/settings/roles',
    action: '管理员工',
    permission: ['users:manage'],
    points: ['店长（总）全权限', '员工按业务域授权', 'API 模式使用后端门店成员'],
  },
  {
    title: '数据库管理',
    desc: '查看同步健康、旧库导入任务、导入日志和后端数据库能力边界。',
    route: '/settings/database',
    action: '查看数据库',
    permission: ['database:manage'],
    points: ['连接健康检查', '旧库导入任务', '导入失败信息'],
  },
  {
    title: '接口契约目录',
    desc: '按路由查看当前 Web 页面真实依赖的接口、权限和数据表。',
    route: '/planning',
    action: '查看规划',
    permission: ['settings:manage'],
    points: ['Stitch 页面清单', 'API 合同目录', '剩余开发边界'],
  },
] as const)

const visibleCards = computed(() => settingCards.value.filter((item) => session.hasPermission([...item.permission] as Permission[])))
</script>

<template>
  <section class="business-page">
    <section class="screen-hero">
      <div>
        <p class="eyebrow">System Settings</p>
        <h2>系统设置</h2>
        <p>按当前账号权限展示门店成员、数据库和契约规划入口；普通员工不会看到超级管理能力。</p>
      </div>
      <span class="session-source">{{ session.member.value.storeName }} · {{ session.roleLabel.value }}</span>
    </section>

    <section class="overview-grid">
      <article v-for="card in visibleCards" :key="card.route" class="overview-card">
        <div class="overview-card__head">
          <div>
            <p class="eyebrow">{{ card.action }}</p>
            <h3>{{ card.title }}</h3>
          </div>
          <router-link :to="card.route" class="ghost-action">{{ card.action }}</router-link>
        </div>
        <p class="muted">{{ card.desc }}</p>
        <div class="table-tags">
          <span v-for="point in card.points" :key="point">{{ point }}</span>
        </div>
      </article>
    </section>

    <section v-if="visibleCards.length === 0" class="panel empty-preview">
      <strong>当前账号没有系统管理权限</strong>
      <p>系统设置只对店长（总）、店长助理或具备管理权限的账号开放。</p>
    </section>
  </section>
</template>

<style scoped>
.overview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}

.overview-card {
  display: grid;
  gap: 14px;
  padding: 16px;
  border: 1px solid var(--line-soft);
  border-radius: 12px;
  background: #fff;
  box-shadow: var(--shadow);
}

.overview-card__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.overview-card h3 {
  margin: 0;
}
</style>
