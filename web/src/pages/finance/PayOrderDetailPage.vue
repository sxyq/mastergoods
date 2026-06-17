<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useSession } from '@/app/stores/session'
import {
  createPayOrder,
  fetchAccounts,
  fetchPayOrder,
  fetchPayOrders,
  fetchSuppliers,
  updatePayOrderStatus,
  type AccountRecord,
  type PayOrder,
  type SupplierRecord,
} from '@/shared/api/client'
import { readQueryId, sameEntityId, type EntityId } from '@/shared/utils/id'
import {
  METHOD_ALIPAY,
  METHOD_BANK,
  METHOD_CASH,
  METHOD_OTHER,
  METHOD_WECHAT,
  PAY_ORDER_CANCELLED,
  PAY_ORDER_DRAFT,
  PAY_ORDER_PAID,
  financeMethodLabel,
  formatCurrency,
  formatDateTime,
  payOrderStatusLabel,
} from '@/shared/utils/business'
import PageEmptyState from '@/shared/ui/PageEmptyState.vue'
import PageStatusBanner from '@/shared/ui/PageStatusBanner.vue'

const route = useRoute()
const session = useSession()

const orders = ref<PayOrder[]>([])
const suppliers = ref<SupplierRecord[]>([])
const accounts = ref<AccountRecord[]>([])
const selectedOrderId = ref<EntityId | null>(null)
const loading = ref(false)
const submitting = ref(false)
const error = ref('')
const success = ref('')
const keyword = ref('')
const statusFilter = ref('all')
const createdAfter = ref('')
const createdBefore = ref('')

const createForm = reactive({
  supplierId: '',
  supplierName: '',
  amount: '',
  method: String(METHOD_BANK),
  referenceNo: '',
  notes: '',
  accountId: '',
  status: String(PAY_ORDER_DRAFT),
})

const queryOrderId = computed(() => readQueryId(route.query.id))
const isApiSource = computed(() => session.source.value === 'api' && Boolean(session.token.value))
const canWrite = computed(() => session.hasPermission(['finance:write']))
const selectedOrder = computed(() => orders.value.find((item) => sameEntityId(item.id, selectedOrderId.value)) ?? null)
const totalAmount = computed(() => orders.value.reduce((sum, item) => sum + item.amount, 0))
const paidCount = computed(() => orders.value.filter((item) => item.status === PAY_ORDER_PAID).length)

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
      orders.value = []
      suppliers.value = []
      accounts.value = []
      error.value = ''
      return
    }
    await loadPage()
  },
  { immediate: true },
)

watch([keyword, statusFilter, createdAfter, createdBefore], async () => {
  if (!isApiSource.value || !session.token.value) return
  await loadOrders()
})

async function loadPage() {
  await Promise.all([loadOrders(), loadDictionaries()])
}

async function loadDictionaries() {
  if (!session.token.value) return
  try {
    const [nextSuppliers, nextAccounts] = await Promise.all([
      fetchSuppliers(session.token.value, { page: 0, size: 200 }),
      fetchAccounts(session.token.value),
    ])
    suppliers.value = nextSuppliers
    accounts.value = nextAccounts
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : '付款单基础数据加载失败'
  }
}

async function loadOrders() {
  if (!session.token.value) return
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    let nextOrders = await fetchPayOrders(session.token.value, {
      keyword: keyword.value.trim() || undefined,
      status: statusFilter.value === 'all' ? undefined : Number(statusFilter.value),
      createdAfter: createdAfter.value ? new Date(`${createdAfter.value}T00:00:00`).getTime() : undefined,
      createdBefore: createdBefore.value ? new Date(`${createdBefore.value}T23:59:59`).getTime() : undefined,
      page: 0,
      size: 200,
    })

    if (queryOrderId.value && !nextOrders.some((item) => sameEntityId(item.id, queryOrderId.value))) {
      try {
        const detail = await fetchPayOrder(session.token.value, queryOrderId.value)
        nextOrders = [detail, ...nextOrders]
      } catch {
        // ignore invalid deep-link
      }
    }

    orders.value = nextOrders
    selectedOrderId.value = queryOrderId.value ?? nextOrders[0]?.id ?? null
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : '付款单列表加载失败'
  } finally {
    loading.value = false
  }
}

function accountName(accountId: number | null) {
  return accounts.value.find((item) => item.id === accountId)?.name || '--'
}

async function handleCreatePayOrder() {
  if (!session.token.value) return
  submitting.value = true
  error.value = ''
  success.value = ''
  try {
    const supplierId = createForm.supplierId ? Number(createForm.supplierId) : null
    const created = await createPayOrder(session.token.value, {
      supplierId,
      supplierName: supplierId ? null : createForm.supplierName.trim(),
      amount: Number(createForm.amount || 0),
      method: Number(createForm.method),
      referenceNo: createForm.referenceNo.trim() || null,
      notes: createForm.notes.trim() || null,
      accountId: createForm.accountId ? Number(createForm.accountId) : null,
      status: Number(createForm.status),
    })
    success.value = `已创建付款单 ${created.orderNo}`
    createForm.amount = ''
    createForm.referenceNo = ''
    createForm.notes = ''
    await loadOrders()
    selectedOrderId.value = created.id
  } catch (submitErr) {
    error.value = submitErr instanceof Error ? submitErr.message : '付款单创建失败'
  } finally {
    submitting.value = false
  }
}

async function handleStatusChange(status: number) {
  if (!session.token.value || !selectedOrder.value) return
  submitting.value = true
  error.value = ''
  success.value = ''
  try {
    const updated = await updatePayOrderStatus(session.token.value, selectedOrder.value.id, status)
    success.value = `付款单状态已更新为${payOrderStatusLabel(updated.status)}`
    await loadOrders()
    selectedOrderId.value = updated.id
  } catch (submitErr) {
    error.value = submitErr instanceof Error ? submitErr.message : '付款单状态更新失败'
  } finally {
    submitting.value = false
  }
}

async function retryPage() {
  await loadPage()
}
</script>

<template>
  <section class="business-page pay-order-page stitch-inspired-page">
    <section class="screen-hero">
      <div>
        <p class="eyebrow">付款单 / Pay Orders</p>
        <h2>付款单详情专页</h2>
        <p>统一处理付款单查看、创建和状态流转，真实调用 `/v2/pay-orders` 与 `/status`。</p>
      </div>
    </section>

    <PageStatusBanner
      v-if="!isApiSource"
      tone="warning"
      title="演示模式"
      message="当前是演示模式。这一页只在真实登录后读取和写入付款单。"
    />
    <PageStatusBanner
      v-else-if="error"
      tone="error"
      title="页面加载异常"
      :message="error"
      action-label="重新加载"
      @action="retryPage"
    />
    <PageStatusBanner v-else-if="success" tone="success" title="操作成功" :message="success" />
    <PageStatusBanner v-if="loading" tone="info" title="正在同步" message="正在加载付款单..." />

    <section class="metrics-grid compact stitch-kpis">
      <article class="metric-card" data-tone="blue">
        <span>付款单数</span>
        <strong>{{ orders.length }}</strong>
        <p>当前筛选结果</p>
      </article>
      <article class="metric-card" data-tone="orange">
        <span>付款总额</span>
        <strong>{{ formatCurrency(totalAmount) }}</strong>
        <p>列表汇总金额</p>
      </article>
      <article class="metric-card" data-tone="green">
        <span>已付款</span>
        <strong>{{ paidCount }}</strong>
        <p>{{ orders.length - paidCount }} 单未付款</p>
      </article>
    </section>

    <section class="panel">
      <div class="business-toolbar">
        <label class="search-box">
          <span>搜索付款单</span>
          <input v-model="keyword" placeholder="付款单号 / 供应商 / 参考号" />
        </label>
        <label class="compact-field">
          <span>状态</span>
          <select v-model="statusFilter">
            <option value="all">全部状态</option>
            <option value="0">草稿</option>
            <option value="1">已付款</option>
            <option value="2">已取消</option>
          </select>
        </label>
        <label class="compact-field">
          <span>开始日期</span>
          <input v-model="createdAfter" type="date" />
        </label>
        <label class="compact-field">
          <span>结束日期</span>
          <input v-model="createdBefore" type="date" />
        </label>
      </div>
    </section>

    <section class="business-split">
      <article class="panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">付款单列表</p>
            <h3>财务付款单</h3>
          </div>
          <span class="session-source">{{ loading ? '加载中...' : `${orders.length} 单` }}</span>
        </div>

        <div class="table-shell">
          <table>
            <thead>
              <tr>
                <th>付款单号</th>
                <th>供应商</th>
                <th>金额</th>
                <th>方式</th>
                <th>状态</th>
                <th>创建时间</th>
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
                <td>{{ order.supplierName || '未命名供应商' }}</td>
                <td>{{ formatCurrency(order.amount) }}</td>
                <td>{{ financeMethodLabel(order.method) }}</td>
                <td>{{ payOrderStatusLabel(order.status) }}</td>
                <td>{{ formatDateTime(order.createdAt) }}</td>
              </tr>
              <tr v-if="!loading && orders.length === 0">
                <td colspan="6" class="empty-cell">暂无付款单</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <aside class="panel detail-panel pay-order-detail-panel">
        <p class="eyebrow">付款单详情</p>
        <h3>{{ selectedOrder?.orderNo || '请选择付款单' }}</h3>

        <div class="detail-stack">
          <article v-if="selectedOrder" class="detail-card">
            <div class="pay-order-hero-card">
              <span>付款金额 (CNY)</span>
              <strong>{{ formatCurrency(selectedOrder.amount) }}</strong>
              <p>{{ payOrderStatusLabel(selectedOrder.status) }} / {{ selectedOrder.orderNo }}</p>
            </div>
            <dl class="detail-list">
              <div>
                <dt>供应商</dt>
                <dd>{{ selectedOrder.supplierName || '未命名供应商' }}</dd>
              </div>
              <div>
                <dt>付款金额</dt>
                <dd>{{ formatCurrency(selectedOrder.amount) }}</dd>
              </div>
              <div>
                <dt>付款方式</dt>
                <dd>{{ financeMethodLabel(selectedOrder.method) }}</dd>
              </div>
              <div>
                <dt>参考号</dt>
                <dd>{{ selectedOrder.referenceNo || '--' }}</dd>
              </div>
              <div>
                <dt>账户</dt>
                <dd>{{ accountName(selectedOrder.accountId) }}</dd>
              </div>
              <div>
                <dt>状态</dt>
                <dd>{{ payOrderStatusLabel(selectedOrder.status) }}</dd>
              </div>
              <div>
                <dt>创建时间</dt>
                <dd>{{ formatDateTime(selectedOrder.createdAt) }}</dd>
              </div>
              <div>
                <dt>更新时间</dt>
                <dd>{{ formatDateTime(selectedOrder.updatedAt) }}</dd>
              </div>
              <div>
                <dt>备注</dt>
                <dd>{{ selectedOrder.notes || '--' }}</dd>
              </div>
            </dl>
            <div class="form-actions">
              <button type="button" :disabled="!canWrite || submitting || selectedOrder.status === PAY_ORDER_DRAFT" @click="handleStatusChange(PAY_ORDER_DRAFT)">
                设为草稿
              </button>
              <button type="button" class="ghost-action" :disabled="!canWrite || submitting || selectedOrder.status === PAY_ORDER_PAID" @click="handleStatusChange(PAY_ORDER_PAID)">
                设为已付款
              </button>
              <button type="button" class="ghost-action" :disabled="!canWrite || submitting || selectedOrder.status === PAY_ORDER_CANCELLED" @click="handleStatusChange(PAY_ORDER_CANCELLED)">
                设为已取消
              </button>
            </div>
          </article>

          <PageEmptyState v-else title="暂无付款单详情" message="请从左侧选择一张付款单。" />

          <article class="detail-card">
            <p class="eyebrow">新建付款单</p>
            <div class="detail-stack">
              <label class="compact-field">
                <span>供应商</span>
                <select v-model="createForm.supplierId">
                  <option value="">手填供应商名称</option>
                  <option v-for="supplier in suppliers" :key="supplier.id" :value="String(supplier.id)">
                    {{ supplier.name }}
                  </option>
                </select>
              </label>
              <label v-if="!createForm.supplierId" class="compact-field">
                <span>供应商名称</span>
                <input v-model="createForm.supplierName" class="table-input" placeholder="请输入供应商名称" />
              </label>
              <label class="compact-field">
                <span>付款金额</span>
                <input v-model="createForm.amount" class="table-input" type="number" min="0" step="0.01" />
              </label>
              <label class="compact-field">
                <span>付款方式</span>
                <select v-model="createForm.method">
                  <option v-for="[value, label] in paymentMethods" :key="value" :value="String(value)">{{ label }}</option>
                </select>
              </label>
              <label class="compact-field">
                <span>资金账户</span>
                <select v-model="createForm.accountId">
                  <option value="">不指定账户</option>
                  <option v-for="account in accounts" :key="account.id" :value="String(account.id)">
                    {{ account.name }} / {{ formatCurrency(account.balance) }}
                  </option>
                </select>
              </label>
              <label class="compact-field">
                <span>初始状态</span>
                <select v-model="createForm.status">
                  <option :value="String(PAY_ORDER_DRAFT)">草稿</option>
                  <option :value="String(PAY_ORDER_PAID)">已付款</option>
                  <option :value="String(PAY_ORDER_CANCELLED)">已取消</option>
                </select>
              </label>
              <label class="compact-field">
                <span>参考号</span>
                <input v-model="createForm.referenceNo" class="table-input" placeholder="银行流水号 / 备注号" />
              </label>
              <label class="compact-field">
                <span>备注</span>
                <textarea v-model="createForm.notes" rows="4"></textarea>
              </label>
              <div class="form-actions">
                <button type="button" :disabled="!canWrite || submitting" @click="handleCreatePayOrder">
                  {{ submitting ? '保存中...' : '创建付款单' }}
                </button>
              </div>
            </div>
          </article>
        </div>
      </aside>
    </section>
  </section>
</template>
