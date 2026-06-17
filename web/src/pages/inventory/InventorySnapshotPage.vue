<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useSession } from '@/app/stores/session'
import {
  createInventorySnapshot,
  fetchInventorySnapshots,
  fetchProducts,
  type InventorySnapshot,
  type ProductRecord,
} from '@/shared/api/client'
import {
  formatCurrency,
  formatDate,
  formatNumber,
  inventoryTrendLabel,
  todayStartAt,
} from '@/shared/utils/business'

const session = useSession()

const products = ref<ProductRecord[]>([])
const snapshots = ref<InventorySnapshot[]>([])
const loading = ref(false)
const submitting = ref(false)
const error = ref('')
const success = ref('')
const searchKeyword = ref('')

const isApiSource = computed(() => session.source.value === 'api' && Boolean(session.token.value))
const snapshotDate = todayStartAt()
const snapshotMap = computed(() => new Map(snapshots.value.map((item) => [item.productId, item])))
const filteredProducts = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return products.value
  return products.value.filter((item) => {
    return item.name.toLowerCase().includes(keyword) || item.code.toLowerCase().includes(keyword)
  })
})
const completedCount = computed(() => filteredProducts.value.filter((item) => snapshotMap.value.has(item.id)).length)
const lowStockCount = computed(() => products.value.filter((item) => item.stock < item.safeStock).length)

watch(
  [() => session.source.value, () => session.token.value],
  async () => {
    if (!isApiSource.value || !session.token.value) {
      products.value = []
      snapshots.value = []
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
  success.value = ''
  try {
    const [nextProducts, nextSnapshots] = await Promise.all([
      fetchProducts(session.token.value, { page: 0, size: 200 }),
      fetchInventorySnapshots(session.token.value, { snapshotDate }),
    ])
    products.value = nextProducts
    snapshots.value = nextSnapshots
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : '库存盘点数据加载失败'
  } finally {
    loading.value = false
  }
}

async function createSnapshotForProduct(productId: number) {
  if (!session.token.value) return
  submitting.value = true
  error.value = ''
  success.value = ''
  try {
    await createInventorySnapshot(session.token.value, { productId, snapshotDate })
    success.value = '库存快照已生成'
    await loadPage()
  } catch (submitErr) {
    error.value = submitErr instanceof Error ? submitErr.message : '生成库存快照失败'
  } finally {
    submitting.value = false
  }
}

async function createAllSnapshots() {
  if (!session.token.value) return
  const pendingProducts = filteredProducts.value.filter((item) => !snapshotMap.value.has(item.id))
  if (pendingProducts.length === 0) {
    success.value = '当前筛选结果已经全部生成今日快照'
    return
  }
  submitting.value = true
  error.value = ''
  success.value = ''
  try {
    for (const product of pendingProducts) {
      await createInventorySnapshot(session.token.value, { productId: product.id, snapshotDate })
    }
    success.value = `已为 ${pendingProducts.length} 个商品生成今日快照`
    await loadPage()
  } catch (submitErr) {
    error.value = submitErr instanceof Error ? submitErr.message : '批量生成库存快照失败'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="business-page">
    <section class="screen-hero">
      <div>
        <p class="eyebrow">库存盘点 / Inventory Snapshots</p>
        <h2>库存盘点专页</h2>
        <p>基于真实商品与 `/v2/inventory/snapshots` 生成当日库存快照，作为 PC 端盘点结果基线。</p>
      </div>
      <div class="hero-actions">
        <button type="button" :disabled="!isApiSource || submitting" @click="createAllSnapshots">批量生成今日快照</button>
      </div>
    </section>

    <p v-if="!isApiSource" class="form-error">当前是演示模式。这一页只在真实登录后生成盘点快照。</p>
    <p v-else-if="error" class="form-error">{{ error }}</p>
    <p v-else-if="success" class="form-success">{{ success }}</p>
    <p v-if="loading" class="form-success">正在加载库存盘点数据...</p>

    <section class="metrics-grid compact">
      <article class="metric-card" data-tone="blue">
        <span>盘点日期</span>
        <strong>{{ formatDate(snapshotDate) }}</strong>
        <p>当日库存快照</p>
      </article>
      <article class="metric-card" data-tone="green">
        <span>已生成快照</span>
        <strong>{{ completedCount }}</strong>
        <p>{{ filteredProducts.length }} 个筛选商品中已完成</p>
      </article>
      <article class="metric-card" data-tone="orange">
        <span>低库存商品</span>
        <strong>{{ lowStockCount }}</strong>
        <p>可联动库存调整与采购补货</p>
      </article>
    </section>

    <section class="panel">
      <div class="business-toolbar">
        <label class="search-box">
          <span>搜索商品</span>
          <input v-model="searchKeyword" placeholder="商品名称 / 编码" />
        </label>
      </div>
      <p class="muted">当前后端只支持写入“真实库存快照”。人工录入盘盈盘亏仍由库存调整专页处理。</p>
    </section>

    <section class="panel">
      <div class="table-shell">
        <table>
          <thead>
            <tr>
              <th>商品编码</th>
              <th>商品名称</th>
              <th>当前库存</th>
              <th>安全库存</th>
              <th>库存状态</th>
              <th>今日快照</th>
              <th>快照金额</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="product in filteredProducts" :key="product.id">
              <td>{{ product.code }}</td>
              <td>{{ product.name }}</td>
              <td>{{ formatNumber(product.stock) }}</td>
              <td>{{ formatNumber(product.safeStock) }}</td>
              <td>{{ inventoryTrendLabel(product.stock, product.safeStock) }}</td>
              <td>{{ snapshotMap.get(product.id)?.quantity != null ? formatNumber(snapshotMap.get(product.id)?.quantity) : '--' }}</td>
              <td>{{ snapshotMap.get(product.id)?.totalValue != null ? formatCurrency(snapshotMap.get(product.id)?.totalValue) : '--' }}</td>
              <td>
                <button
                  type="button"
                  class="ghost-action"
                  :disabled="submitting || !isApiSource"
                  @click="createSnapshotForProduct(product.id)"
                >
                  {{ snapshotMap.has(product.id) ? '刷新快照' : '生成快照' }}
                </button>
              </td>
            </tr>
            <tr v-if="!loading && filteredProducts.length === 0">
              <td colspan="8" class="empty-cell">暂无可盘点商品</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </section>
</template>
