<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSession } from '@/app/stores/session'
import {
  createSaleOrder,
  fetchCustomers,
  fetchProducts,
  fetchSaleOrder,
  updateSaleOrderDraft,
  type CustomerRecord,
  type ProductRecord,
  type SaleOrderCreatePayload,
  type SaleOrderUpdateDraftPayload,
} from '@/shared/api/client'
import { readQueryId } from '@/shared/utils/id'
import { formatCurrency } from '@/shared/utils/business'

interface LineItemForm {
  productId: string
  quantity: string
  unitPrice: string
}

const route = useRoute()
const router = useRouter()
const session = useSession()

const customers = ref<CustomerRecord[]>([])
const products = ref<ProductRecord[]>([])
const lines = ref<LineItemForm[]>([{ productId: '', quantity: '1', unitPrice: '0' }])
const loading = ref(false)
const saving = ref(false)
const error = ref('')

const form = reactive({
  customerId: '',
  customerName: '',
  discountAmount: '0',
  notes: '',
})

const orderId = computed(() => readQueryId(route.query.id))
const isApiSource = computed(() => session.source.value === 'api' && Boolean(session.token.value))
const isEditMode = computed(() => orderId.value != null)
const canSubmit = computed(() => {
  const filledLines = lines.value.filter((line) => line.productId && Number(line.quantity) > 0)
  return isApiSource.value && !saving.value && filledLines.length > 0
})
const orderAmount = computed(() => {
  const total = lines.value.reduce((sum, line) => sum + (Number(line.quantity) || 0) * (Number(line.unitPrice) || 0), 0)
  return total - (Number(form.discountAmount) || 0)
})

watch(
  [() => session.source.value, () => session.token.value, orderId],
  async () => {
    if (!isApiSource.value || !session.token.value) {
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
  try {
    const [nextCustomers, nextProducts] = await Promise.all([
      fetchCustomers(session.token.value, { page: 0, size: 200 }),
      fetchProducts(session.token.value, { page: 0, size: 200, status: 1 }),
    ])
    customers.value = nextCustomers
    products.value = nextProducts
    if (orderId.value) {
      const order = await fetchSaleOrder(session.token.value, orderId.value)
      form.customerId = order.customerId ? String(order.customerId) : ''
      form.customerName = order.customerName || ''
      form.discountAmount = String(order.discountAmount || 0)
      form.notes = order.notes || ''
      lines.value = order.items.length > 0
        ? order.items.map((item) => ({
            productId: item.productId ? String(item.productId) : '',
            quantity: String(item.quantity),
            unitPrice: String(item.unitPrice),
          }))
        : [{ productId: '', quantity: '1', unitPrice: '0' }]
    } else {
      resetForm()
    }
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : '销售单资料加载失败'
  } finally {
    loading.value = false
  }
}

function addLine() {
  lines.value.push({ productId: '', quantity: '1', unitPrice: '0' })
}

function removeLine(index: number) {
  if (lines.value.length === 1) {
    lines.value = [{ productId: '', quantity: '1', unitPrice: '0' }]
    return
  }
  lines.value.splice(index, 1)
}

function syncCustomer(customerId: string) {
  const customer = customers.value.find((item) => item.id === Number(customerId))
  form.customerId = customerId
  form.customerName = customer?.name || ''
}

function syncProduct(index: number, productId: string) {
  const product = products.value.find((item) => item.id === Number(productId))
  lines.value[index].productId = productId
  if (product) {
    lines.value[index].unitPrice = String(product.salePrice)
  }
}

async function submitForm() {
  if (!session.token.value) return
  saving.value = true
  error.value = ''
  try {
    const items = lines.value
      .filter((line) => line.productId && Number(line.quantity) > 0)
      .map((line) => ({
        productId: Number(line.productId),
        quantity: Number(line.quantity),
        unitPrice: Number(line.unitPrice || 0),
      }))
    if (items.length === 0) {
      throw new Error('请至少添加一行商品')
    }

    const saved = orderId.value
      ? await updateSaleOrderDraft(session.token.value, orderId.value, {
          discountAmount: Number(form.discountAmount || 0),
          notes: form.notes.trim() || null,
          items,
        } satisfies SaleOrderUpdateDraftPayload)
      : await createSaleOrder(session.token.value, {
          customerId: form.customerId ? Number(form.customerId) : null,
          customerName: form.customerName.trim() || null,
          discountAmount: Number(form.discountAmount || 0),
          notes: form.notes.trim() || null,
          items,
        } satisfies SaleOrderCreatePayload)

    await router.push({ path: '/documents/sales/detail', query: { id: String(saved.id) } })
  } catch (saveErr) {
    error.value = saveErr instanceof Error ? saveErr.message : '销售单保存失败'
  } finally {
    saving.value = false
  }
}

function resetForm() {
  form.customerId = ''
  form.customerName = ''
  form.discountAmount = '0'
  form.notes = ''
  lines.value = [{ productId: '', quantity: '1', unitPrice: '0' }]
}
</script>

<template>
  <section class="business-page">
    <section class="screen-hero">
      <div>
        <p class="eyebrow">销售开单 / Sale Order Draft</p>
        <h2>{{ isEditMode ? '编辑销售草稿' : '新建销售单' }}</h2>
        <p>对齐安卓端销售开单字段，按真实客户、商品与折扣计算订单金额。</p>
      </div>
      <div class="hero-actions">
        <button type="button" class="ghost-action" @click="router.push('/documents/sales')">返回列表</button>
      </div>
    </section>

    <p v-if="!isApiSource" class="form-error">当前是演示模式。这一页只在真实登录后保存销售单。</p>
    <p v-else-if="error" class="form-error">{{ error }}</p>
    <p v-else-if="loading" class="form-success">正在加载销售单资料...</p>

    <section class="business-split">
      <article class="panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">订单表单</p>
            <h3>客户与商品明细</h3>
          </div>
          <span class="session-source">应收合计 {{ formatCurrency(orderAmount) }}</span>
        </div>

        <form class="partner-form product-form" @submit.prevent="submitForm">
          <label>
            <span>快捷选择客户</span>
            <select :value="form.customerId" @change="syncCustomer(($event.target as HTMLSelectElement).value)">
              <option value="">手动录入客户</option>
              <option v-for="customer in customers" :key="customer.id" :value="String(customer.id)">
                {{ customer.name }}
              </option>
            </select>
          </label>
          <label>
            <span>客户名称</span>
            <input v-model="form.customerName" placeholder="散客 / 客户名称" />
          </label>
          <label>
            <span>折扣金额</span>
            <input v-model="form.discountAmount" type="number" min="0" step="0.01" />
          </label>
          <label class="wide-field">
            <span>备注</span>
            <textarea v-model="form.notes" rows="3" placeholder="订单备注、送货说明等" />
          </label>
        </form>

        <div class="document-lines">
          <div class="panel-head">
            <div>
              <p class="eyebrow">商品明细</p>
              <h3>{{ lines.length }} 行商品</h3>
            </div>
            <button type="button" class="ghost-action" @click="addLine">新增一行</button>
          </div>

          <div class="table-shell">
            <table>
              <thead>
                <tr>
                  <th>商品</th>
                  <th>数量</th>
                  <th>单价</th>
                  <th>小计</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(line, index) in lines" :key="index">
                  <td>
                    <select class="table-input" :value="line.productId" @change="syncProduct(index, ($event.target as HTMLSelectElement).value)">
                      <option value="">请选择商品</option>
                      <option v-for="product in products" :key="product.id" :value="String(product.id)">
                        {{ product.name }} / {{ product.code }}
                      </option>
                    </select>
                  </td>
                  <td><input v-model="line.quantity" class="table-input" type="number" min="0" step="0.01" /></td>
                  <td><input v-model="line.unitPrice" class="table-input" type="number" min="0" step="0.01" /></td>
                  <td>{{ formatCurrency((Number(line.quantity) || 0) * (Number(line.unitPrice) || 0)) }}</td>
                  <td><button type="button" class="ghost-action" @click="removeLine(index)">删除</button></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="form-actions">
          <button type="button" :disabled="!canSubmit" @click="submitForm">{{ saving ? '保存中...' : '保存销售单' }}</button>
          <button type="button" class="ghost-action" :disabled="saving" @click="resetForm">重置表单</button>
        </div>
      </article>

      <aside class="panel detail-panel">
        <p class="eyebrow">订单汇总</p>
        <h3>当前草稿摘要</h3>
        <div class="detail-stack">
          <article class="detail-card">
            <dl class="detail-list">
              <div>
                <dt>客户</dt>
                <dd>{{ form.customerName || '散客' }}</dd>
              </div>
              <div>
                <dt>商品行数</dt>
                <dd>{{ lines.length }}</dd>
              </div>
              <div>
                <dt>折扣金额</dt>
                <dd>{{ formatCurrency(Number(form.discountAmount || 0)) }}</dd>
              </div>
              <div>
                <dt>应收合计</dt>
                <dd>{{ formatCurrency(orderAmount) }}</dd>
              </div>
            </dl>
          </article>
          <article class="detail-card">
            <strong>接口对齐</strong>
            <p class="muted">创建走 `POST /v2/sale-orders`，编辑草稿走 `PUT /v2/sale-orders/{id}/draft`。</p>
          </article>
        </div>
      </aside>
    </section>
  </section>
</template>
