<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSession } from '@/app/stores/session'
import {
  createSaleOrderPayment,
  fetchSaleOrder,
  fetchSaleOrderPayments,
  fetchSaleOrders,
  type PaymentRecord,
  type SaleOrder,
} from '@/shared/api/client'
import { readQueryId, sameEntityId, type EntityId } from '@/shared/utils/id'
import {
  METHOD_ALIPAY,
  METHOD_BANK,
  METHOD_CASH,
  METHOD_OTHER,
  METHOD_WECHAT,
  formatCurrency,
  formatDateTime,
  salePaymentStatus,
  saleShippingStatus,
} from '@/shared/utils/business'
import PageEmptyState from '@/shared/ui/PageEmptyState.vue'
import PageStatusBanner from '@/shared/ui/PageStatusBanner.vue'

const route = useRoute()
const router = useRouter()
const session = useSession()

const orders = ref<SaleOrder[]>([])
const payments = ref<PaymentRecord[]>([])
const selectedOrderId = ref<EntityId | null>(null)
const loading = ref(false)
const paymentsLoading = ref(false)
const submitting = ref(false)
const error = ref('')
const success = ref('')

const paymentForm = reactive({
  amount: '',
  method: String(METHOD_CASH),
  referenceNo: '',
})

const queryOrderId = computed(() => readQueryId(route.query.id))
const isApiSource = computed(() => session.source.value === 'api' && Boolean(session.token.value))
const canWrite = computed(() => session.hasAnyPermission(['sales:write', 'finance:write']))
const selectedOrder = computed(() => orders.value.find((item) => sameEntityId(item.id, selectedOrderId.value)) ?? null)
const remainingAmount = computed(() => {
  if (!selectedOrder.value) return 0
  return Math.max(selectedOrder.value.totalAmount - selectedOrder.value.paidAmount, 0)
})
const unpaidAmount = computed(() => orders.value.reduce((sum, item) => sum + Math.max(item.totalAmount - item.paidAmount, 0), 0))
const canCollectPayment = computed(() => Boolean(selectedOrder.value) && remainingAmount.value > 0 && canWrite.value)

const paymentMethods = [
  [METHOD_CASH, '现金'],
  [METHOD_WECHAT, '微信'],
  [METHOD_ALIPAY, '支付宝'],
  [METHOD_BANK, '银行卡'],
  [METHOD_OTHER, '其他'],
]

watch(
  [() => session.source.value, () => session.token.value, queryOrderId],
  async () => {
    if (!isApiSource.value || !session.token.value) {
      orders.value = []
      payments.value = []
      selectedOrderId.value = null
      error.value = ''
      return
    }
    await loadPage()
  },
  { immediate: true },
)

watch(
  selectedOrderId,
  async (nextId, prevId) => {
    if (!nextId || sameEntityId(nextId, prevId) || !session.token.value) return
    await loadPayments(nextId)
  },
)

async function loadPage() {
  if (!session.token.value) return
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    const list = await fetchSaleOrders(session.token.value, {
      paymentStatus: 0,
      page: 0,
      size: 200,
    })

    let nextOrders = [...list]
    if (queryOrderId.value && !nextOrders.some((item) => sameEntityId(item.id, queryOrderId.value))) {
      try {
        const detail = await fetchSaleOrder(session.token.value, queryOrderId.value)
        nextOrders = [detail, ...nextOrders]
      } catch {
        // ignore query prefetch error and use list result
      }
    }

    orders.value = nextOrders
    selectedOrderId.value = queryOrderId.value ?? nextOrders[0]?.id ?? null
    if (selectedOrderId.value) {
      await loadPayments(selectedOrderId.value)
    } else {
      payments.value = []
    }
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : '销售收款页面加载失败'
  } finally {
    loading.value = false
  }
}

async function loadPayments(orderId: EntityId) {
  if (!session.token.value) return
  paymentsLoading.value = true
  error.value = ''
  try {
    payments.value = await fetchSaleOrderPayments(session.token.value, orderId)
    paymentForm.amount = remainingAmount.value > 0 ? remainingAmount.value.toFixed(2) : ''
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : '收款记录加载失败'
  } finally {
    paymentsLoading.value = false
  }
}

async function handleCollectPayment() {
  if (!session.token.value || !selectedOrder.value) return
  submitting.value = true
  error.value = ''
  success.value = ''
  try {
    await createSaleOrderPayment(session.token.value, selectedOrder.value.id, {
      amount: Number(paymentForm.amount || 0),
      method: Number(paymentForm.method),
      referenceNo: paymentForm.referenceNo.trim() || null,
    })
    success.value = '收款记录已登记'
    const previousId = selectedOrder.value.id
    await loadPage()
    if (selectedOrder.value && sameEntityId(selectedOrder.value.id, previousId) && remainingAmount.value <= 0) {
      const nextOrder = orders.value.find((item) => !sameEntityId(item.id, previousId))
      selectedOrderId.value = nextOrder?.id ?? previousId
    }
  } catch (submitErr) {
    error.value = submitErr instanceof Error ? submitErr.message : '登记收款失败'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="business-page sales-payment-page stitch-inspired-page">
    <section class="screen-hero">
      <div>
        <p class="eyebrow">销售收款 / Sales Payment</p>
        <h2>销售收款专页</h2>
        <p>独立处理未结清销售单的收款登记，真实写入 `/v2/sale-orders/{id}/payments`。</p>
      </div>
      <div class="hero-actions">
        <button type="button" class="ghost-action" @click="router.push('/documents/sales')">返回销售单</button>
        <button
          v-if="selectedOrder"
          type="button"
          class="ghost-action"
          @click="router.push({ path: '/documents/sales/detail', query: { id: String(selectedOrder.id) } })"
        >
          查看销售详情
        </button>
      </div>
    </section>

    <PageStatusBanner
      v-if="!isApiSource"
      tone="warning"
      title="演示模式"
      message="当前是演示模式。这一页只在真实登录后读取待收款销售单。"
    />
    <PageStatusBanner
      v-else-if="error"
      tone="error"
      title="页面加载异常"
      :message="error"
      action-label="重新加载"
      @action="loadPage"
    />
    <PageStatusBanner v-else-if="success" tone="success" title="操作成功" :message="success" />
    <PageStatusBanner v-if="loading" tone="info" title="正在同步" message="正在加载待收款销售单..." />

    <section class="metrics-grid compact stitch-kpis">
      <article class="metric-card" data-tone="blue">
        <span>待收款订单</span>
        <strong>{{ orders.length }}</strong>
        <p>当前只展示未完全结清订单</p>
      </article>
      <article class="metric-card" data-tone="orange">
        <span>待收总额</span>
        <strong>{{ formatCurrency(unpaidAmount) }}</strong>
        <p>来自真实销售单应收差额</p>
      </article>
      <article class="metric-card" data-tone="green">
        <span>当前待收</span>
        <strong>{{ formatCurrency(remainingAmount) }}</strong>
        <p>{{ selectedOrder?.orderNo || '请选择订单' }}</p>
      </article>
    </section>

    <section class="business-split">
      <article class="panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">待收款订单</p>
            <h3>销售单列表</h3>
          </div>
          <span class="session-source">{{ orders.length }} 单</span>
        </div>

        <div class="table-shell">
          <table>
            <thead>
              <tr>
                <th>销售单号</th>
                <th>客户</th>
                <th>订单金额</th>
                <th>已收金额</th>
                <th>待收金额</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="order in orders"
                :key="order.id"
                :class="{ selected: sameEntityId(order.id, selectedOrderId) }"
                @click="selectedOrderId = order.id"
              >
                <td>{{ order.orderNo }}</td>
                <td>{{ order.customerName || '散客' }}</td>
                <td>{{ formatCurrency(order.totalAmount) }}</td>
                <td>{{ formatCurrency(order.paidAmount) }}</td>
                <td>{{ formatCurrency(Math.max(order.totalAmount - order.paidAmount, 0)) }}</td>
                <td>{{ salePaymentStatus(order.totalAmount, order.paidAmount, order.status) }}</td>
              </tr>
              <tr v-if="!loading && orders.length === 0">
                <td colspan="6" class="empty-cell">暂无待收款销售单</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <aside class="panel detail-panel sales-payment-detail">
        <p class="eyebrow">订单详情</p>
        <h3>{{ selectedOrder?.orderNo || '请选择销售单' }}</h3>

        <div v-if="selectedOrder" class="detail-stack">
          <article class="detail-card sales-payment-amount-card">
            <span>待收金额 (CNY)</span>
            <strong>{{ formatCurrency(remainingAmount) }}</strong>
            <p>{{ selectedOrder.customerName || '散客' }} / {{ selectedOrder.orderNo }}</p>
          </article>

          <article class="detail-card">
            <dl class="detail-list">
              <div>
                <dt>客户名称</dt>
                <dd>{{ selectedOrder.customerName || '散客' }}</dd>
              </div>
              <div>
                <dt>出库状态</dt>
                <dd>{{ saleShippingStatus(selectedOrder.status) }}</dd>
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
                <dt>待收金额</dt>
                <dd>{{ formatCurrency(remainingAmount) }}</dd>
              </div>
              <div>
                <dt>创建时间</dt>
                <dd>{{ formatDateTime(selectedOrder.createdAt) }}</dd>
              </div>
            </dl>
          </article>

          <article class="detail-card">
            <p class="eyebrow">登记收款</p>
            <div class="detail-stack">
              <label class="compact-field">
                <span>本次收款金额</span>
                <div class="amount-input-shell">
                  <input v-model="paymentForm.amount" class="table-input" type="number" min="0" step="0.01" />
                  <button type="button" class="ghost-action" @click="paymentForm.amount = remainingAmount.toFixed(2)">全额</button>
                </div>
              </label>
              <div class="compact-field">
                <span>收款方式</span>
                <div class="payment-method-grid">
                  <button
                    v-for="[value, label] in paymentMethods"
                    :key="value"
                    type="button"
                    class="payment-method-card"
                    :class="{ active: paymentForm.method === String(value) }"
                    @click="paymentForm.method = String(value)"
                  >
                    <strong>{{ label }}</strong>
                    <small>{{ value === METHOD_CASH ? '当面收现' : value === METHOD_WECHAT ? '扫码收款' : value === METHOD_ALIPAY ? '线上到账' : value === METHOD_BANK ? '对公转账' : '其他方式' }}</small>
                  </button>
                </div>
              </div>
              <label class="compact-field">
                <span>参考流水号</span>
                <input v-model="paymentForm.referenceNo" class="table-input" placeholder="可选" />
              </label>
              <div class="form-actions">
                <button type="button" :disabled="!canCollectPayment || submitting" @click="handleCollectPayment">
                  {{ submitting ? '提交中...' : '登记收款' }}
                </button>
              </div>
            </div>
          </article>

          <article class="detail-card">
            <p class="eyebrow">历史收款记录</p>
            <PageStatusBanner v-if="paymentsLoading" tone="info" title="正在加载" message="正在加载收款记录..." />
            <div v-else-if="payments.length" class="mini-list">
              <div v-for="payment in payments" :key="payment.id">
                <strong>{{ formatCurrency(payment.amount) }}</strong>
                <span>{{ formatDateTime(payment.createdAt) }} / {{ payment.referenceNo || '无参考号' }}</span>
              </div>
            </div>
            <PageEmptyState v-else title="暂无收款记录" message="当前销售单还没有收款记录。" />
          </article>
        </div>

        <PageEmptyState v-else title="暂无可处理订单" message="当前未找到待收款销售单。" />
      </aside>
    </section>
  </section>
</template>
