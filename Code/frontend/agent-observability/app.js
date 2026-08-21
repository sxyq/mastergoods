const state = { data: null, selectedCase: null, filters: { batch: "", result: "all", kind: "all", search: "" } };
const $ = (selector) => document.querySelector(selector);
const escapeHtml = (value) => String(value ?? "").replace(/[&<>"']/g, (character) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[character]));
const formatNumber = (value) => value == null || value === "" || Number.isNaN(Number(value)) ? "未上报" : Number(value).toLocaleString("zh-CN");
const formatDuration = (value) => value == null || value === "" ? "未上报" : Number(value) >= 1000 ? `${(Number(value) / 1000).toFixed(1)} s` : `${formatNumber(value)} ms`;
const formatTime = (value) => value == null ? "未标记时间" : new Date(value).toLocaleString("zh-CN", { hour12: false });
const statusClass = (value) => String(value ?? "unknown").toLowerCase().replace(/\s+/g, "-");
const statusText = (value) => ({ Passed: "通过", Failed: "失败", Blocked: "阻塞", Recorded: "已记录", completed: "已完成", failed: "失败" }[value] ?? value ?? "未上报");
const prettyJson = (value) => value == null || value === "" ? "未上报" : typeof value === "string" ? value : JSON.stringify(value, null, 2);

function percentile(values, ratio) {
  const sorted = values.filter((value) => Number.isFinite(Number(value))).map(Number).sort((a, b) => a - b);
  if (!sorted.length) return null;
  const position = (sorted.length - 1) * ratio;
  const low = Math.floor(position);
  const high = Math.ceil(position);
  return low === high ? sorted[low] : Math.round(sorted[low] + (sorted[high] - sorted[low]) * (position - low));
}
function metricCard(label, value, foot, tone = "") {
  return `<article class="metric-card ${tone}"><span class="metric-label">${escapeHtml(label)}</span><strong class="metric-value">${escapeHtml(value)}</strong><span class="metric-foot">${escapeHtml(foot)}</span></article>`;
}
function activeCases() {
  return state.filters.batch === "all" ? state.data.cases : state.data.cases.filter((item) => item.batch_id === state.filters.batch);
}
function activeRun() {
  return state.data.test_runs.find((run) => run.batch_id === state.filters.batch) ?? null;
}
function activeLatency() {
  if (state.filters.batch !== "all") return state.data.latency_by_batch?.[state.filters.batch] ?? { samples: [] };
  const samples = state.data.cases.map((item) => ({ sample: item.test_id, total_ms: item.elapsed_ms ?? item.performance_summary?.duration_ms ?? null, model_ms: item.performance_summary?.model_duration_ms ?? null, tool_count: item.tool_calls?.length ?? 0, source_artifact: item.source_artifact }));
  return { samples, p50_ms: percentile(samples.map((item) => item.total_ms), 0.5), p95_ms: percentile(samples.map((item) => item.total_ms), 0.95), source_artifact: "多个测试批次的运行证据" };
}
function activeToken() {
  return state.filters.batch === "all" ? null : state.data.token_telemetry_by_batch?.[state.filters.batch] ?? null;
}
function counts() {
  const cases = activeCases();
  const run = activeRun();
  return cases.length
    ? { total: cases.length, passed: cases.filter((item) => item.result === "Passed").length, failed: cases.filter((item) => item.result === "Failed").length, blocked: cases.filter((item) => item.result === "Blocked").length, tools: cases.reduce((sum, item) => sum + (item.tool_calls?.length ?? 0), 0) }
    : { total: run?.request_count ?? 0, passed: run?.passed, failed: run?.failed, blocked: run?.blocked, tools: activeLatency().samples.reduce((sum, item) => sum + (Number(item.tool_count) || 0), 0) || null };
}
function renderMetrics() {
  const c = counts();
  const run = activeRun();
  const latency = activeLatency();
  const token = activeToken();
  const pass = c.passed == null ? "未判定" : `${c.passed} / ${c.total}`;
  const passFoot = c.passed == null ? (run?.result === "Recorded" ? "该批次只记录性能样本" : "没有可判定运行") : `${Math.round(c.passed / Math.max(c.total, 1) * 100)}% · 失败 ${c.failed ?? 0}`;
  $("#metrics-grid").innerHTML = [
    metricCard("当前批次样本", c.total, run?.label ?? "全部测试批次", "info"),
    metricCard("通过 / 总数", pass, passFoot, c.failed ? "warning" : ""),
    metricCard("工具调用", c.tools ?? "未上报", "按真实 tool_calls 计数", "info"),
    metricCard("P50 / P95", `${formatDuration(latency.p50_ms)} / ${formatDuration(latency.p95_ms)}`, "总时延；当前批次", ""),
    metricCard("Prompt / Completion", token ? `${formatNumber(token.prompt_tokens_total)} / ${formatNumber(token.completion_tokens_total)}` : "未上报 / 未上报", token?.source ?? "当前批次没有 usage 字段", token ? "info" : "warning"),
    metricCard("缓存命中", token?.cache_hit == null ? "未上报" : token.cache_hit ? "命中" : "未命中", token?.cache_status ?? "当前批次未上报 cache usage", "warning"),
  ].join("");
}
function renderBatchSelect() {
  const runs = [...state.data.test_runs].reverse();
  $("#batch-filter").innerHTML = [`<option value="all">全部批次（只合并运行记录）</option>`].concat(runs.map((run) => `<option value="${escapeHtml(run.batch_id)}">${escapeHtml(run.label)} · ${escapeHtml(statusText(run.result))}</option>`)).join("");
  $("#batch-filter").value = state.filters.batch;
  $("#batch-count-note").textContent = `${state.data.test_runs.length} 个证据批次 · 只读本地文件`;
}
function renderBatches() {
  $("#batch-overview").innerHTML = state.data.test_runs.map((run) => {
    const result = run.result ?? "Recorded";
    return `<button class="batch-card ${run.batch_id === state.filters.batch ? "selected" : ""}" type="button" data-batch-id="${escapeHtml(run.batch_id)}"><span class="batch-card-top"><span class="batch-wave">${escapeHtml(run.wave_id ?? "未分 Wave")}</span><span class="status ${statusClass(result)}">${escapeHtml(statusText(result))}</span></span><strong>${escapeHtml(run.label)}</strong><span>${escapeHtml(run.env ?? "环境未上报")}</span><small>${escapeHtml(run.request_count ?? 0)} 个样本 · 通过 ${escapeHtml(run.passed ?? "未判定")} · 失败 ${escapeHtml(run.failed ?? "未判定")}</small></button>`;
  }).join("") || `<div class="empty-state">没有批次证据</div>`;
  document.querySelectorAll("[data-batch-id]").forEach((button) => button.addEventListener("click", () => {
    state.filters.batch = button.dataset.batchId;
    state.selectedCase = activeCases()[0] ?? null;
    $("#batch-filter").value = state.filters.batch;
    renderAll();
  }));
}
function renderSmoke() {
  const smoke = state.data.production_smoke;
  if (!smoke) {
    $("#production-smoke").innerHTML = `<div class="panel-heading"><div><p class="eyebrow">Production smoke</p><h3>未找到生产 smoke 证据</h3></div></div>`;
    return;
  }
  const tools = (smoke.tool_calls ?? []).map((tool) => `${tool.tool_name} / ${statusText(tool.status)} / ${formatDuration(tool.duration_ms)}`).join(" · ") || "未观察到工具调用";
  $("#production-smoke").innerHTML = `<div class="panel-heading"><div><p class="eyebrow">Historical production smoke</p><h3>生产容器单次复测</h3></div><span class="panel-note">${escapeHtml(smoke.source_artifact)}</span></div><div class="smoke-grid"><div class="smoke-status ${smoke.result === "Passed" ? "ok" : ""}"><span>链路结论</span><strong>${escapeHtml(statusText(smoke.result))}</strong><span>工具查询：${escapeHtml(statusText(smoke.tool_query_result))}<br />临时会话清理：HTTP ${escapeHtml(smoke.cleanup_http ?? "未上报")}</span></div><div class="smoke-details"><div class="smoke-detail"><span>模型</span><strong>${escapeHtml(smoke.model)}</strong></div><div class="smoke-detail"><span>LLM 状态</span><strong>${escapeHtml(statusText(smoke.llm_status))}</strong></div><div class="smoke-detail"><span>运行模式</span><strong>${escapeHtml(smoke.mode)}</strong></div><div class="smoke-detail"><span>计划来源</span><strong>${escapeHtml(smoke.plan_source)}</strong></div><div class="smoke-detail"><span>真实工具</span><strong>${escapeHtml(tools)}</strong></div><p class="smoke-answer">${escapeHtml(smoke.answer ?? "未返回正式回答")}</p></div></div>`;
}
function renderProvenance() {
  const run = activeRun();
  const entries = [...state.data.provenance];
  if (run) entries.unshift({ label: "当前批次来源", value: `${run.label} · ${run.source_artifact}`, level: "verified" });
  $("#provenance-list").innerHTML = entries.map((item) => `<div class="provenance-item ${item.level === "missing" || item.level === "warning" ? "warning" : ""}"><strong>${escapeHtml(item.label)}</strong><span>${escapeHtml(item.value)}</span></div>`).join("");
}
function linePath(values, width, height, padding, max) {
  const step = values.length > 1 ? (width - padding.left - padding.right) / (values.length - 1) : width / 2;
  return values.map((value, index) => {
    const x = padding.left + index * step;
    const y = height - padding.bottom - ((Number(value) || 0) / (max || 1)) * (height - padding.top - padding.bottom);
    return `${index ? "L" : "M"}${x.toFixed(1)} ${y.toFixed(1)}`;
  }).join(" ");
}
function renderLatency() {
  const latency = activeLatency();
  const samples = latency.samples ?? [];
  $("#latency-note").textContent = `${samples.length} 个当前批次样本`;
  $("#latency-caption").textContent = `单位：ms · ${latency.source_artifact ?? "当前批次原始证据"}`;
  $("#model-latency-legend").hidden = !samples.some((item) => Number.isFinite(Number(item.model_ms)));
  if (!samples.length) {
    $("#latency-chart").innerHTML = `<div class="empty-state">当前批次没有时延样本</div>`;
    return;
  }
  const width = 900;
  const height = 285;
  const padding = { top: 16, right: 18, bottom: 33, left: 45 };
  const total = samples.map((item) => Number(item.total_ms) || 0);
  const model = samples.map((item) => Number(item.model_ms) || 0);
  const max = Math.max(...total, 1);
  const grid = [0, 0.25, 0.5, 0.75, 1].map((ratio) => {
    const y = height - padding.bottom - ratio * (height - padding.top - padding.bottom);
    return `<line x1="${padding.left}" x2="${width - padding.right}" y1="${y}" y2="${y}" stroke="#d8e0e5" stroke-dasharray="2 5"/><text x="4" y="${y + 4}" fill="#9aa6af" font-size="10">${Math.round(max * ratio / 1000)}s</text>`;
  }).join("");
  const dots = total.map((value, index) => {
    const x = padding.left + index * (width - padding.left - padding.right) / Math.max(total.length - 1, 1);
    const y = height - padding.bottom - value / max * (height - padding.top - padding.bottom);
    return `<circle cx="${x}" cy="${y}" r="3" fill="#2164d5"><title>${escapeHtml(samples[index].sample ?? index + 1)}: ${escapeHtml(formatDuration(value))}</title></circle>`;
  }).join("");
  const modelPath = samples.some((item) => Number.isFinite(Number(item.model_ms))) ? `<path d="${linePath(model, width, height, padding, max)}" fill="none" stroke="#dd6654" stroke-width="2" stroke-dasharray="5 4"/>` : "";
  $("#latency-chart").innerHTML = `<svg viewBox="0 0 ${width} ${height}" role="img" aria-label="当前批次运行总时延"><g>${grid}</g><path d="${linePath(total, width, height, padding, max)}" fill="none" stroke="#2164d5" stroke-width="2.5"/>${modelPath}${dots}</svg>`;
}
function renderTelemetry() {
  const token = activeToken();
  const rows = [
    ["首字延迟", token?.first_token_latency_ms == null ? "未上报" : formatDuration(token.first_token_latency_ms), token?.first_token_latency_ms == null ? "当前批次没有首字延迟" : "SSE audit 首个 model_stream delta", token?.first_token_latency_ms == null],
    ["Token/s", token?.estimated_output_tokens_per_second_average == null ? "未上报" : `${token.estimated_output_tokens_per_second_average} tok/s`, token?.speed_status ?? "当前批次没有 token usage", token?.estimated_output_tokens_per_second_average == null],
    ["缓存命中", token?.cache_hit == null ? "未上报" : token.cache_hit ? "命中" : "未命中", token?.cache_status ?? "当前批次没有 cache usage", token?.cache_hit == null],
  ];
  $("#token-summary").innerHTML = rows.map(([label, value, note, missing]) => `<div class="telemetry-row ${missing ? "missing" : ""}"><div><span>${escapeHtml(label)}</span><small>${escapeHtml(note)}</small></div><strong>${escapeHtml(value)}</strong></div>`).join("");
  const prompt = token?.prompt_tokens_total;
  const completion = token?.completion_tokens_total;
  if (prompt == null && completion == null) {
    $("#token-bars").innerHTML = `<div class="empty-state">当前批次未上报 Prompt / Completion Token。</div>`;
    return;
  }
  const max = Math.max(Number(prompt) || 0, Number(completion) || 0, 1);
  $("#token-bars").innerHTML = [["Prompt", prompt, ""], ["Completion", completion, "completion"]].map(([label, value, klass]) => `<div><div class="token-bar-label"><span>${label}</span><strong>${formatNumber(value)}</strong></div><div class="token-bar-track"><div class="token-bar-fill ${klass}" style="width:${Math.max(4, (Number(value) || 0) / max * 100)}%"></div></div></div>`).join("");
}
function renderTools() {
  const counts = {};
  activeCases().forEach((item) => (item.tool_calls ?? []).forEach((tool) => { if (tool.tool_name) counts[tool.tool_name] = (counts[tool.tool_name] ?? 0) + 1; }));
  const rows = Object.entries(counts).sort((a, b) => b[1] - a[1]).slice(0, 24);
  const max = Math.max(...rows.map((item) => item[1]), 1);
  $("#tool-count-note").textContent = `${Object.keys(counts).length} 个工具出现过调用`;
  $("#tool-chart").innerHTML = rows.map(([tool, count]) => `<div class="tool-row"><span class="tool-name" title="${escapeHtml(tool)}">${escapeHtml(tool)}</span><span class="tool-track"><i class="tool-fill" style="width:${count / max * 100}%"></i></span><span class="tool-count">${count}</span></div>`).join("") || `<div class="empty-state">当前批次没有可展开的工具调用</div>`;
}
function renderDatabase() {
  const run = activeRun();
  const database = state.filters.batch === "all" ? null : run?.database ?? state.data.database_by_batch?.[state.filters.batch] ?? activeCases()[0]?.database ?? null;
  $("#database-note").textContent = database ? "来源于批次证据中的 pre / post 计数" : "当前批次未提供数据库计数";
  if (!database?.pre && !database?.post) {
    $("#database-state").innerHTML = `<div class="empty-state">当前批次未提供数据库前后计数</div>`;
    return;
  }
  const keys = [...new Set([...Object.keys(database.pre ?? {}), ...Object.keys(database.post ?? {})])];
  const rows = keys.map((key) => {
    const before = database.pre?.[key];
    const after = database.post?.[key];
    const delta = before != null && after != null ? Number(after) - Number(before) : null;
    return `<div class="database-row ${delta !== 0 && delta != null ? "changed" : ""}"><span>${escapeHtml(key)}</span><strong>${escapeHtml(before ?? "未上报")}</strong><strong>${escapeHtml(after ?? "未上报")}</strong><em>${delta == null ? "未上报" : delta > 0 ? `+${delta}` : delta}</em></div>`;
  }).join("");
  const cleanup = database.conversation_delete_status ?? database.cleanup_http ?? activeCases()[0]?.cleanup?.conversation_delete_status ?? null;
  $("#database-state").innerHTML = `<div class="database-header"><span>表</span><span>测试前</span><span>测试后</span><span>变化</span></div>${rows}<div class="database-foot"><span>业务表是否未变</span><strong>${database.business_tables_unchanged === true ? "是" : database.business_tables_unchanged === false ? "否" : "未上报"}</strong><span>临时会话清理 HTTP</span><strong>${escapeHtml(cleanup ?? "未上报")}</strong></div>`;
}
function renderStream() {
  const trace = state.data.stream_trace;
  const shown = trace && (state.filters.batch === "all" || state.filters.batch === trace.batch_id);
  $("#stream-panel").hidden = !shown;
  if (!shown) return;
  $("#stream-summary").innerHTML = `<div class="stream-meta"><span>运行 ID <strong class="mono">${escapeHtml(trace.run_id)}</strong></span><span>LLM 状态 <strong>${escapeHtml(statusText(trace.llm_status))}</strong></span><span>事件数 <strong>${escapeHtml(trace.event_count)}</strong></span><span>首个模型 delta <strong>${escapeHtml(formatDuration(trace.first_model_stream_delta_latency_ms))}</strong></span></div><div class="stream-events">${trace.events.map((event) => `<div class="stream-event"><span>${escapeHtml(event.seq ?? "-")}</span><strong>${escapeHtml(event.event_type)}</strong><small>${escapeHtml(event.tool_name ?? event.delta_source ?? "")}</small><p>${escapeHtml(event.result_summary ?? event.content ?? "")}</p></div>`).join("")}</div><p class="artifact-line">证据：${escapeHtml(trace.source_artifact)}</p>`;
}
function filteredCases() {
  const query = state.filters.search.trim().toLowerCase();
  return activeCases().filter((item) => {
    if (state.filters.result !== "all" && item.result !== state.filters.result) return false;
    if (state.filters.kind !== "all" && item.kind !== state.filters.kind) return false;
    const haystack = [item.test_id, item.tool, ...(item.tool_names ?? []), item.prompt, item.run_id, item.answer].filter(Boolean).join(" ").toLowerCase();
    return !query || haystack.includes(query);
  });
}
function renderTraceTable() {
  const cases = filteredCases();
  $("#visible-count").textContent = `${cases.length} / ${activeCases().length} 条当前批次运行记录`;
  $("#trace-table").innerHTML = cases.map((item) => {
    const prompt = item.prompt ?? "未上报 Prompt";
    const tools = item.tool_calls?.length ? item.tool_calls.map((tool) => tool.tool_name).join(" → ") : "无工具结果";
    return `<tr class="trace-row ${state.selectedCase?.test_id === item.test_id ? "selected" : ""}" data-test-id="${escapeHtml(item.test_id)}"><td class="mono">${escapeHtml(item.test_id)}</td><td class="prompt-cell" title="${escapeHtml(prompt)}">${escapeHtml(prompt)}</td><td class="tool-cell" title="${escapeHtml(tools)}">${escapeHtml(tools)}</td><td><span class="status ${statusClass(item.result)}">${escapeHtml(statusText(item.result))}</span></td><td>${escapeHtml(item.http_status ?? "未上报")}</td><td>${escapeHtml(formatDuration(item.elapsed_ms ?? item.performance_summary?.duration_ms))}</td></tr>`;
  }).join("") || `<tr><td colspan="6" class="empty-state">没有匹配的运行记录；性能和 SSE 证据见对应批次和日志。</td></tr>`;
  document.querySelectorAll(".trace-row").forEach((row) => row.addEventListener("click", () => {
    state.selectedCase = activeCases().find((item) => item.test_id === row.dataset.testId) ?? null;
    renderTraceTable();
    renderDetail();
  }));
}
function detailMeta(label, value, mono = false) {
  return `<div><span>${escapeHtml(label)}</span><strong class="${mono ? "mono" : ""}">${escapeHtml(value ?? "未上报")}</strong></div>`;
}
function renderDetail() {
  const visible = filteredCases();
  const item = visible.includes(state.selectedCase) ? state.selectedCase : visible[0] ?? null;
  if (!item) {
    $("#trace-detail").innerHTML = `<div class="empty-state">当前批次没有普通运行记录。性能和 SSE 证据见上方批次、图表和日志。</div>`;
    return;
  }
  state.selectedCase = item;
  const tools = item.tool_calls ?? [];
  const timeline = tools.length ? tools.map((tool, index) => `<article class="tool-trace-card"><div class="tool-trace-heading"><span class="tool-order">${escapeHtml(tool.sequence ?? index + 1)}</span><div><strong>${escapeHtml(tool.tool_name ?? "未命名工具")}</strong><small class="mono">${escapeHtml(tool.tool_call_id ?? "未上报调用 ID")}</small></div><span class="status ${statusClass(tool.status)}">${escapeHtml(statusText(tool.status))}</span></div><div class="tool-trace-grid"><div><span>调用参数</span><pre>${escapeHtml(prettyJson(tool.tool_input ?? tool.query_window))}</pre></div><div><span>返回摘要</span><p>${escapeHtml(tool.result_summary ?? tool.error_message ?? "未上报")}</p><small>${escapeHtml(formatDuration(tool.duration_ms))} · 返回 ${escapeHtml(tool.returned_count ?? "未上报")} 条${tool.is_truncated ? " · 已截断" : ""}</small></div></div></article>`).join("") : `<div class="empty-state">未产生可记录的工具调用。HTTP ${escapeHtml(item.http_status ?? "未上报")} · ${escapeHtml(item.reasons?.join("；") ?? "没有更多失败原因")}</div>`;
  const refs = item.evidence_refs?.length ? `<div class="evidence-ref-list">${item.evidence_refs.map((ref) => `<div><strong>${escapeHtml(ref.label ?? ref.tool_name)}</strong><span>${escapeHtml(ref.value ?? "未上报")}</span></div>`).join("")}</div>` : "<p>未上报结构化 evidence_refs</p>";
  const artifacts = (item.artifacts?.length ? item.artifacts : [item.source_artifact]).filter(Boolean).join("\\n");
  $("#trace-detail").innerHTML = `<h4>${escapeHtml(item.prompt ?? item.tool ?? "运行详情")}</h4><div class="detail-meta">${detailMeta("批次", item.batch_label)}${detailMeta("Wave", item.wave_id)}${detailMeta("测试 ID", item.test_id, true)}${detailMeta("运行 ID", item.run_id, true)}${detailMeta("结果", statusText(item.result))}${detailMeta("HTTP", item.http_status)}${detailMeta("LLM 状态", statusText(item.llm_status))}${detailMeta("工具数量", tools.length)}${detailMeta("总时延", formatDuration(item.elapsed_ms ?? item.performance_summary?.duration_ms))}</div><div class="detail-block"><h5>原始用户 Prompt</h5><p>${escapeHtml(item.prompt ?? "未上报")}</p></div><div class="detail-block"><h5>模型计划与正式回答</h5><p class="detail-subtitle">${escapeHtml(item.plan_summary ?? item.plan_source ?? "计划来源未上报")}</p><p>${escapeHtml(item.answer ?? "未返回正式回答")}</p></div><div class="detail-block tool-timeline"><h5>同一回答中的工具链 · 按执行顺序</h5>${timeline}</div><div class="detail-block"><h5>结构化数据证据</h5>${refs}</div><div class="detail-block"><h5>失败原因 / 审计标识</h5><p>${escapeHtml(item.reasons?.join("；") ?? "无失败原因")}</p><pre>${escapeHtml(item.observability ? prettyJson(item.observability) : "未上报")}</pre></div><div class="detail-block"><h5>证据文件路径</h5><pre>${escapeHtml(artifacts)}</pre></div>`;
}
function renderLogs() {
  const batch = state.filters.batch;
  const rows = state.data.logs.filter((item) => batch === "all" || item.batch_id === batch);
  $("#log-count").textContent = `${rows.length} 条事件 · ${batch === "all" ? "全部批次" : "当前批次"}`;
  $("#log-stream").innerHTML = rows.map((item) => `<div class="log-line"><time>${escapeHtml(formatTime(item.time))}</time><span class="log-kind">${escapeHtml(item.event_type ?? item.kind)}</span><span class="log-content">${escapeHtml(item.content)}</span><small>${escapeHtml(item.test_id ?? item.source_artifact ?? "")}</small></div>`).join("") || `<div class="empty-state">当前批次没有日志事件</div>`;
}
function renderAll() {
  const run = activeRun();
  $("#capture-time").textContent = `证据生成 ${new Date(state.data.generated_at).toLocaleString("zh-CN")}`;
  $("#source-model").textContent = `${run?.model ?? state.data.scope.model} · ${run?.batch_id ?? "全部批次"}`;
  $("#source-host").textContent = run?.env ?? `${state.data.scope.host} / store ${state.data.scope.store_id}`;
  $("#source-batch").textContent = run?.wave_id ?? "跨批次查看";
  renderMetrics();
  renderBatches();
  renderSmoke();
  renderLatency();
  renderTelemetry();
  renderTools();
  renderProvenance();
  renderDatabase();
  renderStream();
  renderTraceTable();
  renderDetail();
  renderLogs();
}
function bindFilters() {
  $("#batch-filter").addEventListener("change", (event) => { state.filters.batch = event.target.value; state.selectedCase = activeCases()[0] ?? null; renderAll(); });
  $("#result-filter").addEventListener("change", (event) => { state.filters.result = event.target.value; renderTraceTable(); renderDetail(); });
  $("#kind-filter").addEventListener("change", (event) => { state.filters.kind = event.target.value; renderTraceTable(); renderDetail(); });
  $("#case-search").addEventListener("input", (event) => { state.filters.search = event.target.value; renderTraceTable(); renderDetail(); });
  $("#reset-filters").addEventListener("click", () => { state.filters.result = "all"; state.filters.kind = "all"; state.filters.search = ""; $("#result-filter").value = "all"; $("#kind-filter").value = "all"; $("#case-search").value = ""; renderTraceTable(); renderDetail(); });
}
async function init() {
  try {
    const response = await fetch("./public/data/agent-evaluation.json", { cache: "no-store" });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    state.data = await response.json();
    const latest = [...state.data.test_runs].reverse().find((run) => run.batch_id === "AG-PT-W3-20260803-SOAK-15MIN-RECHECK") ?? [...state.data.test_runs].reverse()[0];
    state.filters.batch = latest?.batch_id ?? "all";
    state.selectedCase = activeCases()[0] ?? null;
    renderBatchSelect();
    bindFilters();
    renderAll();
  } catch (error) {
    document.querySelector("main").innerHTML = `<section class="panel"><h2>证据数据读取失败</h2><p>${escapeHtml(error.message)}</p><p>请先在此目录执行 <code>npm run build-data</code>。</p></section>`;
  }
}
init();
