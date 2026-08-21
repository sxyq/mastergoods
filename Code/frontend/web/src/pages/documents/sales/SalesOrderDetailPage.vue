<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSession } from '@/app/stores/session'
import {
  cancelSaleOrder,
  confirmSaleOrder,
  createSaleOrderPayment,
  fetchSaleOrder,
  fetchSaleOrderPayments,
  fetchSaleOrderReceiptPdf,
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
  saleOrderStatusLabel,
  salePaymentStatus,
  saleShippingStatus,
  SALE_CANCELLED,
  SALE_COMPLETED,
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
const receiptExporting = ref(false)

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
const subtotalAmount = computed(() => order.value?.items.reduce((sum, item) => sum + item.amount, 0) ?? 0)
const orderStatus = computed(() => order.value?.status ?? SALE_DRAFT)
const paidOff = computed(() => order.value ? order.value.paidAmount >= order.value.totalAmount && order.value.totalAmount > 0 : false)
const canConfirm = computed(() => order.value?.status === SALE_DRAFT && canWrite.value)
const canCancel = computed(() => Boolean(order.value) && order.value?.status !== SALE_CANCELLED && canWrite.value)
const canCollectPayment = computed(() => {
  return Boolean(order.value) && order.value?.status !== SALE_CANCELLED && remainingAmount.value > 0
})
const orderStatusLabel = computed(() => order.value ? saleOrderStatusLabel(order.value.status) : '-')
const shippingStatusLabel = computed(() => order.value ? saleShippingStatus(order.value.status) : '-')
const paymentStatusLabel = computed(() => order.value ? salePaymentStatus(order.value.totalAmount, order.value.paidAmount, order.value.status) : '-')
const recentPayments = computed(() => payments.value.slice(0, 3))
const paymentMethods = [
  [METHOD_CASH, '现金'],
  [METHOD_WECHAT, '微信'],
  [METHOD_ALIPAY, '支付宝'],
  [METHOD_BANK, '银行卡'],
  [METHOD_OTHER, '其他'],
]

const progressSteps = computed(() => {
  const current = orderStatus.value
  return [
    { label: '下单', icon: 'shopping_cart', done: true, time: order.value ? formatDateTime(order.value.createdAt).slice(5) : '-' },
    { label: '审核', icon: 'fact_check', done: current !== SALE_DRAFT && current !== SALE_CANCELLED, time: current !== SALE_DRAFT ? '已确认' : '待处理' },
    { label: '出库', icon: 'inventory_2', done: current === SALE_COMPLETED, time: current === SALE_COMPLETED ? '已出库' : '待出库' },
    { label: '收款', icon: 'payments', done: paidOff.value, time: paidOff.value ? '已结清' : '待收款' },
    { label: '完成', icon: 'done_all', done: current === SALE_COMPLETED && paidOff.value, time: current === SALE_COMPLETED && paidOff.value ? '完成' : '进行中' },
  ]
})

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

async function handleCancel() {
  if (!session.token.value || !order.value) return
  submitting.value = true
  error.value = ''
  success.value = ''
  try {
    order.value = await cancelSaleOrder(session.token.value, order.value.id)
    success.value = '销售单已作废'
  } catch (submitErr) {
    error.value = submitErr instanceof Error ? submitErr.message : '销售单作废失败'
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

async function printReceipt() {
  if (!order.value || !session.token.value || receiptExporting.value) return
  receiptExporting.value = true
  const printFrame = document.createElement('iframe')
  printFrame.setAttribute('aria-hidden', 'true')
  printFrame.style.position = 'fixed'
  printFrame.style.width = '1px'
  printFrame.style.height = '1px'
  printFrame.style.opacity = '0'
  printFrame.style.pointerEvents = 'none'
  document.body.appendChild(printFrame)
  let pdfUrl: string | null = null
  try {
    const blob = await fetchSaleOrderReceiptPdf(session.token.value, order.value.id)
    pdfUrl = URL.createObjectURL(blob)
    printFrame.addEventListener('load', () => {
      printFrame.contentWindow?.focus()
      printFrame.contentWindow?.print()
      window.setTimeout(() => {
        printFrame.remove()
        if (pdfUrl) URL.revokeObjectURL(pdfUrl)
      }, 60_000)
    }, { once: true })
    printFrame.src = pdfUrl
    success.value = '服务端小票 PDF 已打开打印面板'
  } catch (printError) {
    printFrame.remove()
    if (pdfUrl) URL.revokeObjectURL(pdfUrl)
    error.value = printError instanceof Error ? printError.message : '小票 PDF 打印失败'
  } finally {
    receiptExporting.value = false
  }
}

async function downloadReceiptPdf() {
  if (!order.value || !session.token.value || receiptExporting.value) return
  receiptExporting.value = true
  try {
    const blob = await fetchSaleOrderReceiptPdf(session.token.value, order.value.id)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `sale-receipt-${order.value.orderNo || order.value.id}.pdf`
    link.click()
    window.setTimeout(() => URL.revokeObjectURL(url), 0)
    success.value = '服务端小票 PDF 已下载'
  } catch (downloadError) {
    error.value = downloadError instanceof Error ? downloadError.message : '小票 PDF 下载失败'
  } finally {
    receiptExporting.value = false
  }
}

</script>

<template>
  <section class="pc-detail-page sales-detail-page">
    <header class="pc-list-titlebar">
      <div class="pc-breadcrumb">
        <span>销售管理</span>
        <span class="material-symbols-outlined">chevron_right</span>
        <span>销售单列表</span>
        <span class="material-symbols-outlined">chevron_right</span>
        <h1>销售单 {{ order?.orderNo || '' }}</h1>
      </div>
      <div class="pc-title-actions">
        <button type="button" class="pc-secondary-action" @click="router.push('/documents/sales')">
          <span class="material-symbols-outlined">arrow_back</span>
          返回
        </button>
        <button type="button" class="pc-secondary-action" :disabled="!order || receiptExporting" @click="printReceipt">
          <span class="material-symbols-outlined">print</span>
          打印
        </button>
        <button type="button" class="pc-secondary-action" :disabled="!order || receiptExporting" @click="downloadReceiptPdf">
          <span class="material-symbols-outlined">download</span>
          下载 PDF
        </button>
        <button
          v-if="order"
          type="button"
          class="pc-secondary-action"
          :disabled="order.status !== SALE_DRAFT || !canWrite"
          @click="router.push({ path: '/documents/sales/edit', query: { id: String(order.id) } })"
        >
          编辑
        </button>
        <button type="button" class="pc-dark-action" :disabled="!canConfirm || submitting" @click="handleConfirm">确认</button>
        <button type="button" class="pc-danger-action" :disabled="!canCancel || submitting" @click="handleCancel">作废</button>
      </div>
    </header>

    <p v-if="!isApiSource" class="form-error">当前是演示模式。这一页只在真实登录后读取销售详情。</p>
    <p v-else-if="error" class="form-error">{{ error }}</p>
    <p v-else-if="success" class="form-success">{{ success }}</p>
    <p v-if="loading" class="form-success">正在加载销售单详情...</p>

    <section v-if="order" class="pc-detail-grid">
      <main class="pc-detail-main">
        <section class="pc-detail-hero-card">
          <div>
            <span class="pc-status-chip" :data-tone="order.status === SALE_COMPLETED ? 'done' : order.status === SALE_CANCELLED ? 'cancelled' : order.status === SALE_DRAFT ? 'draft' : 'running'">
              {{ orderStatusLabel }}
            </span>
            <h2>{{ order.orderNo }}</h2>
            <p>{{ order.notes || '暂无单据备注' }}</p>
          </div>
          <div class="pc-detail-amount">
            <span>应收总额</span>
            <strong>{{ formatCurrency(order.totalAmount) }}</strong>
            <small>{{ shippingStatusLabel }} / {{ paymentStatusLabel }}</small>
          </div>
        </section>

        <section class="pc-detail-card">
          <div class="pc-section-title">
            <h2>客户与单据信息</h2>
          </div>
          <div class="pc-info-grid">
            <div><span>客户名称</span><strong>{{ order.customerName || '散客' }}</strong></div>
            <div><span>联系人</span><strong>未登记</strong></div>
            <div><span>销售员</span><strong>{{ session.member.value.name }}</strong></div>
            <div><span>创建时间</span><strong>{{ formatDateTime(order.createdAt) }}</strong></div>
            <div><span>结算方式</span><strong>按单结算</strong></div>
            <div><span>更新时间</span><strong>{{ formatDateTime(order.updatedAt) }}</strong></div>
          </div>
        </section>

        <section class="pc-detail-card">
          <div class="pc-section-title">
            <span class="material-symbols-outlined">timeline</span>
            <h2>业务进度</h2>
          </div>
          <div class="pc-progress-line">
            <article v-for="step in progressSteps" :key="step.label" :class="{ done: step.done }">
              <div><span class="material-symbols-outlined">{{ step.icon }}</span></div>
              <strong>{{ step.label }}</strong>
              <small>{{ step.time }}</small>
            </article>
          </div>
        </section>

        <section class="pc-detail-card pc-detail-table-card">
          <div class="pc-section-title">
            <span class="material-symbols-outlined">list_alt</span>
            <h2>商品明细</h2>
          </div>
          <div class="pc-table-scroll">
            <table class="pc-data-table">
              <thead>
                <tr>
                  <th>SKU / 商品名称</th>
                  <th>规格型号</th>
                  <th class="align-right">单价 (¥)</th>
                  <th class="align-right">数量</th>
                  <th class="align-right">折扣</th>
                  <th class="align-right">小计 (¥)</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in order.items" :key="item.id || `${item.productId}-${item.productName}`">
                  <td>
                    <div class="pc-doc-cell">
                      <strong>{{ item.productName || item.productCode }}</strong>
                      <small>SKU: {{ item.productCode }}</small>
                    </div>
                  </td>
                  <td class="muted">-</td>
                  <td class="align-right">{{ formatCurrency(item.unitPrice) }}</td>
                  <td class="align-right">{{ item.quantity }}</td>
                  <td class="align-right">-</td>
                  <td class="align-right amount-strong">{{ formatCurrency(item.amount) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </main>

      <aside class="pc-detail-side">
        <section class="pc-detail-card">
          <div class="pc-section-title">
            <span class="material-symbols-outlined">account_balance_wallet</span>
            <h2>财务汇总</h2>
          </div>
          <dl class="pc-finance-list">
            <div><dt>商品总额</dt><dd>{{ formatCurrency(subtotalAmount) }}</dd></div>
            <div><dt>整单折扣</dt><dd class="danger">- {{ formatCurrency(order.discountAmount) }}</dd></div>
            <div><dt>已收金额</dt><dd>{{ formatCurrency(order.paidAmount) }}</dd></div>
            <div class="total"><dt>待收金额</dt><dd>{{ formatCurrency(remainingAmount) }}</dd></div>
          </dl>
          <div class="pc-payment-box">
            <label class="pc-field">
              <span>本次收款金额</span>
              <input v-model="paymentForm.amount" type="number" min="0" step="0.01" />
            </label>
            <label class="pc-field">
              <span>收款方式</span>
              <select v-model="paymentForm.method">
                <option v-for="[value, label] in paymentMethods" :key="value" :value="String(value)">{{ label }}</option>
              </select>
            </label>
            <label class="pc-field">
              <span>参考流水号</span>
              <input v-model="paymentForm.referenceNo" placeholder="可选" />
            </label>
            <button type="button" class="pc-dark-action" :disabled="!canCollectPayment || submitting" @click="handleCollectPayment">登记收款</button>
          </div>
        </section>

        <section class="pc-detail-card">
          <div class="pc-section-title">
            <span class="material-symbols-outlined">local_shipping</span>
            <h2>物流配送</h2>
          </div>
          <div class="pc-logistics-list">
            <div><span></span><p>当前状态：{{ shippingStatusLabel }}</p><small>{{ formatDateTime(order.updatedAt) }}</small></div>
            <div><span></span><p>收款状态：{{ paymentStatusLabel }}</p><small>{{ payments.length }} 条收款记录</small></div>
          </div>
        </section>

        <section class="pc-detail-card pc-runtrace-card">
          <div class="pc-section-title">
            <span class="material-symbols-outlined">hub</span>
            <h2>AI RunTrace 溯源</h2>
          </div>
          <p>智能系统已关联该销售单的收款、出库与库存影响记录。</p>
          <ul>
            <li v-for="payment in recentPayments" :key="payment.id">
              <span class="material-symbols-outlined">account_balance</span>
              <div>
                <small>对应资金流水</small>
                <strong>{{ financeMethodLabel(payment.method) }} · {{ formatCurrency(payment.amount) }}</strong>
              </div>
            </li>
            <li>
              <span class="material-symbols-outlined">inventory</span>
              <div>
                <small>出库状态</small>
                <strong>{{ shippingStatusLabel }}</strong>
              </div>
            </li>
          </ul>
        </section>
      </aside>
    </section>

    <div v-else-if="!loading" class="empty-preview">
      <strong>未选择销售单</strong>
      <p>请从销售单列表进入详情页。</p>
    </div>

  </section>
</template>
