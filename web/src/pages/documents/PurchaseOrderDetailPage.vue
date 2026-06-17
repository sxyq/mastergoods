<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSession } from '@/app/stores/session'
import {
  fetchPurchaseOrder,
  fetchPurchaseReceiptsByOrder,
  type PurchaseOrder,
  type PurchaseReceipt,
} from '@/shared/api/client'
import { readQueryId } from '@/shared/utils/id'
import {
  SALE_DRAFT,
  formatCurrency,
  formatDateTime,
  purchaseOrderStatusLabel,
  purchasePaymentStatus,
  purchaseReceiptFlowStatus,
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
const pendingReceiptAmount = computed(() => order.value ? Math.max(order.value.totalAmount - order.value.receivedAmount, 0) : 0)
const pendingPayAmount = computed(() => order.value ? Math.max(order.value.totalAmount - order.value.paidAmount, 0) : 0)

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
</script>

<template>
  <section class="business-page">
    <section class="screen-hero">
      <div>
        <p class="eyebrow">采购详情 / Purchase Order Detail</p>
        <h2>采购单详情专页</h2>
        <p>对齐真实采购单、入库记录和金额进度，作为采购编辑、入库与退货的主入口。</p>
      </div>
      <div class="hero-actions">
        <button type="button" class="ghost-action" @click="router.push('/documents/purchases')">返回列表</button>
        <button
          type="button"
          class="ghost-action"
          :disabled="!canEditDraft || !order"
          @click="router.push({ path: '/documents/purchases/edit', query: { id: String(order?.id) } })"
        >
          编辑草稿
        </button>
      </div>
    </section>

    <p v-if="!isApiSource" class="form-error">当前是演示模式。这一页只在真实登录后读取采购单详情。</p>
    <p v-else-if="error" class="form-error">{{ error }}</p>
    <p v-else-if="loading" class="form-success">正在加载采购单详情...</p>

    <template v-if="order">
      <section class="metrics-grid compact">
        <article class="metric-card" data-tone="blue">
          <span>采购金额</span>
          <strong>{{ formatCurrency(order.totalAmount) }}</strong>
          <p>{{ order.items.length }} 行商品</p>
        </article>
        <article class="metric-card" data-tone="orange">
          <span>待入库金额</span>
          <strong>{{ formatCurrency(pendingReceiptAmount) }}</strong>
          <p>{{ purchaseReceiptStatus(order.totalAmount, order.receivedAmount, order.status) }}</p>
        </article>
        <article class="metric-card" data-tone="green">
          <span>待付款金额</span>
          <strong>{{ formatCurrency(pendingPayAmount) }}</strong>
          <p>{{ purchasePaymentStatus(order.totalAmount, order.paidAmount, order.status) }}</p>
        </article>
      </section>

      <section class="business-split">
        <article class="panel">
          <div class="panel-head">
            <div>
              <p class="eyebrow">采购主信息</p>
              <h3>{{ order.orderNo }}</h3>
            </div>
            <span class="session-source">
              {{ purchaseOrderStatusLabel(order.totalAmount, order.paidAmount, order.receivedAmount, order.status) }}
            </span>
          </div>

          <div class="detail-card">
            <dl class="detail-list">
              <div>
                <dt>供应商</dt>
                <dd>{{ order.supplierName || '未命名供应商' }}</dd>
              </div>
              <div>
                <dt>创建时间</dt>
                <dd>{{ formatDateTime(order.createdAt) }}</dd>
              </div>
              <div>
                <dt>更新时间</dt>
                <dd>{{ formatDateTime(order.updatedAt) }}</dd>
              </div>
              <div>
                <dt>入库进度</dt>
                <dd>{{ purchaseReceiptStatus(order.totalAmount, order.receivedAmount, order.status) }}</dd>
              </div>
              <div>
                <dt>付款进度</dt>
                <dd>{{ purchasePaymentStatus(order.totalAmount, order.paidAmount, order.status) }}</dd>
              </div>
              <div>
                <dt>备注</dt>
                <dd>{{ order.notes || '--' }}</dd>
              </div>
            </dl>
          </div>

          <div class="panel-head section-head">
            <div>
              <p class="eyebrow">商品明细</p>
              <h3>采购行项目</h3>
            </div>
          </div>
          <div class="table-shell">
            <table>
              <thead>
                <tr>
                  <th>编码</th>
                  <th>商品</th>
                  <th>数量</th>
                  <th>进货价</th>
                  <th>小计</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in order.items" :key="item.id || `${item.productCode}-${item.productName}`">
                  <td>{{ item.productCode || '--' }}</td>
                  <td>{{ item.productName || item.productCode || '未命名商品' }}</td>
                  <td>{{ item.quantity }}</td>
                  <td>{{ formatCurrency(item.unitCost) }}</td>
                  <td>{{ formatCurrency(item.amount) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>

        <aside class="panel detail-panel">
          <p class="eyebrow">后续动作</p>
          <h3>入库与退货入口</h3>

          <div class="detail-stack">
            <article class="detail-card">
              <div class="form-actions">
                <button type="button" :disabled="!order" @click="router.push({ path: '/documents/purchase-receipts', query: { orderId: String(order.id) } })">
                  去做入库
                </button>
                <button type="button" class="ghost-action" :disabled="!order" @click="router.push({ path: '/documents/purchase-returns', query: { orderId: String(order.id) } })">
                  去做退货
                </button>
                <button type="button" class="ghost-action" @click="router.push('/documents/pay-orders/detail')">
                  处理付款
                </button>
              </div>
            </article>

            <article class="detail-card">
              <p class="eyebrow">入库记录</p>
              <div v-if="receipts.length" class="mini-list">
                <div v-for="receipt in receipts" :key="receipt.id">
                  <strong>{{ receipt.receiptNo }}</strong>
                  <span>{{ formatCurrency(receipt.totalAmount) }} / {{ purchaseReceiptFlowStatus(receipt.status) }}</span>
                  <span>{{ formatDateTime(receipt.createdAt) }}</span>
                </div>
              </div>
              <p v-else class="muted">当前采购单还没有真实入库记录。</p>
            </article>

            <article class="detail-card">
              <p class="eyebrow">能力边界</p>
              <p class="muted">当前详情页已接真实采购单与入库记录；采购付款与退货提交仍以独立专页和后端能力为准。</p>
            </article>
          </div>
        </aside>
      </section>
    </template>

    <div v-else-if="!loading" class="empty-preview">
      <strong>未选择采购单</strong>
      <p>请从采购单列表进入详情页。</p>
    </div>
  </section>
</template>
