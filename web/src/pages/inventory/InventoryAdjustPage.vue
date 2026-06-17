<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSession } from '@/app/stores/session'
import {
  createInventoryLedgerEntry,
  fetchInventoryLedger,
  fetchProducts,
  type InventoryLedgerEntry,
  type ProductRecord,
} from '@/shared/api/client'
import {
  formatCurrency,
  formatDateTime,
  formatNumber,
  inventoryTrendLabel,
} from '@/shared/utils/business'

const route = useRoute()
const router = useRouter()
const session = useSession()

const products = ref<ProductRecord[]>([])
const ledgerEntries = ref<InventoryLedgerEntry[]>([])
const loading = ref(false)
const submitting = ref(false)
const error = ref('')
const success = ref('')
const searchKeyword = ref('')
const selectedProductId = ref<number | null>(null)

const form = reactive({
  adjustmentType: '盘亏',
  quantity: '1',
  sourceNo: '',
  notes: '',
})

const adjustmentOptions = [
  { label: '盘亏', sign: -1, sourceType: 'STOCK_LOSS' },
  { label: '盘盈', sign: 1, sourceType: 'STOCK_GAIN' },
  { label: '领用', sign: -1, sourceType: 'MANUAL_USE' },
  { label: '入库补录', sign: 1, sourceType: 'MANUAL_INBOUND' },
]

const queryProductId = computed(() => {
  const raw = route.query.productId
  const first = Array.isArray(raw) ? raw[0] : raw
  const parsed = Number(first)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null
})
const isApiSource = computed(() => session.source.value === 'api' && Boolean(session.token.value))
const canWrite = computed(() => session.hasPermission(['inventory:write']))
const selectedProduct = computed(() => products.value.find((item) => item.id === selectedProductId.value) ?? products.value[0] ?? null)
const filteredProducts = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return products.value
  return products.value.filter((item) => item.name.toLowerCase().includes(keyword) || item.code.toLowerCase().includes(keyword))
})
const activeAdjustment = computed(() => adjustmentOptions.find((item) => item.label === form.adjustmentType) ?? adjustmentOptions[0])
const projectedStock = computed(() => {
  if (!selectedProduct.value) return 0
  return selectedProduct.value.stock + (Number(form.quantity || 0) * activeAdjustment.value.sign)
})

watch(
  [() => session.source.value, () => session.token.value, queryProductId],
  async () => {
    if (!isApiSource.value || !session.token.value) {
      products.value = []
      ledgerEntries.value = []
      error.value = ''
      return
    }
    await loadPage()
  },
  { immediate: true },
)

watch(selectedProductId, async (productId) => {
  if (!session.token.value || !productId) return
  await loadLedger(productId)
})

async function loadPage() {
  if (!session.token.value) return
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    products.value = await fetchProducts(session.token.value, { page: 0, size: 200 })
    selectedProductId.value = queryProductId.value ?? products.value[0]?.id ?? null
    if (selectedProductId.value) {
      await loadLedger(selectedProductId.value)
    }
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : '库存调整页面加载失败'
  } finally {
    loading.value = false
  }
}

async function loadLedger(productId: number) {
  if (!session.token.value) return
  ledgerEntries.value = await fetchInventoryLedger(session.token.value, { productId })
}

async function submitAdjustment() {
  if (!session.token.value || !selectedProduct.value) return
  submitting.value = true
  error.value = ''
  success.value = ''
  try {
    await createInventoryLedgerEntry(session.token.value, {
      productId: selectedProduct.value.id,
      sourceType: activeAdjustment.value.sourceType,
      sourceNo: form.sourceNo.trim() || null,
      quantityChange: Number(form.quantity || 0) * activeAdjustment.value.sign,
      unitCost: selectedProduct.value.purchasePrice,
      notes: form.notes.trim() || form.adjustmentType,
    })
    success.value = `${selectedProduct.value.name} 已完成库存调整`
    form.quantity = '1'
    form.sourceNo = ''
    form.notes = ''
    await loadPage()
  } catch (submitErr) {
    error.value = submitErr instanceof Error ? submitErr.message : '库存调整提交失败'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="business-page">
    <section class="screen-hero">
      <div>
        <p class="eyebrow">库存调整 / Inventory Adjust</p>
        <h2>库存调整专页</h2>
        <p>按真实商品库存与 `/v2/inventory/ledger` 写入调整流水，替代原先仅更新商品库存的临时逻辑。</p>
      </div>
      <div class="hero-actions">
        <button type="button" class="ghost-action" @click="router.push('/inventory/snapshots')">查看盘点</button>
      </div>
    </section>

    <p v-if="!isApiSource" class="form-error">当前是演示模式。这一页只在真实登录后写入库存流水。</p>
    <p v-else-if="error" class="form-error">{{ error }}</p>
    <p v-else-if="success" class="form-success">{{ success }}</p>
    <p v-if="loading" class="form-success">正在加载商品与库存流水...</p>

    <section class="business-split">
      <article class="panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">商品选择</p>
            <h3>{{ selectedProduct?.name || '请选择商品' }}</h3>
          </div>
        </div>

        <div class="business-toolbar">
          <label class="search-box">
            <span>搜索商品</span>
            <input v-model="searchKeyword" placeholder="商品名称 / 编码" />
          </label>
        </div>

        <div class="table-shell">
          <table>
            <thead>
              <tr>
                <th>编码</th>
                <th>商品</th>
                <th>库存</th>
                <th>安全库存</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="product in filteredProducts"
                :key="product.id"
                :class="{ selected: product.id === selectedProduct?.id }"
                @click="selectedProductId = product.id"
              >
                <td>{{ product.code }}</td>
                <td>{{ product.name }}</td>
                <td>{{ formatNumber(product.stock) }}</td>
                <td>{{ formatNumber(product.safeStock) }}</td>
                <td>{{ inventoryTrendLabel(product.stock, product.safeStock) }}</td>
              </tr>
              <tr v-if="!loading && filteredProducts.length === 0">
                <td colspan="5" class="empty-cell">暂无可调整商品</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <aside class="panel detail-panel">
        <p class="eyebrow">调整表单</p>
        <h3>{{ selectedProduct?.name || '未选商品' }}</h3>

        <div v-if="selectedProduct" class="detail-stack">
          <article class="detail-card">
            <dl class="detail-list">
              <div>
                <dt>当前库存</dt>
                <dd>{{ formatNumber(selectedProduct.stock) }}</dd>
              </div>
              <div>
                <dt>安全库存</dt>
                <dd>{{ formatNumber(selectedProduct.safeStock) }}</dd>
              </div>
              <div>
                <dt>调整后库存</dt>
                <dd>{{ formatNumber(projectedStock) }}</dd>
              </div>
              <div>
                <dt>库存金额</dt>
                <dd>{{ formatCurrency(selectedProduct.stock * selectedProduct.purchasePrice) }}</dd>
              </div>
            </dl>
          </article>

          <label class="compact-field">
            <span>调整类型</span>
            <select v-model="form.adjustmentType">
              <option v-for="option in adjustmentOptions" :key="option.label" :value="option.label">{{ option.label }}</option>
            </select>
          </label>
          <label class="compact-field">
            <span>调整数量</span>
            <input v-model="form.quantity" class="table-input" type="number" min="0" step="0.01" />
          </label>
          <label class="compact-field">
            <span>来源单号</span>
            <input v-model="form.sourceNo" class="table-input" placeholder="可选，如盘点单号" />
          </label>
          <label class="compact-field">
            <span>备注</span>
            <textarea v-model="form.notes" rows="4" placeholder="记录调整原因与说明" />
          </label>

          <div class="form-actions">
            <button type="button" :disabled="submitting || !isApiSource || !canWrite" @click="submitAdjustment">
              {{ submitting ? '提交中...' : '提交库存调整' }}
            </button>
          </div>

          <article class="detail-card">
            <p class="eyebrow">最近库存流水</p>
            <div v-if="ledgerEntries.length" class="mini-list">
              <div v-for="entry in ledgerEntries.slice(0, 6)" :key="entry.id">
                <strong>{{ entry.sourceType }} / {{ formatNumber(entry.quantityChange) }}</strong>
                <span>{{ formatDateTime(entry.createdAt) }}</span>
              </div>
            </div>
            <p v-else class="muted">当前商品还没有库存流水。</p>
          </article>
        </div>

        <div v-else class="empty-preview">
          <strong>未选择商品</strong>
          <p>请先从左侧选择需要调整库存的商品。</p>
        </div>
      </aside>
    </section>
  </section>
</template>
