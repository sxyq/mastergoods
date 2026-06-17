<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useSession } from '@/app/stores/session'
import { fetchSaleOrders, type SaleOrder } from '@/shared/api/client'
import { sameEntityId, type EntityId } from '@/shared/utils/id'
import {
  formatCurrency,
  formatDateTime,
  salePaymentStatus,
  saleShippingStatus,
  SALE_DRAFT,
} from '@/shared/utils/business'

const router = useRouter()
const session = useSession()

const orders = ref<SaleOrder[]>([])
const loading = ref(false)
const error = ref('')
const searchKeyword = ref('')
const statusFilter = ref('all')
const selectedOrderId = ref<EntityId | null>(null)

const isApiSource = computed(() => session.source.value === 'api' && Boolean(session.token.value))
const canWrite = computed(() => session.hasPermission(['sales:write']))
const selectedOrder = computed(() => orders.value.find((item) => sameEntityId(item.id, selectedOrderId.value)) ?? orders.value[0] ?? null)
const totalAmount = computed(() => orders.value.reduce((sum, item) => sum + item.totalAmount, 0))
const unpaidAmount = computed(() => orders.value.reduce((sum, item) => sum + Math.max(item.totalAmount - item.paidAmount, 0), 0))
const pendingCount = computed(() => orders.value.filter((item) => item.status === SALE_DRAFT).length)

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
    orders.value = await fetchSaleOrders(session.token.value, {
      keyword: searchKeyword.value.trim() || undefined,
      status: statusFilter.value === 'all' ? undefined : Number(statusFilter.value),
      page: 0,
      size: 200,
    })
    if (!orders.value.some((item) => sameEntityId(item.id, selectedOrderId.value))) {
      selectedOrderId.value = orders.value[0]?.id ?? null
    }
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : '销售单加载失败'
  } finally {
    loading.value = false
  }
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
</script>

<template>
  <section class="business-page">
    <section class="screen-hero">
      <div>
        <p class="eyebrow">销售单 / Sale Orders</p>
        <h2>销售单列表专页</h2>
        <p>对齐安卓端销售单列表与 `/v2/sale-orders`，支持真实订单筛选、详情跳转与草稿编辑入口。</p>
      </div>
      <div class="hero-actions">
        <button type="button" :disabled="!canWrite || !isApiSource" @click="openCreate">新建销售单</button>
      </div>
    </section>

    <p v-if="!isApiSource" class="form-error">当前是演示模式。这一页只在真实登录后加载销售单。</p>
    <p v-else-if="error" class="form-error">{{ error }}</p>
    <p v-else-if="loading" class="form-success">正在加载真实销售单...</p>

    <section class="metrics-grid compact">
      <article class="metric-card" data-tone="blue">
        <span>销售单数</span>
        <strong>{{ orders.length }}</strong>
        <p>{{ pendingCount }} 单待审核</p>
      </article>
      <article class="metric-card" data-tone="green">
        <span>销售金额</span>
        <strong>{{ formatCurrency(totalAmount) }}</strong>
        <p>当前筛选结果汇总</p>
      </article>
      <article class="metric-card" data-tone="orange">
        <span>待收款</span>
        <strong>{{ formatCurrency(unpaidAmount) }}</strong>
        <p>未完全结清的应收金额</p>
      </article>
    </section>

    <section class="panel">
      <div class="business-toolbar">
        <label class="search-box">
          <span>搜索订单</span>
          <input v-model="searchKeyword" placeholder="单据号 / 客户名" />
        </label>
        <label class="compact-field">
          <span>订单状态</span>
          <select v-model="statusFilter">
            <option value="all">全部状态</option>
            <option value="0">草稿</option>
            <option value="3">已确认</option>
            <option value="1">已完成</option>
            <option value="2">已作废</option>
          </select>
        </label>
      </div>
    </section>

    <section class="business-split">
      <article class="panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">订单主表</p>
            <h3>销售单列表</h3>
          </div>
          <span class="session-source">{{ orders.length }} 条</span>
        </div>

        <div class="table-shell">
          <table>
            <thead>
              <tr>
                <th>单据编号</th>
                <th>客户名称</th>
                <th>订单金额</th>
                <th>已付金额</th>
                <th>出库状态</th>
                <th>收款状态</th>
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
                <td>{{ order.customerName || '散客' }}</td>
                <td>{{ formatCurrency(order.totalAmount) }}</td>
                <td>{{ formatCurrency(order.paidAmount) }}</td>
                <td>{{ saleShippingStatus(order.status) }}</td>
                <td>{{ salePaymentStatus(order.totalAmount, order.paidAmount, order.status) }}</td>
                <td>{{ formatDateTime(order.createdAt) }}</td>
              </tr>
              <tr v-if="!loading && orders.length === 0">
                <td colspan="7" class="empty-cell">暂无销售单</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <aside class="panel detail-panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">订单摘要</p>
            <h3>{{ selectedOrder?.orderNo || '请选择销售单' }}</h3>
          </div>
        </div>

        <div v-if="selectedOrder" class="detail-stack">
          <article class="detail-card">
            <dl class="detail-list">
              <div>
                <dt>客户</dt>
                <dd>{{ selectedOrder.customerName || '散客' }}</dd>
              </div>
              <div>
                <dt>商品行数</dt>
                <dd>{{ selectedOrder.items.length }}</dd>
              </div>
              <div>
                <dt>应收金额</dt>
                <dd>{{ formatCurrency(selectedOrder.totalAmount) }}</dd>
              </div>
              <div>
                <dt>已收金额</dt>
                <dd>{{ formatCurrency(selectedOrder.paidAmount) }}</dd>
              </div>
              <div>
                <dt>出库状态</dt>
                <dd>{{ saleShippingStatus(selectedOrder.status) }}</dd>
              </div>
              <div>
                <dt>收款状态</dt>
                <dd>{{ salePaymentStatus(selectedOrder.totalAmount, selectedOrder.paidAmount, selectedOrder.status) }}</dd>
              </div>
            </dl>
          </article>

          <article class="detail-card">
            <p class="eyebrow">商品明细</p>
            <div class="mini-list">
              <div v-for="item in selectedOrder.items.slice(0, 5)" :key="item.id || `${item.productId}-${item.productName}`">
                <strong>{{ item.productName || item.productCode }}</strong>
                <span>{{ item.quantity }} x {{ formatCurrency(item.unitPrice) }}</span>
              </div>
            </div>
          </article>

          <div class="form-actions">
            <button type="button" :disabled="!isApiSource" @click="openDetail(selectedOrder.id)">查看详情</button>
            <button type="button" :disabled="!isApiSource" @click="openPayment(selectedOrder.id)">去收款</button>
            <button type="button" class="ghost-action" :disabled="!isApiSource" @click="openReturn(selectedOrder.id)">做退货</button>
            <button
              type="button"
              class="ghost-action"
              :disabled="selectedOrder.status !== SALE_DRAFT || !canWrite || !isApiSource"
              @click="openEdit(selectedOrder.id)"
            >
              编辑草稿
            </button>
          </div>
        </div>
        <div v-else class="empty-preview">
          <strong>暂无可查看订单</strong>
          <p>当前筛选条件下还没有销售单。</p>
        </div>
      </aside>
    </section>
  </section>
</template>
