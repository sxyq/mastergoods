<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSession } from '@/app/stores/session'
import {
  fetchPurchaseOrder,
  fetchPurchaseReceiptsByOrder,
  type PurchaseOrder,
  type PurchaseOrderItem,
  type PurchaseReceipt,
} from '@/shared/api/client'
import { readQueryId } from '@/shared/utils/id'
import {
  SALE_DRAFT,
  formatCurrency,
  formatDateTime,
  purchasePaymentStatus,
  purchaseReceiptStatus,
} from '@/shared/utils/business'

const route = useRoute()
const router = useRouter()
const session = useSession()

const order = ref<PurchaseOrder | null>(null)
const receipts = ref<PurchaseReceipt[]>([])
const loading = ref(false)
const error = ref('')

const orderId = computed(() => readQueryId(route.query.id))
const isApiSource = computed(() => session.source.value === 'api' && Boolean(session.token.value))
const canWrite = computed(() => session.hasPermission(['purchase:write']))
const canEditDraft = computed(() => Boolean(order.value) && order.value?.status === SALE_DRAFT && canWrite.value)
const pendingPayAmount = computed(() => order.value ? Math.max(order.value.totalAmount - order.value.paidAmount, 0) : 0)
const isReceiptDone = computed(() => Boolean(order.value) && order.value!.receivedAmount >= order.value!.totalAmount && order.value!.totalAmount > 0)
const isPaymentDone = computed(() => Boolean(order.value) && order.value!.paidAmount >= order.value!.totalAmount && order.value!.totalAmount > 0)

const progressSteps = computed(() => {
  const currentOrder = order.value
  return [
    { label: '下达订单', time: currentOrder ? formatDateTime(currentOrder.createdAt).slice(5, 16) : '-', done: true, current: false, icon: 'check' },
    { label: '审核通过', time: currentOrder?.status === SALE_DRAFT ? '待审核' : '已确认', done: currentOrder?.status !== SALE_DRAFT, current: currentOrder?.status === SALE_DRAFT, icon: 'check' },
    { label: purchaseReceiptStatus(currentOrder?.totalAmount ?? 0, currentOrder?.receivedAmount ?? 0, currentOrder?.status ?? 0), time: isReceiptDone.value ? '已完成' : '处理中', done: isReceiptDone.value, current: !isReceiptDone.value, icon: 'inventory_2' },
    { label: purchasePaymentStatus(currentOrder?.totalAmount ?? 0, currentOrder?.paidAmount ?? 0, currentOrder?.status ?? 0), time: isPaymentDone.value ? '已完成' : '待处理', done: isPaymentDone.value, current: !isPaymentDone.value, icon: 'payments' },
    { label: '完成', time: isReceiptDone.value && isPaymentDone.value ? '已完成' : '等待', done: isReceiptDone.value && isPaymentDone.value, current: false, icon: 'flag' },
  ]
})

const receivedQuantityMap = computed(() => {
  const map = new Map<string, number>()
  for (const receipt of receipts.value) {
    for (const receiptItem of receipt.items) {
      const keys = new Set<string>()
      if (receiptItem.productId) keys.add(String(receiptItem.productId))
      if (receiptItem.productCode) keys.add(receiptItem.productCode)
      if (receiptItem.productName) keys.add(receiptItem.productName)
      for (const key of keys) {
        map.set(key, (map.get(key) ?? 0) + receiptItem.quantity)
      }
    }
  }
  return map
})

watch(
  [() => session.source.value, () => session.token.value, orderId],
  async () => {
    if (!isApiSource.value || !session.token.value || !orderId.value) {
      order.value = null
      receipts.value = []
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
  try {
    const [nextOrder, nextReceipts] = await Promise.all([
      fetchPurchaseOrder(session.token.value, orderId.value),
      fetchPurchaseReceiptsByOrder(session.token.value, orderId.value),
    ])
    order.value = nextOrder
    receipts.value = nextReceipts
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : '采购单详情加载失败'
  } finally {
    loading.value = false
  }
}

function receivedQuantity(item: PurchaseOrderItem) {
  const key = item.productId ? String(item.productId) : item.productCode || item.productName || ''
  return key ? (receivedQuantityMap.value.get(key) ?? 0) : 0
}
</script>

<template>
  <section class="pc-detail-page purchase-detail-page">
    <header class="pc-list-titlebar">
      <div class="pc-breadcrumb">
        <span>采购管理</span>
        <span class="material-symbols-outlined">chevron_right</span>
        <span>采购单列表</span>
        <span class="material-symbols-outlined">chevron_right</span>
        <h1>详情</h1>
      </div>
      <div class="pc-title-actions">
        <button type="button" class="pc-secondary-action">
          <span class="material-symbols-outlined">print</span>
          打印
        </button>
        <button type="button" class="pc-secondary-action">
          <span class="material-symbols-outlined">download</span>
          导出
        </button>
        <button
          type="button"
          class="pc-secondary-action"
          :disabled="!canEditDraft || !order"
          @click="router.push({ path: '/documents/purchases/edit', query: { id: String(order?.id) } })"
        >
          编辑
        </button>
        <button
          type="button"
          class="pc-dark-action"
          :disabled="!order || !isApiSource"
          @click="router.push({ path: '/documents/purchase-receipts', query: { orderId: String(order?.id) } })"
        >
          生成入库单
        </button>
      </div>
    </header>

    <p v-if="!isApiSource" class="form-error">当前是演示模式。这一页只在真实登录后读取采购单详情。</p>
    <p v-else-if="error" class="form-error">{{ error }}</p>
    <p v-else-if="loading" class="form-success">正在加载采购单详情...</p>

    <template v-if="order">
      <section class="purchase-detail-head">
        <div>
          <h2>{{ order.orderNo }}</h2>
          <span class="pc-status-chip" :data-tone="isReceiptDone ? 'done' : 'running'">
            {{ purchaseReceiptStatus(order.totalAmount, order.receivedAmount, order.status) }}
          </span>
          <span class="pc-status-chip" :data-tone="isPaymentDone ? 'done' : 'running'">
            {{ purchasePaymentStatus(order.totalAmount, order.paidAmount, order.status) }}
          </span>
          <p>创建时间: {{ formatDateTime(order.createdAt) }}</p>
        </div>
      </section>

      <section class="purchase-bento-grid">
        <article class="pc-detail-card purchase-base-card">
          <div class="pc-section-title">
            <span class="material-symbols-outlined">info</span>
            <h2>基础信息</h2>
          </div>
          <div class="pc-info-grid">
            <div><span>供应商名称</span><strong>{{ order.supplierName || '未命名供应商' }}</strong></div>
            <div><span>联系人</span><strong>未登记</strong></div>
            <div><span>采购员</span><strong>{{ session.member.value.name }}</strong></div>
            <div><span>预计到货日期</span><strong>待确认</strong></div>
            <div><span>收货仓库</span><strong>总仓 - 深圳</strong></div>
            <div><span>备注</span><strong>{{ order.notes || '-' }}</strong></div>
          </div>
        </article>

        <article class="pc-detail-card purchase-settlement-card">
          <div class="pc-section-title">
            <span class="material-symbols-outlined">account_balance_wallet</span>
            <h2>结算信息</h2>
          </div>
          <dl class="pc-finance-list">
            <div><dt>应付总额</dt><dd>{{ formatCurrency(order.totalAmount) }}</dd></div>
            <div><dt>已付金额</dt><dd class="success">{{ formatCurrency(order.paidAmount) }}</dd></div>
            <div class="total"><dt>待付余额</dt><dd class="danger">{{ formatCurrency(pendingPayAmount) }}</dd></div>
          </dl>
          <button type="button" class="pc-secondary-action" @click="router.push('/documents/pay-orders/detail')">查看付款流水</button>
        </article>

        <article class="pc-detail-card purchase-progress-card">
          <h2>采购进度</h2>
          <div class="pc-progress-line purchase-progress-line">
            <article v-for="step in progressSteps" :key="step.label" :class="{ done: step.done, current: step.current }">
              <div><span class="material-symbols-outlined">{{ step.done ? 'check' : step.icon }}</span></div>
              <strong>{{ step.label }}</strong>
              <small>{{ step.time }}</small>
            </article>
          </div>
        </article>

        <article class="pc-detail-card pc-detail-table-card purchase-lines-card">
          <div class="pc-section-title">
            <span class="material-symbols-outlined">list_alt</span>
            <h2>采购明细</h2>
            <small>共 {{ order.items.length }} 项</small>
          </div>
          <div class="pc-table-scroll">
            <table class="pc-data-table">
              <thead>
                <tr>
                  <th>商品信息</th>
                  <th class="align-right">单价</th>
                  <th class="align-center">采购数</th>
                  <th class="align-center">已入库</th>
                  <th class="align-right">小计</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in order.items" :key="item.id || `${item.productCode}-${item.productName}`">
                  <td>
                    <div class="purchase-product-cell">
                      <span class="material-symbols-outlined">inventory_2</span>
                      <div>
                        <strong>{{ item.productName || item.productCode || '未命名商品' }}</strong>
                        <small>SKU: {{ item.productCode || '-' }}</small>
                      </div>
                    </div>
                  </td>
                  <td class="align-right">{{ formatCurrency(item.unitCost) }}</td>
                  <td class="align-center amount-strong">{{ item.quantity }}</td>
                  <td class="align-center success-text">{{ receivedQuantity(item) }}</td>
                  <td class="align-right amount-strong">{{ formatCurrency(item.amount) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>

        <article class="pc-detail-card purchase-runtrace-card">
          <div class="pc-section-title">
            <span class="material-symbols-outlined">memory</span>
            <h2>AI RunTrace 溯源</h2>
            <em>Smart Track</em>
          </div>
          <div class="purchase-trace-list">
            <article>
              <span></span>
              <small>需求来源证明</small>
              <div>
                <span class="material-symbols-outlined">description</span>
                <p><strong>对应预测需求单 #YQ-001</strong>AI 预测近期销量将提升，建议补足核心件库存。</p>
              </div>
            </article>
            <article>
              <span></span>
              <small>系统库存映射</small>
              <div>
                <span class="material-symbols-outlined">schema</span>
                <p><strong>关联 SKU 库存映射</strong>入库时自动衔接商品主数据与库存流水。</p>
              </div>
            </article>
            <article class="pending">
              <span></span>
              <small>待执行</small>
              <div>
                <p>等待入库单生成后捕获物流凭证...</p>
              </div>
            </article>
          </div>
        </article>
      </section>
    </template>

    <div v-else-if="!loading" class="empty-preview">
      <strong>未选择采购单</strong>
      <p>请从采购单列表进入详情页。</p>
    </div>
  </section>
</template>
