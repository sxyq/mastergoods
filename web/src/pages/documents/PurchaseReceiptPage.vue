<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSession } from '@/app/stores/session'
import {
  confirmPurchaseReceipt,
  createPurchaseReceipt,
  fetchPurchaseOrders,
  fetchPurchaseReceipts,
  fetchPurchaseReceiptsByOrder,
  type PurchaseOrder,
  type PurchaseReceipt,
  type PurchaseReceiptWritePayload,
} from '@/shared/api/client'
import { readQueryId, type EntityId } from '@/shared/utils/id'
import {
  formatCurrency,
  formatDateTime,
  purchaseReceiptFlowStatus,
  purchaseReceiptStatus,
} from '@/shared/utils/business'

const route = useRoute()
const router = useRouter()
const session = useSession()

const sourceOrders = ref<PurchaseOrder[]>([])
const receipts = ref<PurchaseReceipt[]>([])
const selectedOrderId = ref<EntityId | null>(null)
const selectedReceiptId = ref<EntityId | null>(null)
const loading = ref(false)
const submitting = ref(false)
const error = ref('')
const success = ref('')

const queryOrderId = computed(() => readQueryId(route.query.orderId))
const isApiSource = computed(() => session.source.value === 'api' && Boolean(session.token.value))
const canWrite = computed(() => session.hasAnyPermission(['purchase:write', 'inventory:write']))
const sourceOrderById = computed(() => new Map(sourceOrders.value.map((item) => [String(item.id), item] as const)))
const receiptById = computed(() => new Map(receipts.value.map((item) => [String(item.id), item] as const)))
const selectedOrder = computed(() => {
  if (selectedOrderId.value != null) {
    const matched = sourceOrderById.value.get(String(selectedOrderId.value))
    if (matched) return matched
  }
  return sourceOrders.value[0] ?? null
})
const selectedReceipt = computed(() => {
  if (selectedReceiptId.value != null) {
    const matched = receiptById.value.get(String(selectedReceiptId.value))
    if (matched) return matched
  }
  return receipts.value[0] ?? null
})

watch(
  [() => session.source.value, () => session.token.value, queryOrderId],
  async () => {
    if (!isApiSource.value || !session.token.value) {
      sourceOrders.value = []
      receipts.value = []
      error.value = ''
      return
    }
    await loadPage()
  },
  { immediate: true },
)

async function loadPage() {
  if (!session.token.value) return
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    const nextOrders = await fetchPurchaseOrders(session.token.value, { page: 0, size: 200 })
    sourceOrders.value = nextOrders
    selectedOrderId.value = queryOrderId.value ?? nextOrders[0]?.id ?? null

    if (selectedOrderId.value) {
      receipts.value = await fetchPurchaseReceiptsByOrder(session.token.value, selectedOrderId.value)
    } else {
      receipts.value = await fetchPurchaseReceipts(session.token.value, { page: 0, size: 200 })
    }
    selectedReceiptId.value = receipts.value[0]?.id ?? null
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : '采购入库页面加载失败'
  } finally {
    loading.value = false
  }
}

async function createReceiptFromOrder() {
  if (!session.token.value || !selectedOrder.value) return
  submitting.value = true
  error.value = ''
  success.value = ''
  try {
    const payload: PurchaseReceiptWritePayload = {
      purchaseOrderId: selectedOrder.value.id,
      supplierId: selectedOrder.value.supplierId,
      supplierName: selectedOrder.value.supplierName,
      items: selectedOrder.value.items.map((item) => ({
        productCode: item.productCode,
        productName: item.productName,
        quantity: item.quantity,
        unitCost: item.unitCost,
      })),
      notes: `由采购单 ${selectedOrder.value.orderNo} 生成入库单`,
    }
    const created = await createPurchaseReceipt(session.token.value, payload)
    success.value = `已生成入库单 ${created.receiptNo}`
    await loadPage()
    selectedReceiptId.value = created.id
  } catch (submitErr) {
    error.value = submitErr instanceof Error ? submitErr.message : '采购入库单创建失败'
  } finally {
    submitting.value = false
  }
}

async function confirmReceipt() {
  if (!session.token.value || !selectedReceipt.value) return
  submitting.value = true
  error.value = ''
  success.value = ''
  try {
    const updated = await confirmPurchaseReceipt(session.token.value, selectedReceipt.value.id)
    success.value = `入库单 ${updated.receiptNo} 已确认`
    await loadPage()
    selectedReceiptId.value = updated.id
  } catch (submitErr) {
    error.value = submitErr instanceof Error ? submitErr.message : '采购入库确认失败'
  } finally {
    submitting.value = false
  }
}

async function chooseSourceOrder(orderId: EntityId) {
  selectedOrderId.value = orderId
  selectedReceiptId.value = null
  if (!session.token.value) return
  loading.value = true
  error.value = ''
  try {
    receipts.value = await fetchPurchaseReceiptsByOrder(session.token.value, orderId)
    selectedReceiptId.value = receipts.value[0]?.id ?? null
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : '入库单切换失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="business-page">
    <section class="screen-hero">
      <div>
        <p class="eyebrow">采购入库 / Purchase Receipts</p>
        <h2>采购入库专页</h2>
        <p>以真实采购单为来源生成入库单，并调用 `/v2/purchase-receipts/{id}/confirm` 完成入库。</p>
      </div>
      <div class="hero-actions">
        <button type="button" class="ghost-action" @click="router.push('/documents/purchases')">返回采购列表</button>
      </div>
    </section>

    <p v-if="!isApiSource" class="form-error">当前是演示模式。这一页只在真实登录后生成与确认入库单。</p>
    <p v-else-if="error" class="form-error">{{ error }}</p>
    <p v-else-if="success" class="form-success">{{ success }}</p>
    <p v-if="loading" class="form-success">正在加载采购入库数据...</p>

    <section class="business-split">
      <article class="panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">来源采购单</p>
            <h3>{{ selectedOrder?.orderNo || '请选择采购单' }}</h3>
          </div>
          <button type="button" :disabled="!selectedOrder || submitting || !isApiSource || !canWrite" @click="createReceiptFromOrder">
            {{ submitting ? '处理中...' : '生成入库单' }}
          </button>
        </div>

        <div class="table-shell">
          <table>
            <thead>
              <tr>
                <th>采购单号</th>
                <th>供应商</th>
                <th>采购金额</th>
                <th>当前入库状态</th>
                <th>创建时间</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="order in sourceOrders"
                :key="order.id"
                :class="{ selected: order.id === selectedOrder?.id }"
                @click="chooseSourceOrder(order.id)"
              >
                <td>{{ order.orderNo }}</td>
                <td>{{ order.supplierName || '未命名供应商' }}</td>
                <td>{{ formatCurrency(order.totalAmount) }}</td>
                <td>{{ purchaseReceiptStatus(order.totalAmount, order.receivedAmount, order.status) }}</td>
                <td>{{ formatDateTime(order.createdAt) }}</td>
              </tr>
              <tr v-if="!loading && sourceOrders.length === 0">
                <td colspan="5" class="empty-cell">暂无可用采购单</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="selectedOrder" class="detail-card section-card">
          <p class="eyebrow">本次将写入的商品</p>
          <div class="mini-list">
            <div v-for="item in selectedOrder.items" :key="item.id || `${item.productCode}-${item.productName}`">
              <strong>{{ item.productName || item.productCode }}</strong>
              <span>{{ item.quantity }} x {{ formatCurrency(item.unitCost) }}</span>
            </div>
          </div>
        </div>
      </article>

      <aside class="panel detail-panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">入库单详情</p>
            <h3>{{ selectedReceipt?.receiptNo || '暂无入库单' }}</h3>
          </div>
          <button type="button" :disabled="!selectedReceipt || submitting || !isApiSource || !canWrite" @click="confirmReceipt">确认入库</button>
        </div>

        <div v-if="selectedReceipt" class="detail-stack">
          <article class="detail-card">
            <dl class="detail-list">
              <div>
                <dt>来源采购单</dt>
                <dd>{{ selectedReceipt.purchaseOrderId || '--' }}</dd>
              </div>
              <div>
                <dt>供应商</dt>
                <dd>{{ selectedReceipt.supplierName || '未命名供应商' }}</dd>
              </div>
              <div>
                <dt>入库金额</dt>
                <dd>{{ formatCurrency(selectedReceipt.totalAmount) }}</dd>
              </div>
              <div>
                <dt>状态</dt>
                <dd>{{ purchaseReceiptFlowStatus(selectedReceipt.status) }}</dd>
              </div>
            </dl>
          </article>

          <article class="detail-card">
            <p class="eyebrow">入库明细</p>
            <div class="mini-list">
              <div v-for="item in selectedReceipt.items" :key="item.id || `${item.productCode}-${item.productName}`">
                <strong>{{ item.productName || item.productCode }}</strong>
                <span>{{ item.quantity }} x {{ formatCurrency(item.unitCost) }}</span>
              </div>
            </div>
          </article>
        </div>

        <div v-else class="empty-preview">
          <strong>当前还没有入库单</strong>
          <p>先从左侧采购单生成真实入库单，再在这里确认入库。</p>
        </div>
      </aside>
    </section>
  </section>
</template>
