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
  discountPercent: string
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
const extraFee = ref('0')

const form = reactive({
  supplierId: '',
  supplierName: '',
  purchaseDate: new Date().toISOString().slice(0, 10),
  expectedArrivalDate: '',
  warehouse: '主仓库 (深圳)',
  purchaser: 'Admin User',
  notes: '',
})

const orderId = computed(() => readQueryId(route.query.id))
const isApiSource = computed(() => session.source.value === 'api' && Boolean(session.token.value))
const isEditMode = computed(() => orderId.value != null)
const canWrite = computed(() => session.hasPermission(['purchase:write']))
const normalizedItems = computed(() => {
  return lines.value
    .filter((line) => Number(line.quantity) > 0 && (line.productId || line.productCode.trim() || line.productName.trim()))
    .map((line) => ({
      productId: line.productId ? Number(line.productId) : null,
      productCode: line.productCode.trim() || null,
      productName: line.productName.trim() || null,
      quantity: Number(line.quantity || 0),
      unitCost: lineUnitCost(line),
    }))
})
const canSubmit = computed(() => canWrite.value && isApiSource.value && !saving.value && normalizedItems.value.length > 0)
const goodsQuantity = computed(() => normalizedItems.value.reduce((sum, item) => sum + item.quantity, 0))
const goodsAmount = computed(() => normalizedItems.value.reduce((sum, item) => sum + item.quantity * item.unitCost, 0))
const payableAmount = computed(() => goodsAmount.value + (Number(extraFee.value) || 0))

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
      form.purchaseDate = new Date(order.createdAt).toISOString().slice(0, 10)
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

function productFor(line: LineItemForm) {
  return products.value.find((item) => item.id === Number(line.productId))
}

function lineUnitCost(line: LineItemForm) {
  const discount = Math.max(0, Number(line.discountPercent || 100)) / 100
  return (Number(line.unitCost) || 0) * discount
}

function lineAmount(line: LineItemForm) {
  return (Number(line.quantity) || 0) * lineUnitCost(line)
}

async function submitForm(next: 'detail' | 'receipt' = 'detail') {
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

    if (next === 'receipt') {
      await router.push({ path: '/documents/purchase-receipts', query: { orderId: String(saved.id) } })
      return
    }
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
  form.purchaseDate = new Date().toISOString().slice(0, 10)
  form.expectedArrivalDate = ''
  form.warehouse = '主仓库 (深圳)'
  form.purchaser = session.member.value.name || 'Admin User'
  form.notes = ''
  extraFee.value = '0'
  lines.value = [createEmptyLine()]
}

function createEmptyLine(): LineItemForm {
  return {
    productId: '',
    productCode: '',
    productName: '',
    quantity: '1',
    unitCost: '0',
    discountPercent: '100',
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
    discountPercent: '100',
  }
}
</script>

<template>
  <section class="pc-form-page purchase-edit-page">
    <header class="pc-list-titlebar">
      <div class="pc-breadcrumb">
        <span>采购管理</span>
        <span class="material-symbols-outlined">chevron_right</span>
        <h1>{{ isEditMode ? '编辑采购单' : '新建采购单' }}</h1>
      </div>
      <div class="pc-title-actions">
        <button type="button" class="pc-icon-action" aria-label="历史记录">
          <span class="material-symbols-outlined">history</span>
        </button>
        <button type="button" class="pc-icon-action" aria-label="帮助">
          <span class="material-symbols-outlined">help_outline</span>
        </button>
        <button type="button" class="pc-secondary-action">
          <span class="material-symbols-outlined">upload_file</span>
          导入外部单据
        </button>
      </div>
    </header>

    <p v-if="!isApiSource" class="form-error">当前是演示模式。这一页只在真实登录后保存采购单。</p>
    <p v-else-if="error" class="form-error">{{ error }}</p>
    <p v-else-if="loading" class="form-success">正在加载采购单资料...</p>

    <section class="pc-form-card">
      <div class="pc-form-card-head pc-form-card-head--bordered">
        <div class="pc-section-title">
          <span class="material-symbols-outlined">info</span>
          <h2>基本信息</h2>
        </div>
        <div>
          <span>单号</span>
          <strong>{{ isEditMode ? String(orderId) : 'PO-自动生成' }}</strong>
        </div>
      </div>
      <div class="pc-form-grid purchase-form-grid">
        <label class="pc-field">
          <span>供应商 <b>*</b></span>
          <select :value="form.supplierId" @change="syncSupplier(($event.target as HTMLSelectElement).value)">
            <option value="">手动录入供应商</option>
            <option v-for="supplier in suppliers" :key="supplier.id" :value="String(supplier.id)">{{ supplier.name }}</option>
          </select>
        </label>
        <label class="pc-field">
          <span>采购日期 <b>*</b></span>
          <input v-model="form.purchaseDate" type="date" />
        </label>
        <label class="pc-field">
          <span>预计到货时间</span>
          <input v-model="form.expectedArrivalDate" type="date" />
        </label>
        <label class="pc-field">
          <span>收货仓库 <b>*</b></span>
          <select v-model="form.warehouse">
            <option>主仓库 (深圳)</option>
            <option>次仓库 (广州)</option>
          </select>
        </label>
        <label class="pc-field">
          <span>采购员</span>
          <select v-model="form.purchaser">
            <option>{{ session.member.value.name }}</option>
            <option>Admin User</option>
          </select>
        </label>
        <label class="pc-field">
          <span>备注</span>
          <input v-model="form.notes" placeholder="添加备注信息" />
        </label>
        <label class="pc-field span-3">
          <span>供应商名称</span>
          <input v-model="form.supplierName" placeholder="搜索供应商名称/拼音首字母" />
        </label>
      </div>
    </section>

    <section class="pc-form-card pc-lines-card">
      <div class="pc-lines-head">
        <div class="pc-section-title">
          <span class="material-symbols-outlined">list_alt</span>
          <h2>商品明细</h2>
        </div>
        <div>
          <button type="button" class="pc-secondary-action">
            <span class="material-symbols-outlined">barcode_scanner</span>
            扫码录入
          </button>
          <button type="button" class="pc-primary-soft-action" @click="addLine">
            <span class="material-symbols-outlined">add</span>
            添加商品
          </button>
        </div>
      </div>

      <div class="pc-table-scroll">
        <table class="pc-data-table pc-edit-table purchase-edit-table">
          <thead>
            <tr>
              <th class="align-center">序号</th>
              <th>商品名称/编码</th>
              <th>规格</th>
              <th>单位</th>
              <th class="align-right">数量</th>
              <th class="align-right">单价 (含税)</th>
              <th class="align-right">折扣 (%)</th>
              <th class="align-right">总额</th>
              <th class="align-center">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(line, index) in lines" :key="index">
              <td class="align-center muted">{{ index + 1 }}</td>
              <td>
                <select class="pc-line-input" :value="line.productId" @change="syncProduct(index, ($event.target as HTMLSelectElement).value)">
                  <option value="">点击选择或手动录入商品</option>
                  <option v-for="product in products" :key="product.id" :value="String(product.id)">{{ product.name }} / {{ product.code }}</option>
                </select>
                <input v-model="line.productName" class="pc-line-input pc-line-input--sub" placeholder="商品名称" />
              </td>
              <td class="muted">{{ productFor(line)?.categoryName || '-' }}</td>
              <td>{{ productFor(line)?.unitName || '-' }}</td>
              <td><input v-model="line.quantity" class="pc-line-input align-right" type="number" min="0" step="0.01" /></td>
              <td><input v-model="line.unitCost" class="pc-line-input align-right" type="number" min="0" step="0.01" /></td>
              <td><input v-model="line.discountPercent" class="pc-line-input align-right" type="number" min="0" max="100" step="1" /></td>
              <td class="align-right amount-strong">{{ formatCurrency(lineAmount(line)) }}</td>
              <td class="align-center">
                <button type="button" class="pc-delete-line" @click="removeLine(index)">
                  <span class="material-symbols-outlined">delete</span>
                </button>
              </td>
            </tr>
            <tr>
              <td colspan="9">
                <button type="button" class="pc-dashed-add-line" @click="addLine">
                  <span class="material-symbols-outlined">add_circle</span>
                  点击添加新行，或直接输入商品名称/条码搜索
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <div class="pc-extra-grid">
      <section class="pc-form-card pc-extra-card">
        <h2>附加费用</h2>
        <div class="pc-extra-row">
          <select>
            <option>运费</option>
            <option>装卸费</option>
            <option>其他</option>
          </select>
          <input v-model="extraFee" type="number" min="0" step="0.01" placeholder="0.00" />
          <button type="button" class="pc-delete-line"><span class="material-symbols-outlined">close</span></button>
        </div>
        <button type="button" class="pc-inline-add"><span class="material-symbols-outlined">add</span> 添加费用项</button>
      </section>

      <section class="pc-form-card pc-extra-card">
        <h2>附件</h2>
        <div class="pc-upload-box">
          <span class="material-symbols-outlined">cloud_upload</span>
          <strong>点击或拖拽文件到此处上传</strong>
          <p>支持 PDF, JPG, PNG 格式，单个最大 10MB</p>
        </div>
      </section>
    </div>

    <footer class="pc-form-totalbar">
      <div class="pc-total-items">
        <div>
          <span>商品总数</span>
          <strong>{{ goodsQuantity }} 件</strong>
        </div>
        <div>
          <span>附加费用</span>
          <strong>{{ formatCurrency(Number(extraFee || 0)) }}</strong>
        </div>
        <div class="pc-total-receivable">
          <span>本单应付总额</span>
          <strong>{{ formatCurrency(payableAmount) }}</strong>
        </div>
      </div>
      <div class="pc-total-actions">
        <button type="button" class="pc-secondary-action" :disabled="!canSubmit" @click="submitForm('detail')">暂存为草稿</button>
        <button type="button" class="pc-secondary-emphasis-action" :disabled="!canSubmit" @click="submitForm('receipt')">直接入库</button>
        <button type="button" class="pc-dark-action" :disabled="!canSubmit" @click="submitForm('detail')">
          <span class="material-symbols-outlined">send</span>
          {{ saving ? '保存中...' : '提交审批' }}
        </button>
      </div>
    </footer>
  </section>
</template>
