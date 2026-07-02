<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSession } from '@/app/stores/session'
import {
  confirmSaleOrder,
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
import { entityIdKey, readQueryId } from '@/shared/utils/id'
import { formatCurrency } from '@/shared/utils/business'

interface LineItemForm {
  productId: string
  quantity: string
  unitPrice: string
  discountPercent: string
  remark: string
}

const route = useRoute()
const router = useRouter()
const session = useSession()

const customers = ref<CustomerRecord[]>([])
const products = ref<ProductRecord[]>([])
const lines = ref<LineItemForm[]>([createEmptyLine()])
const loading = ref(false)
const saving = ref(false)
const error = ref('')

const form = reactive({
  customerId: '',
  customerName: '',
  saleDate: new Date().toISOString().slice(0, 10),
  salesperson: '张经理',
  warehouse: '主仓库 (深圳南山)',
  discountAmount: '0',
  notes: '',
})

const orderId = computed(() => readQueryId(route.query.id))
const isApiSource = computed(() => session.source.value === 'api' && Boolean(session.token.value))
const isEditMode = computed(() => orderId.value != null)
const canWrite = computed(() => session.hasPermission(['sales:write']))
const customerIndex = computed(() => new Map(customers.value.map((item) => [entityIdKey(item.id), item] as const)))
const productIndex = computed(() => new Map(products.value.map((item) => [entityIdKey(item.id), item] as const)))
const subtotalAmount = computed(() => lines.value.reduce((sum, line) => sum + lineAmount(line), 0))
const totalQuantity = computed(() => lines.value.reduce((sum, line) => sum + (Number(line.quantity) || 0), 0))
const orderAmount = computed(() => Math.max(0, subtotalAmount.value - (Number(form.discountAmount) || 0)))
const filledLineCount = computed(() => lines.value.reduce((count, line) => count + (line.productId && Number(line.quantity) > 0 ? 1 : 0), 0))
const lineRows = computed(() => lines.value.map((line, index) => ({
  index,
  line,
  product: line.productId ? productIndex.value.get(entityIdKey(line.productId)) ?? null : null,
  amount: lineAmount(line),
})))
const canSubmit = computed(() => canWrite.value && isApiSource.value && !saving.value && filledLineCount.value > 0)

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
      form.saleDate = new Date(order.createdAt).toISOString().slice(0, 10)
      lines.value = order.items.length > 0
        ? order.items.map((item) => ({
            productId: item.productId ? String(item.productId) : '',
            quantity: String(item.quantity),
            unitPrice: String(item.unitPrice),
            discountPercent: '100',
            remark: '',
          }))
        : [createEmptyLine()]
    } else {
      resetForm()
    }
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : '销售单资料加载失败'
  } finally {
    loading.value = false
  }
}

function createEmptyLine(): LineItemForm {
  return { productId: '', quantity: '1', unitPrice: '0', discountPercent: '100', remark: '' }
}

function addLine() {
  lines.value.push(createEmptyLine())
}

function removeLine(index: number) {
  if (lines.value.length === 1) {
    lines.value = [createEmptyLine()]
    return
  }
  lines.value.splice(index, 1)
}

function syncCustomer(customerId: string) {
  const customer = customerIndex.value.get(entityIdKey(customerId))
  form.customerId = customerId
  form.customerName = customer?.name || ''
}

function syncProduct(index: number, productId: string) {
  const product = productIndex.value.get(entityIdKey(productId))
  lines.value[index].productId = productId
  if (product) {
    lines.value[index].unitPrice = String(product.salePrice)
  }
}

function lineAmount(line: LineItemForm) {
  const quantity = Number(line.quantity) || 0
  const unitPrice = Number(line.unitPrice) || 0
  const discount = Math.max(0, Number(line.discountPercent) || 0) / 100
  return quantity * unitPrice * discount
}

async function saveDraft() {
  await submitForm(false)
}

async function saveAndConfirm() {
  await submitForm(true)
}

async function submitForm(shouldConfirm: boolean) {
  if (!session.token.value) return
  saving.value = true
  error.value = ''
  try {
    const items = lines.value
      .filter((line) => line.productId && Number(line.quantity) > 0)
      .map((line) => ({
        productId: line.productId,
        quantity: Number(line.quantity),
        unitPrice: Number(line.unitPrice || 0) * (Math.max(0, Number(line.discountPercent) || 0) / 100),
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
          customerId: form.customerId || null,
          customerName: form.customerName.trim() || null,
          discountAmount: Number(form.discountAmount || 0),
          notes: form.notes.trim() || null,
          items,
        } satisfies SaleOrderCreatePayload)

    const target = shouldConfirm
      ? await confirmSaleOrder(session.token.value, saved.id, { notes: form.notes.trim() || null })
      : saved
    await router.push({ path: '/documents/sales/detail', query: { id: String(target.id) } })
  } catch (saveErr) {
    error.value = saveErr instanceof Error ? saveErr.message : '销售单保存失败'
  } finally {
    saving.value = false
  }
}

function resetForm() {
  form.customerId = ''
  form.customerName = ''
  form.saleDate = new Date().toISOString().slice(0, 10)
  form.salesperson = '张经理'
  form.warehouse = '主仓库 (深圳南山)'
  form.discountAmount = '0'
  form.notes = ''
  lines.value = [createEmptyLine()]
}
</script>

<template>
  <section class="pc-form-page sales-edit-page">
    <header class="pc-list-titlebar">
      <div class="pc-breadcrumb">
        <span>销售管理</span>
        <span class="material-symbols-outlined">chevron_right</span>
        <span>销售单</span>
        <span class="material-symbols-outlined">chevron_right</span>
        <h1>{{ isEditMode ? '编辑销售单' : '新建销售单' }}</h1>
      </div>
      <div class="pc-title-actions">
        <button type="button" class="pc-secondary-action" @click="router.push('/documents/sales')">
          <span class="material-symbols-outlined">arrow_back</span>
          返回列表
        </button>
      </div>
    </header>

    <p v-if="!isApiSource" class="form-error">当前是演示模式。这一页只在真实登录后保存销售单。</p>
    <p v-else-if="error" class="form-error">{{ error }}</p>
    <p v-else-if="loading" class="form-success">正在加载销售单资料...</p>

    <section class="pc-form-card">
      <div class="pc-form-card-head">
        <h2>基本信息</h2>
        <div>
          <span>单据编号</span>
          <strong>{{ isEditMode ? String(orderId) : '保存后自动生成' }}</strong>
        </div>
      </div>
      <div class="pc-form-grid">
        <label class="pc-field span-2">
          <span>客户 <b>*</b></span>
          <select :value="form.customerId" @change="syncCustomer(($event.target as HTMLSelectElement).value)">
            <option value="">手动录入客户</option>
            <option v-for="customer in customers" :key="customer.id" :value="String(customer.id)">{{ customer.name }}</option>
          </select>
        </label>
        <label class="pc-field">
          <span>销售日期 <b>*</b></span>
          <input v-model="form.saleDate" type="date" />
        </label>
        <label class="pc-field">
          <span>销售员</span>
          <select v-model="form.salesperson">
            <option>张经理</option>
            <option>{{ session.member.value.name }}</option>
          </select>
        </label>
        <label class="pc-field span-2">
          <span>客户名称</span>
          <input v-model="form.customerName" placeholder="散客 / 客户名称" />
        </label>
        <label class="pc-field">
          <span>发货仓库 <b>*</b></span>
          <select v-model="form.warehouse">
            <option>主仓库 (深圳南山)</option>
            <option>分仓 (广州天河)</option>
          </select>
        </label>
        <label class="pc-field">
          <span>整单折扣</span>
          <input v-model="form.discountAmount" type="number" min="0" step="0.01" />
        </label>
        <label class="pc-field span-4">
          <span>单据备注</span>
          <input v-model="form.notes" placeholder="请输入备注信息" />
        </label>
      </div>
    </section>

    <section class="pc-form-card pc-lines-card">
      <div class="pc-lines-head">
        <h2>商品明细</h2>
        <div>
          <button type="button" class="pc-secondary-action">
            <span class="material-symbols-outlined">qr_code_scanner</span>
            扫码添加
          </button>
          <button type="button" class="pc-primary-soft-action" @click="addLine">
            <span class="material-symbols-outlined">add</span>
            添加商品
          </button>
        </div>
      </div>

      <div class="pc-table-scroll">
        <table class="pc-data-table pc-edit-table">
          <thead>
            <tr>
              <th class="align-center">序号</th>
              <th>商品名称 / 编号</th>
              <th>规格型号</th>
              <th>单位</th>
              <th class="align-right">数量</th>
              <th class="align-right">单价 (¥)</th>
              <th class="align-right">折扣 (%)</th>
              <th class="align-right">金额 (¥)</th>
              <th>备注</th>
              <th class="align-center">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in lineRows" :key="row.index">
              <template v-if="row.line">
                <td class="align-center muted">{{ row.index + 1 }}</td>
                <td>
                  <select class="pc-line-input" :value="row.line.productId" @change="syncProduct(row.index, ($event.target as HTMLSelectElement).value)">
                    <option value="">输入商品拼音/条码查询</option>
                    <option v-for="product in products" :key="product.id" :value="String(product.id)">
                      {{ product.name }} / {{ product.code }}
                    </option>
                  </select>
                </td>
                <td class="muted">{{ row.product?.categoryName || '-' }}</td>
                <td class="muted">{{ row.product?.unitName || '-' }}</td>
                <td><input v-model="row.line.quantity" class="pc-line-input align-right" type="number" min="0" step="0.01" /></td>
                <td><input v-model="row.line.unitPrice" class="pc-line-input align-right" type="number" min="0" step="0.01" /></td>
                <td><input v-model="row.line.discountPercent" class="pc-line-input align-right" type="number" min="0" max="100" step="1" /></td>
                <td class="align-right amount-strong">{{ formatCurrency(row.amount) }}</td>
                <td><input v-model="row.line.remark" class="pc-line-input" placeholder="-" /></td>
                <td class="align-center">
                  <button type="button" class="pc-delete-line" @click="removeLine(row.index)">
                    <span class="material-symbols-outlined">delete</span>
                  </button>
                </td>
              </template>
            </tr>
          </tbody>
        </table>
      </div>

      <button type="button" class="pc-add-line" @click="addLine">
        <span class="material-symbols-outlined">add_circle</span>
        添加一行
      </button>
    </section>

    <footer class="pc-form-totalbar">
      <div class="pc-total-items">
        <div>
          <span>合计数量</span>
          <strong>{{ totalQuantity }}</strong>
        </div>
        <div>
          <span>整单金额</span>
          <strong>{{ formatCurrency(subtotalAmount) }}</strong>
        </div>
        <div>
          <span>折扣总额</span>
          <strong class="danger">- {{ formatCurrency(Number(form.discountAmount || 0)) }}</strong>
        </div>
        <div class="pc-total-receivable">
          <span>应收金额</span>
          <strong>{{ formatCurrency(orderAmount) }}</strong>
        </div>
      </div>
      <div class="pc-total-actions">
        <button type="button" class="pc-secondary-action">
          <span class="material-symbols-outlined">print</span>
          打印单据
        </button>
        <button type="button" class="pc-secondary-action" :disabled="!canSubmit" @click="saveDraft">
          保存为草稿
        </button>
        <button type="button" class="pc-dark-action" :disabled="!canSubmit" @click="saveAndConfirm">
          {{ saving ? '保存中...' : '保存并出库' }}
        </button>
      </div>
    </footer>
  </section>
</template>
