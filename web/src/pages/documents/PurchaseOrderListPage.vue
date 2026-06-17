<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useSession } from '@/app/stores/session'
import { fetchPurchaseOrders, type PurchaseOrder } from '@/shared/api/client'
import { sameEntityId, type EntityId } from '@/shared/utils/id'
import {
  formatCurrency,
  formatDateTime,
  purchaseOrderStatusLabel,
  purchasePaymentStatus,
  purchaseReceiptStatus,
  SALE_DRAFT,
} from '@/shared/utils/business'

const router = useRouter()
const session = useSession()

const orders = ref<PurchaseOrder[]>([])
const loading = ref(false)
const error = ref('')
const searchKeyword = ref('')
const statusFilter = ref('all')
const selectedOrderId = ref<EntityId | null>(null)

const isApiSource = computed(() => session.source.value === 'api' && Boolean(session.token.value))
const canWrite = computed(() => session.hasPermission(['purchase:write']))
const selectedOrder = computed(() => orders.value.find((item) => sameEntityId(item.id, selectedOrderId.value)) ?? orders.value[0] ?? null)
const pendingReceiptCount = computed(() => orders.value.filter((item) => purchaseReceiptStatus(item.totalAmount, item.receivedAmount, item.status) !== '已入库').length)
const payableAmount = computed(() => orders.value.reduce((sum, item) => sum + Math.max(item.totalAmount - item.paidAmount, 0), 0))
const totalAmount = computed(() => orders.value.reduce((sum, item) => sum + item.totalAmount, 0))

watch(
  [() => session.source.value, () => session.token.value, searchKeyword, statusFilter],
  async () => {
    if (!isApiSource.value || !session.token.value) {
      orders.value = []
      error.value = ''
      return
    }
    await loadOrders()
  },
  { immediate: true },
)

async function loadOrders() {
  if (!session.token.value) return
  loading.value = true
  error.value = ''
  try {
    orders.value = await fetchPurchaseOrders(session.token.value, {
      keyword: searchKeyword.value.trim() || undefined,
      status: statusFilter.value === 'all' ? undefined : Number(statusFilter.value),
      page: 0,
      size: 200,
    })
    if (!orders.value.some((item) => sameEntityId(item.id, selectedOrderId.value))) {
      selectedOrderId.value = orders.value[0]?.id ?? null
    }
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : '采购单加载失败'
  } finally {
    loading.value = false
  }
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

function openReturns(orderId?: EntityId) {
  router.push({ path: '/documents/purchase-returns', query: orderId ? { orderId: String(orderId) } : undefined })
}

function openPayOrders() {
  router.push('/documents/pay-orders/detail')
}
</script>

<template>
  <section class="business-page">
    <section class="screen-hero">
      <div>
        <p class="eyebrow">采购单 / Purchase Orders</p>
        <h2>采购单列表专页</h2>
        <p>按真实采购单、入库进度与付款进度组织列表，承接采购入库与采购退货专页入口。</p>
      </div>
      <div class="hero-actions">
        <button type="button" :disabled="!canWrite || !isApiSource" @click="openCreate">新建采购单</button>
        <button type="button" :disabled="!isApiSource" @click="openReceipts(selectedOrder?.id)">采购入库</button>
        <button type="button" class="ghost-action" :disabled="!isApiSource" @click="openReturns(selectedOrder?.id)">采购退货</button>
        <button type="button" class="ghost-action" :disabled="!isApiSource" @click="openPayOrders">处理付款</button>
      </div>
    </section>

    <p v-if="!isApiSource" class="form-error">当前是演示模式。这一页只在真实登录后加载采购单。</p>
    <p v-else-if="error" class="form-error">{{ error }}</p>
    <p v-else-if="loading" class="form-success">正在加载真实采购单...</p>

    <section class="metrics-grid compact">
      <article class="metric-card" data-tone="blue">
        <span>采购单数</span>
        <strong>{{ orders.length }}</strong>
        <p>{{ orders.filter((item) => item.status === SALE_DRAFT).length }} 单草稿</p>
      </article>
      <article class="metric-card" data-tone="orange">
        <span>待入库</span>
        <strong>{{ pendingReceiptCount }}</strong>
        <p>仍未完成入库的采购单</p>
      </article>
      <article class="metric-card" data-tone="green">
        <span>待付款</span>
        <strong>{{ formatCurrency(payableAmount) }}</strong>
        <p>累计采购金额 {{ formatCurrency(totalAmount) }}</p>
      </article>
    </section>

    <section class="panel">
      <div class="business-toolbar">
        <label class="search-box">
          <span>搜索采购单</span>
          <input v-model="searchKeyword" placeholder="采购单号 / 供应商" />
        </label>
        <label class="compact-field">
          <span>状态</span>
          <select v-model="statusFilter">
            <option value="all">全部状态</option>
            <option value="0">草稿</option>
            <option value="1">已完成</option>
          </select>
        </label>
      </div>
    </section>

    <section class="business-split">
      <article class="panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">采购列表</p>
            <h3>真实采购单</h3>
          </div>
          <span class="session-source">{{ orders.length }} 条</span>
        </div>

        <div class="table-shell">
          <table>
            <thead>
              <tr>
                <th>采购单号</th>
                <th>供应商</th>
                <th>商品数</th>
                <th>采购金额</th>
                <th>入库状态</th>
                <th>付款状态</th>
                <th>创建时间</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="order in orders"
                :key="order.id"
                :class="{ selected: order.id === selectedOrder?.id }"
                @click="selectedOrderId = order.id"
              >
                <td>{{ order.orderNo }}</td>
                <td>{{ order.supplierName || '未命名供应商' }}</td>
                <td>{{ order.items.length }}</td>
                <td>{{ formatCurrency(order.totalAmount) }}</td>
                <td>{{ purchaseReceiptStatus(order.totalAmount, order.receivedAmount, order.status) }}</td>
                <td>{{ purchasePaymentStatus(order.totalAmount, order.paidAmount, order.status) }}</td>
                <td>{{ formatDateTime(order.createdAt) }}</td>
              </tr>
              <tr v-if="!loading && orders.length === 0">
                <td colspan="7" class="empty-cell">暂无采购单</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <aside class="panel detail-panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">采购摘要</p>
            <h3>{{ selectedOrder?.orderNo || '请选择采购单' }}</h3>
          </div>
        </div>

        <div v-if="selectedOrder" class="detail-stack">
          <article class="detail-card">
            <dl class="detail-list">
              <div>
                <dt>供应商</dt>
                <dd>{{ selectedOrder.supplierName || '未命名供应商' }}</dd>
              </div>
              <div>
                <dt>采购金额</dt>
                <dd>{{ formatCurrency(selectedOrder.totalAmount) }}</dd>
              </div>
              <div>
                <dt>已入库金额</dt>
                <dd>{{ formatCurrency(selectedOrder.receivedAmount) }}</dd>
              </div>
              <div>
                <dt>已付款金额</dt>
                <dd>{{ formatCurrency(selectedOrder.paidAmount) }}</dd>
              </div>
              <div>
                <dt>主状态</dt>
                <dd>{{ purchaseOrderStatusLabel(selectedOrder.totalAmount, selectedOrder.paidAmount, selectedOrder.receivedAmount, selectedOrder.status) }}</dd>
              </div>
            </dl>
          </article>

          <article class="detail-card">
            <p class="eyebrow">商品明细</p>
            <div class="mini-list">
              <div v-for="item in selectedOrder.items.slice(0, 5)" :key="item.id || `${item.productCode}-${item.productName}`">
                <strong>{{ item.productName || item.productCode }}</strong>
                <span>{{ item.quantity }} x {{ formatCurrency(item.unitCost) }}</span>
              </div>
            </div>
          </article>

          <div class="form-actions">
            <button type="button" class="ghost-action" :disabled="!isApiSource" @click="openDetail(selectedOrder.id)">查看详情</button>
            <button
              type="button"
              class="ghost-action"
              :disabled="!canWrite || !isApiSource || selectedOrder.status !== SALE_DRAFT"
              @click="openEdit(selectedOrder.id)"
            >
              编辑草稿
            </button>
            <button type="button" :disabled="!isApiSource" @click="openReceipts(selectedOrder.id)">去做入库</button>
            <button type="button" class="ghost-action" :disabled="!isApiSource" @click="openReturns(selectedOrder.id)">去做退货</button>
            <button type="button" class="ghost-action" :disabled="!isApiSource" @click="openPayOrders">去做付款</button>
          </div>
        </div>

        <div v-else class="empty-preview">
          <strong>暂无可查看采购单</strong>
          <p>请先选择一张采购单。</p>
        </div>
      </aside>
    </section>
  </section>
</template>
