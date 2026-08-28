<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { Building2, ChevronLeft, ChevronRight, RefreshCw, Search, ShieldAlert, UsersRound } from 'lucide-vue-next'
import AdminLayout from '@/features/admin/components/AdminLayout.vue'
import AdminPanelState from '@/features/admin/components/AdminPanelState.vue'
import { useSession } from '@/app/stores/session'
import { fetchAdminStores, fetchAdminUsers, type AdminStoreSummary, type AdminUserPayload } from '@/shared/api/admin'

const session = useSession()
const tab = ref<'users' | 'stores'>('users')
const search = ref('')
const page = ref(0)
const size = 20
const loading = ref(false)
const error = ref<unknown>(null)
const users = ref<AdminUserPayload[]>([])
const stores = ref<AdminStoreSummary[]>([])
const total = ref(0)
const hasNext = ref(false)

const errorMessage = computed(() => error.value instanceof Error ? error.value.message : '管理员组织数据读取失败')
async function load() {
  if (!session.token.value) { error.value = new Error('管理员会话已失效'); return }
  loading.value = true; error.value = null
  try {
    if (tab.value === 'users') {
      const result = await fetchAdminUsers(session.token.value, { query: search.value.trim() || undefined, page: page.value, size })
      users.value = result.items; total.value = result.total; hasNext.value = result.hasNext
    } else {
      const result = await fetchAdminStores(session.token.value, { page: page.value, size })
      stores.value = result.items; total.value = result.total; hasNext.value = result.hasNext
    }
  } catch (cause) { error.value = cause }
  finally { loading.value = false }
}

function switchTab(next: 'users' | 'stores') { tab.value = next; page.value = 0; void load() }
function previous() { if (page.value > 0) { page.value -= 1; void load() } }
function next() { if (hasNext.value) { page.value += 1; void load() } }
function statusTone(status: string) { return /active|enabled|正常|启用/i.test(status) ? 'ok' : /disabled|inactive|停用/i.test(status) ? 'bad' : 'warn' }
function date(value?: string) { return value ? new Date(value).toLocaleString('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }) : '-' }

watch(search, () => { page.value = 0; void load() })
onMounted(load)
</script>

<template>
  <AdminLayout active-id="users">
    <section class="admin-page-v2">
      <header class="admin-page-v2__header">
        <div><div class="admin-page-v2__crumb">Admin / Organization / <strong>Directory</strong></div><h1>用户与门店</h1><p>查看授权范围内的用户、门店和成员关系。</p></div>
        <div class="admin-page-v2__actions"><button class="admin-button-v2" type="button" :disabled="loading" @click="load"><RefreshCw :class="{ 'is-spinning': loading }" aria-hidden="true" />刷新</button></div>
      </header>
      <div v-if="error" class="admin-error-v2" role="alert"><ShieldAlert aria-hidden="true" /><span>{{ errorMessage }}</span><button type="button" @click="load">重试</button></div>
      <div class="admin-grid">
        <article class="admin-card-v2 admin-span-12">
          <div class="admin-card-v2__header"><div class="admin-toolbar"><button class="admin-button-v2" :class="{ 'admin-button-v2--dark': tab === 'users' }" type="button" @click="switchTab('users')"><UsersRound aria-hidden="true" />用户</button><button class="admin-button-v2" :class="{ 'admin-button-v2--dark': tab === 'stores' }" type="button" @click="switchTab('stores')"><Building2 aria-hidden="true" />门店</button></div><span class="admin-card-v2__meta">第 {{ page + 1 }} 页 · {{ total }} 条</span></div>
          <div class="admin-card-v2__body admin-toolbar"><label v-if="tab === 'users'" class="admin-field"><Search aria-hidden="true" /><input v-model="search" type="search" placeholder="搜索昵称或脱敏手机号" aria-label="搜索用户" /></label><span v-else class="admin-card-v2__meta">门店列表按当前管理员授权范围返回。</span></div>
          <AdminPanelState v-if="loading" state="loading" title="正在读取组织数据" />
          <AdminPanelState v-else-if="error" state="error" :message="errorMessage" @retry="load" />
          <AdminPanelState v-else-if="(tab === 'users' ? users : stores).length === 0" state="empty" title="当前范围暂无记录" message="服务端没有返回可见的组织数据，请调整授权范围后重试。" />
          <div v-else class="admin-table-wrap">
            <table v-if="tab === 'users'" class="admin-table-v2"><thead><tr><th>用户</th><th>手机号</th><th>状态</th><th>创建时间</th><th>更新时间</th></tr></thead><tbody><tr v-for="item in users" :key="item.userId"><td><strong>{{ item.nickname || '未命名用户' }}</strong><small><code>{{ item.userId }}</code></small></td><td>{{ item.phoneMasked || '已脱敏' }}</td><td><span class="admin-status-v2" :class="`admin-status-v2--${statusTone(item.status)}`">{{ item.status || '未知' }}</span></td><td>{{ date(item.createdAt) }}</td><td>{{ date(item.updatedAt) }}</td></tr></tbody></table>
            <table v-else class="admin-table-v2"><thead><tr><th>门店</th><th>Owner ID</th><th>状态</th><th>成员</th><th>创建时间</th></tr></thead><tbody><tr v-for="item in stores" :key="item.storeId"><td><strong>{{ item.name || '未命名门店' }}</strong><small><code>{{ item.storeId }}</code></small></td><td><code>{{ item.ownerUserId }}</code></td><td><span class="admin-status-v2" :class="`admin-status-v2--${statusTone(item.status)}`">{{ item.status || '未知' }}</span></td><td>{{ item.memberCount }}</td><td>{{ date(item.createdAt) }}</td></tr></tbody></table>
          </div>
          <footer v-if="!loading && !error && total > 0" class="admin-pagination"><span>显示第 {{ page * size + 1 }} - {{ page * size + (tab === 'users' ? users.length : stores.length) }} 条</span><button type="button" :disabled="page === 0" aria-label="上一页" @click="previous"><ChevronLeft aria-hidden="true" /></button><button type="button" :disabled="!hasNext" aria-label="下一页" @click="next"><ChevronRight aria-hidden="true" /></button></footer>
        </article>
      </div>
    </section>
  </AdminLayout>
</template>
