<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { Activity, BarChart3, Bot, CircleAlert, Database, RefreshCw, ShieldAlert, Store, UsersRound } from 'lucide-vue-next'
import AdminLayout from '@/features/admin/components/AdminLayout.vue'
import AdminPageHeader from '@/features/admin/components/AdminPageHeader.vue'
import AdminPanelState from '@/features/admin/components/AdminPanelState.vue'
import AdminScopeFilter from '@/features/admin/components/AdminScopeFilter.vue'
import { useSession } from '@/app/stores/session'
import { useAdminSession } from '@/app/stores/admin-session'
import { fetchAdminOverview, type AdminOverviewPayload } from '@/shared/api/admin'

const session = useSession()
const adminSession = useAdminSession()
const range = ref('近 30 天')
const loading = ref(false)
const error = ref<unknown>(null)
const overview = ref<AdminOverviewPayload | null>(null)
const ownerUserId = ref('')
const storeId = ref('')
const rangeDays: Record<string, number> = { 今天: 1, '近 7 天': 7, '近 30 天': 30, '近 90 天': 90 }
const metricConfig: Record<string, { label: string; icon: typeof UsersRound }> = { users: { label: '用户总数', icon: UsersRound }, stores: { label: '门店总数', icon: Store }, agent_runs: { label: 'Agent 运行', icon: Bot }, agent_tool_calls: { label: '工具调用', icon: Activity }, totalTokens: { label: 'Token 总量', icon: Database } }
const metrics = computed(() => (overview.value?.metrics ?? []).map((item) => ({ ...item, ...(metricConfig[item.key] ?? { label: item.key, icon: BarChart3 }) })))
const trendMax = computed(() => Math.max(...(overview.value?.trend.map((point) => point.value) ?? [1]), 1))
const chartPoints = computed(() => (overview.value?.trend ?? []).map((point) => ({ ...point, height: Math.max(4, Math.round(point.value / trendMax.value * 100)) })))
function formatNumber(value: number) { return new Intl.NumberFormat('zh-CN', { notation: value >= 1000000 ? 'compact' : 'standard', maximumFractionDigits: 1 }).format(value) }
async function load() { if (!session.token.value) { error.value = new Error('管理员会话已失效'); return }; loading.value = true; error.value = null; const now = new Date(); const from = new Date(now.getTime() - (rangeDays[range.value] ?? 30) * 86400000); try { overview.value = await fetchAdminOverview(session.token.value, { from: from.toISOString(), to: now.toISOString(), ownerUserId: ownerUserId.value || undefined, storeId: storeId.value || undefined }) } catch (cause) { error.value = cause } finally { loading.value = false } }
function applyScope() { void load() }
watch(range, () => { void load() })
onMounted(async () => { await adminSession.ensure(session.token.value); await load() })
</script>
<template>
  <AdminLayout active-id="overview"><section class="admin-page-v2"><AdminPageHeader v-model:range="range" title="平台总览" :refreshing="loading" @refresh="load" /><div class="admin-toolbar admin-overview-scope"><AdminScopeFilter v-model:owner-user-id="ownerUserId" v-model:store-id="storeId" :owner-user-ids="adminSession.session.value?.ownerUserIds" :store-ids="adminSession.session.value?.storeIds" /><button class="admin-button-v2" type="button" @click="applyScope">应用范围</button></div><div v-if="error" class="admin-error-v2" role="alert"><ShieldAlert aria-hidden="true" /><span>{{ error instanceof Error ? error.message : '平台总览读取失败' }}</span><button type="button" @click="load">重试</button></div><div class="admin-grid"><article v-for="metric in metrics" :key="metric.key" class="admin-card-v2 admin-stat-v2 admin-span-3"><div class="admin-stat-v2__label"><span>{{ metric.label }}</span><component :is="metric.icon" aria-hidden="true" /></div><strong>{{ formatNumber(metric.value) }}</strong><small>{{ metric.unit || (overview?.estimated ? '估算值' : '服务端统计') }}</small></article></div><div class="admin-grid"><article class="admin-card-v2 admin-span-8"><div class="admin-card-v2__header"><div><h2>Agent 调用趋势</h2><p>按服务端返回的时间粒度展示，数值不会用本地快照补齐。</p></div><span class="admin-card-v2__meta">{{ overview?.estimated ? 'ESTIMATED' : 'EXACT' }}</span></div><AdminPanelState v-if="loading" state="loading" title="正在读取趋势" /><AdminPanelState v-else-if="error" state="error" :message="error instanceof Error ? error.message : '平台总览读取失败'" @retry="load" /><AdminPanelState v-else-if="!overview || chartPoints.length === 0" state="empty" title="当前范围暂无趋势数据" message="服务端没有返回当前时间范围的趋势点。" /><div v-else class="admin-overview-chart"><div v-for="point in chartPoints" :key="point.at" class="admin-overview-chart__bar" :title="`${point.at} · ${point.value}`"><i :style="{ height: `${point.height}%` }" /></div></div></article><article class="admin-card-v2 admin-span-4 admin-card-v2--pad"><h2>授权范围</h2><p v-if="overview?.scope">{{ overview.scope.allOwners ? '全部 Owner' : `${overview.scope.ownerUserIds.length} 个 Owner` }} · {{ overview.scope.storeIds.length ? `${overview.scope.storeIds.length} 个门店` : '门店范围未限定' }}</p><AdminPanelState v-else-if="!loading && !error" state="empty" title="未返回范围" message="页面不根据名称猜测 Owner 或门店范围。" /></article></div><div class="admin-grid"><article class="admin-card-v2 admin-span-12 admin-card-v2--pad"><div class="admin-toolbar"><RefreshCw :class="{ 'is-spinning': loading }" aria-hidden="true" /><strong>{{ overview ? `生成于 ${overview.generatedAt || '-'}` : '等待管理员接口' }}</strong><span class="admin-card-v2__meta">{{ overview?.scopeCompleteness || 'UNKNOWN' }}</span></div><p><CircleAlert aria-hidden="true" /> 运行详情、工具事件和审计记录请从侧栏进入；此页只展示汇总指标。</p></article></div></section></AdminLayout>
</template>
<style scoped>
.admin-overview-chart { display:flex; height:220px; align-items:end; gap:7px; padding:24px 20px 20px; }
.admin-overview-chart__bar { display:flex; height:100%; flex:1; align-items:end; min-width:8px; }
.admin-overview-chart__bar i { display:block; width:100%; border-radius:4px 4px 1px 1px; background:#477eac; }
p svg { width:14px; vertical-align:-3px; }
.admin-overview-scope, .admin-runs-scope { max-width:1400px; margin:0 auto 14px; }
</style>
