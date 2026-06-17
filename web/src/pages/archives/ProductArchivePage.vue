<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useSession } from '@/app/stores/session'
import {
  fetchLowStockProducts,
  fetchProductCategories,
  fetchProducts,
  type ProductCategoryRecord,
  type ProductRecord,
} from '@/shared/api/client'
import {
  formatCurrency,
  formatDateTime,
  formatNumber,
  inventoryTrendLabel,
} from '@/shared/utils/business'

const router = useRouter()
const session = useSession()

const products = ref<ProductRecord[]>([])
const lowStockProducts = ref<ProductRecord[]>([])
const categories = ref<ProductCategoryRecord[]>([])
const loading = ref(false)
const error = ref('')
const searchKeyword = ref('')
const statusFilter = ref('all')
const categoryFilter = ref('all')
const selectedProductId = ref<number | null>(null)

const isApiSource = computed(() => session.source.value === 'api' && Boolean(session.token.value))
const canWrite = computed(() => session.hasPermission(['archives:write']))
const selectedProduct = computed(() => products.value.find((item) => item.id === selectedProductId.value) ?? products.value[0] ?? null)
const lowStockIds = computed(() => new Set(lowStockProducts.value.map((item) => item.id)))
const inventoryValue = computed(() => products.value.reduce((sum, item) => sum + item.stock * item.purchasePrice, 0))
const activeProducts = computed(() => products.value.filter((item) => item.status === 1).length)

watch(
  [
    () => session.source.value,
    () => session.token.value,
    searchKeyword,
    statusFilter,
    categoryFilter,
  ],
  async () => {
    if (!isApiSource.value || !session.token.value) {
      products.value = []
      lowStockProducts.value = []
      categories.value = []
      error.value = ''
      return
    }
    await loadProducts()
  },
  { immediate: true },
)

watch(selectedProduct, (product) => {
  if (product && selectedProductId.value == null) {
    selectedProductId.value = product.id
  }
})

async function loadProducts() {
  if (!session.token.value) return
  loading.value = true
  error.value = ''
  try {
    const [nextProducts, nextLowStockProducts, nextCategories] = await Promise.all([
      fetchProducts(session.token.value, {
        keyword: searchKeyword.value.trim() || undefined,
        status: statusFilter.value === 'all' ? undefined : Number(statusFilter.value),
        categoryId: categoryFilter.value === 'all' ? undefined : Number(categoryFilter.value),
        page: 0,
        size: 200,
      }),
      fetchLowStockProducts(session.token.value, 50),
      fetchProductCategories(session.token.value),
    ])
    products.value = nextProducts
    lowStockProducts.value = nextLowStockProducts
    categories.value = nextCategories.filter((item) => item.status === 1)
    if (!products.value.some((item) => item.id === selectedProductId.value)) {
      selectedProductId.value = products.value[0]?.id ?? null
    }
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : '商品档案加载失败'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  router.push('/archives/products/edit')
}

function openEdit(productId: number) {
  router.push({ path: '/archives/products/edit', query: { id: String(productId) } })
}

function openInventoryAdjust(productId: number) {
  router.push({ path: '/inventory/adjust', query: { productId: String(productId) } })
}

function openProductLedger(productId: number) {
  router.push({ path: '/inventory/product-ledger', query: { productId: String(productId) } })
}

function openSnapshots() {
  router.push('/inventory/snapshots')
}
</script>

<template>
  <section class="business-page">
    <section class="screen-hero">
      <div>
        <p class="eyebrow">商品档案 / Product Master</p>
        <h2>商品档案专页</h2>
        <p>按真实后端商品、分类、单位与库存预警加载，承接商品编辑与库存处理入口。</p>
      </div>
      <div class="hero-actions">
        <button type="button" :disabled="!canWrite || !isApiSource" @click="openCreate">新增商品</button>
        <button type="button" class="ghost-action" :disabled="!isApiSource" @click="openSnapshots">库存盘点</button>
      </div>
    </section>

    <p v-if="!isApiSource" class="form-error">当前是演示模式。这一页已按真实接口实现，登录后即可读取商品、分类和库存数据。</p>
    <p v-else-if="error" class="form-error">{{ error }}</p>
    <p v-else-if="loading" class="form-success">正在加载真实商品档案...</p>

    <section class="metrics-grid compact">
      <article class="metric-card" data-tone="blue">
        <span>商品总数</span>
        <strong>{{ products.length }}</strong>
        <p>{{ activeProducts }} 个启用商品</p>
      </article>
      <article class="metric-card" data-tone="orange">
        <span>低库存</span>
        <strong>{{ lowStockProducts.length }}</strong>
        <p>需要采购补货的商品</p>
      </article>
      <article class="metric-card" data-tone="green">
        <span>库存金额</span>
        <strong>{{ formatCurrency(inventoryValue) }}</strong>
        <p>按当前进货价估算</p>
      </article>
    </section>

    <section class="panel">
      <div class="business-toolbar">
        <label class="search-box">
          <span>搜索商品</span>
          <input v-model="searchKeyword" placeholder="商品名称 / 编码" />
        </label>
        <label class="compact-field">
          <span>状态</span>
          <select v-model="statusFilter">
            <option value="all">全部状态</option>
            <option value="1">启用</option>
            <option value="0">停用</option>
          </select>
        </label>
        <label class="compact-field">
          <span>分类</span>
          <select v-model="categoryFilter">
            <option value="all">全部分类</option>
            <option v-for="category in categories" :key="category.id" :value="String(category.id)">
              {{ category.name }}
            </option>
          </select>
        </label>
      </div>
    </section>

    <section class="business-split">
      <article class="panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">真实商品列表</p>
            <h3>商品主数据</h3>
          </div>
          <span class="session-source">{{ products.length }} 条记录</span>
        </div>

        <div class="table-shell">
          <table>
            <thead>
              <tr>
                <th>商品编码</th>
                <th>商品名称</th>
                <th>分类 / 单位</th>
                <th>零售价</th>
                <th>进货价</th>
                <th>当前库存</th>
                <th>状态</th>
                <th>更新时间</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="product in products"
                :key="product.id"
                :class="{ selected: product.id === selectedProduct?.id }"
                @click="selectedProductId = product.id"
              >
                <td>{{ product.code }}</td>
                <td>
                  {{ product.name }}
                  <small>{{ product.defaultSupplier?.supplierName || '暂无默认供应商' }}</small>
                </td>
                <td>{{ product.categoryName || '--' }} / {{ product.unitName || '--' }}</td>
                <td>{{ formatCurrency(product.salePrice) }}</td>
                <td>{{ formatCurrency(product.purchasePrice) }}</td>
                <td>{{ formatNumber(product.stock) }}</td>
                <td>
                  <span class="inline-status" :data-tone="inventoryTrendLabel(product.stock, product.safeStock)">
                    {{ lowStockIds.has(product.id) ? inventoryTrendLabel(product.stock, product.safeStock) : (product.status === 1 ? '启用' : '停用') }}
                  </span>
                </td>
                <td>{{ formatDateTime(product.updatedAt) }}</td>
              </tr>
              <tr v-if="!loading && products.length === 0">
                <td colspan="8" class="empty-cell">暂无商品数据</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <aside class="panel detail-panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">商品详情</p>
            <h3>{{ selectedProduct?.name || '请选择商品' }}</h3>
          </div>
        </div>

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
                <dt>零售价</dt>
                <dd>{{ formatCurrency(selectedProduct.salePrice) }}</dd>
              </div>
              <div>
                <dt>进货价</dt>
                <dd>{{ formatCurrency(selectedProduct.purchasePrice) }}</dd>
              </div>
              <div>
                <dt>当前库存</dt>
                <dd>{{ formatNumber(selectedProduct.stock) }}</dd>
              </div>
              <div>
                <dt>安全库存</dt>
                <dd>{{ formatNumber(selectedProduct.safeStock) }}</dd>
              </div>
            </dl>
          </article>

          <article class="detail-card">
            <p class="eyebrow">供应链</p>
            <strong>{{ selectedProduct.defaultSupplier?.supplierName || '暂无默认供应商' }}</strong>
            <p class="muted">{{ selectedProduct.supplierRelations.length }} 条供应商关系，更新时间 {{ formatDateTime(selectedProduct.updatedAt) }}</p>
            <div class="table-tags">
              <span v-for="relation in selectedProduct.supplierRelations.slice(0, 5)" :key="relation.id">
                {{ relation.supplierName }}{{ relation.isDefault ? ' / 默认' : '' }}
              </span>
            </div>
          </article>

          <div class="form-actions">
            <button type="button" :disabled="!canWrite || !isApiSource" @click="openEdit(selectedProduct.id)">编辑商品</button>
            <button type="button" class="ghost-action" :disabled="!canWrite || !isApiSource" @click="openInventoryAdjust(selectedProduct.id)">
              库存调整
            </button>
            <button type="button" class="ghost-action" :disabled="!isApiSource" @click="openProductLedger(selectedProduct.id)">
              库存流水
            </button>
          </div>
        </div>
        <div v-else class="empty-preview">
          <strong>暂无可查看商品</strong>
          <p>筛选条件为空或当前账号还没有真实商品档案。</p>
        </div>
      </aside>
    </section>
  </section>
</template>
