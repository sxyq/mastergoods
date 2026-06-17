<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useSession } from '@/app/stores/session'
import {
  fetchCashflowSummary,
  fetchCustomerSalesReport,
  fetchInventoryFlowReport,
  fetchLowStockReport,
  fetchProfitByCustomers,
  fetchProfitByProducts,
  fetchProfitSummary,
  fetchReconciliationSummary,
  fetchRefundRecords,
  fetchSalesSummary,
  fetchSalesTrend,
  fetchStockOutRecords,
  fetchTopProducts,
  fetchTopReceivableCustomers,
  type CashflowSummaryReport,
  type CustomerReceivableReportItem,
  type CustomerSalesReportItem,
  type InventoryFlowReportItem,
  type LowStockReportItem,
  type ProfitByCustomerReportItem,
  type ProfitByProductReportItem,
  type ProfitSummaryReport,
  type ReconciliationSummaryReport,
  type RefundRecordReportItem,
  type SalesSummaryReport,
  type SalesTrendPoint,
  type StockOutRecordReportItem,
  type TopProductReportItem,
} from '@/shared/api/client'
import { contractsForRoute } from '@/shared/api/contracts'
import {
  financeMethodLabel,
  formatCurrency,
  formatDate,
  formatDateTime,
  inventorySourceLabel,
  reportRangeForPeriod,
  riskLevelLabel,
  salesTrendBucket,
  type ReportPeriodKey,
} from '@/shared/utils/business'
import PageEmptyState from '@/shared/ui/PageEmptyState.vue'
import PageStatusBanner from '@/shared/ui/PageStatusBanner.vue'

const session = useSession()
const loading = ref(false)
const error = ref('')
const period = ref<ReportPeriodKey>('today')
const customStart = ref(toDateInput(new Date()))
const customEnd = ref(toDateInput(new Date()))
const sectionErrors = reactive<Record<string, string>>({})

const salesSummary = ref<SalesSummaryReport | null>(null)
const profitSummary = ref<ProfitSummaryReport | null>(null)
const cashflowSummary = ref<CashflowSummaryReport | null>(null)
const reconciliationSummary = ref<ReconciliationSummaryReport | null>(null)
const trendPoints = ref<SalesTrendPoint[]>([])
const topProducts = ref<TopProductReportItem[]>([])
const profitProducts = ref<ProfitByProductReportItem[]>([])
const profitCustomers = ref<ProfitByCustomerReportItem[]>([])
const customerSales = ref<CustomerSalesReportItem[]>([])
const receivableCustomers = ref<CustomerReceivableReportItem[]>([])
const refundRecords = ref<RefundRecordReportItem[]>([])
const stockOutRecords = ref<StockOutRecordReportItem[]>([])
const inventoryFlow = ref<InventoryFlowReportItem[]>([])
const lowStock = ref<LowStockReportItem[]>([])

const isApiSource = computed(() => session.source.value === 'api' && Boolean(session.token.value))
const reportContracts = contractsForRoute('/reports')
const currentRange = computed(() => {
  const custom = {
    startAt: new Date(`${customStart.value}T00:00:00`).getTime(),
    endAt: new Date(`${customEnd.value}T23:59:59`).getTime(),
  }
  return reportRangeForPeriod(period.value, custom)
})
const trendMax = computed(() => Math.max(...trendPoints.value.map((item) => item.totalSalesAmount), 1))
const trendPath = computed(() => {
  if (trendPoints.value.length === 0) return ''
  const width = 620
  const height = 180
  return trendPoints.value
    .map((item, index) => {
      const x = trendPoints.value.length === 1 ? width / 2 : (index / (trendPoints.value.length - 1)) * width
      const y = height - (item.totalSalesAmount / trendMax.value) * 140 - 20
      return `${index === 0 ? 'M' : 'L'} ${x.toFixed(2)} ${y.toFixed(2)}`
    })
    .join(' ')
})
const trendBars = computed(() => {
  const width = 620
  const gap = 10
  const count = Math.max(trendPoints.value.length, 1)
  const barWidth = Math.max((width - gap * (count - 1)) / count, 24)
  return trendPoints.value.map((item, index) => ({
    x: index * (barWidth + gap),
    width: barWidth,
    height: Math.max((item.totalOrderCount / Math.max(...trendPoints.value.map((point) => point.totalOrderCount), 1)) * 72, 10),
  }))
})

watch(
  [() => session.source.value, () => session.token.value, period, customStart, customEnd],
  async () => {
    if (!isApiSource.value || !session.token.value) {
      error.value = ''
      return
    }
    await loadReports()
  },
  { immediate: true },
)

async function loadReports() {
  if (!session.token.value) return
  loading.value = true
  error.value = ''
  Object.keys(sectionErrors).forEach((key) => delete sectionErrors[key])
  const range = currentRange.value

  const jobs = await Promise.allSettled([
    fetchSalesSummary(session.token.value, range.startAt, range.endAt),
    fetchProfitSummary(session.token.value, range.startAt, range.endAt),
    fetchCashflowSummary(session.token.value, range.startAt, range.endAt),
    fetchReconciliationSummary(session.token.value, range.startAt, range.endAt),
    fetchSalesTrend(session.token.value, range.startAt, range.endAt, salesTrendBucket(period.value)),
    fetchTopProducts(session.token.value, range.startAt, range.endAt, 8),
    fetchProfitByProducts(session.token.value, range.startAt, range.endAt, 8),
    fetchProfitByCustomers(session.token.value, range.startAt, range.endAt, 8),
    fetchCustomerSalesReport(session.token.value, range.startAt, range.endAt, 8),
    fetchTopReceivableCustomers(session.token.value, 8),
    fetchRefundRecords(session.token.value, range.startAt, range.endAt, 8),
    fetchStockOutRecords(session.token.value, range.startAt, range.endAt, 8),
    fetchInventoryFlowReport(session.token.value, range.startAt, range.endAt, 8),
    fetchLowStockReport(session.token.value, 8),
  ])

  assignResult(jobs[0], salesSummary, 'summarySales')
  assignResult(jobs[1], profitSummary, 'summaryProfit')
  assignResult(jobs[2], cashflowSummary, 'summaryCashflow')
  assignResult(jobs[3], reconciliationSummary, 'summaryReconciliation')
  assignResult(jobs[4], trendPoints, 'trend')
  assignResult(jobs[5], topProducts, 'topProducts')
  assignResult(jobs[6], profitProducts, 'profitProducts')
  assignResult(jobs[7], profitCustomers, 'profitCustomers')
  assignResult(jobs[8], customerSales, 'customerSales')
  assignResult(jobs[9], receivableCustomers, 'receivableCustomers')
  assignResult(jobs[10], refundRecords, 'refundRecords')
  assignResult(jobs[11], stockOutRecords, 'stockOutRecords')
  assignResult(jobs[12], inventoryFlow, 'inventoryFlow')
  assignResult(jobs[13], lowStock, 'lowStock')

  const failed = Object.values(sectionErrors)
  if (failed.length > 0) {
    error.value = `部分报表区块加载失败：${failed[0]}`
  }
  loading.value = false
}

function assignResult<T>(result: PromiseSettledResult<T>, target: { value: T }, key: string) {
  if (result.status === 'fulfilled') {
    target.value = result.value
    return
  }
  sectionErrors[key] = result.reason instanceof Error ? result.reason.message : '加载失败'
}

function setPeriod(next: ReportPeriodKey) {
  period.value = next
}

async function refreshReports() {
  await loadReports()
}

function exportCsv() {
  const sections = [
    ['经营总览', [
      ['销售额', salesSummary.value?.totalSalesAmount ?? 0],
      ['已收金额', salesSummary.value?.totalPaidAmount ?? 0],
      ['退款金额', salesSummary.value?.totalRefundAmount ?? 0],
      ['利润额', profitSummary.value?.estimatedProfitAmount ?? 0],
      ['净现金流', cashflowSummary.value?.netCashFlow ?? 0],
      ['总应收', reconciliationSummary.value?.totalReceivableAmount ?? 0],
      ['总应付', reconciliationSummary.value?.totalPayableAmount ?? 0],
    ]],
    ['热销商品', topProducts.value.map((item) => [item.productName, item.totalQuantity, item.totalAmount])],
    ['商品利润', profitProducts.value.map((item) => [item.productName, item.totalProfitAmount, item.profitRate])],
    ['客户利润', profitCustomers.value.map((item) => [item.customerName || '散客', item.totalProfitAmount, item.profitRate])],
    ['退款记录', refundRecords.value.map((item) => [item.orderNo, item.customerName, item.refundAmount, financeMethodLabel(item.method)])],
    ['低库存', lowStock.value.map((item) => [item.productName, item.stock, item.safeStock])],
  ]

  const csv = sections
    .map(([title, rows]) => {
      const sectionRows = rows as Array<Array<string | number>>
      return [title as string, ...sectionRows.map((row) => row.map(escapeCsv).join(','))].join('\n')
    })
    .join('\n\n')
  const blob = new Blob([`\ufeff${csv}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `zhihuiji-report-${Date.now()}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

function printPage() {
  window.print()
}

function escapeCsv(value: string | number) {
  const text = String(value ?? '')
  return /[",\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text
}

function toDateInput(date: Date) {
  return [
    date.getFullYear(),
    `${date.getMonth() + 1}`.padStart(2, '0'),
    `${date.getDate()}`.padStart(2, '0'),
  ].join('-')
}

function percent(value: number | null | undefined) {
  return `${(value ?? 0).toFixed(1)}%`
}
</script>

<template>
  <section class="business-page reports-page stitch-inspired-page stitch-reports-page">
    <section class="screen-hero">
      <div>
        <p class="eyebrow">经营报表 / Reports</p>
        <h2>经营报表专页</h2>
        <p>按真实销售、库存、财务数据生成 PC 经营分析，支持打印与 CSV 导出。</p>
      </div>
      <div class="hero-actions">
        <button type="button" class="ghost-action" @click="refreshReports">刷新数据</button>
        <button type="button" class="ghost-action" @click="printPage">打印当前页</button>
        <button type="button" @click="exportCsv">导出 CSV</button>
      </div>
    </section>

    <PageStatusBanner
      v-if="!isApiSource"
      tone="warning"
      title="演示模式"
      message="当前是演示模式。这一页只在真实登录后读取经营报表。"
    />
    <PageStatusBanner
      v-else-if="error"
      tone="error"
      title="部分区块加载失败"
      :message="error"
      action-label="重新加载"
      @action="refreshReports"
    />
    <PageStatusBanner v-if="loading" tone="info" title="正在同步" message="正在加载经营报表..." />

    <section class="panel">
      <div class="business-toolbar">
        <div class="status-tabs">
          <button type="button" :class="{ active: period === 'today' }" @click="setPeriod('today')">今日</button>
          <button type="button" :class="{ active: period === 'week' }" @click="setPeriod('week')">本周</button>
          <button type="button" :class="{ active: period === 'month' }" @click="setPeriod('month')">本月</button>
          <button type="button" :class="{ active: period === 'custom' }" @click="setPeriod('custom')">自定义</button>
        </div>
        <label class="compact-field">
          <span>开始日期</span>
          <input v-model="customStart" type="date" :disabled="period !== 'custom'" />
        </label>
        <label class="compact-field">
          <span>结束日期</span>
          <input v-model="customEnd" type="date" :disabled="period !== 'custom'" />
        </label>
      </div>
    </section>

    <section class="metrics-grid stitch-kpis">
      <article class="metric-card" data-tone="blue">
        <span>销售额</span>
        <strong>{{ formatCurrency(salesSummary?.totalSalesAmount || 0) }}</strong>
        <p>{{ salesSummary?.totalOrderCount || 0 }} 单 / 已收 {{ formatCurrency(salesSummary?.totalPaidAmount || 0) }}</p>
      </article>
      <article class="metric-card" data-tone="green">
        <span>利润额</span>
        <strong>{{ formatCurrency(profitSummary?.estimatedProfitAmount || 0) }}</strong>
        <p>利润率 {{ percent(profitSummary?.estimatedProfitRate) }}</p>
      </article>
      <article class="metric-card" data-tone="orange">
        <span>净现金流</span>
        <strong>{{ formatCurrency(cashflowSummary?.netCashFlow || 0) }}</strong>
        <p>收入 {{ formatCurrency(cashflowSummary?.totalIncomeAmount || 0) }} / 支出 {{ formatCurrency(cashflowSummary?.totalExpenseAmount || 0) }}</p>
      </article>
      <article class="metric-card" data-tone="red">
        <span>对账差额</span>
        <strong>{{ formatCurrency((reconciliationSummary?.totalReceivableAmount || 0) - (reconciliationSummary?.totalPayableAmount || 0)) }}</strong>
        <p>应收 {{ formatCurrency(reconciliationSummary?.totalReceivableAmount || 0) }} / 应付 {{ formatCurrency(reconciliationSummary?.totalPayableAmount || 0) }}</p>
      </article>
    </section>

    <section class="panel">
      <div class="panel-head">
        <div>
          <p class="eyebrow">销售趋势</p>
          <h3>销售额与订单数</h3>
        </div>
        <span class="session-source">{{ trendPoints.length }} 个时间桶</span>
      </div>
      <div v-if="trendPoints.length" class="report-chart-card">
        <svg viewBox="0 0 620 220" class="report-chart">
          <g v-for="(bar, index) in trendBars" :key="index">
            <rect
              :x="bar.x"
              :y="200 - bar.height"
              :width="bar.width"
              :height="bar.height"
              rx="6"
              class="report-bar"
            />
          </g>
          <path v-if="trendPath" :d="trendPath" class="report-line" />
        </svg>
        <div class="report-trend-labels">
          <span v-for="point in trendPoints" :key="`${point.startAt}-${point.endAt}`">
            {{ period === 'today' ? formatDateTime(point.startAt) : formatDate(point.startAt) }}
          </span>
        </div>
      </div>
      <PageEmptyState v-else title="暂无趋势数据" message="当前时间范围内没有销售趋势数据。" />
      <p v-if="sectionErrors.trend" class="muted">{{ sectionErrors.trend }}</p>
    </section>

    <section class="report-grid">
      <article class="panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">商品分析</p>
            <h3>热销商品</h3>
          </div>
        </div>
        <div class="table-shell">
          <table>
            <thead>
              <tr>
                <th>商品</th>
                <th>销量</th>
                <th>销售额</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in topProducts" :key="item.productId">
                <td>{{ item.productName }}</td>
                <td>{{ item.totalQuantity }}</td>
                <td>{{ formatCurrency(item.totalAmount) }}</td>
              </tr>
              <tr v-if="topProducts.length === 0">
                <td colspan="3" class="empty-cell">暂无热销商品数据</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-if="sectionErrors.topProducts" class="muted">{{ sectionErrors.topProducts }}</p>
      </article>

      <article class="panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">商品利润</p>
            <h3>利润商品排行</h3>
          </div>
        </div>
        <div class="table-shell">
          <table>
            <thead>
              <tr>
                <th>商品</th>
                <th>利润额</th>
                <th>利润率</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in profitProducts" :key="item.productId">
                <td>{{ item.productName }}</td>
                <td>{{ formatCurrency(item.totalProfitAmount) }}</td>
                <td>{{ percent(item.profitRate) }}</td>
              </tr>
              <tr v-if="profitProducts.length === 0">
                <td colspan="3" class="empty-cell">暂无商品利润数据</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-if="sectionErrors.profitProducts" class="muted">{{ sectionErrors.profitProducts }}</p>
      </article>

      <article class="panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">客户分析</p>
            <h3>客户利润</h3>
          </div>
        </div>
        <div class="table-shell">
          <table>
            <thead>
              <tr>
                <th>客户</th>
                <th>利润额</th>
                <th>利润率</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in profitCustomers" :key="item.customerId || item.customerName || Math.random()">
                <td>{{ item.customerName || '散客' }}</td>
                <td>{{ formatCurrency(item.totalProfitAmount) }}</td>
                <td>{{ percent(item.profitRate) }}</td>
              </tr>
              <tr v-if="profitCustomers.length === 0">
                <td colspan="3" class="empty-cell">暂无客户利润数据</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <article class="panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">客户销售</p>
            <h3>客户销售与应收</h3>
          </div>
        </div>
        <div class="table-shell">
          <table>
            <thead>
              <tr>
                <th>客户</th>
                <th>订单数</th>
                <th>销售额 / 应收</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in customerSales" :key="item.customerId || `${item.customerName}-${index}`">
                <td>{{ item.customerName || '散客' }}</td>
                <td>{{ item.totalOrders }}</td>
                <td>{{ formatCurrency(item.totalAmount) }}</td>
              </tr>
              <tr v-for="item in receivableCustomers" :key="`receivable-${item.customerId}`">
                <td>{{ item.customerName }}</td>
                <td>应收</td>
                <td>{{ formatCurrency(item.balance) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>
    </section>

    <section class="report-grid">
      <article class="panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">风险与履约</p>
            <h3>退款记录</h3>
          </div>
        </div>
        <div class="table-shell">
          <table>
            <thead>
              <tr>
                <th>销售单</th>
                <th>客户</th>
                <th>退款金额</th>
                <th>方式</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in refundRecords" :key="item.paymentId">
                <td>{{ item.orderNo }}</td>
                <td>{{ item.customerName }}</td>
                <td>{{ formatCurrency(item.refundAmount) }}</td>
                <td>{{ financeMethodLabel(item.method) }}</td>
              </tr>
              <tr v-if="refundRecords.length === 0">
                <td colspan="4" class="empty-cell">暂无退款记录</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <article class="panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">出库记录</p>
            <h3>销售出库明细</h3>
          </div>
        </div>
        <div class="table-shell">
          <table>
            <thead>
              <tr>
                <th>销售单</th>
                <th>商品</th>
                <th>数量</th>
                <th>金额</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in stockOutRecords" :key="`${item.orderId}-${item.productId}`">
                <td>{{ item.orderNo }}</td>
                <td>{{ item.productName }}</td>
                <td>{{ item.quantity }}</td>
                <td>{{ formatCurrency(item.amount) }}</td>
              </tr>
              <tr v-if="stockOutRecords.length === 0">
                <td colspan="4" class="empty-cell">暂无出库记录</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <article class="panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">库存流水</p>
            <h3>库存流向</h3>
          </div>
        </div>
        <div class="table-shell">
          <table>
            <thead>
              <tr>
                <th>商品</th>
                <th>数量</th>
                <th>来源</th>
                <th>时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in inventoryFlow" :key="`${item.orderId}-${item.productId}-${index}`">
                <td>{{ item.productName }}</td>
                <td>{{ item.quantity }}</td>
                <td>{{ item.sourceLabel || inventorySourceLabel(String(item.sourceType)) }}</td>
                <td>{{ formatDateTime(item.flowTime) }}</td>
              </tr>
              <tr v-if="inventoryFlow.length === 0">
                <td colspan="4" class="empty-cell">暂无库存流水</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <article class="panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">低库存预警</p>
            <h3>补货风险</h3>
          </div>
        </div>
        <div class="table-shell">
          <table>
            <thead>
              <tr>
                <th>商品</th>
                <th>当前库存</th>
                <th>安全库存</th>
                <th>风险等级</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in lowStock" :key="item.productId">
                <td>{{ item.productName }}</td>
                <td>{{ item.stock }}</td>
                <td>{{ item.safeStock }}</td>
                <td>{{ riskLevelLabel(item.stock <= 0 ? 'high' : 'medium') }}</td>
              </tr>
              <tr v-if="lowStock.length === 0">
                <td colspan="4" class="empty-cell">暂无低库存预警</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>
    </section>

    <section class="panel">
      <p class="eyebrow">接口目录</p>
      <h3>本页已接入接口</h3>
      <div class="contract-list horizontal">
        <article v-for="contract in reportContracts" :key="`${contract.method}-${contract.path}`">
          <span>{{ contract.method }}</span>
          <strong>{{ contract.path }}</strong>
          <p>{{ contract.purpose }}</p>
        </article>
      </div>
    </section>
  </section>
</template>
