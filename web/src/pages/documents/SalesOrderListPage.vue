<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useSession } from '@/app/stores/session'
import { fetchSaleOrders, type SaleOrder } from '@/shared/api/client'
import { type EntityId } from '@/shared/utils/id'
import {
  formatCurrency,
  formatDateTime,
  saleOrderStatusLabel,
  salePaymentStatus,
  saleShippingStatus,
  SALE_CANCELLED,
  SALE_COMPLETED,
  SALE_CONFIRMED,
  SALE_DRAFT,
} from '@/shared/utils/business'

type SalesListTab = 'all' | 'draft' | 'ship' | 'settle' | 'completed' | 'cancelled'

const router = useRouter()
const session = useSession()

const orders = ref<SaleOrder[]>([])
const loading = ref(false)
const error = ref('')
const searchKeyword = ref('')
const activeTab = ref<SalesListTab>('all')
const startDate = ref('')
const endDate = ref('')
const pageSize = ref(10)
const currentPage = ref(1)

const isApiSource = computed(() => session.source.value === 'api' && Boolean(session.token.value))
const canWrite = computed(() => session.hasPermission(['sales:write']))
const totalAmount = computed(() => orders.value.reduce((sum, item) => sum + item.totalAmount, 0))
const unpaidAmount = computed(() => orders.value.reduce((sum, item) => sum + Math.max(item.totalAmount - item.paidAmount, 0), 0))
const draftCount = computed(() => orders.value.filter((item) => item.status === SALE_DRAFT).length)
const pendingShipCount = computed(() => orders.value.filter((item) => item.status === SALE_CONFIRMED).length)
const pendingSettleCount = computed(() => orders.value.filter((item) => item.paidAmount < item.totalAmount && item.status !== SALE_CANCELLED).length)
const totalPages = computed(() => Math.max(1, Math.ceil(orders.value.length / pageSize.value)))
const pagedOrders = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return orders.value.slice(start, start + pageSize.value)
})
const pageStart = computed(() => (orders.value.length === 0 ? 0 : (currentPage.value - 1) * pageSize.value + 1))
const pageEnd = computed(() => Math.min(currentPage.value * pageSize.value, orders.value.length))

const statusTabs = computed(() => [
  { key: 'all' as const, label: '全部', count: orders.value.length },
  { key: 'draft' as const, label: '待审核', count: draftCount.value },
  { key: 'ship' as const, label: '待出库', count: pendingShipCount.value },
  { key: 'settle' as const, label: '待结算', count: pendingSettleCount.value },
  { key: 'completed' as const, label: '已完成', count: orders.value.filter((item) => item.status === SALE_COMPLETED).length },
  { key: 'cancelled' as const, label: '已作废', count: orders.value.filter((item) => item.status === SALE_CANCELLED).length },
])

watch(
  [() => session.source.value, () => session.token.value, searchKeyword, activeTab, startDate, endDate],
  async () => {
    currentPage.value = 1
    if (!isApiSource.value || !session.token.value) {
      orders.value = []
      error.value = ''
      return
    }
    await loadOrders()
  },
  { immediate: true },
)

watch(pageSize, () => {
  currentPage.value = 1
})

async function loadOrders() {
  if (!session.token.value) return
  loading.value = true
  error.value = ''
  try {
    orders.value = await fetchSaleOrders(session.token.value, {
      keyword: searchKeyword.value.trim() || undefined,
      status: statusParam(activeTab.value),
      paymentStatus: activeTab.value === 'settle' ? 0 : undefined,
      createdAfter: dateStart(startDate.value),
      createdBefore: dateEnd(endDate.value),
      page: 0,
      size: 200,
    })
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : '销售单加载失败'
  } finally {
    loading.value = false
  }
}

function statusParam(tab: SalesListTab) {
  if (tab === 'draft') return SALE_DRAFT
  if (tab === 'ship') return SALE_CONFIRMED
  if (tab === 'completed') return SALE_COMPLETED
  if (tab === 'cancelled') return SALE_CANCELLED
  return undefined
}

function dateStart(value: string) {
  if (!value) return undefined
  const date = new Date(`${value}T00:00:00`)
  return Number.isNaN(date.getTime()) ? undefined : date.getTime()
}

function dateEnd(value: string) {
  if (!value) return undefined
  const date = new Date(`${value}T23:59:59`)
  return Number.isNaN(date.getTime()) ? undefined : date.getTime()
}

function openCreate() {
  router.push('/documents/sales/edit')
}

function openDetail(orderId: EntityId) {
  router.push({ path: '/documents/sales/detail', query: { id: String(orderId) } })
}

function openEdit(orderId: EntityId) {
  router.push({ path: '/documents/sales/edit', query: { id: String(orderId) } })
}

function openPayment(orderId: EntityId) {
  router.push({ path: '/documents/sales/payment', query: { id: String(orderId) } })
}

function openReturn(orderId: EntityId) {
  router.push({ path: '/documents/sales-returns', query: { orderId: String(orderId) } })
}

function orderTone(order: SaleOrder) {
  if (order.status === SALE_CANCELLED) return 'cancelled'
  if (order.status === SALE_DRAFT) return 'draft'
  if (order.status === SALE_COMPLETED) return 'done'
  return 'running'
}

function paymentTone(order: SaleOrder) {
  if (order.status === SALE_CANCELLED) return 'cancelled'
  if (order.paidAmount <= 0) return 'pending'
  if (order.paidAmount < order.totalAmount) return 'running'
  return 'done'
}

function setPage(page: number) {
  currentPage.value = Math.min(Math.max(page, 1), totalPages.value)
}
</script>

<template>
  <section class="pc-list-page sales-list-page">
    <header class="pc-list-titlebar">
      <div class="pc-breadcrumb">
        <span>销售管理</span>
        <span class="material-symbols-outlined">chevron_right</span>
        <h1>销售单</h1>
      </div>
      <div class="pc-title-actions">
        <button type="button" class="pc-icon-action" aria-label="通知">
          <span class="material-symbols-outlined">notifications</span>
          <i></i>
        </button>
        <button type="button" class="pc-primary-action" :disabled="!canWrite || !isApiSource" @click="openCreate">
          <span class="material-symbols-outlined">add</span>
          新增销售单
        </button>
      </div>
    </header>

    <p v-if="!isApiSource" class="form-error">当前是演示模式。这一页只在真实登录后加载销售单。</p>
    <p v-else-if="error" class="form-error">{{ error }}</p>
    <p v-else-if="loading" class="form-success">正在加载真实销售单...</p>

    <section class="pc-list-toolbar">
      <div class="pc-filter-row">
        <label class="pc-search-field">
          <span class="material-symbols-outlined">search</span>
          <input v-model="searchKeyword" placeholder="按订单号/客户名称搜索..." />
        </label>
        <label class="pc-date-field">
          <input v-model="startDate" type="date" />
        </label>
        <span class="pc-date-separator">-</span>
        <label class="pc-date-field">
          <input v-model="endDate" type="date" />
        </label>
        <button type="button" class="pc-secondary-action" @click="loadOrders">
          <span class="material-symbols-outlined">filter_list</span>
          高级筛选
        </button>
      </div>
      <div class="pc-list-summary">
        <span>销售金额 {{ formatCurrency(totalAmount) }}</span>
        <span>待收款 {{ formatCurrency(unpaidAmount) }}</span>
      </div>
    </section>

    <section class="pc-data-card">
      <nav class="pc-table-tabs" aria-label="销售单状态">
        <button
          v-for="tab in statusTabs"
          :key="tab.key"
          type="button"
          :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
          <span v-if="tab.count > 0">{{ tab.count }}</span>
        </button>
      </nav>

      <div class="pc-table-scroll">
        <table class="pc-data-table">
          <thead>
            <tr>
              <th class="pc-check-cell"><input type="checkbox" /></th>
              <th>单据信息</th>
              <th>客户名称</th>
              <th class="align-right">销售金额</th>
              <th class="align-right">已付金额</th>
              <th class="align-center">出库状态</th>
              <th class="align-center">收款状态</th>
              <th class="align-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in pagedOrders" :key="order.id" @dblclick="openDetail(order.id)">
              <td class="pc-check-cell"><input type="checkbox" /></td>
              <td>
                <div class="pc-doc-cell">
                  <div>
                    <strong>{{ order.orderNo }}</strong>
                    <span class="pc-status-chip" :data-tone="orderTone(order)">{{ saleOrderStatusLabel(order.status) }}</span>
                  </div>
                  <small>{{ formatDateTime(order.createdAt) }}</small>
                </div>
              </td>
              <td>{{ order.customerName || '散客' }}</td>
              <td class="align-right amount-strong">{{ formatCurrency(order.totalAmount) }}</td>
              <td class="align-right" :class="{ muted: order.paidAmount <= 0 }">{{ formatCurrency(order.paidAmount) }}</td>
              <td class="align-center">
                <span class="pc-inline-status" :data-tone="orderTone(order)">
                  <span class="material-symbols-outlined">{{ order.status === SALE_COMPLETED ? 'check_circle' : order.status === SALE_CONFIRMED ? 'local_shipping' : 'pending' }}</span>
                  {{ saleShippingStatus(order.status) }}
                </span>
              </td>
              <td class="align-center">
                <span class="pc-inline-status" :data-tone="paymentTone(order)">
                  <span class="material-symbols-outlined">{{ paymentTone(order) === 'done' ? 'task_alt' : paymentTone(order) === 'running' ? 'payments' : 'pending_actions' }}</span>
                  {{ salePaymentStatus(order.totalAmount, order.paidAmount, order.status) }}
                </span>
              </td>
              <td class="align-right">
                <div class="pc-row-actions">
                  <button type="button" title="详情" @click="openDetail(order.id)">
                    <span class="material-symbols-outlined">visibility</span>
                  </button>
                  <button type="button" title="收款" :disabled="!isApiSource" @click="openPayment(order.id)">
                    <span class="material-symbols-outlined">payments</span>
                  </button>
                  <button type="button" title="退货" :disabled="!isApiSource" @click="openReturn(order.id)">
                    <span class="material-symbols-outlined">keyboard_return</span>
                  </button>
                  <button type="button" title="编辑" :disabled="order.status !== SALE_DRAFT || !canWrite || !isApiSource" @click="openEdit(order.id)">
                    <span class="material-symbols-outlined">edit</span>
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!loading && pagedOrders.length === 0">
              <td colspan="8" class="empty-cell">暂无销售单</td>
            </tr>
          </tbody>
        </table>
      </div>

      <footer class="pc-pagination">
        <span>共 <b>{{ orders.length }}</b> 条记录，当前显示 {{ pageStart }}-{{ pageEnd }} 条</span>
        <div>
          <select v-model.number="pageSize">
            <option :value="10">10 条/页</option>
            <option :value="20">20 条/页</option>
            <option :value="50">50 条/页</option>
          </select>
          <button type="button" :disabled="currentPage === 1" @click="setPage(currentPage - 1)">
            <span class="material-symbols-outlined">chevron_left</span>
          </button>
          <button type="button" class="active">{{ currentPage }}</button>
          <button type="button" :disabled="currentPage >= totalPages" @click="setPage(currentPage + 1)">
            <span class="material-symbols-outlined">chevron_right</span>
          </button>
        </div>
      </footer>
    </section>
  </section>
</template>
