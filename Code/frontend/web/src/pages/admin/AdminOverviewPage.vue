<script setup lang="ts">
import { computed, ref } from 'vue'
import {
  Activity,
  ArrowUpRight,
  Bot,
  Check,
  CircleAlert,
  CircleCheck,
  Copy,
  Database,
  Gauge,
  KeyRound,
  ListFilter,
  Search,
  ShieldCheck,
  Store,
  UsersRound,
  WholeWord,
  X,
} from 'lucide-vue-next'
import type { Component } from 'vue'
import AdminLayout from '@/features/admin/components/AdminLayout.vue'
import AdminPageHeader from '@/features/admin/components/AdminPageHeader.vue'
import AdminPanelState from '@/features/admin/components/AdminPanelState.vue'
import AdminStatusBadge, { type AdminStatus } from '@/features/admin/components/AdminStatusBadge.vue'

interface AdminMetric {
  label: string
  value: string
  detail: string
  icon: Component
  tone: 'blue' | 'green' | 'violet' | 'amber' | 'neutral'
}

interface AdminRun {
  id: string
  user: string
  store: string
  task: string
  model: string
  tokens: string
  time: string
  duration: string
  status: AdminStatus
  tool: string
}

const range = ref('近 30 天')
const query = ref('')
const selectedRun = ref<AdminRun | null>(null)
const toast = ref('')
const refreshing = ref(false)
let toastTimer: ReturnType<typeof setTimeout> | undefined

const metrics: AdminMetric[] = [
  { label: '用户总数', value: '12,864', detail: '+8.4% 较上月', icon: UsersRound, tone: 'blue' },
  { label: '活跃门店', value: '128', detail: '3 个需要关注', icon: Store, tone: 'green' },
  { label: 'Agent 运行', value: '1,284', detail: '98.7% 已完成', icon: Bot, tone: 'violet' },
  { label: 'Token 消耗', value: '4.82M', detail: '今日累计', icon: WholeWord, tone: 'amber' },
  { label: '平均首字延迟', value: '1.24s', detail: '近 30 天样本', icon: Gauge, tone: 'neutral' },
]

const runs: AdminRun[] = [
  { id: 'run-260828-0182', user: '林晓梅', store: '望江仓储店', task: '查询低库存商品并生成补货草稿', model: 'deepseek-flash-0731', tokens: '12,480', time: '14:27:08', duration: '8.42s', status: 'completed', tool: 'list_low_stock_products' },
  { id: 'run-260828-0181', user: '周凯', store: '新城配送中心', task: '统计本月销售额和回款情况', model: 'deepseek-flash-0731', tokens: '8,921', time: '14:25:41', duration: '3.18s', status: 'completed', tool: 'get_sales_summary' },
  { id: 'run-260828-0179', user: '陈玉兰', store: '东湖生鲜店', task: '创建一张客户资料草稿', model: 'deepseek-flash-0731', tokens: '6,204', time: '14:22:16', duration: '6.70s', status: 'review', tool: 'create_customer_draft' },
  { id: 'run-260828-0177', user: '赵明', store: '望江仓储店', task: '检查今日库存同步状态', model: 'deepseek-flash-0731', tokens: '4,188', time: '14:18:32', duration: '2.09s', status: 'attention', tool: 'get_sync_status' },
]

const activity = [
  { time: '14:27:16', event: 'tool.completed', detail: 'list_low_stock_products', tone: 'success' },
  { time: '14:27:15', event: 'message.delta', detail: 'stream chunk 18 / 18', tone: 'info' },
  { time: '14:27:13', event: 'context.compacted', detail: '12,480 → 4,096 tokens', tone: 'info' },
  { time: '14:27:10', event: 'tool.started', detail: 'list_low_stock_products', tone: 'info' },
  { time: '14:27:08', event: 'run.started', detail: 'run-260828-0182', tone: 'info' },
  { time: '14:26:59', event: 'request.received', detail: 'store=望江仓储店', tone: 'info' },
]

const toolDomains = [
  { label: '库存与商品', detail: '成功率 99.1% · 1.84M tokens', requests: '486', share: '38.0%', tone: 'green' },
  { label: '销售与财务', detail: '成功率 98.4% · 1.62M tokens', requests: '421', share: '32.9%', tone: 'blue' },
  { label: '草稿与审计', detail: '成功率 97.8% · 1.36M tokens', requests: '377', share: '29.1%', tone: 'violet' },
]

const trendBars = [32, 42, 36, 58, 48, 64, 53, 70, 61, 78, 67, 88, 76, 94, 82, 96]
const filteredRuns = computed(() => {
  const normalized = query.value.trim().toLowerCase()
  if (!normalized) return runs
  return runs.filter((run) => [run.id, run.user, run.store, run.task, run.tool].some((value) => value.toLowerCase().includes(normalized)))
})
const ringStyle = computed(() => ({ background: 'conic-gradient(#43a56b 0 94%, #ecece8 94% 100%)' }))

function notify(message: string) {
  toast.value = message
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toast.value = ''
  }, 2200)
}

function openRun(run: AdminRun) {
  selectedRun.value = run
}

function handleNavigate(id: string) {
  if (id === 'overview') return
  notify('管理员身份与页面权限由服务端接入后开放')
}

function refreshOverview() {
  refreshing.value = true
  window.setTimeout(() => {
    refreshing.value = false
    notify('示例快照已刷新，正式数据接口尚未接入')
  }, 420)
}
</script>

<template>
  <AdminLayout active-id="overview" @navigate="handleNavigate" @notice="notify">
    <section class="admin-page">
      <AdminPageHeader v-model:range="range" title="平台总览" @refresh="refreshOverview" />

      <div class="admin-preview-notice" role="status">
        <span class="admin-status-dot admin-status-dot--online" aria-hidden="true" />
        <strong>LOCAL PREVIEW</strong>
        <span class="admin-notice-divider" aria-hidden="true" />
        <span>展示数据用于管理员后台视觉与交互核验</span>
        <AdminStatusBadge status="unavailable" label="目标 API 未接入" />
      </div>

      <section class="admin-metric-grid" aria-label="平台指标示例">
        <article v-for="metric in metrics" :key="metric.label" class="admin-metric-card">
          <header>
            <span>{{ metric.label }}</span>
            <component :is="metric.icon" :class="`admin-metric-icon admin-metric-icon--${metric.tone}`" aria-hidden="true" />
          </header>
          <strong>{{ metric.value }}</strong>
          <p>{{ metric.detail }}</p>
        </article>
      </section>

      <section class="admin-dashboard-grid">
        <article class="admin-panel admin-trend-panel">
          <header class="admin-panel-header">
            <div><h2>Agent 调用趋势</h2><p>请求、Token 与成功率 · 示例快照</p></div>
            <button type="button" class="admin-icon-button" aria-label="筛选趋势" title="筛选趋势" @click="notify('趋势筛选将在管理员接口接入后生效')"><ListFilter aria-hidden="true" /></button>
          </header>
          <div class="admin-trend-summary"><strong>1,284</strong><span>次运行</span><em>+12.6%</em></div>
          <div class="admin-chart-area" role="img" aria-label="Agent 调用趋势示例图">
            <div class="admin-chart-y-axis"><span>1.2K</span><span>800</span><span>400</span><span>0</span></div>
            <div class="admin-chart-grid-lines" aria-hidden="true"><i /><i /><i /><i /></div>
            <div class="admin-bar-chart"><span v-for="(height, index) in trendBars" :key="index" class="admin-bar" :class="{ 'admin-bar--active': index > 11 }" :style="{ height: `${height}%` }" /></div>
          </div>
          <footer class="admin-chart-legend"><span><i class="admin-legend-dot admin-legend-dot--blue" />调用次数</span><span><i class="admin-legend-dot admin-legend-dot--violet" />Token 消耗</span><span class="admin-chart-period">{{ range }}</span></footer>
        </article>

        <article class="admin-panel admin-resource-panel">
          <header class="admin-panel-header"><div><h2>运行资源</h2><p>当前 Agent 资源可用性 · 示例快照</p></div><span class="admin-success-rate"><strong>98.7%</strong> 成功率</span></header>
          <div class="admin-resource-body">
            <div class="admin-availability-ring" :style="ringStyle"><div><strong>94%</strong><span>可用率</span></div></div>
            <div class="admin-resource-list">
              <div><span><i class="admin-resource-dot admin-resource-dot--green" />活跃运行</span><strong>38</strong><small>当前进行中</small></div>
              <div><span><i class="admin-resource-dot admin-resource-dot--muted" />排队任务</span><strong>12</strong><small>等待执行</small></div>
              <div><span><i class="admin-resource-dot admin-resource-dot--violet" />启用工具</span><strong>24</strong><small>全部可用</small></div>
              <div><span><i class="admin-resource-dot admin-resource-dot--blue" />上下文窗口</span><strong>32K</strong><small>当前模型</small></div>
            </div>
          </div>
        </article>
      </section>

      <section class="admin-dashboard-grid">
        <article class="admin-panel admin-runs-panel">
          <header class="admin-panel-header"><div><h2>最近 Agent 运行</h2><p>按用户、门店和工具查看运行链路</p></div><button type="button" class="admin-button admin-button--ghost" @click="notify('完整运行中心将在后续阶段接入')">查看全部 <ArrowUpRight aria-hidden="true" /></button></header>
          <div class="admin-table-toolbar"><label class="admin-search-field"><Search aria-hidden="true" /><input v-model="query" type="search" placeholder="搜索用户、门店或运行 ID" aria-label="搜索运行记录" /></label><button type="button" class="admin-button admin-button--secondary admin-filter-button" @click="notify('筛选条件：全部状态')"><ListFilter aria-hidden="true" /><span>全部</span><span aria-hidden="true">⌄</span></button></div>
          <div class="admin-table-scroll"><table class="admin-table"><thead><tr><th>运行任务</th><th>用户 / 门店</th><th>工具</th><th class="admin-numeric">Token</th><th>状态</th></tr></thead><tbody><tr v-for="run in filteredRuns" :key="run.id" :class="{ 'admin-row-selected': selectedRun?.id === run.id }" tabindex="0" @click="openRun(run)" @keydown.enter="openRun(run)"><td><button type="button" class="admin-table-link" @click.stop="openRun(run)"><strong>{{ run.task }}</strong><small>{{ run.id }} · {{ run.time }}</small></button></td><td><span class="admin-cell-stack"><strong>{{ run.user }}</strong><small>{{ run.store }}</small></span></td><td><code>{{ run.tool }}</code></td><td class="admin-numeric"><span class="admin-token-value">{{ run.tokens }}</span><small>{{ run.duration }}</small></td><td><AdminStatusBadge :status="run.status" /></td></tr><tr v-if="filteredRuns.length === 0"><td colspan="5" class="admin-empty-cell">没有匹配的运行记录</td></tr></tbody></table></div>
          <footer class="admin-panel-footer"><span>显示 {{ filteredRuns.length }} / {{ runs.length }} 条示例记录</span><button type="button" @click="notify('当前页面为视觉预览，未读取正式运行数据')">打开运行中心 <ArrowUpRight aria-hidden="true" /></button></footer>
        </article>

        <article class="admin-panel admin-provider-panel">
          <header class="admin-panel-header"><div><h2>工具域分布</h2><p>按调用次数统计 · 示例快照</p></div><span class="admin-panel-meta">近 30 天</span></header>
          <div class="admin-distribution-stripes" aria-label="工具域调用分布示例"><i v-for="index in 40" :key="index" class="admin-stripe" :class="index <= 15 ? 'admin-stripe--green' : index <= 28 ? 'admin-stripe--blue' : 'admin-stripe--violet'" /></div>
          <div class="admin-provider-list"><div v-for="row in toolDomains" :key="row.label" class="admin-provider-row"><span class="admin-provider-name"><i class="admin-resource-dot" :class="`admin-resource-dot--${row.tone}`" /><span><strong>{{ row.label }}</strong><small>{{ row.detail }}</small></span></span><span class="admin-provider-count"><strong>{{ row.requests }}</strong><small>{{ row.share }}</small></span></div></div>
        </article>
      </section>

      <section class="admin-dashboard-grid admin-dashboard-grid--lower">
        <article class="admin-panel admin-activity-panel">
          <header class="admin-panel-header"><div><h2>实时调用日志</h2><p>最近一次运行的事件顺序 · 示例快照</p></div><span class="admin-live-label"><i class="admin-status-dot admin-status-dot--online" /> LIVE PREVIEW</span></header>
          <div class="admin-log-summary"><div><span>模型 ID</span><strong>deepseek-flash-0731</strong></div><div><span>上下文窗口</span><strong>32,768 tokens</strong></div></div>
          <div class="admin-log-list" aria-label="Agent 实时调用日志示例"><div v-for="entry in activity" :key="`${entry.time}-${entry.event}`" class="admin-log-row"><time>{{ entry.time }}</time><span :class="`admin-log-event admin-log-event--${entry.tone}`">{{ entry.event }}</span><small>{{ entry.detail }}</small></div></div>
          <footer class="admin-panel-footer"><span><Database aria-hidden="true" /> audit_write: preview only</span><button type="button" @click="notify('仅可复制脱敏摘要；正式接口尚未接入')"><Copy aria-hidden="true" />复制摘要</button></footer>
        </article>
        <article class="admin-panel admin-audit-panel">
          <header class="admin-panel-header"><div><h2>最近操作</h2><p>管理员审计轨迹 · 示例快照</p></div><button type="button" class="admin-icon-button" aria-label="查看审计记录" title="查看审计记录" @click="notify('审计查询将在后续阶段接入')"><ArrowUpRight aria-hidden="true" /></button></header>
          <div class="admin-audit-list"><div class="admin-audit-row"><CircleCheck aria-hidden="true" /><span><strong>管理员查看运行详情</strong><small>系统管理员 · 14:27:18</small></span></div><div class="admin-audit-row"><ShieldCheck aria-hidden="true" /><span><strong>权限范围校验通过</strong><small>SERVER SESSION · 14:26:59</small></span></div><div class="admin-audit-row admin-audit-row--warning"><CircleAlert aria-hidden="true" /><span><strong>同步任务需要关注</strong><small>东湖生鲜店 · 14:18:36</small></span></div><div class="admin-audit-row"><KeyRound aria-hidden="true" /><span><strong>读取 Agent 运行审计</strong><small>只读操作 · 14:15:04</small></span></div></div>
        </article>
      </section>

      <section class="admin-preview-state" aria-label="面板接入状态"><AdminPanelState state="unavailable" title="数据面板处于视觉预览状态" message="正式管理员 session、权限范围与统计接口接入后，将替换此处示例快照。" /></section>
      <footer class="admin-page-footer"><span><Activity aria-hidden="true" /> Master Goods Admin Preview</span><span>Visual reference: chenyme/grok2api frontend · MIT</span></footer>
    </section>

    <button v-if="selectedRun" type="button" class="admin-drawer-scrim" aria-label="关闭运行详情" @click="selectedRun = null" />
    <aside v-if="selectedRun" class="admin-run-drawer" aria-label="运行详情"><header class="admin-drawer-header"><div><span class="admin-section-kicker">RUN DETAIL</span><h2>运行详情</h2></div><button type="button" class="admin-icon-button" aria-label="关闭运行详情" title="关闭运行详情" @click="selectedRun = null"><X aria-hidden="true" /></button></header><code class="admin-drawer-id">{{ selectedRun.id }}</code><div class="admin-drawer-block"><span>用户 / 门店</span><strong>{{ selectedRun.user }} · {{ selectedRun.store }}</strong></div><div class="admin-drawer-block"><span>任务</span><strong>{{ selectedRun.task }}</strong></div><div class="admin-drawer-block"><span>模型 ID</span><strong class="admin-mono">{{ selectedRun.model }}</strong></div><div class="admin-drawer-block"><span>目标工具</span><strong class="admin-mono">{{ selectedRun.tool }}</strong></div><div class="admin-drawer-stats"><div><span>Token</span><strong>{{ selectedRun.tokens }}</strong></div><div><span>耗时</span><strong>{{ selectedRun.duration }}</strong></div></div><div class="admin-drawer-callout"><Check aria-hidden="true" /><span>工具完成事件已写入审计，正式回答已生成。</span></div><button type="button" class="admin-button admin-button--primary admin-drawer-action" @click="notify('审计证据查看将在正式接口接入后开放')">查看审计证据 <ArrowUpRight aria-hidden="true" /></button></aside>
    <div v-if="toast" class="admin-toast" role="status"><CircleCheck aria-hidden="true" />{{ toast }}</div>
  </AdminLayout>
</template>

<style>
.admin-shell { --admin-background:#fdfdfc; --admin-foreground:#1d1d1f; --admin-surface:#fff; --admin-muted:#737373; --admin-muted-light:#a0a0a0; --admin-border:#e8e8e5; --admin-border-strong:#dededb; --admin-secondary:#f3f3f1; --admin-blue:#4a8cc9; --admin-green:#43a56b; --admin-violet:#9073d0; --admin-amber:#c89449; --admin-shadow:0 12px 32px rgba(24,24,20,.045); display:flex; min-height:100vh; background:var(--admin-background); color:var(--admin-foreground); font-family:Inter,ui-sans-serif,system-ui,-apple-system,BlinkMacSystemFont,"Segoe UI","PingFang SC","Microsoft YaHei",sans-serif; font-synthesis:none; text-rendering:optimizeLegibility; -webkit-font-smoothing:antialiased; }
.admin-shell *, .admin-shell *::before, .admin-shell *::after { box-sizing:border-box; }.admin-shell button,.admin-shell input,.admin-shell select{font:inherit}.admin-shell button{color:inherit}.admin-shell button:focus-visible,.admin-shell input:focus-visible,.admin-shell select:focus-visible{outline:1px solid #858585;outline-offset:2px}
.admin-sidebar{position:fixed;inset:0 auto 0 0;z-index:30;display:flex;width:288px;flex-direction:column;overflow-y:auto;background:var(--admin-surface);padding:24px 16px 18px}.admin-sidebar-brand{display:flex;height:28px;align-items:center;justify-content:space-between;padding:0 10px}.admin-brand-lockup{display:grid;gap:1px;padding:0;border:0;background:none;text-align:left;cursor:pointer}.admin-brand-lockup strong{font-size:15px;font-weight:650;letter-spacing:.01em}.admin-brand-lockup span{color:var(--admin-muted-light);font-size:9px;letter-spacing:.12em}.admin-github-link{margin-left:auto}.admin-icon-button{display:inline-grid;width:32px;height:32px;flex:0 0 32px;place-items:center;padding:0;border:0;border-radius:8px;background:transparent;color:var(--admin-muted);cursor:pointer;transition:background-color .16s ease-out,color .16s ease-out}.admin-icon-button svg{width:16px;height:16px;stroke-width:1.8}.admin-icon-button:hover{background:var(--admin-secondary);color:var(--admin-foreground)}.admin-sidebar-close{display:none}.admin-sidebar-nav{display:grid;gap:18px;margin-top:32px}.admin-nav-caption{margin:0 10px 8px;color:var(--admin-muted-light);font-size:10px;font-weight:650;letter-spacing:.12em;text-transform:uppercase}.admin-nav-caption--spaced{margin-top:10px}.admin-nav-list{display:grid;gap:3px}.admin-nav-item{display:flex;min-height:38px;align-items:center;gap:10px;width:100%;padding:9px 10px;border:0;border-radius:8px;background:transparent;color:var(--admin-muted);font-size:13px;text-align:left;cursor:pointer;transition:background-color .16s ease-out,color .16s ease-out}.admin-nav-item svg{width:16px;height:16px;flex:0 0 16px;stroke-width:1.75}.admin-nav-item span{min-width:0;flex:1}.admin-nav-item small{display:inline-grid;min-width:20px;height:18px;place-items:center;border-radius:999px;background:var(--admin-secondary);color:var(--admin-muted);font-size:10px}.admin-nav-item:hover{background:var(--admin-secondary);color:var(--admin-foreground)}.admin-nav-item--active{background:var(--admin-foreground);color:#fff}.admin-nav-item--active:hover{background:#2c2c2c;color:#fff}.admin-nav-item--active small{background:rgba(255,255,255,.14);color:#fff}.admin-documentation-toggle svg:last-child{width:14px;height:14px;margin-left:auto;transition:transform .16s ease-out}.admin-documentation-toggle svg.is-collapsed{transform:rotate(-90deg)}.admin-documentation-items{display:grid;gap:2px;margin:2px 0 4px 36px}.admin-documentation-items button{padding:6px 8px;border:0;border-radius:6px;background:transparent;color:var(--admin-muted);font-size:12px;text-align:left;cursor:pointer}.admin-documentation-items button:hover{background:var(--admin-secondary);color:var(--admin-foreground)}.admin-sidebar-footer{display:grid;gap:11px;margin-top:auto;padding:18px 10px 0;border-top:1px solid var(--admin-border)}.admin-connection-status{display:flex;align-items:center;gap:7px;color:var(--admin-muted);font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:10px}.admin-status-dot{display:inline-block;width:6px;height:6px;flex:0 0 6px;border-radius:50%;background:var(--admin-muted-light)}.admin-status-dot--online{background:var(--admin-green);box-shadow:0 0 0 3px rgba(67,165,107,.12)}.admin-account-control{display:flex;align-items:center;gap:9px;width:100%;padding:8px 0;border:0;background:transparent;text-align:left;cursor:pointer}.admin-account-avatar{display:inline-grid;width:30px;height:30px;flex:0 0 30px;place-items:center;border-radius:8px;background:var(--admin-foreground);color:#fff;font-size:10px;font-weight:650}.admin-account-copy{display:grid;min-width:0;flex:1;gap:3px}.admin-account-copy strong{overflow:hidden;font-size:11px;font-weight:550;text-overflow:ellipsis;white-space:nowrap}.admin-account-copy small{color:var(--admin-muted-light);font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:8px;letter-spacing:.08em}.admin-account-menu{width:16px;height:16px;color:var(--admin-muted-light)}.admin-sidebar-footer-links{display:grid;grid-template-columns:1fr 1fr;gap:6px}.admin-sidebar-footer-links button{display:inline-flex;align-items:center;justify-content:center;gap:5px;min-height:30px;padding:0 5px;border:1px solid var(--admin-border);border-radius:7px;background:#fff;color:var(--admin-muted);font-size:10px;cursor:pointer}.admin-sidebar-footer-links button:hover{background:var(--admin-secondary);color:var(--admin-foreground)}.admin-sidebar-footer-links svg{width:13px;height:13px}.admin-sidebar-mark{display:inline-flex;align-items:center;gap:6px;color:var(--admin-muted-light);font-size:9px}.admin-sidebar-mark svg{width:13px;height:13px}.admin-sidebar-backdrop{display:none}
.admin-main-column{min-width:0;flex:1;margin-left:288px}.admin-mobile-header{display:none}.admin-content{width:100%;max-width:1400px;margin:0 auto;padding:34px 44px 24px}.admin-page{display:grid;gap:18px;min-width:0}.admin-page-header{display:flex;align-items:flex-end;justify-content:space-between;gap:20px}.admin-breadcrumb-block{min-width:0}.admin-breadcrumbs{display:flex;align-items:center;gap:7px;color:var(--admin-muted-light);font-size:10px}.admin-breadcrumbs strong{color:var(--admin-muted);font-weight:500}.admin-breadcrumb-block h1{margin:11px 0 0;color:var(--admin-foreground);font-size:28px;font-weight:520;line-height:1.1;letter-spacing:-.02em}.admin-page-actions{display:flex;align-items:center;gap:8px}.admin-period-selector{display:inline-flex;height:34px;align-items:center;gap:7px;padding:0 9px;border:1px solid var(--admin-border);border-radius:8px;background:#fff;color:var(--admin-muted)}.admin-period-selector>svg{width:14px;height:14px}.admin-period-selector svg:last-child{width:13px;height:13px;color:var(--admin-muted-light)}.admin-period-selector select{min-width:78px;appearance:none;border:0;outline:0;background:transparent;color:var(--admin-foreground);font-size:11px;cursor:pointer}.admin-button{display:inline-flex;min-height:34px;align-items:center;justify-content:center;gap:7px;padding:0 11px;border:1px solid transparent;border-radius:8px;font-size:11px;font-weight:520;cursor:pointer;transition:background-color .16s ease-out,border-color .16s ease-out,color .16s ease-out}.admin-button svg{width:14px;height:14px;stroke-width:1.8}.admin-button--secondary{border-color:var(--admin-border);background:#fff;color:var(--admin-foreground)}.admin-button--secondary:hover{border-color:var(--admin-border-strong);background:var(--admin-secondary)}.admin-button--ghost{border-color:transparent;background:transparent;color:var(--admin-muted)}.admin-button--ghost:hover{background:var(--admin-secondary);color:var(--admin-foreground)}.admin-button--primary{background:var(--admin-foreground);color:#fff}.admin-button--primary:hover{background:#343434}.admin-button:disabled{cursor:not-allowed;opacity:.58}.is-spinning{animation:admin-spin .8s linear infinite}@keyframes admin-spin{to{transform:rotate(360deg)}}
.admin-preview-notice{display:flex;min-height:34px;align-items:center;gap:8px;margin-top:4px;padding:0 11px;border:1px solid var(--admin-border);border-radius:8px;background:rgba(255,255,255,.7);color:var(--admin-muted);font-size:10px}.admin-preview-notice strong{color:var(--admin-muted);font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:9px;font-weight:600;letter-spacing:.08em}.admin-notice-divider{width:1px;height:14px;background:var(--admin-border)}.admin-preview-notice>span:nth-child(4){overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.admin-preview-notice .admin-status-badge{margin-left:auto}
.admin-metric-grid{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:9px}.admin-metric-card,.admin-panel{border:1px solid var(--admin-border);border-radius:8px;background:var(--admin-surface);box-shadow:var(--admin-shadow)}.admin-metric-card{min-height:116px;padding:15px 16px}.admin-metric-card header{display:flex;align-items:center;justify-content:space-between;gap:8px;color:var(--admin-muted);font-size:10px}.admin-metric-card header svg{width:16px;height:16px;stroke-width:1.7}.admin-metric-icon--blue{color:var(--admin-blue)}.admin-metric-icon--green{color:var(--admin-green)}.admin-metric-icon--violet{color:var(--admin-violet)}.admin-metric-icon--amber{color:var(--admin-amber)}.admin-metric-icon--neutral{color:var(--admin-muted)}.admin-metric-card>strong{display:block;margin-top:19px;font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:23px;font-weight:450;letter-spacing:-.03em}.admin-metric-card p{margin:6px 0 0;color:var(--admin-muted-light);font-size:9px}.admin-dashboard-grid{display:grid;grid-template-columns:minmax(0,1.38fr) minmax(320px,.82fr);gap:9px}.admin-dashboard-grid--lower{grid-template-columns:minmax(0,1.18fr) minmax(320px,.82fr)}.admin-panel{min-width:0;padding:18px}.admin-panel-header{display:flex;align-items:flex-start;justify-content:space-between;gap:12px}.admin-panel-header h2{margin:0;color:var(--admin-foreground);font-size:14px;font-weight:550;letter-spacing:-.01em}.admin-panel-header p{margin:6px 0 0;color:var(--admin-muted-light);font-size:9px}.admin-trend-panel,.admin-resource-panel{min-height:313px}.admin-trend-summary{display:flex;align-items:baseline;gap:7px;margin-top:21px}.admin-trend-summary strong{font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:20px;font-weight:450}.admin-trend-summary span{color:var(--admin-muted);font-size:10px}.admin-trend-summary em{margin-left:4px;color:var(--admin-green);font-size:10px;font-style:normal}.admin-chart-area{position:relative;height:162px;margin-top:14px;padding:0 10px 0 34px}.admin-chart-y-axis{position:absolute;inset:0 auto 0 0;display:flex;flex-direction:column;justify-content:space-between;padding:0 0 2px;color:var(--admin-muted-light);font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:8px}.admin-chart-grid-lines{position:absolute;inset:0 0 0 34px;display:flex;flex-direction:column;justify-content:space-between;pointer-events:none}.admin-chart-grid-lines i{display:block;width:100%;border-top:1px solid #f0f0ed}.admin-bar-chart{position:absolute;inset:0 10px 0 34px;display:flex;align-items:flex-end;gap:7px;padding:0 2px}.admin-bar{display:block;min-width:5px;flex:1;border-radius:3px 3px 1px 1px;background:rgba(74,140,201,.22);transition:height .22s ease-out}.admin-bar--active{background:rgba(144,115,208,.58)}.admin-chart-legend{display:flex;align-items:center;gap:16px;margin-top:14px;color:var(--admin-muted);font-size:9px}.admin-chart-legend span{display:inline-flex;align-items:center;gap:5px}.admin-legend-dot{width:6px;height:6px;border-radius:50%}.admin-legend-dot--blue{background:var(--admin-blue)}.admin-legend-dot--violet{background:var(--admin-violet)}.admin-chart-period{margin-left:auto;color:var(--admin-muted-light)}.admin-success-rate{color:var(--admin-muted);font-size:9px;white-space:nowrap}.admin-success-rate strong{color:var(--admin-green);font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:14px;font-weight:500}.admin-resource-body{display:grid;grid-template-columns:minmax(145px,.85fr) minmax(140px,1.15fr);gap:12px;align-items:center;min-height:227px}.admin-availability-ring{position:relative;display:grid;width:154px;height:154px;place-items:center;border-radius:50%}.admin-availability-ring::before{position:absolute;width:116px;height:116px;border-radius:50%;background:var(--admin-surface);content:''}.admin-availability-ring>div{position:relative;display:grid;gap:5px;text-align:center}.admin-availability-ring strong{font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:26px;font-weight:450}.admin-availability-ring span{color:var(--admin-muted);font-size:9px}.admin-resource-list{display:grid;gap:12px}.admin-resource-list>div{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:4px 8px;align-items:center}.admin-resource-list span{display:flex;min-width:0;align-items:center;gap:6px;color:var(--admin-muted);font-size:9px}.admin-resource-list strong{font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:13px;font-weight:500}.admin-resource-list small{grid-column:1 / -1;padding-left:12px;color:var(--admin-muted-light);font-size:8px}.admin-resource-dot{display:inline-block;width:6px;height:6px;flex:0 0 6px;border-radius:50%;background:var(--admin-muted-light)}.admin-resource-dot--green{background:var(--admin-green)}.admin-resource-dot--blue{background:var(--admin-blue)}.admin-resource-dot--violet{background:var(--admin-violet)}.admin-resource-dot--muted{background:#c5c5c0}
.admin-runs-panel{overflow:hidden}.admin-table-toolbar{display:flex;align-items:center;gap:8px;margin-top:18px}.admin-search-field{display:flex;min-width:0;flex:1;height:32px;align-items:center;gap:7px;padding:0 9px;border:1px solid var(--admin-border);border-radius:7px;color:var(--admin-muted-light)}.admin-search-field svg{width:14px;height:14px;flex:0 0 14px}.admin-search-field input{width:100%;min-width:0;border:0;outline:0;background:transparent;color:var(--admin-foreground);font-size:10px}.admin-search-field input::placeholder{color:var(--admin-muted-light)}.admin-filter-button{height:32px;min-height:32px;flex:0 0 auto}.admin-table-scroll{margin:14px -18px 0;overflow-x:auto}.admin-table{width:100%;min-width:640px;border-collapse:collapse;text-align:left}.admin-table th{padding:8px 18px;border-top:1px solid var(--admin-border);border-bottom:1px solid var(--admin-border);color:var(--admin-muted-light);font-size:9px;font-weight:500;white-space:nowrap}.admin-table td{padding:10px 18px;border-bottom:1px solid #f0f0ed;color:var(--admin-foreground);font-size:10px;vertical-align:middle}.admin-table tbody tr{cursor:pointer;transition:background-color .14s ease-out}.admin-table tbody tr:hover,.admin-table tbody tr:focus-visible{background:#fcfcfa;outline:none}.admin-table tbody tr.admin-row-selected{background:#f8f8f5}.admin-table-link{display:grid;max-width:280px;gap:5px;padding:0;border:0;background:transparent;text-align:left;cursor:pointer}.admin-table-link strong{overflow:hidden;font-size:10px;font-weight:550;text-overflow:ellipsis;white-space:nowrap}.admin-table-link small,.admin-cell-stack small,.admin-table td small{color:var(--admin-muted);font-size:9px}.admin-cell-stack{display:grid;gap:5px}.admin-cell-stack strong{font-size:10px;font-weight:500}.admin-table code{color:#6767a1;font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:9px;white-space:nowrap}.admin-numeric{text-align:right!important}.admin-token-value{display:block;font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:10px}.admin-table td.admin-numeric small{display:block;margin-top:5px}.admin-empty-cell{height:80px;color:var(--admin-muted);text-align:center!important}.admin-panel-footer{display:flex;align-items:center;justify-content:space-between;gap:12px;min-height:35px;margin-top:1px;padding-top:11px;color:var(--admin-muted-light);font-size:9px}.admin-panel-footer button{display:inline-flex;align-items:center;gap:5px;padding:0;border:0;background:transparent;color:var(--admin-muted);font-size:9px;cursor:pointer}.admin-panel-footer button:hover{color:var(--admin-foreground)}.admin-panel-footer svg{width:13px;height:13px}
.admin-provider-panel{min-height:362px}.admin-panel-meta{color:var(--admin-muted-light);font-size:9px}.admin-distribution-stripes{display:flex;gap:3px;margin-top:31px}.admin-stripe{height:8px;flex:1;border-radius:2px;background:var(--admin-border)}.admin-stripe--green{background:rgba(67,165,107,.65)}.admin-stripe--blue{background:rgba(74,140,201,.55)}.admin-stripe--violet{background:rgba(144,115,208,.55)}.admin-provider-list{display:grid;gap:0;margin-top:24px}.admin-provider-row{display:flex;align-items:center;justify-content:space-between;gap:10px;min-height:61px;border-bottom:1px solid var(--admin-border)}.admin-provider-row:last-child{border-bottom:0}.admin-provider-name{display:flex;min-width:0;align-items:center;gap:9px}.admin-provider-name>span{display:grid;min-width:0;gap:5px}.admin-provider-name strong,.admin-provider-name small,.admin-provider-count strong,.admin-provider-count small{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.admin-provider-name strong{font-size:10px;font-weight:500}.admin-provider-name small{color:var(--admin-muted);font-size:9px}.admin-provider-count{display:grid;flex:0 0 45px;gap:5px;text-align:right}.admin-provider-count strong{font-size:11px;font-weight:500}.admin-provider-count small{color:var(--admin-muted);font-size:9px}
.admin-activity-panel,.admin-audit-panel{min-height:294px}.admin-live-label{display:inline-flex;align-items:center;gap:6px;color:var(--admin-green);font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:9px;letter-spacing:.06em;white-space:nowrap}.admin-log-summary{display:grid;grid-template-columns:1fr 1fr;gap:1px;margin:23px -18px 0;border-top:1px solid var(--admin-border);border-bottom:1px solid var(--admin-border);background:var(--admin-border)}.admin-log-summary>div{display:grid;gap:6px;background:var(--admin-surface);padding:10px 18px}.admin-log-summary span{color:var(--admin-muted);font-size:9px}.admin-log-summary strong{overflow:hidden;color:#6767a1;font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:9px;font-weight:400;text-overflow:ellipsis;white-space:nowrap}.admin-log-list{display:grid;padding-top:7px}.admin-log-row{display:grid;grid-template-columns:54px 128px minmax(0,1fr);gap:8px;min-height:28px;align-items:center;font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:9px}.admin-log-row time{color:var(--admin-muted-light)}.admin-log-event{white-space:nowrap}.admin-log-event--success{color:var(--admin-green)}.admin-log-event--info{color:#5a86b0}.admin-log-row small{overflow:hidden;color:var(--admin-muted);text-overflow:ellipsis;white-space:nowrap}.admin-activity-panel .admin-panel-footer{margin-top:5px;border-top:1px solid var(--admin-border)}.admin-activity-panel .admin-panel-footer span{display:inline-flex;align-items:center;gap:6px}.admin-audit-list{display:grid;margin-top:19px}.admin-audit-row{display:flex;min-height:55px;align-items:center;gap:10px;border-bottom:1px solid var(--admin-border)}.admin-audit-row:last-child{border-bottom:0}.admin-audit-row>svg{width:16px;height:16px;flex:0 0 16px;color:var(--admin-green);stroke-width:1.7}.admin-audit-row--warning>svg{color:var(--admin-amber)}.admin-audit-row span{display:grid;min-width:0;gap:5px}.admin-audit-row strong{overflow:hidden;font-size:10px;font-weight:500;text-overflow:ellipsis;white-space:nowrap}.admin-audit-row small{overflow:hidden;color:var(--admin-muted);font-size:9px;text-overflow:ellipsis;white-space:nowrap}
.admin-preview-state{margin-top:0}.admin-panel-state{display:grid;grid-template-columns:auto minmax(0,1fr) auto;grid-template-rows:auto auto;align-items:center;gap:2px 9px;padding:10px 12px;border:1px solid var(--admin-border);border-radius:8px;background:rgba(255,255,255,.68)}.admin-panel-state__icon{grid-row:1 / -1;width:16px;height:16px;color:var(--admin-muted-light)}.admin-panel-state strong{color:var(--admin-muted);font-size:10px;font-weight:550}.admin-panel-state p{margin:0;color:var(--admin-muted-light);font-size:9px}.admin-panel-state code{grid-column:2;color:var(--admin-muted-light);font-size:8px}.admin-panel-state button{grid-column:3;grid-row:1 / -1}.admin-page-footer{display:flex;justify-content:space-between;gap:16px;margin-top:1px;color:var(--admin-muted);font-size:9px}.admin-page-footer span{display:inline-flex;align-items:center;gap:6px}.admin-page-footer svg{width:13px;height:13px}
.admin-status-badge{display:inline-flex;min-height:20px;align-items:center;gap:5px;padding:0 7px;border:1px solid transparent;border-radius:999px;font-size:9px;white-space:nowrap}.admin-status-badge svg{width:12px;height:12px;stroke-width:1.8}.admin-status-badge--success{border-color:rgba(67,165,107,.2);background:rgba(67,165,107,.08);color:#318254}.admin-status-badge--review{border-color:rgba(144,115,208,.2);background:rgba(144,115,208,.08);color:#7760b1}.admin-status-badge--attention{border-color:rgba(200,148,73,.24);background:rgba(200,148,73,.09);color:#9b6d25}.admin-status-badge--failed,.admin-status-badge--blocked{border-color:rgba(192,80,80,.22);background:rgba(192,80,80,.08);color:#a04c4c}.admin-status-badge--running{border-color:rgba(74,140,201,.22);background:rgba(74,140,201,.08);color:#427bad}.admin-status-badge--unavailable{border-color:var(--admin-border);background:var(--admin-secondary);color:var(--admin-muted)}
.admin-drawer-scrim{position:fixed;inset:0;z-index:35;border:0;background:rgba(24,24,20,.12);cursor:pointer}.admin-run-drawer{position:fixed;inset:0 0 0 auto;z-index:40;width:360px;overflow-y:auto;border-left:1px solid var(--admin-border);background:var(--admin-surface);padding:28px 22px;box-shadow:-16px 0 35px rgba(24,24,20,.08)}.admin-drawer-header{display:flex;align-items:flex-start;justify-content:space-between;gap:12px;border-bottom:1px solid var(--admin-border);padding-bottom:18px}.admin-section-kicker{color:var(--admin-muted-light);font-size:9px;letter-spacing:.12em}.admin-drawer-header h2{margin:8px 0 0;font-size:18px;font-weight:500}.admin-drawer-id{display:block;margin:18px 0 19px;color:#6767a1;font-size:10px}.admin-drawer-block{display:grid;gap:7px;border-bottom:1px solid var(--admin-border);padding:13px 0}.admin-drawer-block span,.admin-drawer-stats span{color:var(--admin-muted);font-size:10px}.admin-drawer-block strong{font-size:11px;font-weight:500;line-height:1.45}.admin-mono{font-family:ui-monospace,SFMono-Regular,Menlo,monospace}.admin-drawer-stats{display:grid;grid-template-columns:1fr 1fr;gap:8px;padding:16px 0}.admin-drawer-stats>div{display:grid;gap:7px;border:1px solid var(--admin-border);border-radius:6px;padding:10px}.admin-drawer-stats strong{font-size:14px;font-weight:500}.admin-drawer-callout{display:flex;gap:8px;border:1px solid rgba(67,165,107,.28);border-radius:6px;background:rgba(67,165,107,.06);padding:11px;color:#318254;font-size:10px;line-height:1.5}.admin-drawer-callout svg{width:15px;height:15px;flex:0 0 15px;stroke-width:1.8}.admin-drawer-action{width:100%;margin-top:18px}.admin-toast{position:fixed;right:24px;bottom:24px;z-index:60;display:inline-flex;align-items:center;gap:8px;border:1px solid var(--admin-border);border-radius:999px;background:var(--admin-foreground);padding:10px 14px;color:#fff;font-size:11px;box-shadow:var(--admin-shadow)}.admin-toast svg{width:15px;height:15px;color:#9bd3ab}
@media (max-width:1180px){.admin-sidebar{width:248px}.admin-main-column{margin-left:248px}.admin-content{padding-inline:28px}.admin-metric-grid{grid-template-columns:repeat(3,minmax(0,1fr))}.admin-dashboard-grid,.admin-dashboard-grid--lower{grid-template-columns:minmax(0,1fr)}}
@media (max-width:760px){.admin-sidebar{left:-288px;width:288px;transition:left .18s ease-out}.admin-sidebar.admin-sidebar--open{left:0}.admin-sidebar-close{display:inline-grid}.admin-github-link{display:none}.admin-sidebar-backdrop{position:fixed;inset:0;z-index:25;display:block;background:rgba(0,0,0,.24)}.admin-main-column{margin-left:0}.admin-mobile-header{display:flex;height:48px;align-items:center;justify-content:space-between;border-bottom:1px solid var(--admin-border);padding:0 14px}.admin-mobile-header strong{font-size:12px;font-weight:600;letter-spacing:.01em}.admin-content{padding:30px 16px 20px}.admin-page-header{align-items:flex-start;flex-direction:column;gap:16px}.admin-page-actions{width:100%}.admin-period-selector{flex:1}.admin-period-selector select{width:100%}.admin-preview-notice{margin-top:3px}.admin-preview-notice .admin-status-badge{display:none}.admin-metric-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.admin-metric-card{min-height:105px;padding:13px}.admin-metric-card>strong{font-size:21px}.admin-panel{padding-inline:14px}.admin-log-summary{margin-inline:-14px}.admin-log-summary>div{padding-inline:14px}.admin-chart-legend{gap:10px}.admin-chart-period{margin-left:0}.admin-resource-body{grid-template-columns:minmax(115px,.8fr) minmax(130px,1.2fr);gap:9px}.admin-availability-ring{width:136px;height:136px}.admin-availability-ring::before{width:103px;height:103px}.admin-run-drawer{width:min(360px,100%)}.admin-page-footer{align-items:flex-start;flex-direction:column;gap:7px}}
@media (max-width:480px){.admin-content{padding-inline:12px}.admin-breadcrumbs{font-size:10px}.admin-breadcrumb-block h1{font-size:22px}.admin-preview-notice{gap:6px;font-size:9px}.admin-notice-divider{display:none}.admin-metric-grid{gap:6px}.admin-metric-card{min-height:98px;padding:11px}.admin-metric-card header{font-size:9px}.admin-metric-card>strong{margin-top:13px;font-size:19px}.admin-metric-card p{margin-top:6px;font-size:9px}.admin-dashboard-grid{gap:6px}.admin-panel{padding:14px 12px}.admin-panel-header h2{font-size:13px}.admin-panel-header p{max-width:200px;line-height:1.4}.admin-trend-panel,.admin-resource-panel{min-height:330px}.admin-chart-area{height:170px;padding-left:31px}.admin-chart-grid-lines{left:32px}.admin-bar-chart{gap:5px;padding-inline:2px}.admin-resource-body{grid-template-columns:1fr;gap:16px}.admin-availability-ring{width:126px;height:126px}.admin-availability-ring::before{width:96px;height:96px}.admin-resource-list{min-height:190px}.admin-table-toolbar{align-items:stretch;flex-direction:column}.admin-filter-button{align-self:flex-start}.admin-log-row{grid-template-columns:48px 1fr;gap:5px}.admin-log-row small{grid-column:2}.admin-log-summary{grid-template-columns:1fr;gap:0}.admin-preview-state .admin-panel-state{grid-template-columns:auto minmax(0,1fr)}.admin-preview-state .admin-panel-state button{grid-column:2;grid-row:auto;justify-self:start}.admin-toast{right:12px;bottom:12px;left:12px;justify-content:center}}
@media (prefers-reduced-motion:reduce){.admin-shell *,.admin-shell *::before,.admin-shell *::after{scroll-behavior:auto!important;transition-duration:.01ms!important;animation-duration:.01ms!important;animation-iteration-count:1!important}}
</style>
