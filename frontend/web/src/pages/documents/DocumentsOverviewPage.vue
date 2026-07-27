<script setup lang="ts">
import { computed } from 'vue'
import { useSession } from '@/app/stores/session'
import type { Permission } from '@/entities/auth/roles'

const session = useSession()

const documentModules = [
  {
    title: '销售业务',
    desc: '销售开单、详情、收款与销售退货',
    primaryRoute: '/documents/sales',
    primaryAction: '查看销售单',
    permission: ['sales:view'],
    stats: ['销售列表', '销售收款', '销售退货'],
    links: [
      { label: '销售单列表', route: '/documents/sales', permission: ['sales:view'] },
      { label: '新建销售单', route: '/documents/sales/edit', permission: ['sales:write'] },
      { label: '销售收款', route: '/documents/sales/payment', permission: ['sales:write', 'finance:write'] },
      { label: '销售退货', route: '/documents/sales-returns', permission: ['sales:view'] },
    ],
  },
  {
    title: '采购业务',
    desc: '采购开单、入库、退货和付款单',
    primaryRoute: '/documents/purchases',
    primaryAction: '查看采购单',
    permission: ['purchase:view'],
    stats: ['采购列表', '采购入库', '采购退货'],
    links: [
      { label: '采购单列表', route: '/documents/purchases', permission: ['purchase:view'] },
      { label: '新建采购单', route: '/documents/purchases/edit', permission: ['purchase:write'] },
      { label: '采购入库', route: '/documents/purchase-receipts', permission: ['purchase:write', 'inventory:write'] },
      { label: '采购退货', route: '/documents/purchase-returns', permission: ['purchase:view'] },
      { label: '付款单详情', route: '/documents/pay-orders/detail', permission: ['finance:view'] },
    ],
  },
  {
    title: '库存单据',
    desc: '库存调整、商品流水和库存盘点',
    primaryRoute: '/inventory/product-ledger',
    primaryAction: '查看库存流水',
    permission: ['inventory:view'],
    stats: ['库存流水', '库存调整', '库存盘点'],
    links: [
      { label: '商品库存流水', route: '/inventory/product-ledger', permission: ['inventory:view'] },
      { label: '库存调整', route: '/inventory/adjust', permission: ['inventory:write'] },
      { label: '库存盘点', route: '/inventory/snapshots', permission: ['inventory:view'] },
    ],
  },
  {
    title: '资金单据',
    desc: '资金流水、日常支出和付款状态跟踪',
    primaryRoute: '/finance/records/detail',
    primaryAction: '查看资金流水',
    permission: ['finance:view'],
    stats: ['资金流水', '日常支出', '付款单'],
    links: [
      { label: '资金流水', route: '/finance/records/detail', permission: ['finance:view'] },
      { label: '日常支出', route: '/finance/daily-expense', permission: ['finance:write'] },
      { label: '付款单详情', route: '/documents/pay-orders/detail', permission: ['finance:view'] },
    ],
  },
] as const

const visibleModules = computed(() => documentModules.filter((item) => session.hasPermission(item.permission)))

function canOpen(permission: readonly Permission[]) {
  return session.hasPermission(permission)
}
</script>

<template>
  <section class="business-page">
    <section class="screen-hero">
      <div>
        <p class="eyebrow">Documents Center</p>
        <h2>单据中心</h2>
        <p>按当前门店角色聚合销售、采购、库存和资金单据入口；无权限的业务域不会展示。</p>
      </div>
      <span class="session-source">{{ session.member.value.name }} · {{ session.member.value.storeName }}</span>
    </section>

    <section class="metrics-grid compact">
      <article class="metric-card" data-tone="blue">
        <span>可访问业务域</span>
        <strong>{{ visibleModules.length }}</strong>
        <p>由当前账号权限实时过滤</p>
      </article>
      <article class="metric-card" data-tone="green">
        <span>当前角色</span>
        <strong>{{ session.roleLabel.value }}</strong>
        <p>{{ session.source.value === 'api' ? '后端权限上下文' : '本地演示权限' }}</p>
      </article>
      <article class="metric-card" data-tone="orange">
        <span>写操作</span>
        <strong>{{ session.permissions.value.filter((item) => item.endsWith(':write')).length }}</strong>
        <p>按钮会按权限隐藏或禁用</p>
      </article>
    </section>

    <section class="overview-grid">
      <article v-for="item in visibleModules" :key="item.title" class="overview-card">
        <div class="overview-card__head">
          <div>
            <p class="eyebrow">{{ item.primaryAction }}</p>
            <h3>{{ item.title }}</h3>
          </div>
          <router-link :to="item.primaryRoute" class="ghost-action">{{ item.primaryAction }}</router-link>
        </div>
        <p class="muted">{{ item.desc }}</p>
        <div class="table-tags">
          <span v-for="stat in item.stats" :key="stat">{{ stat }}</span>
        </div>
        <div class="overview-links">
          <template v-for="link in item.links" :key="link.route">
            <router-link v-if="canOpen(link.permission)" :to="link.route">{{ link.label }}</router-link>
            <span v-else class="disabled">{{ link.label }}</span>
          </template>
        </div>
      </article>
    </section>

    <section v-if="visibleModules.length === 0" class="panel empty-preview">
      <strong>当前账号没有可访问单据</strong>
      <p>请联系店长（总）调整角色权限，或切换到具备业务权限的门店成员。</p>
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

.overview-links {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.overview-links a,
.overview-links span {
  padding: 8px 10px;
  border: 1px solid var(--line);
  border-radius: 8px;
  color: var(--text);
  font-size: 12px;
  font-weight: 800;
}

.overview-links span.disabled {
  color: var(--faint);
  background: #f3f5f7;
}
</style>
