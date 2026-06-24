<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useSession } from '@/app/stores/session'
import { fetchPurchaseOrders, type PurchaseOrder } from '@/shared/api/client'
import { type EntityId } from '@/shared/utils/id'
import {
  formatCurrency,
  formatDateTime,
  purchaseOrderStatusLabel,
  purchasePaymentStatus,
  purchaseReceiptStatus,
  SALE_CANCELLED,
  SALE_COMPLETED,
  SALE_DRAFT,
} from '@/shared/utils/business'

type PurchaseListTab = 'all' | 'draft' | 'receipt' | 'pay' | 'completed' | 'cancelled'

const router = useRouter()
const session = useSession()

const rawOrders = ref<PurchaseOrder[]>([])
const loading = ref(false)
const error = ref('')
const searchKeyword = ref('')
const activeTab = ref<PurchaseListTab>('all')
const pageSize = ref(10)
const currentPage = ref(1)

const isApiSource = computed(() => session.source.value === 'api' && Boolean(session.token.value))
const canWrite = computed(() => session.hasPermission(['purchase:write']))
const filteredOrders = computed(() => rawOrders.value.filter(matchesActiveTab))
const orderSummary = computed(() => rawOrders.value.reduce(
  (summary, item) => {
    summary.totalAmount += item.totalAmount
    summary.payableAmount += Math.max(item.totalAmount - item.paidAmount, 0)
    if (item.status === SALE_DRAFT) summary.draft += 1
    else if (item.status === SALE_CANCELLED) summary.cancelled += 1
    else if (isCompleted(item)) summary.completed += 1
    else if (isPendingReceipt(item)) summary.receipt += 1
    else if (isPendingPay(item)) summary.pay += 1
    return summary
  },
  {
    draft: 0,
    receipt: 0,
    pay: 0,
    completed: 0,
    cancelled: 0,
    totalAmount: 0,
    payableAmount: 0,
  },
))
const displayedOrders = filteredOrders
const payableAmount = computed(() => orderSummary.value.payableAmount)
const totalAmount = computed(() => orderSummary.value.totalAmount)
const totalPages = computed(() => Math.max(1, Math.ceil(displayedOrders.value.length / pageSize.value)))
const pagedOrders = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return displayedOrders.value.slice(start, start + pageSize.value)
})
const pageStart = computed(() => (displayedOrders.value.length === 0 ? 0 : (currentPage.value - 1) * pageSize.value + 1))
const pageEnd = computed(() => Math.min(currentPage.value * pageSize.value, displayedOrders.value.length))

const statusTabs = computed(() => [
  { key: 'all' as const, label: '全部', count: rawOrders.value.length },
  { key: 'draft' as const, label: '待审核', count: orderSummary.value.draft },
  { key: 'receipt' as const, label: '待入库', count: orderSummary.value.receipt },
  { key: 'pay' as const, label: '待付款', count: orderSummary.value.pay },
  { key: 'completed' as const, label: '已完成', count: orderSummary.value.completed },
  { key: 'cancelled' as const, label: '已作废', count: orderSummary.value.cancelled },
])

watch(
  [() => session.source.value, () => session.token.value, searchKeyword],
  async () => {
    currentPage.value = 1
    if (!isApiSource.value || !session.token.value) {
      rawOrders.value = []
      error.value = ''
      return
    }
    await loadOrders()
  },
  { immediate: true },
)

watch([activeTab, pageSize], () => {
  currentPage.value = 1
})

async function loadOrders() {
  if (!session.token.value) return
  loading.value = true
  error.value = ''
  try {
    rawOrders.value = await fetchPurchaseOrders(session.token.value, {
      keyword: searchKeyword.value.trim() || undefined,
      page: 0,
      size: 200,
    })
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : '采购单加载失败'
  } finally {
    loading.value = false
  }
}

function matchesActiveTab(order: PurchaseOrder) {
  if (activeTab.value === 'draft') return order.status === SALE_DRAFT
  if (activeTab.value === 'receipt') return isPendingReceipt(order)
  if (activeTab.value === 'pay') return isPendingPay(order)
  if (activeTab.value === 'completed') return isCompleted(order)
  if (activeTab.value === 'cancelled') return order.status === SALE_CANCELLED
  return true
}

function isPendingReceipt(order: PurchaseOrder) {
  return order.status !== SALE_CANCELLED && order.receivedAmount < order.totalAmount
}

function isPendingPay(order: PurchaseOrder) {
  return order.status !== SALE_CANCELLED && order.paidAmount < order.totalAmount
}

function isCompleted(order: PurchaseOrder) {
  return order.status === SALE_COMPLETED || (order.totalAmount > 0 && order.receivedAmount >= order.totalAmount && order.paidAmount >= order.totalAmount)
}

function openCreate() {
  router.push('/documents/purchases/edit')
}

function openEdit(orderId: EntityId) {
  router.push({ path: '/documents/purchases/edit', query: { id: String(orderId) } })
}

function openDetail(orderId: EntityId) {
  router.push({ path: '/documents/purchases/detail', query: { id: String(orderId) } })
}

function openReceipts(orderId?: EntityId) {
  router.push({ path: '/documents/purchase-receipts', query: orderId ? { orderId: String(orderId) } : undefined })
}

function openPayOrders(orderId?: EntityId) {
  router.push({ path: '/documents/pay-orders/detail', query: orderId ? { purchaseOrderId: String(orderId) } : undefined })
}

function orderTone(order: PurchaseOrder) {
  if (order.status === SALE_CANCELLED) return 'cancelled'
  if (order.status === SALE_DRAFT) return 'draft'
  if (isCompleted(order)) return 'done'
  return 'running'
}

function payTone(order: PurchaseOrder) {
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
  <section class="pc-list-page purchase-list-page">
    <header class="pc-list-titlebar">
      <div class="pc-breadcrumb">
        <span>采购管理</span>
        <span class="material-symbols-outlined">chevron_right</span>
        <h1>采购单</h1>
      </div>
      <div class="pc-title-actions">
        <button type="button" class="pc-icon-action" aria-label="通知">
          <span class="material-symbols-outlined">notifications</span>
          <i></i>
        </button>
        <button type="button" class="pc-primary-action" :disabled="!canWrite || !isApiSource" @click="openCreate">
          <span class="material-symbols-outlined">add</span>
          新建采购单
        </button>
      </div>
    </header>

    <p v-if="!isApiSource" class="form-error">当前是演示模式。这一页只在真实登录后加载采购单。</p>
    <p v-else-if="error" class="form-error">{{ error }}</p>
    <p v-else-if="loading" class="form-success">正在加载真实采购单...</p>

    <section class="pc-list-toolbar">
      <div class="pc-filter-row">
        <label class="pc-search-field">
          <span class="material-symbols-outlined">search</span>
          <input v-model="searchKeyword" placeholder="单据号 / 供应商" />
        </label>
        <label class="pc-date-field"><input type="date" /></label>
        <span class="pc-date-separator">-</span>
        <label class="pc-date-field"><input type="date" /></label>
        <button type="button" class="pc-secondary-action" @click="loadOrders">
          <span class="material-symbols-outlined">tune</span>
          更多筛选
        </button>
      </div>
      <div class="pc-list-summary">
        <span>采购金额 {{ formatCurrency(totalAmount) }}</span>
        <span>待付款 {{ formatCurrency(payableAmount) }}</span>
      </div>
    </section>

    <section class="pc-data-card">
      <nav class="pc-table-tabs" aria-label="采购单状态">
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
              <th>供应商</th>
              <th class="align-right">采购金额</th>
              <th class="align-right">已付款</th>
              <th class="align-center">入库状态</th>
              <th class="align-center">付款状态</th>
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
                    <span class="pc-status-chip" :data-tone="orderTone(order)">
                      {{ purchaseOrderStatusLabel(order.totalAmount, order.paidAmount, order.receivedAmount, order.status) }}
                    </span>
                  </div>
                  <small>{{ formatDateTime(order.createdAt) }}</small>
                </div>
              </td>
              <td>{{ order.supplierName || '未命名供应商' }}</td>
              <td class="align-right amount-strong">{{ formatCurrency(order.totalAmount) }}</td>
              <td class="align-right" :class="{ muted: order.paidAmount <= 0 }">{{ formatCurrency(order.paidAmount) }}</td>
              <td class="align-center">
                <span class="pc-inline-status" :data-tone="orderTone(order)">
                  <span class="material-symbols-outlined">{{ isCompleted(order) ? 'check_circle' : isPendingReceipt(order) ? 'inventory_2' : 'pending' }}</span>
                  {{ purchaseReceiptStatus(order.totalAmount, order.receivedAmount, order.status) }}
                </span>
              </td>
              <td class="align-center">
                <span class="pc-inline-status" :data-tone="payTone(order)">
                  <span class="material-symbols-outlined">{{ payTone(order) === 'done' ? 'task_alt' : payTone(order) === 'running' ? 'payments' : 'pending_actions' }}</span>
                  {{ purchasePaymentStatus(order.totalAmount, order.paidAmount, order.status) }}
                </span>
              </td>
              <td class="align-right">
                <div class="pc-row-actions">
                  <button type="button" title="详情" @click="openDetail(order.id)">
                    <span class="material-symbols-outlined">visibility</span>
                  </button>
                  <button type="button" title="入库" :disabled="!isApiSource" @click="openReceipts(order.id)">
                    <span class="material-symbols-outlined">inventory_2</span>
                  </button>
                  <button type="button" title="付款" :disabled="!isApiSource" @click="openPayOrders(order.id)">
                    <span class="material-symbols-outlined">payments</span>
                  </button>
                  <button type="button" title="编辑" :disabled="order.status !== SALE_DRAFT || !canWrite || !isApiSource" @click="openEdit(order.id)">
                    <span class="material-symbols-outlined">edit</span>
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!loading && pagedOrders.length === 0">
              <td colspan="8" class="empty-cell">暂无采购单</td>
            </tr>
          </tbody>
        </table>
      </div>

      <footer class="pc-pagination">
        <span>共 <b>{{ displayedOrders.length }}</b> 条记录，当前显示 {{ pageStart }}-{{ pageEnd }} 条</span>
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
