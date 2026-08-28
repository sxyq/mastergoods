import { useMemo, useState } from "react";
import {
  Activity,
  ArrowUpRight,
  Bot,
  Check,
  ChevronDown,
  CircleAlert,
  CircleCheck,
  Clock3,
  Database,
  Eye,
  Github,
  Gauge,
  KeyRound,
  LayoutDashboard,
  ListFilter,
  Menu,
  MoreHorizontal,
  RefreshCw,
  Search,
  Server,
  Settings2,
  ShieldCheck,
  Store,
  UsersRound,
  WholeWord,
  X,
} from "lucide-react";

const navItems = [
  { id: "overview", label: "平台总览", icon: LayoutDashboard },
  { id: "users", label: "用户与门店", icon: UsersRound },
  { id: "agent", label: "Agent 运行", icon: Bot, badge: "12" },
  { id: "audit", label: "操作审计", icon: Eye },
  { id: "system", label: "系统状态", icon: Server },
];

const documentationGroups = [
  { label: "Agent", icon: Bot, items: ["运行记录", "工具调用", "上下文窗口"] },
  { label: "组织", icon: UsersRound, items: ["门店成员", "权限范围"] },
  { label: "系统", icon: Database, items: ["服务健康", "数据保留"] },
];

const metrics = [
  { label: "用户总数", value: "12,864", detail: "+8.4% 较上月", icon: UsersRound },
  { label: "活跃门店", value: "128", detail: "3 个需要关注", icon: Store },
  { label: "Agent 运行", value: "1,284", detail: "98.7% 已完成", icon: Bot },
  { label: "Token 消耗", value: "4.82M", detail: "今日累计", icon: WholeWord },
  { label: "平均首字延迟", value: "1.24s", detail: "近 30 天样本", icon: Gauge },
];

const runs = [
  { id: "run-260828-0182", user: "林晓梅", store: "望江仓储店", task: "查询低库存商品并生成补货草稿", model: "deepseek-flash-0731", tokens: "12,480", time: "14:27:08", duration: "8.42s", status: "completed", tool: "list_low_stock_products" },
  { id: "run-260828-0181", user: "周凯", store: "新城配送中心", task: "统计本月销售额和回款情况", model: "deepseek-flash-0731", tokens: "8,921", time: "14:25:41", duration: "3.18s", status: "completed", tool: "get_sales_summary" },
  { id: "run-260828-0179", user: "陈玉兰", store: "东湖生鲜店", task: "创建一张客户资料草稿", model: "deepseek-flash-0731", tokens: "6,204", time: "14:22:16", duration: "6.70s", status: "review", tool: "create_customer_draft" },
  { id: "run-260828-0177", user: "赵明", store: "望江仓储店", task: "检查今日库存同步状态", model: "deepseek-flash-0731", tokens: "4,188", time: "14:18:32", duration: "2.09s", status: "attention", tool: "get_sync_status" },
];

const activity = [
  { time: "14:27:16", event: "tool.completed", detail: "list_low_stock_products", tone: "success" },
  { time: "14:27:15", event: "message.delta", detail: "stream chunk 18 / 18", tone: "info" },
  { time: "14:27:13", event: "context.compacted", detail: "12,480 → 4,096 tokens", tone: "info" },
  { time: "14:27:10", event: "tool.started", detail: "list_low_stock_products", tone: "info" },
  { time: "14:27:08", event: "run.started", detail: "run-260828-0182", tone: "info" },
  { time: "14:26:59", event: "request.received", detail: "store=望江仓储店", tone: "info" },
];

const statusLabels = { completed: "已完成", review: "待确认", attention: "需关注" };
const trendBars = [32, 42, 36, 58, 48, 64, 53, 70, 61, 78, 67, 88, 76, 94, 82, 96];
const providerRows = [
  { label: "库存与商品", detail: "成功率 99.1% · 1.84M tokens", requests: "486", share: "38.0%", tone: "emerald" },
  { label: "销售与财务", detail: "成功率 98.4% · 1.62M tokens", requests: "421", share: "32.9%", tone: "blue" },
  { label: "草稿与审计", detail: "成功率 97.8% · 1.36M tokens", requests: "377", share: "29.1%", tone: "violet" },
];

export function App() {
  const [activeNav, setActiveNav] = useState("overview");
  const [selectedRun, setSelectedRun] = useState(null);
  const [query, setQuery] = useState("");
  const [range, setRange] = useState("近 30 天");
  const [toast, setToast] = useState("");
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [openDocs, setOpenDocs] = useState({ Agent: true });

  const filteredRuns = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    if (!normalized) return runs;
    return runs.filter((run) => [run.id, run.user, run.store, run.task, run.tool].some((value) => value.toLowerCase().includes(normalized)));
  }, [query]);
  const currentTitle = navItems.find((item) => item.id === activeNav)?.label ?? "平台总览";

  function notify(message) {
    setToast(message);
    window.setTimeout(() => setToast(""), 2200);
  }

  function selectNav(id) {
    setActiveNav(id);
    setSidebarOpen(false);
    if (id !== "overview") notify(`已切换到${navItems.find((item) => item.id === id)?.label}`);
  }

  function toggleDocumentation(label) {
    setOpenDocs((current) => ({ ...current, [label]: !current[label] }));
  }

  return (
    <div className="app-shell">
      <aside className={`sidebar ${sidebarOpen ? "sidebar--open" : ""}`}>
        <div className="sidebar-brand">
          <a className="brand-lockup" href="#overview" onClick={() => selectNav("overview")}><strong>MASTER GOODS</strong><span>ADMIN CONSOLE</span></a>
          <button className="icon-button sidebar-close" type="button" aria-label="关闭导航" title="关闭导航" onClick={() => setSidebarOpen(false)}><X /></button>
          <button className="icon-button github-link" type="button" aria-label="打开项目主页" title="项目主页" onClick={() => notify("项目主页链接仅用于原型展示")}><Github /></button>
        </div>

        <nav className="sidebar-nav" aria-label="后台导航">
          <div className="nav-caption">控制台</div>
          <div className="nav-list">
            {navItems.map(({ id, label, icon: Icon, badge }) => (
              <button className={`nav-item ${activeNav === id ? "nav-item--active" : ""}`} key={id} type="button" aria-current={activeNav === id ? "page" : undefined} onClick={() => selectNav(id)}>
                <Icon /><span>{label}</span>{badge ? <small>{badge}</small> : null}
              </button>
            ))}
          </div>
          <div className="nav-caption nav-caption--spaced">监控视图</div>
          <div className="nav-list documentation-list">
            {documentationGroups.map(({ label, icon: Icon, items }) => {
              const open = Boolean(openDocs[label]);
              return (
                <div className="documentation-group" key={label}>
                  <button className="nav-item documentation-toggle" type="button" aria-expanded={open} onClick={() => toggleDocumentation(label)}><Icon /><span>{label}</span><ChevronDown className={open ? "" : "is-collapsed"} /></button>
                  {open ? <div className="documentation-items">{items.map((item) => <button key={item} type="button" onClick={() => notify(`${label} / ${item}`)}>{item}</button>)}</div> : null}
                </div>
              );
            })}
          </div>
        </nav>

        <div className="sidebar-footer">
          <div className="connection-status"><span className="status-dot status-dot--online" /> 服务正常</div>
          <div className="account-control"><span className="account-avatar">SA</span><span className="account-copy"><strong>系统管理员</strong><small>SUPER_ADMIN</small></span><button className="icon-button account-menu" type="button" aria-label="管理员菜单" title="管理员菜单" onClick={() => notify("当前管理员：系统管理员")}><MoreHorizontal /></button></div>
        </div>
      </aside>

      {sidebarOpen ? <button className="sidebar-backdrop" type="button" aria-label="关闭导航" onClick={() => setSidebarOpen(false)} /> : null}

      <div className="main-column">
        <header className="mobile-header"><button className="icon-button" type="button" aria-label="打开导航" title="打开导航" onClick={() => setSidebarOpen(true)}><Menu /></button><strong>MASTER GOODS</strong><button className="icon-button" type="button" aria-label="打开设置" title="打开设置" onClick={() => selectNav("system")}><Settings2 /></button></header>
        <main className="content">
          <header className="page-header">
            <div className="breadcrumb-block"><div className="breadcrumbs"><span>管理后台</span><span>/</span><strong>{currentTitle}</strong></div><h1>{currentTitle}</h1></div>
            <div className="page-actions"><label className="period-selector"><Clock3 /><select value={range} onChange={(event) => setRange(event.target.value)} aria-label="统计时间范围"><option>今天</option><option>近 7 天</option><option>近 30 天</option><option>近 90 天</option></select><ChevronDown /></label><button className="button button-secondary" type="button" onClick={() => notify(`已刷新${range}数据`)}><RefreshCw /> 刷新</button></div>
          </header>

          <div className="preview-notice"><span className="status-dot status-dot--online" /><span>LOCAL PREVIEW</span><span className="notice-divider" /><span>展示数据用于管理员后台视觉与交互核验</span></div>

          <section className="metric-grid" aria-label="平台指标">
            {metrics.map(({ label, value, detail, icon: Icon }) => <article className="metric-card" key={label}><header><span>{label}</span><Icon /></header><strong>{value}</strong><p>{detail}</p></article>)}
          </section>

          <section className="dashboard-grid">
            <article className="panel trend-panel"><div className="panel-header"><div><h2>Agent 调用趋势</h2><p>请求、Token 与成功率</p></div><button className="icon-button" type="button" aria-label="筛选趋势" title="筛选趋势" onClick={() => notify("趋势筛选：全部调用")}><ListFilter /></button></div><div className="trend-summary"><strong>1,284</strong><span>次运行</span><em>+12.6%</em></div><div className="chart-area" aria-label="Agent 调用趋势图"><div className="chart-y-axis"><span>1.2K</span><span>800</span><span>400</span><span>0</span></div><div className="chart-grid-lines"><i /><i /><i /><i /></div><div className="bar-chart">{trendBars.map((height, index) => <span key={index} style={{ height: `${height}%` }} className={index > 11 ? "bar bar--active" : "bar"} />)}</div></div><div className="chart-legend"><span><i className="legend-dot legend-dot--blue" />调用次数</span><span><i className="legend-dot legend-dot--violet" />Token 消耗</span><span className="chart-period">{range}</span></div></article>
            <article className="panel resource-panel"><div className="panel-header"><div><h2>运行资源</h2><p>当前 Agent 资源可用性</p></div><span className="success-rate"><strong>98.7%</strong> 成功率</span></div><div className="resource-body"><div className="availability-ring"><div><strong>94%</strong><span>可用率</span></div></div><div className="resource-list"><div><span><i className="resource-dot resource-dot--green" />活跃运行</span><strong>38</strong><small>当前进行中</small></div><div><span><i className="resource-dot resource-dot--muted" />排队任务</span><strong>12</strong><small>等待执行</small></div><div><span><i className="resource-dot resource-dot--violet" />启用工具</span><strong>24</strong><small>全部可用</small></div><div><span><i className="resource-dot resource-dot--blue" />上下文窗口</span><strong>32K</strong><small>当前模型</small></div></div></div></article>
          </section>

          <section className="dashboard-grid">
            <article className="panel runs-panel"><div className="panel-header"><div><h2>最近 Agent 运行</h2><p>按用户、门店和工具查看运行链路</p></div><button className="button button-ghost" type="button" onClick={() => selectNav("agent")}>查看全部 <ArrowUpRight /></button></div><div className="table-toolbar"><label className="search-field"><Search /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="搜索用户、门店或运行 ID" aria-label="搜索运行记录" /></label><button className="button button-secondary filter-button" type="button" onClick={() => notify("筛选条件：全部状态")}><ListFilter /><span>全部</span><ChevronDown /></button></div><div className="table-scroll"><table><thead><tr><th>运行任务</th><th>用户 / 门店</th><th>工具</th><th className="numeric">Token</th><th>状态</th></tr></thead><tbody>{filteredRuns.map((run) => <tr key={run.id} className={selectedRun?.id === run.id ? "row-selected" : ""} onClick={() => setSelectedRun(run)}><td><button className="table-link" type="button" onClick={() => setSelectedRun(run)}><strong>{run.task}</strong><small>{run.id} · {run.time}</small></button></td><td><span className="cell-stack"><strong>{run.user}</strong><small>{run.store}</small></span></td><td><code>{run.tool}</code></td><td className="numeric"><span className="token-value">{run.tokens}</span><small>{run.duration}</small></td><td><span className={`status-badge status-badge--${run.status}`}><i />{statusLabels[run.status]}</span></td></tr>)}{filteredRuns.length === 0 ? <tr><td className="empty-cell" colSpan="5">没有匹配的运行记录</td></tr> : null}</tbody></table></div><div className="panel-footer"><span>显示 {filteredRuns.length} / {runs.length} 条记录</span><button type="button" onClick={() => notify("完整运行记录暂为原型入口")}>打开运行中心 <ArrowUpRight /></button></div></article>
            <article className="panel provider-panel"><div className="panel-header"><div><h2>工具域分布</h2><p>按调用次数统计</p></div><span className="panel-meta">近 30 天</span></div><div className="distribution-stripes" aria-label="工具域调用分布">{Array.from({ length: 40 }, (_, index) => <i key={index} className={index < 15 ? "stripe stripe--green" : index < 28 ? "stripe stripe--blue" : "stripe stripe--violet"} />)}</div><div className="provider-list">{providerRows.map((row) => <div className="provider-row" key={row.label}><span className="provider-name"><i className={`resource-dot resource-dot--${row.tone}`} /><span><strong>{row.label}</strong><small>{row.detail}</small></span></span><span className="provider-count"><strong>{row.requests}</strong><small>{row.share}</small></span></div>)}</div></article>
          </section>

          <section className="dashboard-grid dashboard-grid--lower"><article className="panel activity-panel"><div className="panel-header"><div><h2>实时调用日志</h2><p>最近一次运行的事件顺序</p></div><span className="live-label"><i className="status-dot status-dot--online" /> LIVE</span></div><div className="log-summary"><div><span>模型 ID</span><strong>deepseek-flash-0731</strong></div><div><span>上下文窗口</span><strong>32,768 tokens</strong></div></div><div className="log-list" aria-label="Agent 实时调用日志">{activity.map((entry) => <div className="log-row" key={`${entry.time}-${entry.event}`}><time>{entry.time}</time><span className={`log-event log-event--${entry.tone}`}>{entry.event}</span><small>{entry.detail}</small></div>)}</div><div className="panel-footer"><span><Database /> audit_write: enabled</span><button type="button" onClick={() => notify("已复制日志摘要")}>复制摘要</button></div></article><article className="panel audit-panel"><div className="panel-header"><div><h2>最近操作</h2><p>管理员审计轨迹</p></div><button className="icon-button" type="button" aria-label="查看审计记录" title="查看审计记录" onClick={() => selectNav("audit")}><ArrowUpRight /></button></div><div className="audit-list"><div className="audit-row"><CircleCheck /><span><strong>管理员查看运行详情</strong><small>系统管理员 · 14:27:18</small></span></div><div className="audit-row"><ShieldCheck /><span><strong>权限范围校验通过</strong><small>SUPER_ADMIN · 14:26:59</small></span></div><div className="audit-row audit-row--warning"><CircleAlert /><span><strong>同步任务需要关注</strong><small>东湖生鲜店 · 14:18:36</small></span></div><div className="audit-row"><KeyRound /><span><strong>读取 Agent 运行审计</strong><small>只读操作 · 14:15:04</small></span></div></div></article></section>
          <footer className="page-footer"><span><Activity /> Master Goods Admin Preview</span><span>Visual reference: chenyme/grok2api frontend · MIT</span></footer>
        </main>
      </div>

      {selectedRun ? <aside className="run-drawer" aria-label="运行详情"><div className="drawer-header"><div><span className="section-kicker">RUN DETAIL</span><h2>运行详情</h2></div><button className="icon-button" type="button" aria-label="关闭运行详情" title="关闭运行详情" onClick={() => setSelectedRun(null)}><X /></button></div><code className="drawer-id">{selectedRun.id}</code><div className="drawer-block"><span>用户 / 门店</span><strong>{selectedRun.user} · {selectedRun.store}</strong></div><div className="drawer-block"><span>任务</span><strong>{selectedRun.task}</strong></div><div className="drawer-block"><span>模型 ID</span><strong className="mono">{selectedRun.model}</strong></div><div className="drawer-block"><span>目标工具</span><strong className="mono">{selectedRun.tool}</strong></div><div className="drawer-stats"><div><span>Token</span><strong>{selectedRun.tokens}</strong></div><div><span>耗时</span><strong>{selectedRun.duration}</strong></div></div><div className="drawer-callout"><Check /><span>工具完成事件已写入审计，正式回答已生成。</span></div><button className="button button-primary drawer-action" type="button" onClick={() => notify("已打开完整审计证据")}>查看审计证据 <ArrowUpRight /></button></aside> : null}
      {toast ? <div className="toast" role="status"><CircleCheck />{toast}</div> : null}
    </div>
  );
}
