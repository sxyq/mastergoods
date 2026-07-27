<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSession } from '@/app/stores/session'
import {
  fetchInventoryLedger,
  fetchProducts,
  type InventoryLedgerEntry,
  type ProductRecord,
} from '@/shared/api/client'
import {
  formatCurrency,
  formatDateTime,
  formatNumber,
  inventorySourceLabel,
  inventoryTrendLabel,
} from '@/shared/utils/business'
import { readQueryId, sameEntityId, type EntityId } from '@/shared/utils/id'

const route = useRoute()
const router = useRouter()
const session = useSession()

const products = ref<ProductRecord[]>([])
const ledgerEntries = ref<InventoryLedgerEntry[]>([])
const loadingProducts = ref(false)
const loadingLedger = ref(false)
const error = ref('')
const searchKeyword = ref('')
const rangeDays = ref('30')
const selectedProductId = ref<EntityId | null>(null)
const pageReady = ref(false)

const queryProductId = computed(() => readQueryId(route.query.productId))
const isApiSource = computed(() => session.source.value === 'api' && Boolean(session.token.value))
const canAdjust = computed(() => session.hasPermission(['inventory:write']))
const productIndex = computed(() => new Map(products.value.map((item) => [String(item.id), item] as const)))
const selectedProduct = computed(() => {
  if (selectedProductId.value == null) return products.value[0] ?? null
  return productIndex.value.get(String(selectedProductId.value)) ?? products.value[0] ?? null
})
const filteredProducts = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return products.value
  return products.value.filter((item) => `${item.name} ${item.code}`.toLowerCase().includes(keyword))
})
const currentBalance = computed(() => ledgerEntries.value[0]?.quantityAfter ?? selectedProduct.value?.stock ?? 0)
const ledgerSummary = computed(() => ledgerEntries.value.reduce((summary, item) => {
  if (item.quantityChange > 0) {
    summary.totalIn += item.quantityChange
  } else if (item.quantityChange < 0) {
    summary.totalOut += Math.abs(item.quantityChange)
  }
  return summary
}, {
  totalIn: 0,
  totalOut: 0,
}))
const totalIn = computed(() => ledgerSummary.value.totalIn)
const totalOut = computed(() => ledgerSummary.value.totalOut)
const balanceAmount = computed(() => currentBalance.value * (selectedProduct.value?.purchasePrice ?? 0))
const startAt = computed(() => {
  if (rangeDays.value === 'all') return undefined
  const days = Number(rangeDays.value)
  if (!Number.isFinite(days) || days <= 0) return undefined
  return Date.now() - days * 24 * 60 * 60 * 1000
})

watch(
  [() => session.source.value, () => session.token.value, queryProductId],
  async () => {
    if (!isApiSource.value || !session.token.value) {
      products.value = []
      ledgerEntries.value = []
      error.value = ''
      pageReady.value = false
      return
    }
    await loadPage()
  },
  { immediate: true },
)

watch([selectedProductId, rangeDays], async () => {
  if (!pageReady.value || !session.token.value || !selectedProductId.value) return
  await loadLedger(selectedProductId.value)
})

async function loadPage() {
  if (!session.token.value) return
  loadingProducts.value = true
  error.value = ''
  pageReady.value = false
  try {
    products.value = await fetchProducts(session.token.value, { page: 0, size: 200 })
    selectedProductId.value = queryProductId.value ?? products.value[0]?.id ?? null
    pageReady.value = true
    if (selectedProductId.value) {
      await loadLedger(selectedProductId.value)
    } else {
      ledgerEntries.value = []
    }
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : '库存流水页面加载失败'
  } finally {
    loadingProducts.value = false
  }
}

async function loadLedger(productId: EntityId) {
  if (!session.token.value) return
  loadingLedger.value = true
  error.value = ''
  try {
    const entries = await fetchInventoryLedger(session.token.value, {
      productId,
      startAt: startAt.value,
    })
    ledgerEntries.value = entries
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : '库存流水加载失败'
  } finally {
    loadingLedger.value = false
  }
}

const quantityLabel = (value: number) => (value > 0 ? `+${formatNumber(value)}` : formatNumber(value))
</script>

<template>
  <section class="business-page">
    <section class="screen-hero">
      <div>
        <p class="eyebrow">库存流水 / Product Ledger</p>
        <h2>商品库存流水专页</h2>
        <p>按真实商品和 `/v2/inventory/ledger` 变动记录，查看结存、来源单号和库存变化轨迹。</p>
      </div>
      <div class="hero-actions">
        <button type="button" class="ghost-action" @click="router.push('/archives/products')">返回商品档案</button>
        <button
          type="button"
          :disabled="!selectedProduct || !canAdjust"
          @click="router.push({ path: '/inventory/adjust', query: selectedProduct ? { productId: String(selectedProduct.id) } : undefined })"
        >
          库存调整
        </button>
      </div>
    </section>

    <p v-if="!isApiSource" class="form-error">当前是演示模式。这一页只在真实登录后读取商品库存流水。</p>
    <p v-else-if="error" class="form-error">{{ error }}</p>
    <p v-else-if="loadingProducts || loadingLedger" class="form-success">正在加载商品与库存流水...</p>

    <section class="metrics-grid compact">
      <article class="metric-card" data-tone="blue">
        <span>当前结存</span>
        <strong>{{ formatNumber(currentBalance) }}</strong>
        <p>{{ selectedProduct?.unitName || '库存单位以商品档案为准' }}</p>
      </article>
      <article class="metric-card" data-tone="green">
        <span>累计入库</span>
        <strong>{{ formatNumber(totalIn) }}</strong>
        <p>{{ ledgerEntries.length }} 条真实流水</p>
      </article>
      <article class="metric-card" data-tone="orange">
        <span>累计出库</span>
        <strong>{{ formatNumber(totalOut) }}</strong>
        <p>库存金额 {{ formatCurrency(balanceAmount) }}</p>
      </article>
    </section>

    <section class="business-split">
      <article class="panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">商品选择</p>
            <h3>{{ selectedProduct?.name || '请选择商品' }}</h3>
          </div>
          <span v-if="selectedProduct" class="session-source">
            {{ inventoryTrendLabel(currentBalance, selectedProduct.safeStock) }}
          </span>
        </div>

        <div class="business-toolbar">
          <label class="search-box">
            <span>搜索商品</span>
            <input v-model="searchKeyword" placeholder="商品名称 / 编码" />
          </label>
          <label class="compact-field">
            <span>时间范围</span>
            <select v-model="rangeDays">
              <option value="7">近 7 天</option>
              <option value="30">近 30 天</option>
              <option value="90">近 90 天</option>
              <option value="all">全部时间</option>
            </select>
          </label>
        </div>

        <div class="table-shell">
          <table>
            <thead>
              <tr>
                <th>编码</th>
                <th>商品</th>
                <th>当前库存</th>
                <th>安全库存</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="product in filteredProducts"
                :key="product.id"
                :class="{ selected: sameEntityId(product.id, selectedProduct?.id) }"
                @click="selectedProductId = product.id"
              >
                <td>{{ product.code }}</td>
                <td>{{ product.name }}</td>
                <td>{{ formatNumber(product.stock) }}</td>
                <td>{{ formatNumber(product.safeStock) }}</td>
                <td>{{ inventoryTrendLabel(product.stock, product.safeStock) }}</td>
              </tr>
              <tr v-if="!loadingProducts && filteredProducts.length === 0">
                <td colspan="5" class="empty-cell">暂无可查看商品</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <aside class="panel detail-panel">
        <p class="eyebrow">商品摘要</p>
        <h3>{{ selectedProduct?.name || '未选商品' }}</h3>

        <div v-if="selectedProduct" class="detail-stack">
          <article class="detail-card">
            <dl class="detail-list">
              <div>
                <dt>商品编码</dt>
                <dd>{{ selectedProduct.code }}</dd>
              </div>
              <div>
                <dt>分类 / 单位</dt>
                <dd>{{ selectedProduct.categoryName || '--' }} / {{ selectedProduct.unitName || '--' }}</dd>
              </div>
              <div>
                <dt>当前结存</dt>
                <dd>{{ formatNumber(currentBalance) }}</dd>
              </div>
              <div>
                <dt>进货价</dt>
                <dd>{{ formatCurrency(selectedProduct.purchasePrice) }}</dd>
              </div>
            </dl>
          </article>

          <article class="detail-card">
            <p class="eyebrow">页面说明</p>
            <p class="muted">当前专页直接读取真实库存流水，不再生成前端模拟库存记录。</p>
          </article>
        </div>

        <div v-else class="empty-preview">
          <strong>暂无商品摘要</strong>
          <p>请先从左侧选择一个商品。</p>
        </div>
      </aside>
    </section>

    <section class="panel">
      <div class="panel-head">
        <div>
          <p class="eyebrow">库存变动记录</p>
          <h3>{{ selectedProduct?.name || '当前商品' }} 的真实流水</h3>
        </div>
        <span class="session-source">{{ ledgerEntries.length }} 条</span>
      </div>

      <div v-if="ledgerEntries.length" class="ledger-stream">
        <article v-for="entry in ledgerEntries" :key="entry.id" class="ledger-entry">
          <div class="ledger-entry-head">
            <div>
              <strong>{{ inventorySourceLabel(entry.sourceType) }}</strong>
              <p>{{ formatDateTime(entry.createdAt) }}</p>
            </div>
            <span :class="entry.quantityChange >= 0 ? 'ledger-change-positive' : 'ledger-change-negative'">
              {{ quantityLabel(entry.quantityChange) }}
            </span>
          </div>

          <div class="ledger-entry-meta">
            <span>单号：{{ entry.sourceNo || '--' }}</span>
            <span>变动前：{{ formatNumber(entry.quantityBefore) }}</span>
            <span>结存：{{ formatNumber(entry.quantityAfter) }}</span>
            <span>单位成本：{{ formatCurrency(entry.unitCost || 0) }}</span>
          </div>

          <p v-if="entry.notes" class="ledger-entry-note">{{ entry.notes }}</p>
        </article>
      </div>

      <div v-else-if="!loadingLedger" class="empty-preview">
        <strong>暂无库存流水</strong>
        <p>当前商品在所选时间范围内还没有真实库存变动记录。</p>
      </div>
    </section>
  </section>
</template>
