<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { pcDesktopScreens } from '@/app/router/stitch-screens'
import { contractsForRoute } from '@/shared/api/contracts'
import {
  fetchCashflowSummary,
  fetchLowStockReport,
  fetchProfitSummary,
  fetchSalesSummary,
  fetchSalesTrend,
  type LowStockReportItem,
  type SalesTrendPoint,
} from '@/shared/api/client'
import { useSession } from '@/app/stores/session'

const session = useSession()
const quickScreens = pcDesktopScreens.filter((item) => item.route !== '/dashboard')
const dashboardContracts = contractsForRoute('/dashboard')
const loading = ref(false)
const loadError = ref('')
const lowStockProducts = ref<LowStockReportItem[]>([])
const trendPoints = ref<SalesTrendPoint[]>([])
const stats = ref([
  { label: '今日销售额', value: '¥24,580', tone: 'blue', trend: '+12.5%' },
  { label: '待收款', value: '¥5,230', tone: 'orange', trend: '3 笔待催收' },
  { label: '低库存预警', value: '12 件', tone: 'red', trend: '需补货' },
  { label: '净现金流', value: '+¥18,350', tone: 'green', trend: '稳定' },
])

watch(
  [() => session.source.value, () => session.token.value],
  async ([source, token]) => {
    if (source !== 'api' || !token) {
      loadError.value = ''
      return
    }
    loading.value = true
    loadError.value = ''
    try {
      const now = Date.now()
      const sevenDaysAgo = now - 7 * 24 * 60 * 60 * 1000
      const [sales, profit, cashflow, lowStock, trend] = await Promise.all([
        fetchSalesSummary(token, sevenDaysAgo, now),
        fetchProfitSummary(token, sevenDaysAgo, now),
        fetchCashflowSummary(token, sevenDaysAgo, now),
        fetchLowStockReport(token, 8),
        fetchSalesTrend(token, sevenDaysAgo, now),
      ])
      stats.value = [
        { label: '近 7 天销售额', value: formatCurrency(sales.totalSalesAmount), tone: 'blue', trend: `${sales.totalOrderCount} 单` },
        { label: '待收款', value: formatCurrency(sales.totalUnpaidAmount), tone: 'orange', trend: formatCurrency(sales.totalPaidAmount) },
        { label: '低库存预警', value: `${lowStock.length} 件`, tone: 'red', trend: '真实库存预警' },
        { label: '净现金流', value: formatCurrency(cashflow.netCashFlow), tone: 'green', trend: formatPercent(profit.estimatedProfitRate) },
      ]
      lowStockProducts.value = lowStock
      trendPoints.value = trend
    } catch (error) {
      loadError.value = error instanceof Error ? error.message : '首页经营数据加载失败'
    } finally {
      loading.value = false
    }
  },
  { immediate: true },
)

const trendBars = computed(() => {
  const points = trendPoints.value
  if (points.length === 0) {
    return Array.from({ length: 7 }, (_, index) => 34 + (index + 1) * 7)
  }
  const max = Math.max(...points.map((item) => item.totalSalesAmount), 1)
  return points.map((item) => 28 + Math.round((item.totalSalesAmount / max) * 96))
})

function formatCurrency(value: number) {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value || 0)
}

function formatPercent(value: number) {
  return `${(value * 100).toFixed(1)}%`
}
</script>

<template>
  <section class="dashboard">
    <div class="metrics-grid">
      <article v-for="stat in stats" :key="stat.label" class="metric-card" :data-tone="stat.tone">
        <span>{{ stat.label }}</span>
        <strong>{{ stat.value }}</strong>
        <p>{{ stat.trend }}</p>
      </article>
    </div>

    <section class="panel two-column">
      <div>
        <h2>近 7 天销售趋势</h2>
        <p>{{ session.source.value === 'api' ? '当前已接入 `/v1/reports/sales-trend`。' : '对齐 Stitch 首页经营总览亮色版，演示模式使用本地趋势。' }}</p>
        <div v-if="loadError" class="form-error">{{ loadError }}</div>
        <div v-if="loading" class="form-success">正在加载真实经营看板...</div>
        <div class="chart-line">
          <span v-for="(height, index) in trendBars" :key="index" :style="{ height: `${height}px` }"></span>
        </div>
      </div>
      <div class="role-summary">
        <h2>低库存关注</h2>
        <p v-if="lowStockProducts.length === 0">当前没有需要立即处理的真实低库存商品，或仍处于演示模式。</p>
        <div class="table-tags">
          <span v-for="item in lowStockProducts" :key="item.productId">{{ item.productName }} / {{ item.stock }}</span>
        </div>
      </div>
    </section>

    <section class="panel dashboard-reference">
      <div class="panel-head">
        <div>
          <p class="eyebrow">{{ session.source.value === 'api' ? 'Real Backend + Stitch' : 'Stitch MCP Desktop' }}</p>
          <h2>PC 设计入口</h2>
        </div>
        <RouterLink to="/planning">产品规划</RouterLink>
      </div>
      <div class="screen-grid">
        <RouterLink v-for="screen in quickScreens" :key="screen.id" :to="screen.route" class="screen-card">
          <span>{{ screen.order }}</span>
          <strong>{{ screen.title }}</strong>
          <small>{{ screen.module }}</small>
        </RouterLink>
      </div>
    </section>

    <section class="panel">
      <p class="eyebrow">接口与数据库</p>
      <h2>首页优先接口</h2>
      <div class="contract-list horizontal">
        <article v-for="contract in dashboardContracts" :key="`${contract.method}-${contract.path}`">
          <span>{{ contract.method }}</span>
          <strong>{{ contract.path }}</strong>
          <p>{{ contract.purpose }}</p>
        </article>
      </div>
    </section>
  </section>
</template>
