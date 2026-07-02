<script setup lang="ts">
import { computed, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSession } from '@/app/stores/session'
import {
  createMediaBinding,
  createProduct,
  deleteMediaAsset,
  deleteMediaBinding,
  fetchMediaBindings,
  fetchProduct,
  fetchProductCategories,
  fetchProductUnits,
  mediaAssetContentUrl,
  updateProduct,
  uploadMediaAsset,
  type MediaBindingRecord,
  type ProductCategoryRecord,
  type ProductUnitRecord,
  type ProductWritePayload,
} from '@/shared/api/client'
import { readQueryId, type EntityId } from '@/shared/utils/id'
import { formatDateTime } from '@/shared/utils/business'

const route = useRoute()
const router = useRouter()
const session = useSession()

const categories = ref<ProductCategoryRecord[]>([])
const units = ref<ProductUnitRecord[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const success = ref('')
const lastUpdatedAt = ref<number | null>(null)

const mediaBindings = ref<MediaBindingRecord[]>([])
const imageBlobUrls = ref<Map<string, string>>(new Map())
const mediaLoading = ref(false)
const mediaError = ref('')
const mediaSuccess = ref('')
const uploading = ref(false)
const selectedFile = ref<File | null>(null)

const productId = computed(() => {
  return readQueryId(route.query.id)
})
const isEditMode = computed(() => productId.value != null)
const isApiSource = computed(() => session.source.value === 'api' && Boolean(session.token.value))
const canWrite = computed(() => session.hasPermission(['archives:write']))

const form = reactive({
  code: '',
  name: '',
  categoryId: '',
  unitId: '',
  salePrice: '0',
  purchasePrice: '0',
  stock: '0',
  safeStock: '0',
  status: '1',
})

const canSubmit = computed(() => {
  return isApiSource.value
    && canWrite.value
    && !saving.value
    && form.code.trim()
    && form.name.trim()
    && form.categoryId
    && form.unitId
})

watch(
  [() => session.source.value, () => session.token.value, productId],
  async () => {
    if (!isApiSource.value || !session.token.value) {
      error.value = ''
      mediaBindings.value = []
      revokeBlobUrls()
      return
    }
    await loadPage()
    if (productId.value) {
      await loadMediaBindings()
    } else {
      mediaBindings.value = []
      revokeBlobUrls()
    }
  },
  { immediate: true },
)

async function loadPage() {
  if (!session.token.value) return
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    const [nextCategories, nextUnits] = await Promise.all([
      fetchProductCategories(session.token.value),
      fetchProductUnits(session.token.value),
    ])
    categories.value = nextCategories.filter((item) => item.status === 1)
    units.value = nextUnits.filter((item) => item.status === 1)
    if (productId.value) {
      const product = await fetchProduct(session.token.value, productId.value)
      form.code = product.code
      form.name = product.name
      form.categoryId = product.categoryId ? String(product.categoryId) : ''
      form.unitId = product.unitId ? String(product.unitId) : ''
      form.salePrice = String(product.salePrice)
      form.purchasePrice = String(product.purchasePrice)
      form.stock = String(product.stock)
      form.safeStock = String(product.safeStock)
      form.status = String(product.status)
      lastUpdatedAt.value = product.updatedAt
    } else {
      resetForm()
    }
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : '商品资料加载失败'
  } finally {
    loading.value = false
  }
}

async function submitForm() {
  if (!session.token.value) return
  saving.value = true
  error.value = ''
  success.value = ''
  try {
    const payload: ProductWritePayload = {
      code: form.code.trim(),
      name: form.name.trim(),
      categoryId: form.categoryId,
      unitId: form.unitId,
      salePrice: Number(form.salePrice || 0),
      purchasePrice: Number(form.purchasePrice || 0),
      stock: Number(form.stock || 0),
      safeStock: Number(form.safeStock || 0),
      status: Number(form.status),
    }
    const saved = productId.value
      ? await updateProduct(session.token.value, productId.value, payload)
      : await createProduct(session.token.value, payload)
    success.value = productId.value ? `商品「${saved.name}」已更新` : `商品「${saved.name}」已创建`
    lastUpdatedAt.value = saved.updatedAt
    await router.push({ path: '/archives/products/edit', query: { id: String(saved.id) } })
  } catch (saveErr) {
    error.value = saveErr instanceof Error ? saveErr.message : '商品保存失败'
  } finally {
    saving.value = false
  }
}

function resetForm() {
  form.code = ''
  form.name = ''
  form.categoryId = ''
  form.unitId = ''
  form.salePrice = '0'
  form.purchasePrice = '0'
  form.stock = '0'
  form.safeStock = '0'
  form.status = '1'
  lastUpdatedAt.value = null
}

function goBack() {
  router.push('/archives/products')
}

async function loadMediaBindings() {
  if (!session.token.value || !productId.value) return
  mediaLoading.value = true
  mediaError.value = ''
  try {
    mediaBindings.value = await fetchMediaBindings(session.token.value, 'product', productId.value)
    await Promise.all(mediaBindings.value.map((binding) => loadImageBlobUrl(binding.assetId)))
  } catch (loadErr) {
    mediaError.value = loadErr instanceof Error ? loadErr.message : '商品图片加载失败'
  } finally {
    mediaLoading.value = false
  }
}

async function loadImageBlobUrl(assetId: EntityId) {
  if (!session.token.value) return
  const key = String(assetId)
  if (imageBlobUrls.value.has(key)) return
  try {
    const response = await fetch(mediaAssetContentUrl(assetId), {
      headers: { Authorization: `Bearer ${session.token.value}` },
    })
    if (!response.ok) return
    const blob = await response.blob()
    if (imageBlobUrls.value.has(key)) return
    const next = new Map(imageBlobUrls.value)
    next.set(key, URL.createObjectURL(blob))
    imageBlobUrls.value = next
  } catch {
    // ignore single image load failure
  }
}

function imageUrl(assetId: EntityId) {
  return imageBlobUrls.value.get(String(assetId)) || ''
}

function onFileSelected(event: Event) {
  const target = event.target as HTMLInputElement
  selectedFile.value = target.files && target.files[0] ? target.files[0] : null
}

async function uploadImage() {
  if (!session.token.value || !productId.value || !selectedFile.value) return
  uploading.value = true
  mediaError.value = ''
  mediaSuccess.value = ''
  try {
    const asset = await uploadMediaAsset(session.token.value, selectedFile.value, 'product_image')
    await createMediaBinding(session.token.value, {
      assetId: asset.id,
      targetType: 'product',
      targetId: productId.value,
    })
    mediaSuccess.value = `图片「${asset.originalFileName}」已上传`
    selectedFile.value = null
    const fileInput = document.querySelector<HTMLInputElement>('#product-image-input')
    if (fileInput) fileInput.value = ''
    await loadMediaBindings()
  } catch (uploadErr) {
    mediaError.value = uploadErr instanceof Error ? uploadErr.message : '图片上传失败'
  } finally {
    uploading.value = false
  }
}

async function removeImage(binding: MediaBindingRecord) {
  if (!session.token.value) return
  if (!window.confirm('确认删除该商品图片？')) return
  uploading.value = true
  mediaError.value = ''
  mediaSuccess.value = ''
  try {
    await deleteMediaBinding(session.token.value, binding.id)
    await deleteMediaAsset(session.token.value, binding.assetId)
    mediaSuccess.value = '图片已删除'
    await loadMediaBindings()
  } catch (removeErr) {
    mediaError.value = removeErr instanceof Error ? removeErr.message : '图片删除失败'
  } finally {
    uploading.value = false
  }
}

function revokeBlobUrls() {
  imageBlobUrls.value.forEach((url) => URL.revokeObjectURL(url))
  imageBlobUrls.value = new Map()
}

onUnmounted(revokeBlobUrls)
</script>

<template>
  <section class="business-page">
    <section class="screen-hero">
      <div>
        <p class="eyebrow">商品编辑 / Product Edit</p>
        <h2>{{ isEditMode ? '编辑商品' : '新增商品' }}</h2>
        <p>字段结构对齐安卓端商品编辑页与 `POST/PUT /v2/products`，用于维护商品基础资料、价格与库存预警。</p>
      </div>
      <div class="hero-actions">
        <button type="button" class="ghost-action" @click="goBack">返回商品档案</button>
      </div>
    </section>

    <p v-if="!isApiSource" class="form-error">当前是演示模式。这一页已经接好真实商品接口，登录后才能保存。</p>
    <p v-else-if="error" class="form-error">{{ error }}</p>
    <p v-else-if="success" class="form-success">{{ success }}</p>

    <section class="business-split">
      <article class="panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">商品表单</p>
            <h3>基础资料与价格库存</h3>
          </div>
          <span v-if="lastUpdatedAt" class="session-source">最近更新 {{ formatDateTime(lastUpdatedAt) }}</span>
        </div>

        <div v-if="loading" class="form-success">正在加载商品资料...</div>

        <form class="partner-form product-form" @submit.prevent="submitForm">
          <label>
            <span>商品编码</span>
            <input v-model="form.code" placeholder="例如 SP-10001" />
          </label>
          <label>
            <span>商品名称</span>
            <input v-model="form.name" placeholder="请输入商品名称" />
          </label>
          <label>
            <span>商品分类</span>
            <select v-model="form.categoryId">
              <option value="">请选择分类</option>
              <option v-for="category in categories" :key="category.id" :value="String(category.id)">
                {{ category.name }}
              </option>
            </select>
          </label>
          <label>
            <span>商品单位</span>
            <select v-model="form.unitId">
              <option value="">请选择单位</option>
              <option v-for="unit in units" :key="unit.id" :value="String(unit.id)">
                {{ unit.name }}
              </option>
            </select>
          </label>
          <label>
            <span>零售价</span>
            <input v-model="form.salePrice" type="number" min="0" step="0.01" />
          </label>
          <label>
            <span>进货价</span>
            <input v-model="form.purchasePrice" type="number" min="0" step="0.01" />
          </label>
          <label>
            <span>当前库存</span>
            <input v-model="form.stock" type="number" min="0" step="0.01" />
          </label>
          <label>
            <span>安全库存</span>
            <input v-model="form.safeStock" type="number" min="0" step="0.01" />
          </label>
          <label>
            <span>启用状态</span>
            <select v-model="form.status">
              <option value="1">启用</option>
              <option value="0">停用</option>
            </select>
          </label>

          <div class="form-actions wide-field">
            <button type="submit" :disabled="!canSubmit">{{ saving ? '保存中...' : (isEditMode ? '保存更新' : '创建商品') }}</button>
            <button type="button" class="ghost-action" :disabled="saving" @click="resetForm">重置</button>
          </div>
        </form>
      </article>

      <aside class="panel detail-panel">
        <p class="eyebrow">安卓对齐</p>
        <h3>当前实现范围</h3>
        <div class="detail-stack">
          <article class="detail-card">
            <ul class="feature-list">
              <li>基础字段：编码、名称、分类、单位</li>
              <li>价格字段：零售价、进货价</li>
              <li>库存字段：当前库存、安全库存</li>
              <li>状态字段：启用 / 停用</li>
            </ul>
          </article>
          <article class="detail-card">
            <strong>说明</strong>
            <p class="muted">当前 PC 页面先对齐安卓端已实际使用的核心字段。多价格级与供应商关系保留给下一轮扩展。</p>
          </article>
        </div>
      </aside>
    </section>

    <section v-if="isEditMode && isApiSource" class="panel">
      <div class="panel-head">
        <div>
          <p class="eyebrow">商品图片 / Media</p>
          <h3>商品图片管理</h3>
        </div>
        <span class="session-source">{{ mediaBindings.length }} 张图片</span>
      </div>

      <p v-if="mediaError" class="form-error">{{ mediaError }}</p>
      <p v-else-if="mediaSuccess" class="form-success">{{ mediaSuccess }}</p>
      <p v-if="mediaLoading" class="form-success">正在加载商品图片...</p>

      <div class="partner-form product-form">
        <label class="wide-field">
          <span>选择图片</span>
          <input id="product-image-input" type="file" accept="image/*" :disabled="!canWrite || uploading" @change="onFileSelected" />
        </label>
        <div class="form-actions wide-field">
          <button type="button" :disabled="!canWrite || uploading || !selectedFile" @click="uploadImage">
            {{ uploading ? '上传中...' : '上传图片' }}
          </button>
        </div>
      </div>

      <div v-if="mediaBindings.length > 0" class="product-image-grid">
        <figure v-for="binding in mediaBindings" :key="binding.id" class="product-image-item">
          <img v-if="imageUrl(binding.assetId)" :src="imageUrl(binding.assetId)" alt="商品图片" style="width:120px;height:120px;object-fit:cover;border-radius:8px;" />
          <span v-else class="material-symbols-outlined">image</span>
          <button type="button" class="ghost-action" :disabled="!canWrite || uploading" @click="removeImage(binding)">删除</button>
        </figure>
      </div>
      <p v-else-if="!mediaLoading" class="muted">暂无商品图片，请上传。</p>
    </section>
  </section>
</template>
