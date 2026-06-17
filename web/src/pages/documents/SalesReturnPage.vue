<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useSession } from '@/app/stores/session'
import {
  addSalesReturnRefund,
  cancelSalesReturn,
  confirmSalesReturn,
  createSalesReturn,
  fetchSaleOrder,
  fetchSaleOrders,
  fetchSalesReturn,
  fetchSalesReturns,
  fetchSalesReturnsByOrder,
  updateSalesReturnDraft,
  type SaleOrder,
  type SalesReturn,
  type SalesReturnCreatePayload,
} from '@/shared/api/client'
import { readQueryId, sameEntityId, type EntityId } from '@/shared/utils/id'
import {
  METHOD_ALIPAY,
  METHOD_BANK,
  METHOD_CASH,
  METHOD_OTHER,
  METHOD_WECHAT,
  SALES_RETURN_CANCELLED,
  SALES_RETURN_DRAFT,
  formatCurrency,
  formatDateTime,
  salesReturnRefundStatus,
  salesReturnStatusLabel,
} from '@/shared/utils/business'

type PageMode = 'manage' | 'create'

interface ReturnDraftItem {
  productId: number | null
  productName: string
  productCode: string
  quantity: string
  unitPrice: string
  maxQuantity: number
}

const route = useRoute()
const session = useSession()

const mode = ref<PageMode>('manage')
const returns = ref<SalesReturn[]>([])
const sourceOrders = ref<SaleOrder[]>([])
const selectedReturnId = ref<EntityId | null>(null)
const createOrderId = ref<EntityId | null>(null)
const loading = ref(false)
const sourceLoading = ref(false)
const submitting = ref(false)
const error = ref('')
const success = ref('')
const keyword = ref('')
const statusFilter = ref('all')
const orderFilterId = ref<EntityId | null>(null)
const noteDraft = ref('')
const initializedFromQuery = ref(false)

const refundForm = reactive({
  amount: '',
  method: String(METHOD_CASH),
  referenceNo: '',
})

const createForm = reactive({
  notes: '',
})
const createItems = ref<ReturnDraftItem[]>([])

const queryReturnId = computed(() => readQueryId(route.query.id))
const queryOrderId = computed(() => readQueryId(route.query.orderId))
const isApiSource = computed(() => session.source.value === 'api' && Boolean(session.token.value))
const canWrite = computed(() => session.hasPermission(['sales:write']))
const selectedReturn = computed(() => returns.value.find((item) => sameEntityId(item.id, selectedReturnId.value)) ?? null)
const selectedSourceOrder = computed(() => sourceOrders.value.find((item) => sameEntityId(item.id, createOrderId.value)) ?? null)
const remainingRefund = computed(() => {
  if (!selectedReturn.value) return 0
  return Math.max(selectedReturn.value.totalAmount - selectedReturn.value.refundAmount, 0)
})
const totalReturnAmount = computed(() => returns.value.reduce((sum, item) => sum + item.totalAmount, 0))
const totalRefundAmount = computed(() => returns.value.reduce((sum, item) => sum + item.refundAmount, 0))
const canEditDraft = computed(() => {
  const current = selectedReturn.value
  return Boolean(current && current.status === SALES_RETURN_DRAFT && canWrite.value)
})
const canRefund = computed(() => {
  const current = selectedReturn.value
  return Boolean(current && current.status !== SALES_RETURN_CANCELLED && remainingRefund.value > 0 && canWrite.value)
})
const canCancel = computed(() => {
  const current = selectedReturn.value
  return Boolean(current && current.status !== SALES_RETURN_CANCELLED && canWrite.value)
})
const createPayload = computed<SalesReturnCreatePayload | null>(() => {
  if (!selectedSourceOrder.value) return null
  const items = createItems.value
    .map((item) => ({
      productId: item.productId,
      quantity: Number(item.quantity || 0),
      unitPrice: Number(item.unitPrice || 0),
    }))
    .filter((item) => item.quantity > 0)
  if (items.length === 0) return null
  return {
    originalOrderId: selectedSourceOrder.value.id,
    customerId: selectedSourceOrder.value.customerId,
    customerName: selectedSourceOrder.value.customerName,
    items,
    notes: createForm.notes.trim() || null,
  }
})

const paymentMethods = [
  [METHOD_CASH, '现金'],
  [METHOD_WECHAT, '微信'],
  [METHOD_ALIPAY, '支付宝'],
  [METHOD_BANK, '银行卡'],
  [METHOD_OTHER, '其他'],
]

watch(
  [() => session.source.value, () => session.token.value],
  async () => {
    if (!isApiSource.value || !session.token.value) {
      returns.value = []
      sourceOrders.value = []
      error.value = ''
      return
    }
    await loadPage()
  },
  { immediate: true },
)

watch(
  [keyword, statusFilter, orderFilterId],
  async () => {
    if (!isApiSource.value || !session.token.value) return
    await loadReturns()
  },
)

watch(selectedReturn, (next) => {
  noteDraft.value = next?.notes || ''
  refundForm.amount = next ? String(Math.max(next.totalAmount - next.refundAmount, 0).toFixed(2)) : ''
  refundForm.referenceNo = ''
})

watch(createOrderId, async (nextId, prevId) => {
  if (!nextId || sameEntityId(nextId, prevId) || !session.token.value) return
  const order = sourceOrders.value.find((item) => sameEntityId(item.id, nextId))
  if (order) {
    applySourceOrder(order)
    return
  }
  try {
    const detail = await fetchSaleOrder(session.token.value, nextId)
    sourceOrders.value = [detail, ...sourceOrders.value.filter((item) => !sameEntityId(item.id, detail.id))]
    applySourceOrder(detail)
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : '来源销售单加载失败'
  }
})

async function loadPage() {
  await Promise.all([loadSourceOrders(), loadReturns()])
  if (!initializedFromQuery.value) {
    initializedFromQuery.value = true
    if (queryOrderId.value) {
      createOrderId.value = queryOrderId.value
      orderFilterId.value = queryOrderId.value
      mode.value = queryReturnId.value ? 'manage' : 'create'
    }
  }
}

async function loadSourceOrders() {
  if (!session.token.value) return
  sourceLoading.value = true
  try {
    let nextOrders = await fetchSaleOrders(session.token.value, { page: 0, size: 200 })
    if (queryOrderId.value && !nextOrders.some((item) => sameEntityId(item.id, queryOrderId.value))) {
      try {
        const detail = await fetchSaleOrder(session.token.value, queryOrderId.value)
        nextOrders = [detail, ...nextOrders]
      } catch {
        // ignore deep-link miss
      }
    }
    sourceOrders.value = nextOrders
    if (!createOrderId.value) {
      createOrderId.value = queryOrderId.value ?? nextOrders[0]?.id ?? null
    }
    if (selectedSourceOrder.value && createItems.value.length === 0) {
      applySourceOrder(selectedSourceOrder.value)
    }
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : '销售单来源加载失败'
  } finally {
    sourceLoading.value = false
  }
}

async function loadReturns() {
  if (!session.token.value) return
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    let nextReturns = orderFilterId.value
      ? await fetchSalesReturnsByOrder(session.token.value, orderFilterId.value)
      : await fetchSalesReturns(session.token.value, {
          keyword: keyword.value.trim() || undefined,
          status: statusFilter.value === 'all' ? undefined : Number(statusFilter.value),
          page: 0,
          size: 200,
        })

    if (orderFilterId.value) {
      const normalizedKeyword = keyword.value.trim()
      nextReturns = nextReturns.filter((item) => {
        const matchKeyword = !normalizedKeyword
          || item.returnNo.includes(normalizedKeyword)
          || (item.customerName || '').includes(normalizedKeyword)
        const matchStatus = statusFilter.value === 'all' || item.status === Number(statusFilter.value)
        return matchKeyword && matchStatus
      })
    }

    if (queryReturnId.value && !nextReturns.some((item) => sameEntityId(item.id, queryReturnId.value))) {
      try {
        const detail = await fetchSalesReturn(session.token.value, queryReturnId.value)
        nextReturns = [detail, ...nextReturns]
      } catch {
        // ignore missing deep-link
      }
    }

    returns.value = nextReturns
    selectedReturnId.value = queryReturnId.value ?? nextReturns[0]?.id ?? null
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : '销售退货单加载失败'
  } finally {
    loading.value = false
  }
}

function applySourceOrder(order: SaleOrder) {
  createForm.notes = order.notes || ''
  createItems.value = order.items.map((item) => ({
    productId: item.productId,
    productName: item.productName || item.productCode || '未命名商品',
    productCode: item.productCode || '--',
    quantity: String(item.quantity),
    unitPrice: String(item.unitPrice),
    maxQuantity: item.quantity,
  }))
}

async function saveDraftNotes() {
  if (!session.token.value || !selectedReturn.value) return
  submitting.value = true
  error.value = ''
  success.value = ''
  try {
    const updated = await updateSalesReturnDraft(session.token.value, selectedReturn.value.id, {
      notes: noteDraft.value.trim() || null,
    })
    success.value = '退货草稿备注已更新'
    await refreshSelected(updated.id)
  } catch (submitErr) {
    error.value = submitErr instanceof Error ? submitErr.message : '退货草稿保存失败'
  } finally {
    submitting.value = false
  }
}

async function handleConfirm() {
  if (!session.token.value || !selectedReturn.value) return
  submitting.value = true
  error.value = ''
  success.value = ''
  try {
    const updated = await confirmSalesReturn(session.token.value, selectedReturn.value.id, {
      notes: noteDraft.value.trim() || null,
    })
    success.value = '销售退货单已确认'
    await refreshSelected(updated.id)
  } catch (submitErr) {
    error.value = submitErr instanceof Error ? submitErr.message : '销售退货确认失败'
  } finally {
    submitting.value = false
  }
}

async function handleRefund() {
  if (!session.token.value || !selectedReturn.value) return
  submitting.value = true
  error.value = ''
  success.value = ''
  try {
    const updated = await addSalesReturnRefund(session.token.value, selectedReturn.value.id, {
      amount: Number(refundForm.amount || 0),
      method: Number(refundForm.method),
      referenceNo: refundForm.referenceNo.trim() || null,
    })
    success.value = '退款记录已写入'
    await refreshSelected(updated.id)
  } catch (submitErr) {
    error.value = submitErr instanceof Error ? submitErr.message : '退款登记失败'
  } finally {
    submitting.value = false
  }
}

async function handleCancel() {
  if (!session.token.value || !selectedReturn.value) return
  submitting.value = true
  error.value = ''
  success.value = ''
  try {
    const updated = await cancelSalesReturn(session.token.value, selectedReturn.value.id)
    success.value = '退货单已取消'
    await refreshSelected(updated.id)
  } catch (submitErr) {
    error.value = submitErr instanceof Error ? submitErr.message : '退货单取消失败'
  } finally {
    submitting.value = false
  }
}

async function handleCreateReturn() {
  if (!session.token.value || !createPayload.value) return
  submitting.value = true
  error.value = ''
  success.value = ''
  try {
    const created = await createSalesReturn(session.token.value, createPayload.value)
    success.value = `已创建退货单 ${created.returnNo}`
    mode.value = 'manage'
    await loadReturns()
    selectedReturnId.value = created.id
  } catch (submitErr) {
    error.value = submitErr instanceof Error ? submitErr.message : '销售退货单创建失败'
  } finally {
    submitting.value = false
  }
}

async function refreshSelected(targetId: EntityId) {
  await loadReturns()
  selectedReturnId.value = targetId
}
</script>

<template>
  <section class="business-page">
    <section class="screen-hero">
      <div>
        <p class="eyebrow">销售退货 / Sales Returns</p>
        <h2>销售退货专页</h2>
        <p>支持真实退货单创建、确认、退款与取消，库存和客户应收按后端状态机联动。</p>
      </div>
      <div class="hero-actions">
        <button type="button" :class="{ 'ghost-action': mode === 'create' }" @click="mode = 'manage'">退货单管理</button>
        <button type="button" :class="{ 'ghost-action': mode === 'manage' }" @click="mode = 'create'">新建退货单</button>
      </div>
    </section>

    <p v-if="!isApiSource" class="form-error">当前是演示模式。这一页只在真实登录后加载销售退货数据。</p>
    <p v-else-if="error" class="form-error">{{ error }}</p>
    <p v-else-if="success" class="form-success">{{ success }}</p>

    <section class="metrics-grid compact">
      <article class="metric-card" data-tone="blue">
        <span>退货单数</span>
        <strong>{{ returns.length }}</strong>
        <p>{{ returns.filter((item) => item.status === SALES_RETURN_DRAFT).length }} 单待确认</p>
      </article>
      <article class="metric-card" data-tone="orange">
        <span>退货金额</span>
        <strong>{{ formatCurrency(totalReturnAmount) }}</strong>
        <p>真实销售退货单汇总</p>
      </article>
      <article class="metric-card" data-tone="green">
        <span>已退款金额</span>
        <strong>{{ formatCurrency(totalRefundAmount) }}</strong>
        <p>{{ formatCurrency(Math.max(totalReturnAmount - totalRefundAmount, 0)) }} 待退款</p>
      </article>
    </section>

    <section v-if="mode === 'manage'" class="business-page">
      <section class="panel">
        <div class="business-toolbar">
          <label class="search-box">
            <span>搜索退货单</span>
            <input v-model="keyword" placeholder="退货单号 / 客户名称" />
          </label>
          <label class="compact-field">
            <span>状态</span>
            <select v-model="statusFilter">
              <option value="all">全部状态</option>
              <option value="0">草稿</option>
              <option value="1">已确认</option>
              <option value="2">已退款</option>
              <option value="3">已取消</option>
            </select>
          </label>
          <label class="compact-field">
            <span>来源销售单</span>
            <select v-model="orderFilterId">
              <option :value="null">全部销售单</option>
              <option v-for="order in sourceOrders" :key="order.id" :value="order.id">
                {{ order.orderNo }} / {{ order.customerName || '散客' }}
              </option>
            </select>
          </label>
        </div>
      </section>

      <section class="business-split">
        <article class="panel">
          <div class="panel-head">
            <div>
              <p class="eyebrow">退货单列表</p>
              <h3>销售退货单</h3>
            </div>
            <span class="session-source">{{ returns.length }} 单</span>
          </div>

          <div class="table-shell">
            <table>
              <thead>
                <tr>
                  <th>退货单号</th>
                  <th>客户</th>
                  <th>来源销售单</th>
                  <th>退货金额</th>
                  <th>退款进度</th>
                  <th>状态</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="item in returns"
                  :key="item.id"
                  :class="{ selected: sameEntityId(item.id, selectedReturnId) }"
                  @click="selectedReturnId = item.id"
                >
                  <td>{{ item.returnNo }}</td>
                  <td>{{ item.customerName || '未命名客户' }}</td>
                  <td>{{ item.originalOrderId || '--' }}</td>
                  <td>{{ formatCurrency(item.totalAmount) }}</td>
                  <td>{{ salesReturnRefundStatus(item.totalAmount, item.refundAmount, item.status) }}</td>
                  <td>{{ salesReturnStatusLabel(item.status) }}</td>
                </tr>
                <tr v-if="!loading && returns.length === 0">
                  <td colspan="6" class="empty-cell">暂无销售退货单</td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>

        <aside class="panel detail-panel">
          <p class="eyebrow">退货单详情</p>
          <h3>{{ selectedReturn?.returnNo || '请选择退货单' }}</h3>

          <div v-if="selectedReturn" class="detail-stack">
            <article class="detail-card">
              <dl class="detail-list">
                <div>
                  <dt>客户</dt>
                  <dd>{{ selectedReturn.customerName || '未命名客户' }}</dd>
                </div>
                <div>
                  <dt>来源销售单</dt>
                  <dd>{{ selectedReturn.originalOrderId || '--' }}</dd>
                </div>
                <div>
                  <dt>退货金额</dt>
                  <dd>{{ formatCurrency(selectedReturn.totalAmount) }}</dd>
                </div>
                <div>
                  <dt>已退款</dt>
                  <dd>{{ formatCurrency(selectedReturn.refundAmount) }}</dd>
                </div>
                <div>
                  <dt>剩余待退款</dt>
                  <dd>{{ formatCurrency(remainingRefund) }}</dd>
                </div>
                <div>
                  <dt>状态</dt>
                  <dd>{{ salesReturnStatusLabel(selectedReturn.status) }}</dd>
                </div>
              </dl>
            </article>

            <article class="detail-card">
              <p class="eyebrow">退货商品</p>
              <div class="mini-list">
                <div v-for="line in selectedReturn.items" :key="line.id">
                  <strong>{{ line.productName || line.productCode || '未命名商品' }}</strong>
                  <span>{{ line.quantity }} x {{ formatCurrency(line.unitPrice) }} = {{ formatCurrency(line.amount) }}</span>
                </div>
              </div>
            </article>

            <article class="detail-card">
              <p class="eyebrow">备注与动作</p>
              <label class="compact-field">
                <span>退货备注</span>
                <textarea v-model="noteDraft" rows="4" :disabled="!canEditDraft"></textarea>
              </label>
              <div class="form-actions">
                <button type="button" :disabled="!canEditDraft || submitting" @click="saveDraftNotes">保存备注</button>
                <button type="button" class="ghost-action" :disabled="!canEditDraft || submitting" @click="handleConfirm">确认退货</button>
                <button type="button" class="ghost-action" :disabled="!canCancel || submitting" @click="handleCancel">取消退货</button>
              </div>
            </article>

            <article class="detail-card">
              <p class="eyebrow">退款登记</p>
              <label class="compact-field">
                <span>退款金额</span>
                <input v-model="refundForm.amount" class="table-input" type="number" min="0" step="0.01" />
              </label>
              <label class="compact-field">
                <span>退款方式</span>
                <select v-model="refundForm.method">
                  <option v-for="[value, label] in paymentMethods" :key="value" :value="String(value)">{{ label }}</option>
                </select>
              </label>
              <label class="compact-field">
                <span>参考流水号</span>
                <input v-model="refundForm.referenceNo" class="table-input" placeholder="可选" />
              </label>
              <div class="form-actions">
                <button type="button" :disabled="!canRefund || submitting" @click="handleRefund">登记退款</button>
              </div>
            </article>
          </div>

          <div v-else class="empty-preview">
            <strong>暂无退货单详情</strong>
            <p>请从左侧选择一张退货单。</p>
          </div>
        </aside>
      </section>
    </section>

    <section v-else class="business-split">
      <article class="panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">来源销售单</p>
            <h3>{{ selectedSourceOrder?.orderNo || '请选择销售单' }}</h3>
          </div>
          <span class="session-source">{{ sourceLoading ? '加载中...' : `${sourceOrders.length} 单` }}</span>
        </div>

        <label class="compact-field">
          <span>选择来源销售单</span>
          <select v-model="createOrderId">
            <option v-for="order in sourceOrders" :key="order.id" :value="order.id">
              {{ order.orderNo }} / {{ order.customerName || '散客' }}
            </option>
          </select>
        </label>

        <div v-if="selectedSourceOrder" class="detail-card section-card">
          <dl class="detail-list">
            <div>
              <dt>客户名称</dt>
              <dd>{{ selectedSourceOrder.customerName || '散客' }}</dd>
            </div>
            <div>
              <dt>订单金额</dt>
              <dd>{{ formatCurrency(selectedSourceOrder.totalAmount) }}</dd>
            </div>
            <div>
              <dt>已收金额</dt>
              <dd>{{ formatCurrency(selectedSourceOrder.paidAmount) }}</dd>
            </div>
            <div>
              <dt>创建时间</dt>
              <dd>{{ formatDateTime(selectedSourceOrder.createdAt) }}</dd>
            </div>
          </dl>
        </div>

        <div class="table-shell">
          <table>
            <thead>
              <tr>
                <th>商品</th>
                <th>原销售数量</th>
                <th>退货数量</th>
                <th>退货单价</th>
                <th>预计金额</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in createItems" :key="`${item.productId}-${index}`">
                <td>
                  {{ item.productName }}
                  <small>{{ item.productCode }}</small>
                </td>
                <td>{{ item.maxQuantity }}</td>
                <td>
                  <input v-model="item.quantity" class="table-input narrow-input" type="number" min="0" :max="item.maxQuantity" step="0.01" />
                </td>
                <td>
                  <input v-model="item.unitPrice" class="table-input narrow-input" type="number" min="0" step="0.01" />
                </td>
                <td>{{ formatCurrency(Number(item.quantity || 0) * Number(item.unitPrice || 0)) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <aside class="panel detail-panel">
        <p class="eyebrow">创建退货单</p>
        <h3>{{ selectedSourceOrder?.customerName || '未选择来源单' }}</h3>

        <div class="detail-stack">
          <article class="detail-card">
            <label class="compact-field">
              <span>退货备注</span>
              <textarea v-model="createForm.notes" rows="5" placeholder="记录退货原因、验货说明、沟通备注"></textarea>
            </label>
            <div class="form-actions">
              <button type="button" :disabled="!createPayload || submitting || !canWrite" @click="handleCreateReturn">
                {{ submitting ? '创建中...' : '创建退货单' }}
              </button>
            </div>
          </article>

          <article class="detail-card">
            <p class="eyebrow">预览摘要</p>
            <dl class="detail-list">
              <div>
                <dt>来源销售单</dt>
                <dd>{{ selectedSourceOrder?.orderNo || '--' }}</dd>
              </div>
              <div>
                <dt>客户</dt>
                <dd>{{ selectedSourceOrder?.customerName || '散客' }}</dd>
              </div>
              <div>
                <dt>退货行数</dt>
                <dd>{{ createPayload?.items.length || 0 }}</dd>
              </div>
              <div>
                <dt>预计退货金额</dt>
                <dd>{{ formatCurrency((createPayload?.items || []).reduce((sum, item) => sum + item.quantity * Number(item.unitPrice || 0), 0)) }}</dd>
              </div>
            </dl>
          </article>
        </div>
      </aside>
    </section>
  </section>
</template>
