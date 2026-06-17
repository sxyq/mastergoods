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
  formatNumber,
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
const pageSize = ref(10)
const currentPage = ref(1)

const isApiSource = computed(() => session.source.value === 'api' && Boolean(session.token.value))
const canWrite = computed(() => session.hasPermission(['archives:write']))
const canInventoryView = computed(() => session.hasPermission(['inventory:view']))
const canInventoryWrite = computed(() => session.hasPermission(['inventory:write']))
const lowStockIds = computed(() => new Set(lowStockProducts.value.map((item) => item.id)))
const inventoryValue = computed(() => products.value.reduce((sum, item) => sum + item.stock * item.purchasePrice, 0))
const activeProducts = computed(() => products.value.filter((item) => item.status === 1).length)
const totalPages = computed(() => Math.max(1, Math.ceil(products.value.length / pageSize.value)))
const pagedProducts = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return products.value.slice(start, start + pageSize.value)
})
const pageStart = computed(() => (products.value.length === 0 ? 0 : (currentPage.value - 1) * pageSize.value + 1))
const pageEnd = computed(() => Math.min(currentPage.value * pageSize.value, products.value.length))

watch(
  [
    () => session.source.value,
    () => session.token.value,
    searchKeyword,
    statusFilter,
    categoryFilter,
  ],
  async () => {
    currentPage.value = 1
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

watch(pageSize, () => {
  currentPage.value = 1
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

function resetFilters() {
  searchKeyword.value = ''
  statusFilter.value = 'all'
  categoryFilter.value = 'all'
}

function productStockTone(product: ProductRecord) {
  if (product.status !== 1) return 'cancelled'
  if (lowStockIds.value.has(product.id) || product.stock <= product.safeStock) return 'draft'
  return 'done'
}

function setPage(page: number) {
  currentPage.value = Math.min(Math.max(page, 1), totalPages.value)
}
</script>

<template>
  <section class="pc-list-page product-list-page">
    <header class="pc-list-titlebar">
      <div class="pc-breadcrumb">
        <span>档案管理</span>
        <span class="material-symbols-outlined">chevron_right</span>
        <h1>商品列表</h1>
      </div>
      <div class="pc-title-actions">
        <button type="button" class="pc-secondary-action" :disabled="!isApiSource || !canInventoryView" @click="openSnapshots">
          <span class="material-symbols-outlined">inventory</span>
          库存盘点
        </button>
        <button type="button" class="pc-primary-action" :disabled="!canWrite || !isApiSource" @click="openCreate">
          <span class="material-symbols-outlined">add</span>
          新增商品
        </button>
      </div>
    </header>

    <p v-if="!isApiSource" class="form-error">当前是演示模式。这一页已按真实接口实现，登录后即可读取商品、分类和库存数据。</p>
    <p v-else-if="error" class="form-error">{{ error }}</p>
    <p v-else-if="loading" class="form-success">正在加载真实商品档案...</p>

    <section class="pc-list-toolbar">
      <div class="pc-filter-row">
        <label class="pc-search-field">
          <span class="material-symbols-outlined">search</span>
          <input v-model="searchKeyword" placeholder="商品名称/编码/拼音码" />
        </label>
        <label class="pc-select-field">
          <span>商品分类</span>
          <select v-model="categoryFilter">
            <option value="all">全部分类</option>
            <option v-for="category in categories" :key="category.id" :value="String(category.id)">{{ category.name }}</option>
          </select>
        </label>
        <label class="pc-select-field">
          <span>状态</span>
          <select v-model="statusFilter">
            <option value="all">全部状态</option>
            <option value="1">启用</option>
            <option value="0">停用</option>
          </select>
        </label>
        <button type="button" class="pc-secondary-action" @click="resetFilters">重置</button>
        <button type="button" class="pc-primary-action" :disabled="!isApiSource" @click="loadProducts">查询</button>
      </div>
      <div class="pc-list-summary">
        <span>商品 {{ products.length }} 个</span>
        <span>低库存 {{ lowStockProducts.length }} 个</span>
        <span>库存金额 {{ formatCurrency(inventoryValue) }}</span>
      </div>
    </section>

    <section class="pc-data-card">
      <div class="pc-table-scroll">
        <table class="pc-data-table product-data-table">
          <thead>
            <tr>
              <th class="pc-check-cell"><input type="checkbox" /></th>
              <th class="product-image-cell">图片</th>
              <th>商品编码</th>
              <th>商品名称</th>
              <th>规格型号</th>
              <th>单位</th>
              <th class="align-right">零售价(元)</th>
              <th class="align-right">进货价(元)</th>
              <th class="align-right">当前库存</th>
              <th class="align-center">状态</th>
              <th class="align-center">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="product in pagedProducts" :key="product.id">
              <td class="pc-check-cell"><input type="checkbox" /></td>
              <td>
                <div class="product-thumb">
                  <span class="material-symbols-outlined">{{ product.status === 1 ? 'image' : 'image_not_supported' }}</span>
                </div>
              </td>
              <td class="amount-strong">{{ product.code }}</td>
              <td>
                <div class="pc-doc-cell">
                  <strong>{{ product.name }}</strong>
                  <small>{{ product.defaultSupplier?.supplierName || '暂无默认供应商' }}</small>
                </div>
              </td>
              <td class="muted">{{ product.categoryName || '-' }}</td>
              <td>{{ product.unitName || '-' }}</td>
              <td class="align-right">{{ formatCurrency(product.salePrice) }}</td>
              <td class="align-right">{{ formatCurrency(product.purchasePrice) }}</td>
              <td class="align-right">
                <span class="product-stock" :data-tone="productStockTone(product)">
                  {{ formatNumber(product.stock) }}
                  <span v-if="lowStockIds.has(product.id)" class="material-symbols-outlined">warning</span>
                </span>
              </td>
              <td class="align-center">
                <span class="pc-status-chip" :data-tone="product.status === 1 ? 'done' : 'cancelled'">
                  {{ product.status === 1 ? '启用' : '停用' }}
                </span>
              </td>
              <td class="align-center">
                <div class="product-text-actions">
                  <button type="button" :disabled="!canWrite || !isApiSource" @click="openEdit(product.id)">编辑</button>
                  <span>|</span>
                  <button type="button" :disabled="!canInventoryWrite || !isApiSource" @click="openInventoryAdjust(product.id)">调整</button>
                  <span>|</span>
                  <button type="button" :disabled="!isApiSource || !canInventoryView" @click="openProductLedger(product.id)">流水</button>
                </div>
              </td>
            </tr>
            <tr v-if="!loading && pagedProducts.length === 0">
              <td colspan="11" class="empty-cell">暂无商品数据</td>
            </tr>
          </tbody>
        </table>
      </div>

      <footer class="pc-pagination">
        <span>共 <b>{{ products.length }}</b> 条数据，启用 {{ activeProducts }} 个，当前显示 {{ pageStart }}-{{ pageEnd }} 条</span>
        <div>
          <select v-model.number="pageSize">
            <option :value="10">10 条/页</option>
            <option :value="20">20 条/页</option>
            <option :value="50">50 条/页</option>
          </select>
          <button type="button" :disabled="currentPage === 1" @click="setPage(currentPage - 1)">
            <span class="material-symbols-outlined">chevron_left</span>
          </button>
          <button type="button" class="active">{{ currentPage }}</button>
          <button type="button" :disabled="currentPage >= totalPages" @click="setPage(currentPage + 1)">
            <span class="material-symbols-outlined">chevron_right</span>
          </button>
        </div>
      </footer>
    </section>
  </section>
</template>
