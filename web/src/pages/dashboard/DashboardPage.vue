<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  fetchAccounts,
  fetchAgentNotifications,
  fetchAgentWorkbench,
  fetchLowStockReport,
  fetchProducts,
  fetchPurchaseOrders,
  fetchSalesSummary,
  fetchSalesTrend,
  fetchSaleOrders,
  type AgentNotification,
  type AgentWorkbench,
  type LowStockReportItem,
  type ProductRecord,
  type PurchaseOrder,
  type SaleOrder,
  type SalesSummaryReport,
  type SalesTrendPoint,
} from '@/shared/api/client'
import { useSession } from '@/app/stores/session'
import { formatCurrency, formatDate, formatRelativeDate, saleShippingStatus } from '@/shared/utils/business'

type DashboardPeriod = 'today' | 'week' | 'month'

interface DashboardActivity {
  id: string
  kind: 'sale' | 'purchase'
  title: string
  code: string
  amount: number
  partner: string
  createdAt: number
  route: { path: string; query?: Record<string, string> }
}

interface DashboardAlert {
  id: string
  tone: 'danger' | 'warning' | 'info'
  title: string
  description: string
  actionLabel: string
  route: string
}

const router = useRouter()
const session = useSession()

const activePeriod = ref<DashboardPeriod>('today')
const loading = ref(false)
const loadError = ref('')
const summary = ref<SalesSummaryReport | null>(null)
const trendPoints = ref<SalesTrendPoint[]>([])
const lowStockProducts = ref<LowStockReportItem[]>([])
const products = ref<ProductRecord[]>([])
const saleOrders = ref<SaleOrder[]>([])
const purchaseOrders = ref<PurchaseOrder[]>([])
const workbench = ref<AgentWorkbench | null>(null)
const notifications = ref<AgentNotification[]>([])
const isDemoMode = computed(() => session.source.value !== 'api' || !session.token.value)

const demoTrendPoints: SalesTrendPoint[] = [
  { startAt: Date.now() - 6 * 24 * 60 * 60 * 1000, endAt: Date.now() - 5 * 24 * 60 * 60 * 1000, totalSalesAmount: 13200, totalOrderCount: 14 },
  { startAt: Date.now() - 5 * 24 * 60 * 60 * 1000, endAt: Date.now() - 4 * 24 * 60 * 60 * 1000, totalSalesAmount: 16400, totalOrderCount: 16 },
  { startAt: Date.now() - 4 * 24 * 60 * 60 * 1000, endAt: Date.now() - 3 * 24 * 60 * 60 * 1000, totalSalesAmount: 15200, totalOrderCount: 15 },
  { startAt: Date.now() - 3 * 24 * 60 * 60 * 1000, endAt: Date.now() - 2 * 24 * 60 * 60 * 1000, totalSalesAmount: 18600, totalOrderCount: 18 },
  { startAt: Date.now() - 2 * 24 * 60 * 60 * 1000, endAt: Date.now() - 1 * 24 * 60 * 60 * 1000, totalSalesAmount: 22400, totalOrderCount: 22 },
  { startAt: Date.now() - 1 * 24 * 60 * 60 * 1000, endAt: Date.now(), totalSalesAmount: 26100, totalOrderCount: 24 },
  { startAt: Date.now(), endAt: Date.now(), totalSalesAmount: 24800, totalOrderCount: 20 },
]

const demoWorkbench: AgentWorkbench = {
  greeting: '今天也要把账目看得明明白白。',
  kpiCards: [],
  quickQuestions: ['今天销量最高的商品是什么？', '哪些客户快到期了？', '帮我找低库存商品'],
  recentConversations: [],
  pendingDrafts: [],
  riskAlerts: [],
  todaySummary: '当前看板以 Stitch PC 稿布局展示，真实接口连接后会自动切换为实时数据。',
  status: 'ready',
  dataPolicy: 'dashboard',
  capabilities: [],
  warnings: [],
}

watch(
  [() => session.source.value, () => session.token.value, activePeriod],
  async () => {
    if (isDemoMode.value) {
      useDemoData()
      return
    }
    await loadDashboard()
  },
  { immediate: true },
)

const periodRange = computed(() => buildRange(activePeriod.value))
const trendDataset = computed(() => (isDemoMode.value ? demoTrendPoints : trendPoints.value))
const summaryData = computed(() => {
  if (summary.value) return summary.value
  return isDemoMode.value ? buildDemoSummary() : buildEmptySummary()
})
const inventoryStats = computed(() => buildInventoryStats())
const activityItems = computed(() => buildActivities())
const alertItems = computed(() => buildAlerts())
const unreadCount = computed(() => notifications.value.filter((item) => !item.isRead).length)
const pendingShipmentCount = computed(() => {
  const count = saleOrders.value.filter((item) => saleShippingStatus(item.status) === '待出库').length
  return count || (isDemoMode.value ? 28 : 0)
})

const trendGeometry = computed(() => buildTrendGeometry(trendDataset.value))
const trendAxisLabels = computed(() => buildTrendAxisLabels(trendGeometry.value.max))
const trendDayLabels = computed(() => buildTrendDayLabels(trendDataset.value))
const inventoryRingStyle = computed(() => {
  const normalEnd = inventoryStats.value.normalPercent
  const warningEnd = normalEnd + inventoryStats.value.warningPercent
  return `conic-gradient(#18a34a 0 ${normalEnd}%, #c4cad3 ${normalEnd}% ${warningEnd}%, #e64646 ${warningEnd}% 100%)`
})

async function loadDashboard() {
  if (!session.token.value) return
  loading.value = true
  loadError.value = ''
  try {
    const { startAt, endAt } = periodRange.value
    const trendStartAt = activePeriod.value === 'today' ? startAt : Math.max(startAt, endAt - 29 * 24 * 60 * 60 * 1000)

    const [summaryResult, trendResult, lowStockResult, productResult, saleResult, purchaseResult, accountsResult, workbenchResult, notificationResult] = await Promise.allSettled([
      fetchSalesSummary(session.token.value, startAt, endAt),
      fetchSalesTrend(session.token.value, trendStartAt, endAt, activePeriod.value === 'today' ? 'hour6' : 'day'),
      fetchLowStockReport(session.token.value, 30),
      fetchProducts(session.token.value, { page: 0, size: 200 }),
      fetchSaleOrders(session.token.value, { page: 0, size: 80 }),
      fetchPurchaseOrders(session.token.value, { page: 0, size: 80 }),
      fetchAccounts(session.token.value),
      fetchAgentWorkbench(session.token.value),
      fetchAgentNotifications(session.token.value, true),
    ])

    summary.value = summaryResult.status === 'fulfilled' ? summaryResult.value : null
    trendPoints.value = trendResult.status === 'fulfilled' ? trendResult.value : []
    lowStockProducts.value = lowStockResult.status === 'fulfilled' ? lowStockResult.value : []
    products.value = productResult.status === 'fulfilled' ? productResult.value : []
    saleOrders.value = saleResult.status === 'fulfilled' ? saleResult.value : []
    purchaseOrders.value = purchaseResult.status === 'fulfilled' ? purchaseResult.value : []
    workbench.value = workbenchResult.status === 'fulfilled' ? workbenchResult.value : null
    notifications.value = notificationResult.status === 'fulfilled' ? notificationResult.value : []

    const errors = [
      summaryResult,
      trendResult,
      lowStockResult,
      productResult,
      saleResult,
      purchaseResult,
      accountsResult,
      workbenchResult,
      notificationResult,
    ].filter((item) => item.status === 'rejected')

    if (errors.length > 0) {
      loadError.value = '部分实时数据未返回，页面已切换为可用状态。'
    }
    if (accountsResult.status === 'fulfilled') {
      // 让计算属性读取到最新余额
      accountBalance.value = accountsResult.value.reduce((sum, item) => sum + item.balance, 0)
    } else {
      accountBalance.value = 0
    }
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '经营看板加载失败'
    summary.value = null
    trendPoints.value = []
    lowStockProducts.value = []
    products.value = []
    saleOrders.value = []
    purchaseOrders.value = []
    workbench.value = null
    notifications.value = []
    accountBalance.value = 0
  } finally {
    loading.value = false
  }
}

const accountBalance = ref(0)

function useDemoData() {
  summary.value = {
    startAt: periodRange.value.startAt,
    endAt: periodRange.value.endAt,
    totalSalesAmount: 45280,
    totalPaidAmount: 39840,
    totalRefundAmount: 1200,
    totalUnpaidAmount: 5440,
    totalOrderCount: 28,
  }
  trendPoints.value = demoTrendPoints
  lowStockProducts.value = [
    { productId: 1, productCode: 'P-1001', productName: 'ThinkPad T14 笔记本', stock: 2, safeStock: 8 },
    { productId: 2, productCode: 'P-2008', productName: '罗技无线鼠标', stock: 4, safeStock: 12 },
    { productId: 3, productCode: 'P-3002', productName: 'A4 打印纸', stock: 0, safeStock: 30 },
  ]
  products.value = []
  saleOrders.value = []
  purchaseOrders.value = []
  workbench.value = demoWorkbench
  notifications.value = [
    { id: 1, taskId: null, title: '库存不足提醒', body: 'ThinkPad T14 笔记本库存已经低于预警线。', level: 'warning', isRead: false, isDelivered: true, createdAt: Date.now() - 12 * 60 * 60 * 1000 },
    { id: 2, taskId: null, title: '应收到期提醒', body: '星锐网络科技有限公司有一笔账款即将到期。', level: 'info', isRead: false, isDelivered: true, createdAt: Date.now() - 8 * 60 * 60 * 1000 },
  ]
  accountBalance.value = 342105.5
}

function buildRange(period: DashboardPeriod) {
  const now = new Date()
  const endAt = now.getTime()
  const start = new Date(now)
  if (period === 'today') {
    start.setHours(0, 0, 0, 0)
  } else if (period === 'week') {
    const day = start.getDay() || 7
    start.setDate(start.getDate() - day + 1)
    start.setHours(0, 0, 0, 0)
  } else {
    start.setDate(1)
    start.setHours(0, 0, 0, 0)
  }
  return { startAt: start.getTime(), endAt }
}

function buildDemoSummary() {
  return {
    startAt: periodRange.value.startAt,
    endAt: periodRange.value.endAt,
    totalSalesAmount: 45280,
    totalPaidAmount: 39840,
    totalRefundAmount: 1200,
    totalUnpaidAmount: 5440,
    totalOrderCount: 28,
  }
}

function buildEmptySummary() {
  return {
    startAt: periodRange.value.startAt,
    endAt: periodRange.value.endAt,
    totalSalesAmount: 0,
    totalPaidAmount: 0,
    totalRefundAmount: 0,
    totalUnpaidAmount: 0,
    totalOrderCount: 0,
  }
}

function buildInventoryStats() {
  if (products.value.length === 0) {
    if (isDemoMode.value) {
      return {
        health: 65,
        normal: 65,
        warning: 25,
        shortage: 10,
        total: 100,
        normalPercent: 65,
        warningPercent: 25,
        shortagePercent: 10,
      }
    }
    return {
      health: 0,
      normal: 0,
      warning: 0,
      shortage: 0,
      total: 0,
      normalPercent: 0,
      warningPercent: 0,
      shortagePercent: 0,
    }
  }
  const source = products.value
  const total = source.length
  let normal = 0
  let warning = 0
  let shortage = 0
  for (const item of source) {
    if (item.stock <= 0) {
      shortage += 1
    } else if (item.stock < item.safeStock) {
      warning += 1
    } else {
      normal += 1
    }
  }
  const safe = Math.max(total, 1)
  const health = Math.max(0, Math.min(100, Math.round((normal / safe) * 100)))
  return {
    health,
    normal,
    warning,
    shortage,
    total,
    normalPercent: Math.round((normal / safe) * 100),
    warningPercent: Math.round((warning / safe) * 100),
    shortagePercent: Math.max(0, 100 - Math.round((normal / safe) * 100) - Math.round((warning / safe) * 100)),
  }
}

function buildActivities() {
  const sales = saleOrders.value.slice(0, 6).map((order) => ({
    id: `sale-${String(order.id)}`,
    kind: 'sale' as const,
    title: `销售单 ${order.orderNo}`,
    code: order.orderNo,
    amount: order.totalAmount,
    partner: `客户：${order.customerName || '散客'}`,
    createdAt: order.createdAt,
    route: { path: '/documents/sales/detail', query: { id: String(order.id) } },
  }))
  const purchases = purchaseOrders.value.slice(0, 6).map((order) => ({
    id: `purchase-${String(order.id)}`,
    kind: 'purchase' as const,
    title: `采购单 ${order.orderNo}`,
    code: order.orderNo,
    amount: order.totalAmount,
    partner: `供应商：${order.supplierName || '未命名供应商'}`,
    createdAt: order.createdAt,
    route: { path: '/documents/purchases/detail', query: { id: String(order.id) } },
  }))
  const items = [...sales, ...purchases]
  if (items.length === 0) {
    if (!isDemoMode.value) {
      return []
    }
    return [
      {
        id: 'demo-sale',
        kind: 'sale' as const,
        title: '销售单 XS-20240315-001',
        code: 'XS-20240315-001',
        amount: 3250,
        partner: '客户：华南科技贸易公司',
        createdAt: Date.now() - 5 * 60 * 1000,
        route: { path: '/documents/sales/detail', query: { id: '1' } },
      },
      {
        id: 'demo-purchase',
        kind: 'purchase' as const,
        title: '采购入库 RK-20240315-012',
        code: 'RK-20240315-012',
        amount: 12800,
        partner: '供应商：深圳电子元器件一厂',
        createdAt: Date.now() - 15 * 60 * 1000,
        route: { path: '/documents/purchases/detail', query: { id: '2' } },
      },
    ]
  }
  return items.sort((left, right) => right.createdAt - left.createdAt).slice(0, 4)
}

function buildAlerts() {
  const alerts: DashboardAlert[] = []
  const warningItems = lowStockProducts.value.slice(0, 2)
  warningItems.forEach((item) => {
    alerts.push({
      id: `stock-${item.productId}`,
      tone: 'danger',
      title: '库存不足提醒',
      description: `"${item.productName}" 当前库存仅剩 ${item.stock} 件，低于预警线。`,
      actionLabel: '立即发起采购单',
      route: '/documents/purchases/edit',
    })
  })

  const overdueOrder = summary.value?.totalUnpaidAmount ?? 0
  if (overdueOrder > 0) {
    alerts.push({
      id: 'receivable',
      tone: 'warning',
      title: '应收账款到期预警',
      description: `当前还有约 ${formatCurrency(overdueOrder)} 的应收未结清。`,
      actionLabel: '前往收款',
      route: '/documents/sales/payment',
    })
  }

  const advice = workbench.value?.quickQuestions[0] || '根据近期销售趋势，可继续关注畅销商品补货节奏。'
  if (workbench.value?.quickQuestions?.length || isDemoMode.value) {
    alerts.push({
      id: 'suggestion',
      tone: 'info',
      title: '智能优化建议',
      description: advice,
      actionLabel: '打开 AI 助手',
      route: '/agent',
    })
  }

  if (alerts.length === 0) {
    if (!isDemoMode.value) {
      return []
    }
    return [
      {
        id: 'demo-stock',
        tone: 'danger',
        title: '库存不足提醒',
        description: '"ThinkPad T14 笔记本" 当前库存仅剩 2 台，低于预警线。',
        actionLabel: '立即发起采购单',
        route: '/documents/purchases/edit',
      },
    ]
  }

  return alerts.slice(0, 3)
}

function buildTrendGeometry(points: SalesTrendPoint[]) {
  const safePoints = points.length > 0
    ? points
    : (isDemoMode.value ? demoTrendPoints : [{ startAt: periodRange.value.startAt, endAt: periodRange.value.endAt, totalSalesAmount: 0, totalOrderCount: 0 }])
  const coords = safePoints.map((item, index) => {
    const x = safePoints.length === 1 ? 50 : (index / (safePoints.length - 1)) * 100
    return {
      x,
      y: item.totalSalesAmount,
    }
  })
  let max = 1
  for (const point of coords) {
    if (point.y > max) {
      max = point.y
    }
  }
  const normalizedCoords = coords.map((point) => ({
    x: point.x,
    y: 92 - (point.y / max) * 72,
  }))
  const line = normalizedCoords.map((point) => `${point.x},${point.y}`).join(' ')
  const area = `M ${normalizedCoords[0].x},100 ${normalizedCoords.map((point) => `L ${point.x},${point.y}`).join(' ')} L ${normalizedCoords[normalizedCoords.length - 1].x},100 Z`
  return { max, coords: normalizedCoords, line, area }
}

function buildTrendAxisLabels(max: number) {
  const safeMax = Math.max(max, 1)
  return [1, 0.8, 0.6, 0.4, 0.2, 0].map((ratio) => compactCurrency(safeMax * ratio))
}

function buildTrendDayLabels(points: SalesTrendPoint[]) {
  const safePoints = points.length > 0 ? points : (isDemoMode.value ? demoTrendPoints : [])
  if (safePoints.length === 0) {
    return []
  }
  const indexes = Array.from(new Set([0, Math.floor((safePoints.length - 1) / 3), Math.floor(((safePoints.length - 1) * 2) / 3), safePoints.length - 1]))
  return indexes.map((index) => ({
    key: `${safePoints[index].startAt}-${index}`,
    label: formatDate(safePoints[index].startAt).slice(5),
  }))
}

function compactCurrency(value: number) {
  if (value >= 1000000) return `${(value / 1000000).toFixed(1)}M`
  if (value >= 10000) return `${(value / 10000).toFixed(value >= 100000 ? 0 : 1)}万`
  if (value >= 1000) return `${Math.round(value / 1000)}k`
  return `${Math.round(value)}`
}

function formatTrendValue(point: SalesTrendPoint) {
  return `${formatCurrency(point.totalSalesAmount)} / ${point.totalOrderCount} 单`
}

function openAction(route: string) {
  router.push(route)
}

function openActivity(item: DashboardActivity) {
  router.push({ path: item.route.path, query: item.route.query })
}

function openQuickCreate() {
  router.push('/documents/sales/edit')
}
</script>

<template>
  <section class="dashboard-page">
    <header class="dashboard-head">
      <div class="dashboard-head__title">
        <h2>经营首页 Dashboard</h2>
        <nav class="dashboard-tabs">
          <button type="button" :class="{ active: activePeriod === 'today' }" @click="activePeriod = 'today'">今日</button>
          <button type="button" :class="{ active: activePeriod === 'week' }" @click="activePeriod = 'week'">本周</button>
          <button type="button" :class="{ active: activePeriod === 'month' }" @click="activePeriod = 'month'">本月</button>
        </nav>
      </div>
      <div class="dashboard-head__actions">
        <button type="button" class="chip-button">
          <span class="material-symbols-outlined">calendar_today</span>
          <span>{{ formatDate(periodRange.startAt).replace(/\//g, '-') }}</span>
        </button>
        <button type="button" class="chip-button" :disabled="loading" @click="loadDashboard">
          <span class="material-symbols-outlined">refresh</span>
          <span>刷新</span>
        </button>
        <button type="button" class="icon-button" @click="openAction('/agent')">
          <span class="material-symbols-outlined">notifications</span>
          <i v-if="unreadCount > 0"></i>
        </button>
        <button type="button" class="primary-button" :disabled="!session.hasPermission(['sales:write'])" @click="openQuickCreate">
          <span class="material-symbols-outlined">add</span>
          <span>新建单据</span>
        </button>
      </div>
    </header>

    <p v-if="loadError" class="dashboard-banner">{{ loadError }}</p>
    <p v-if="loading" class="dashboard-banner dashboard-banner--soft">正在加载真实经营看板...</p>

    <section class="dashboard-metrics">
      <article class="dashboard-metric">
        <div class="dashboard-metric__head">
          <span>今日销售额</span>
          <b class="trend-chip trend-chip--green">+12.5%</b>
        </div>
        <strong>{{ formatCurrency(summaryData.totalSalesAmount) }}</strong>
        <p>对应当前筛选周期内的真实销售汇总</p>
      </article>
      <article class="dashboard-metric">
        <div class="dashboard-metric__head">
          <span>待发货订单</span>
          <b class="trend-chip trend-chip--gray">{{ pendingShipmentCount }} 笔</b>
        </div>
        <strong>{{ pendingShipmentCount }}</strong>
        <p>{{ summaryData.totalOrderCount }} 单销售记录中待处理的出库单</p>
      </article>
      <article class="dashboard-metric">
        <div class="dashboard-metric__head">
          <span>库存预警数</span>
          <b class="trend-chip trend-chip--red">{{ lowStockProducts.length || inventoryStats.warning }} 件商品</b>
        </div>
        <strong>{{ lowStockProducts.length || inventoryStats.warning }}</strong>
        <p>低于安全库存的商品正在等待补货</p>
      </article>
      <article class="dashboard-metric">
        <div class="dashboard-metric__head">
          <span>账户总余额</span>
          <b class="trend-chip trend-chip--green">稳定</b>
        </div>
        <strong>{{ formatCurrency(accountBalance) }}</strong>
        <p>当前门店可用资金与账户余额合计</p>
      </article>
    </section>

    <section class="dashboard-grid dashboard-grid--top">
      <article class="dashboard-panel dashboard-trend">
        <div class="panel-head panel-head--tight">
          <div>
            <h3>销售趋势分析</h3>
            <p>近 30 天日销售额变化</p>
          </div>
          <span class="pulse-badge"><i></i> 稳步增长中</span>
        </div>
        <div class="trend-wrap">
          <div class="trend-axis trend-axis--y">
            <span v-for="label in trendAxisLabels" :key="label">{{ label }}</span>
          </div>
          <div class="trend-grid">
            <span v-for="n in 6" :key="n" />
          </div>
          <svg class="trend-svg" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">
            <defs>
              <linearGradient id="dashboard-trend-fill" x1="0" x2="0" y1="0" y2="1">
                <stop offset="0%" stop-color="#9ca3af" stop-opacity="0.35" />
                <stop offset="100%" stop-color="#9ca3af" stop-opacity="0.03" />
              </linearGradient>
            </defs>
            <path :d="trendGeometry.area" fill="url(#dashboard-trend-fill)" />
            <polyline :points="trendGeometry.line" fill="none" stroke="#a1a1aa" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <div class="trend-axis trend-axis--x">
            <span v-for="item in trendDayLabels" :key="item.key">{{ item.label }}</span>
          </div>
        </div>
        <div class="trend-footer">
          <span v-for="point in trendDataset.slice(-4)" :key="`${point.startAt}-${point.endAt}`">{{ formatRelativeDate(point.startAt) }} · {{ formatTrendValue(point) }}</span>
        </div>
      </article>

      <article class="dashboard-panel dashboard-inventory">
        <div class="panel-head panel-head--tight">
          <div>
            <h3>库存状态概览</h3>
            <p>当前库存健康度分析</p>
          </div>
        </div>
        <div class="inventory-ring" :style="{ background: inventoryRingStyle }">
          <div>
            <strong>{{ inventoryStats.health }}%</strong>
            <span>健康度</span>
          </div>
        </div>
        <div class="inventory-legend">
          <div>
            <span class="dot dot--green" />
            <span>正常库存</span>
            <b>{{ inventoryStats.normalPercent }}%</b>
          </div>
          <div>
            <span class="dot dot--gray" />
            <span>低库存预警</span>
            <b>{{ inventoryStats.warningPercent }}%</b>
          </div>
          <div>
            <span class="dot dot--red" />
            <span>缺货告警</span>
            <b>{{ inventoryStats.shortagePercent }}%</b>
          </div>
        </div>
      </article>
    </section>

    <section class="dashboard-grid dashboard-grid--bottom">
      <article class="dashboard-panel dashboard-activity">
        <div class="panel-head panel-head--tight">
          <h3>实时业务动态</h3>
          <button type="button" class="text-button" @click="openAction('/documents/sales')">查看全部</button>
        </div>
        <div class="activity-list">
          <button
            v-for="item in activityItems"
            :key="item.id"
            type="button"
            class="activity-row"
            @click="openActivity(item)"
          >
            <span class="activity-icon" :data-kind="item.kind">
              <span class="material-symbols-outlined">{{ item.kind === 'sale' ? 'receipt_long' : 'inventory_2' }}</span>
            </span>
            <span class="activity-main">
              <span class="activity-title">
                {{ item.title }}
                <small>{{ item.code }}</small>
              </span>
              <span class="activity-sub">{{ item.partner }}</span>
            </span>
            <span class="activity-side">
              <b>{{ formatCurrency(item.amount) }}</b>
              <small>{{ formatRelativeDate(item.createdAt) }}</small>
            </span>
          </button>
        </div>
      </article>

      <article class="dashboard-panel dashboard-agent">
        <div class="panel-head panel-head--tight dashboard-agent__head">
          <span class="material-symbols-outlined">smart_toy</span>
          <h3>RunTrace 智能助手</h3>
        </div>
        <div class="alert-list">
          <article v-for="item in alertItems" :key="item.id" class="alert-card" :data-tone="item.tone">
            <div class="alert-card__body">
              <h4>{{ item.title }}</h4>
              <p>{{ item.description }}</p>
            </div>
            <button type="button" class="alert-card__action" @click="openAction(item.route)">{{ item.actionLabel }}</button>
          </article>
        </div>
        <div class="dashboard-agent__summary">
          <strong>{{ workbench?.greeting || (isDemoMode ? demoWorkbench.greeting : '智慧记 AI 助手') }}</strong>
          <p>{{ workbench?.todaySummary || (isDemoMode ? demoWorkbench.todaySummary : '当前按真实接口返回状态展示；没有数据时不会回退本地演示摘要。') }}</p>
        </div>
      </article>
    </section>
  </section>
</template>

<style scoped>
.dashboard-page {
  display: grid;
  gap: 16px;
}

.dashboard-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
}

.dashboard-head__title {
  display: grid;
  gap: 10px;
}

.dashboard-head h2 {
  margin: 0;
  font-size: 24px;
  line-height: 1.2;
}

.dashboard-tabs {
  display: flex;
  gap: 8px;
  border-bottom: 1px solid var(--line-soft);
  width: fit-content;
}

.dashboard-tabs button {
  border: 0;
  border-bottom: 2px solid transparent;
  border-radius: 0;
  background: transparent;
  color: var(--muted);
  box-shadow: none;
  padding: 10px 16px;
  font-weight: 700;
}

.dashboard-tabs button.active {
  color: var(--text);
  border-bottom-color: #111317;
}

.dashboard-head__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chip-button,
.primary-button,
.icon-button,
.text-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 10px;
  border: 1px solid var(--line);
  background: #fff;
  color: var(--text);
  box-shadow: none;
}

.chip-button,
.text-button {
  padding: 9px 12px;
}

.icon-button {
  position: relative;
  width: 40px;
  height: 40px;
  justify-content: center;
  padding: 0;
}

.icon-button i {
  position: absolute;
  top: 9px;
  right: 9px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--red);
}

.primary-button {
  padding: 9px 14px;
  background: #e93535;
  border-color: #e93535;
  color: #fff;
}

.dashboard-banner {
  margin: 0;
  padding: 10px 14px;
  border-radius: 10px;
  border: 1px solid #f2d6b1;
  background: #fff8ef;
  color: #8b5e23;
}

.dashboard-banner--soft {
  border-color: var(--line);
  background: #fafbfc;
  color: var(--muted);
}

.dashboard-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.dashboard-metric,
.dashboard-panel {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  box-shadow: none;
}

.dashboard-metric {
  min-height: 120px;
  padding: 18px;
  display: grid;
  gap: 10px;
}

.dashboard-metric__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.dashboard-metric span,
.dashboard-metric p,
.panel-head p,
.activity-sub,
.activity-side small,
.trend-footer span,
.inventory-legend span,
.alert-card p,
.dashboard-agent__summary p {
  color: var(--muted);
}

.dashboard-metric strong {
  font-size: 28px;
  line-height: 1;
  color: var(--text);
}

.trend-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 58px;
  padding: 4px 8px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 800;
}

.trend-chip--green {
  background: rgba(18, 172, 91, 0.12);
  color: #0f8e47;
}

.trend-chip--gray {
  background: #f2f4f6;
  color: #61666f;
}

.trend-chip--red {
  background: rgba(233, 53, 53, 0.12);
  color: #c92d2d;
}

.dashboard-grid--top {
  display: grid;
  grid-template-columns: minmax(0, 1.95fr) minmax(320px, 0.85fr);
  gap: 16px;
}

.dashboard-grid--bottom {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(360px, 0.8fr);
  gap: 16px;
}

.dashboard-panel {
  padding: 18px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.panel-head--tight {
  margin-bottom: 14px;
}

.panel-head h3,
.dashboard-agent__head h3 {
  margin: 0;
  font-size: 20px;
  line-height: 1.2;
}

.panel-head p {
  margin: 4px 0 0;
}

.pulse-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(26, 173, 70, 0.12);
  color: #128a45;
  font-size: 12px;
  font-weight: 700;
}

.pulse-badge i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #1aad46;
}

.trend-wrap {
  position: relative;
  height: 302px;
  margin-top: 8px;
  padding: 20px 20px 26px 30px;
  border: 1px solid #eef0f2;
  border-radius: 12px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcfd 100%);
}

.trend-axis--y {
  position: absolute;
  top: 20px;
  left: 6px;
  bottom: 34px;
  display: grid;
  align-content: space-between;
  color: #9aa0a6;
  font-size: 11px;
}

.trend-grid {
  position: absolute;
  inset: 20px 20px 34px 30px;
  display: grid;
  grid-template-rows: repeat(6, 1fr);
  pointer-events: none;
}

.trend-grid span {
  border-bottom: 1px dashed rgba(148, 163, 184, 0.18);
}

.trend-svg {
  position: absolute;
  inset: 20px 20px 34px 30px;
  width: calc(100% - 50px);
  height: calc(100% - 54px);
  overflow: visible;
}

.trend-axis--x {
  position: absolute;
  left: 30px;
  right: 20px;
  bottom: 10px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  color: #9aa0a6;
  font-size: 11px;
}

.trend-footer {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  margin-top: 14px;
}

.trend-footer span {
  font-size: 12px;
}

.dashboard-inventory {
  display: grid;
  align-content: start;
  gap: 16px;
}

.inventory-ring {
  width: 160px;
  height: 160px;
  margin: 16px auto 4px;
  padding: 16px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  position: relative;
}

.inventory-ring::after {
  content: '';
  position: absolute;
  inset: 16px;
  background: #fff;
  border-radius: 50%;
}

.inventory-ring > div {
  position: relative;
  z-index: 1;
  display: grid;
  place-items: center;
}

.inventory-ring strong {
  font-size: 24px;
  line-height: 1;
}

.inventory-ring span {
  margin-top: 4px;
  color: var(--muted);
  font-size: 12px;
}

.inventory-legend {
  display: grid;
  gap: 12px;
}

.inventory-legend > div {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: space-between;
  color: var(--text);
  font-size: 14px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex: 0 0 auto;
}

.dot--green {
  background: #18a34a;
}

.dot--gray {
  background: #c4cad3;
}

.dot--red {
  background: #e64646;
}

.activity-list {
  display: grid;
}

.activity-row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  padding: 14px 0;
  border: 0;
  border-bottom: 1px solid #eef0f2;
  background: transparent;
  box-shadow: none;
  text-align: left;
}

.activity-row:last-child {
  border-bottom: 0;
}

.activity-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: grid;
  place-items: center;
}

.activity-icon[data-kind='sale'] {
  background: rgba(26, 173, 70, 0.12);
  color: #128a45;
}

.activity-icon[data-kind='purchase'] {
  background: #eef0f2;
  color: #69707d;
}

.activity-main {
  display: grid;
  gap: 4px;
}

.activity-title {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: baseline;
  font-weight: 700;
  color: var(--text);
}

.activity-title small {
  font-weight: 400;
  color: var(--muted);
}

.activity-side {
  display: grid;
  justify-items: end;
  gap: 4px;
}

.activity-side b {
  font-size: 14px;
}

.dashboard-agent {
  display: grid;
  gap: 14px;
}

.dashboard-agent__head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dashboard-agent__head .material-symbols-outlined {
  color: #111317;
}

.alert-list {
  display: grid;
  gap: 12px;
}

.alert-card {
  display: grid;
  gap: 12px;
  padding: 14px;
  border-radius: 12px;
  border: 1px solid var(--line);
  background: #fafbfc;
}

.alert-card[data-tone='danger'] {
  background: rgba(232, 70, 70, 0.06);
  border-color: rgba(232, 70, 70, 0.16);
}

.alert-card[data-tone='warning'] {
  background: rgba(255, 191, 71, 0.08);
  border-color: rgba(255, 191, 71, 0.18);
}

.alert-card[data-tone='info'] {
  background: rgba(26, 173, 70, 0.06);
  border-color: rgba(26, 173, 70, 0.14);
}

.alert-card h4 {
  margin: 0 0 6px;
  font-size: 14px;
}

.alert-card p {
  margin: 0;
  line-height: 1.6;
  font-size: 13px;
}

.alert-card__action {
  width: fit-content;
  border: 0;
  background: transparent;
  color: #111317;
  padding: 0;
  font-weight: 700;
  box-shadow: none;
}

.dashboard-agent__summary {
  padding: 14px;
  border-radius: 12px;
  border: 1px solid var(--line);
  background: #fff;
}

.dashboard-agent__summary strong {
  display: block;
  margin-bottom: 6px;
  color: var(--text);
}

@media (max-width: 1180px) {
  .dashboard-metrics,
  .dashboard-grid--top,
  .dashboard-grid--bottom {
    grid-template-columns: 1fr;
  }

  .dashboard-head {
    flex-direction: column;
  }
}
</style>
