<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSession } from '@/app/stores/session'
import {
  confirmSaleOrder,
  createSaleOrderPayment,
  fetchSaleOrder,
  fetchSaleOrderPayments,
  type PaymentRecord,
  type SaleOrder,
} from '@/shared/api/client'
import { readQueryId } from '@/shared/utils/id'
import {
  METHOD_ALIPAY,
  METHOD_BANK,
  METHOD_CASH,
  METHOD_OTHER,
  METHOD_WECHAT,
  formatCurrency,
  formatDateTime,
  financeMethodLabel,
  salePaymentStatus,
  saleShippingStatus,
  SALE_CANCELLED,
  SALE_DRAFT,
} from '@/shared/utils/business'

const route = useRoute()
const router = useRouter()
const session = useSession()

const order = ref<SaleOrder | null>(null)
const payments = ref<PaymentRecord[]>([])
const loading = ref(false)
const submitting = ref(false)
const error = ref('')
const success = ref('')

const paymentForm = reactive({
  amount: '',
  method: String(METHOD_CASH),
  referenceNo: '',
})

const orderId = computed(() => readQueryId(route.query.id))
const isApiSource = computed(() => session.source.value === 'api' && Boolean(session.token.value))
const canWrite = computed(() => session.hasPermission(['sales:write']))
const remainingAmount = computed(() => {
  if (!order.value) return 0
  return Math.max(order.value.totalAmount - order.value.paidAmount, 0)
})
const canConfirm = computed(() => order.value?.status === SALE_DRAFT && canWrite.value)
const canCollectPayment = computed(() => {
  return Boolean(order.value) && order.value?.status !== SALE_CANCELLED && remainingAmount.value > 0
})
const paymentMethods = [
  [METHOD_CASH, '现金'],
  [METHOD_WECHAT, '微信'],
  [METHOD_ALIPAY, '支付宝'],
  [METHOD_BANK, '银行卡'],
  [METHOD_OTHER, '其他'],
]

watch(
  [() => session.source.value, () => session.token.value, orderId],
  async () => {
    if (!isApiSource.value || !session.token.value || !orderId.value) {
      order.value = null
      payments.value = []
      error.value = ''
      return
    }
    await loadDetail()
  },
  { immediate: true },
)

async function loadDetail() {
  if (!session.token.value || !orderId.value) return
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    const [nextOrder, nextPayments] = await Promise.all([
      fetchSaleOrder(session.token.value, orderId.value),
      fetchSaleOrderPayments(session.token.value, orderId.value),
    ])
    order.value = nextOrder
    payments.value = nextPayments
    paymentForm.amount = remainingAmount.value > 0 ? remainingAmount.value.toFixed(2) : ''
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : '销售单详情加载失败'
  } finally {
    loading.value = false
  }
}

async function handleConfirm() {
  if (!session.token.value || !order.value) return
  submitting.value = true
  error.value = ''
  success.value = ''
  try {
    order.value = await confirmSaleOrder(session.token.value, order.value.id, {
      notes: order.value.notes ?? null,
    })
    success.value = '销售单已确认'
  } catch (submitErr) {
    error.value = submitErr instanceof Error ? submitErr.message : '销售单确认失败'
  } finally {
    submitting.value = false
  }
}

async function handleCollectPayment() {
  if (!session.token.value || !order.value) return
  submitting.value = true
  error.value = ''
  success.value = ''
  try {
    await createSaleOrderPayment(session.token.value, order.value.id, {
      amount: Number(paymentForm.amount || 0),
      method: Number(paymentForm.method),
      referenceNo: paymentForm.referenceNo.trim() || null,
    })
    success.value = '收款记录已写入'
    await loadDetail()
  } catch (submitErr) {
    error.value = submitErr instanceof Error ? submitErr.message : '销售收款失败'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="business-page">
    <section class="screen-hero">
      <div>
        <p class="eyebrow">销售详情 / Sale Order Detail</p>
        <h2>销售单详情专页</h2>
        <p>对齐安卓端销售详情与收款流程，可确认草稿订单并写入真实收款记录。</p>
      </div>
      <div class="hero-actions">
        <button type="button" class="ghost-action" @click="router.push('/documents/sales')">返回列表</button>
        <button
          type="button"
          class="ghost-action"
          :disabled="!order || order.status !== SALE_DRAFT || !canWrite"
          @click="router.push({ path: '/documents/sales/edit', query: { id: String(order?.id) } })"
        >
          编辑草稿
        </button>
        <button
          v-if="order"
          type="button"
          class="ghost-action"
          @click="router.push({ path: '/documents/sales/payment', query: { id: String(order.id) } })"
        >
          专页收款
        </button>
      </div>
    </section>

    <p v-if="!isApiSource" class="form-error">当前是演示模式。这一页只在真实登录后读取销售详情。</p>
    <p v-else-if="error" class="form-error">{{ error }}</p>
    <p v-else-if="success" class="form-success">{{ success }}</p>
    <p v-if="loading" class="form-success">正在加载销售单详情...</p>

    <section v-if="order" class="business-split">
      <article class="panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">订单主信息</p>
            <h3>{{ order.orderNo }}</h3>
          </div>
          <span class="session-source">{{ saleShippingStatus(order.status) }} / {{ salePaymentStatus(order.totalAmount, order.paidAmount, order.status) }}</span>
        </div>

        <div class="detail-card">
          <dl class="detail-list">
            <div>
              <dt>客户名称</dt>
              <dd>{{ order.customerName || '散客' }}</dd>
            </div>
            <div>
              <dt>创建时间</dt>
              <dd>{{ formatDateTime(order.createdAt) }}</dd>
            </div>
            <div>
              <dt>商品行数</dt>
              <dd>{{ order.items.length }}</dd>
            </div>
            <div>
              <dt>订单金额</dt>
              <dd>{{ formatCurrency(order.totalAmount) }}</dd>
            </div>
            <div>
              <dt>已付金额</dt>
              <dd>{{ formatCurrency(order.paidAmount) }}</dd>
            </div>
            <div>
              <dt>待收金额</dt>
              <dd>{{ formatCurrency(remainingAmount) }}</dd>
            </div>
          </dl>
        </div>

        <div class="panel-head section-head">
          <div>
            <p class="eyebrow">商品明细</p>
            <h3>订单行项目</h3>
          </div>
        </div>
        <div class="table-shell">
          <table>
            <thead>
              <tr>
                <th>商品</th>
                <th>数量</th>
                <th>单价</th>
                <th>小计</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in order.items" :key="item.id || `${item.productId}-${item.productName}`">
                <td>{{ item.productName || item.productCode }}</td>
                <td>{{ item.quantity }}</td>
                <td>{{ formatCurrency(item.unitPrice) }}</td>
                <td>{{ formatCurrency(item.amount) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="form-actions">
          <button type="button" :disabled="!canConfirm || submitting" @click="handleConfirm">{{ submitting ? '处理中...' : '确认销售单' }}</button>
        </div>
      </article>

      <aside class="panel detail-panel">
        <p class="eyebrow">收款管理</p>
        <h3>销售收款</h3>

        <div class="detail-stack">
          <article class="detail-card">
            <label class="compact-field">
              <span>本次收款金额</span>
              <input v-model="paymentForm.amount" class="table-input" type="number" min="0" step="0.01" />
            </label>
            <label class="compact-field">
              <span>收款方式</span>
              <select v-model="paymentForm.method">
                <option v-for="[value, label] in paymentMethods" :key="value" :value="String(value)">{{ label }}</option>
              </select>
            </label>
            <label class="compact-field">
              <span>参考流水号</span>
              <input v-model="paymentForm.referenceNo" class="table-input" placeholder="可选" />
            </label>
            <div class="form-actions">
              <button type="button" :disabled="!canCollectPayment || submitting" @click="handleCollectPayment">登记收款</button>
              <button
                type="button"
                class="ghost-action"
                :disabled="!order"
                @click="router.push({ path: '/documents/sales-returns', query: { orderId: String(order?.id) } })"
              >
                去做退货
              </button>
            </div>
          </article>

          <article class="detail-card">
            <p class="eyebrow">收款记录</p>
            <div v-if="payments.length" class="mini-list">
              <div v-for="payment in payments" :key="payment.id">
                <strong>{{ formatCurrency(payment.amount) }}</strong>
                <span>{{ financeMethodLabel(payment.method) }} / {{ formatDateTime(payment.createdAt) }}</span>
              </div>
            </div>
            <p v-else class="muted">当前还没有收款记录。</p>
          </article>
        </div>
      </aside>
    </section>

    <div v-else-if="!loading" class="empty-preview">
      <strong>未选择销售单</strong>
      <p>请从销售单列表进入详情页。</p>
    </div>
  </section>
</template>
