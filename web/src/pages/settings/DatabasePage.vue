<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { fetchImportJobs, fetchSyncHealth, importLegacySqlite, type ImportJob, type SyncHealth } from '@/shared/api/client'
import { useSession } from '@/app/stores/session'

const session = useSession()
const allowed = computed(() => session.hasPermission(['database:manage']))
const isApiSource = computed(() => session.source.value === 'api' && Boolean(session.token.value))
const loading = ref(false)
const submitting = ref(false)
const loadError = ref('')
const success = ref('')
const syncHealth = ref<SyncHealth | null>(null)
const importJobs = ref<ImportJob[]>([])
const form = reactive({
  legacyDbPath: '/Users/sunyiyang/Desktop/Project/master-goods/migration_output/zhihuiji.source-backup.db',
  resetOwnedData: false,
})

watch(
  [allowed, () => session.source.value, () => session.token.value],
  async ([canVisit]) => {
    if (!canVisit) return
    if (!isApiSource.value) {
      syncHealth.value = null
      importJobs.value = []
      loadError.value = ''
      return
    }
    await refreshData()
  },
  { immediate: true },
)

async function refreshData() {
  if (!session.token.value) return
  loading.value = true
  loadError.value = ''
  try {
    const [health, jobs] = await Promise.all([
      fetchSyncHealth(session.token.value),
      fetchImportJobs(session.token.value),
    ])
    syncHealth.value = health
    importJobs.value = jobs.sort((a, b) => b.updatedAt - a.updatedAt)
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '数据库状态加载失败'
  } finally {
    loading.value = false
  }
}

async function submitImport() {
  if (!session.token.value) return
  submitting.value = true
  loadError.value = ''
  success.value = ''
  try {
    const result = await importLegacySqlite(session.token.value, {
      legacyDbPath: form.legacyDbPath.trim(),
      resetOwnedData: form.resetOwnedData,
    })
    success.value = `旧库导入完成：商品 ${result.products} / 客户 ${result.customers} / 供应商 ${result.suppliers} / 销售单 ${result.saleOrders}`
    await refreshData()
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '旧库导入失败'
  } finally {
    submitting.value = false
  }
}

function formatDateTime(timestamp?: number | null) {
  if (!timestamp) return '--'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(timestamp)
}
</script>

<template>
  <section class="database-page">
    <div v-if="!allowed" class="access-denied">
      <h2>当前角色不可访问</h2>
      <p>{{ session.roleLabel.value }} 没有数据库连接、导入、备份和恢复权限。</p>
    </div>

    <template v-else>
      <div class="panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Database Console</p>
            <h2>数据库连接与健康检查</h2>
          </div>
          <button v-if="isApiSource" type="button" class="ghost-action" :disabled="loading" @click="refreshData">
            {{ loading ? '刷新中' : '刷新状态' }}
          </button>
        </div>
        <p class="muted">
          {{ isApiSource
            ? '当前页面已连接真实后端 `/v2/sync/health` 与 `/v2/import-jobs`。真实后端当前仍按 owner_user_id 做作用域隔离。'
            : '当前处于本地演示模式。点击登录并进入真实 owner 账号后，这里会直接显示后端同步健康与导入任务。' }}
        </p>
        <div v-if="loadError" class="form-error">{{ loadError }}</div>
        <div v-if="success" class="form-success">{{ success }}</div>
      </div>

      <div class="db-grid">
        <article class="db-card">
          <strong>同步健康</strong>
          <span>{{ syncHealth?.status ?? (isApiSource ? '等待加载' : '演示模式') }}</span>
          <p>{{ syncHealth?.message ?? '浏览器不直接暴露数据库账号密码，真实连接由后端执行。' }}</p>
          <div class="db-meta">
            <small>Owner Scope: {{ syncHealth?.ownerScoped ?? false ? 'true' : 'false' }}</small>
            <small>Server Time: {{ formatDateTime(syncHealth?.serverTime) }}</small>
          </div>
        </article>

        <article class="db-card">
          <strong>支持实体</strong>
          <span>{{ syncHealth?.supportedEntityTypes?.length ?? 0 }} 类</span>
          <p>当前后端支持的同步实体类型与可上传实体类型。</p>
          <div class="table-tags">
            <span v-for="item in syncHealth?.supportedEntityTypes ?? []" :key="item">{{ item }}</span>
          </div>
        </article>

        <article class="db-card">
          <strong>上传实体</strong>
          <span>{{ syncHealth?.uploadableEntityTypes?.length ?? 0 }} 类</span>
          <p>安卓与 Web 后续都应严格落在这些真实可上传实体集合上。</p>
          <div class="table-tags">
            <span v-for="item in syncHealth?.uploadableEntityTypes ?? []" :key="item">{{ item }}</span>
          </div>
        </article>

        <article class="db-card">
          <strong>后端作用域</strong>
          <span>owner_user_id</span>
          <p>真实后端目前还是 owner 级数据域，门店成员与角色接口尚未后端化，因此 Web 侧 RBAC 先作为前端规划层存在。</p>
        </article>
      </div>

      <section class="panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Legacy SQLite Import</p>
            <h2>旧库导入</h2>
          </div>
        </div>
        <form class="database-form" @submit.prevent="submitImport">
          <label>
            旧库路径
            <input v-model="form.legacyDbPath" :disabled="!isApiSource || submitting" />
          </label>
          <label class="database-check">
            <input v-model="form.resetOwnedData" type="checkbox" :disabled="!isApiSource || submitting" />
            <span>导入前清空当前 owner 数据</span>
          </label>
          <button type="submit" :disabled="!isApiSource || submitting">
            {{ submitting ? '导入中' : '执行旧库导入' }}
          </button>
        </form>
      </section>

      <section class="panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Import Jobs</p>
            <h2>导入任务</h2>
          </div>
        </div>

        <div v-if="!isApiSource" class="form-success">演示模式下不读取真实导入任务。登录真实 owner 账号后会自动显示后端任务列表。</div>

        <div v-else class="table-shell">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>来源</th>
                <th>状态</th>
                <th>阶段</th>
                <th>失败信息</th>
                <th>更新时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="job in importJobs" :key="job.id">
                <td>{{ job.id }}</td>
                <td>{{ job.sourceType }} / {{ job.sourceUri || '--' }}</td>
                <td>{{ job.status }}</td>
                <td>{{ job.stage || '--' }}</td>
                <td>{{ job.failureMessage || '--' }}</td>
                <td>{{ formatDateTime(job.updatedAt) }}</td>
              </tr>
              <tr v-if="importJobs.length === 0">
                <td colspan="6" class="empty-cell">当前没有导入任务</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>
  </section>
</template>
