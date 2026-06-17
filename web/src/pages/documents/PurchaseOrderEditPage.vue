<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSession } from '@/app/stores/session'
import {
  createPurchaseOrder,
  fetchProducts,
  fetchPurchaseOrder,
  fetchSuppliers,
  updatePurchaseOrder,
  type ProductRecord,
  type PurchaseOrderItem,
  type PurchaseOrderWritePayload,
  type SupplierRecord,
} from '@/shared/api/client'
import { readQueryId } from '@/shared/utils/id'
import { formatCurrency } from '@/shared/utils/business'

interface LineItemForm {
  productId: string
  productCode: string
  productName: string
  quantity: string
  unitCost: string
}

const route = useRoute()
const router = useRouter()
const session = useSession()

const suppliers = ref<SupplierRecord[]>([])
const products = ref<ProductRecord[]>([])
const lines = ref<LineItemForm[]>([createEmptyLine()])
const loading = ref(false)
const saving = ref(false)
const error = ref('')

const form = reactive({
  supplierId: '',
  supplierName: '',
  notes: '',
})

const orderId = computed(() => readQueryId(route.query.id))
const isApiSource = computed(() => session.source.value === 'api' && Boolean(session.token.value))
const isEditMode = computed(() => orderId.value != null)
const canSubmit = computed(() => {
  const filledLines = normalizedItems.value
  return isApiSource.value && !saving.value && filledLines.length > 0
})
const orderAmount = computed(() => normalizedItems.value.reduce((sum, item) => sum + item.quantity * item.unitCost, 0))
const normalizedItems = computed(() => {
  return lines.value
    .filter((line) => Number(line.quantity) > 0 && (line.productId || line.productCode.trim() || line.productName.trim()))
    .map((line) => ({
      productId: line.productId ? Number(line.productId) : null,
      productCode: line.productCode.trim() || null,
      productName: line.productName.trim() || null,
      quantity: Number(line.quantity || 0),
      unitCost: Number(line.unitCost || 0),
    }))
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
    const [nextSuppliers, nextProducts] = await Promise.all([
      fetchSuppliers(session.token.value, { page: 0, size: 200, status: 1 }),
      fetchProducts(session.token.value, { page: 0, size: 200, status: 1 }),
    ])
    suppliers.value = nextSuppliers
    products.value = nextProducts

    if (orderId.value) {
      const order = await fetchPurchaseOrder(session.token.value, orderId.value)
      form.supplierId = order.supplierId ? String(order.supplierId) : ''
      form.supplierName = order.supplierName || ''
      form.notes = order.notes || ''
      lines.value = order.items.length > 0 ? order.items.map((item) => toLineItem(item, nextProducts)) : [createEmptyLine()]
    } else {
      resetForm()
    }
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : '采购单资料加载失败'
  } finally {
    loading.value = false
  }
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

function syncSupplier(supplierId: string) {
  const supplier = suppliers.value.find((item) => item.id === Number(supplierId))
  form.supplierId = supplierId
  form.supplierName = supplier?.name || ''
}

function syncProduct(index: number, productId: string) {
  const product = products.value.find((item) => item.id === Number(productId))
  const current = lines.value[index]
  current.productId = productId
  if (product) {
    current.productCode = product.code
    current.productName = product.name
    current.unitCost = String(product.purchasePrice)
  }
}

async function submitForm() {
  if (!session.token.value) return
  saving.value = true
  error.value = ''
  try {
    if (normalizedItems.value.length === 0) {
      throw new Error('请至少添加一行采购商品')
    }

    const payload: PurchaseOrderWritePayload = {
      supplierId: form.supplierId ? Number(form.supplierId) : null,
      supplierName: form.supplierName.trim() || null,
      notes: form.notes.trim() || null,
      items: normalizedItems.value,
    }

    const saved = orderId.value
      ? await updatePurchaseOrder(session.token.value, orderId.value, payload)
      : await createPurchaseOrder(session.token.value, payload)

    await router.push({ path: '/documents/purchases/detail', query: { id: String(saved.id) } })
  } catch (saveErr) {
    error.value = saveErr instanceof Error ? saveErr.message : '采购单保存失败'
  } finally {
    saving.value = false
  }
}

function resetForm() {
  form.supplierId = ''
  form.supplierName = ''
  form.notes = ''
  lines.value = [createEmptyLine()]
}

function createEmptyLine(): LineItemForm {
  return {
    productId: '',
    productCode: '',
    productName: '',
    quantity: '1',
    unitCost: '0',
  }
}

function toLineItem(item: PurchaseOrderItem, sourceProducts: ProductRecord[]): LineItemForm {
  const matchedProduct = sourceProducts.find((product) => {
    if (item.productCode && product.code === item.productCode) return true
    if (item.productName && product.name === item.productName) return true
    return false
  })

  return {
    productId: matchedProduct ? String(matchedProduct.id) : '',
    productCode: item.productCode || matchedProduct?.code || '',
    productName: item.productName || matchedProduct?.name || '',
    quantity: String(item.quantity),
    unitCost: String(item.unitCost),
  }
}
</script>

<template>
  <section class="business-page">
    <section class="screen-hero">
      <div>
        <p class="eyebrow">采购开单 / Purchase Draft</p>
        <h2>{{ isEditMode ? '编辑采购单' : '新建采购单' }}</h2>
        <p>对齐安卓端采购开单字段，按真实供应商、商品、数量和进货价保存采购单。</p>
      </div>
      <div class="hero-actions">
        <button type="button" class="ghost-action" @click="router.push('/documents/purchases')">返回列表</button>
        <button
          v-if="isEditMode && orderId"
          type="button"
          class="ghost-action"
          @click="router.push({ path: '/documents/purchases/detail', query: { id: String(orderId) } })"
        >
          查看详情
        </button>
      </div>
    </section>

    <p v-if="!isApiSource" class="form-error">当前是演示模式。这一页只在真实登录后保存采购单。</p>
    <p v-else-if="error" class="form-error">{{ error }}</p>
    <p v-else-if="loading" class="form-success">正在加载采购单资料...</p>

    <section class="business-split">
      <article class="panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">采购表单</p>
            <h3>供应商与商品明细</h3>
          </div>
          <span class="session-source">采购合计 {{ formatCurrency(orderAmount) }}</span>
        </div>

        <form class="partner-form product-form" @submit.prevent="submitForm">
          <label>
            <span>快捷选择供应商</span>
            <select :value="form.supplierId" @change="syncSupplier(($event.target as HTMLSelectElement).value)">
              <option value="">手动录入供应商</option>
              <option v-for="supplier in suppliers" :key="supplier.id" :value="String(supplier.id)">
                {{ supplier.name }}
              </option>
            </select>
          </label>
          <label>
            <span>供应商名称</span>
            <input v-model="form.supplierName" placeholder="供应商名称" />
          </label>
          <label class="wide-field">
            <span>备注</span>
            <textarea v-model="form.notes" rows="3" placeholder="采购备注、交付说明等" />
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
                  <th>编码</th>
                  <th>名称</th>
                  <th>数量</th>
                  <th>进货价</th>
                  <th>小计</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(line, index) in lines" :key="index">
                  <td>
                    <select class="table-input" :value="line.productId" @change="syncProduct(index, ($event.target as HTMLSelectElement).value)">
                      <option value="">手动录入商品</option>
                      <option v-for="product in products" :key="product.id" :value="String(product.id)">
                        {{ product.name }} / {{ product.code }}
                      </option>
                    </select>
                  </td>
                  <td><input v-model="line.productCode" class="table-input" placeholder="商品编码" /></td>
                  <td><input v-model="line.productName" class="table-input" placeholder="商品名称" /></td>
                  <td><input v-model="line.quantity" class="table-input" type="number" min="0" step="0.01" /></td>
                  <td><input v-model="line.unitCost" class="table-input" type="number" min="0" step="0.01" /></td>
                  <td>{{ formatCurrency((Number(line.quantity) || 0) * (Number(line.unitCost) || 0)) }}</td>
                  <td><button type="button" class="ghost-action" @click="removeLine(index)">删除</button></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="form-actions">
          <button type="button" :disabled="!canSubmit" @click="submitForm">{{ saving ? '保存中...' : '保存采购单' }}</button>
          <button type="button" class="ghost-action" :disabled="saving" @click="resetForm">重置表单</button>
        </div>
      </article>

      <aside class="panel detail-panel">
        <p class="eyebrow">采购摘要</p>
        <h3>当前草稿总览</h3>

        <div class="detail-stack">
          <article class="detail-card">
            <dl class="detail-list">
              <div>
                <dt>供应商</dt>
                <dd>{{ form.supplierName || '未填写' }}</dd>
              </div>
              <div>
                <dt>商品行数</dt>
                <dd>{{ normalizedItems.length }}</dd>
              </div>
              <div>
                <dt>采购合计</dt>
                <dd>{{ formatCurrency(orderAmount) }}</dd>
              </div>
            </dl>
          </article>

          <article class="detail-card">
            <p class="eyebrow">待保存商品</p>
            <div v-if="normalizedItems.length" class="mini-list">
              <div v-for="(item, index) in normalizedItems.slice(0, 6)" :key="`${item.productId}-${item.productCode}-${index}`">
                <strong>{{ item.productName || item.productCode || '未命名商品' }}</strong>
                <span>{{ item.quantity }} x {{ formatCurrency(item.unitCost) }}</span>
              </div>
            </div>
            <p v-else class="muted">请至少补充一行采购商品。</p>
          </article>

          <article class="detail-card">
            <p class="eyebrow">保存说明</p>
            <p class="muted">当前专页按真实采购单接口保存，不额外模拟审批、付款或退货流程。</p>
          </article>
        </div>
      </aside>
    </section>
  </section>
</template>
