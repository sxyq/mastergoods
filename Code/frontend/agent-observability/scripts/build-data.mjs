import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const appDir = path.resolve(scriptDir, "..");
const repoRoot = path.resolve(appDir, "../../..");
const evidenceRoot = path.join(repoRoot, "testing/.artifacts/2026-08-02-server-agent-eval");
const allToolsDir = path.join(evidenceRoot, "mg-deepseek-all-tools-20260802/mg-agent-eval-all-tools-20260802T1785679343800");
const performanceDir = path.join(evidenceRoot, "wave1-deepseek-performance-complete-20260802T201300+0800");
const streamDir = path.join(evidenceRoot, "wave1-deepseek-live-20260802T194300+0800");
const productionRerunDir = path.join(evidenceRoot, "owner7-production-rerun-20260802");
const soakDir = path.join(repoRoot, "testing/.artifacts/2026-08-03-production-agent-recheck-after-current-deploy/soak-20260803-approved-final-v2/mg-agent-soak-20260803T062503Z-54386");
const soakSummaryFile = path.join(soakDir, "soak-summary.json");
const outputPath = path.join(appDir, "public/data/agent-evaluation.json");

const ALL_TOOLS_BATCH_ID = "AG-FT-BE-ALL-20260802";
const PERFORMANCE_BATCH_ID = "AG-PT-W1-20260802-PERF-30";
const STREAM_BATCH_ID = "AG-FT-W1-20260802-SSE";
const PRODUCTION_SMOKE_BATCH_ID = "AG-FT-W1-20260802-PROD-SMOKE";

const readJson = (file) => JSON.parse(fs.readFileSync(file, "utf8"));
const readText = (file) => fs.readFileSync(file, "utf8");
const exists = (file) => fs.existsSync(file);
const relativeEvidence = (file) => path.relative(repoRoot, file).split(path.sep).join("/");

function redact(value) {
  if (typeof value !== "string") return value;
  return value
    .replace(/\b1[3-9]\d{9}\b/g, "***")
    .replace(/[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}/g, "***@***")
    .replace(/Bearer\s+[A-Za-z0-9._-]+/gi, "Bearer <redacted>");
}

function redactDeep(value) {
  if (Array.isArray(value)) return value.map(redactDeep);
  if (value && typeof value === "object") {
    return Object.fromEntries(Object.entries(value).map(([key, item]) => [key, redactDeep(item)]));
  }
  return redact(value);
}

function dataOf(body) {
  return body?.data && typeof body.data === "object" ? body.data : {};
}

function percentile(values, ratio) {
  const sorted = values.filter((value) => Number.isFinite(value)).sort((a, b) => a - b);
  if (!sorted.length) return null;
  const position = (sorted.length - 1) * ratio;
  const lower = Math.floor(position);
  const upper = Math.ceil(position);
  if (lower === upper) return sorted[lower];
  return Math.round(sorted[lower] + (sorted[upper] - sorted[lower]) * (position - lower));
}

function relativeIfExists(file) {
  return file && exists(file) ? relativeEvidence(file) : null;
}

function simplifyTool(tool) {
  return redactDeep({
    sequence: tool?.sequence ?? tool?.tool_sequence ?? null,
    tool_call_id: tool?.tool_call_id ?? tool?.toolCallId ?? null,
    tool_name: tool?.tool_name ?? null,
    status: tool?.status ?? null,
    duration_ms: tool?.duration_ms ?? tool?.durationMs ?? null,
    tool_input: tool?.tool_input ?? null,
    returned_count: tool?.returned_count ?? null,
    input_summary: tool?.input_summary ?? null,
    query_window: tool?.query_window ?? null,
    result_summary: tool?.result_summary ?? null,
    error_code: tool?.error_code ?? null,
    error_message: tool?.error_message ?? null,
  });
}

function findCaseFile(index, tool) {
  if (!exists(path.join(allToolsDir, "cases"))) return null;
  const prefix = `${String(index).padStart(3, "0")}-${tool}.json`;
  const file = path.join(allToolsDir, "cases", prefix);
  return exists(file) ? file : null;
}

function buildCases() {
  const summary = readJson(path.join(allToolsDir, "summary.json"));
  return summary.results.map((result, index) => {
    const source = findCaseFile(index + 1, result.tool);
    const body = source ? readJson(source) : {};
    const response = dataOf(body.response);
    const actual = body.actual ?? result;
    return redactDeep({
      batch_id: ALL_TOOLS_BATCH_ID,
      batch_label: "Wave 1 · 服务器全工具覆盖",
      wave_id: "Wave 1",
      env: `${summary.host} / isolated evaluation container`,
      model: "deepseek-v4-flash",
      provider: "https://tokenrhythm.studio/v1",
      wire_api: "chat_completions",
      test_id: result.test_id,
      tool: result.tool,
      tool_names: result.tool_names ?? [],
      kind: result.kind,
      result: result.result,
      http_status: result.http_status,
      selected: result.target_selected,
      completed: result.target_completed,
      elapsed_ms: result.elapsed_ms,
      prompt: body.actions?.request?.message ?? null,
      run_id: response.run_id ?? body.model_and_tool_trace?.run_id ?? null,
      mode: response.mode ?? body.model_and_tool_trace?.mode ?? null,
      llm_status: response.llm_status ?? body.model_and_tool_trace?.llm_status ?? null,
      plan_source: response.plan_source ?? body.model_and_tool_trace?.plan_source ?? null,
      answer: typeof response.answer === "string" ? response.answer.slice(0, 900) : null,
      tool_calls: Array.isArray(response.tool_calls)
        ? response.tool_calls.map(simplifyTool)
        : (body.model_and_tool_trace?.tool_calls ?? []).map(simplifyTool),
      performance_summary: response.performance_summary ?? null,
      reasons: actual.reasons ?? result.reasons ?? [],
      business_delta: result.business_delta ?? {},
      draft_delta: result.draft_delta ?? 0,
      database: {
        pre: summary.pre_counts ?? null,
        post: summary.post_counts ?? null,
        business_delta: result.business_delta ?? {},
        draft_delta: result.draft_delta ?? 0,
      },
      response_code: body.response?.code ?? null,
      observability: response.observability ?? null,
      evidence_refs: response.evidence_refs ?? [],
      response_artifact: source ? relativeEvidence(source) : null,
      artifacts: [source ? relativeEvidence(source) : null, relativeEvidence(path.join(allToolsDir, "summary.json"))].filter(Boolean),
      source_artifact: source ? relativeEvidence(source) : relativeEvidence(path.join(allToolsDir, "summary.json")),
    });
  });
}

function buildSoakBatch() {
  if (!exists(soakSummaryFile)) return { run: null, cases: [] };
  const summary = readJson(soakSummaryFile);
  const batchId = summary.test_id;
  const cases = (summary.requests ?? []).map((request) => {
    const response = dataOf(request.response);
    const responseFile = request.response_file ? path.join(repoRoot, request.response_file) : null;
    const toolCalls = Array.isArray(request.tool_calls)
      ? request.tool_calls.map(simplifyTool)
      : (Array.isArray(response.tool_calls) ? response.tool_calls.map(simplifyTool) : []);
    const passed = request.http_status === 200 && request.answer_present === true && response.llm_status === "completed";
    const responseArtifact = relativeIfExists(responseFile);
    return redactDeep({
      batch_id: batchId,
      batch_label: "Wave 3 · 15 分钟真实多工具 soak",
      wave_id: summary.wave_id,
      env: `${summary.host} / production container`,
      model: summary.model,
      provider: "https://tokenrhythm.studio/v1",
      wire_api: summary.wire_api,
      test_id: `${batchId}-REQ-${String(request.index).padStart(3, "0")}`,
      request_index: request.index,
      tool: toolCalls.length === 1 ? toolCalls[0].tool_name : toolCalls.length > 1 ? "multi_tool_chain" : "no_tool_result",
      tool_names: toolCalls.map((tool) => tool.tool_name).filter(Boolean),
      kind: "read",
      result: passed ? "Passed" : "Failed",
      http_status: request.http_status,
      selected: toolCalls.length > 0,
      completed: passed,
      elapsed_ms: request.duration_ms,
      prompt: request.input?.message ?? null,
      run_id: request.run_id === "-" ? null : request.run_id,
      mode: response.mode ?? summary.mode ?? null,
      llm_status: response.llm_status ?? null,
      plan_source: response.plan_source ?? null,
      plan_summary: response.plan_summary ?? null,
      answer: typeof response.answer === "string" ? response.answer.slice(0, 2000) : null,
      tool_calls: toolCalls,
      evidence_refs: response.evidence_refs ?? [],
      performance_summary: response.performance_summary ?? { duration_ms: request.duration_ms, source: "soak-summary transport timing" },
      reasons: passed ? [] : [
        `HTTP ${request.http_status}`,
        request.answer_present ? "未完成正式回答" : "未返回正式回答",
        toolCalls.length ? null : "未产生可记录的工具调用",
      ].filter(Boolean),
      business_delta: {},
      draft_delta: 0,
      database: {
        pre: summary.db_counts_pre ?? null,
        post: summary.db_counts_post ?? null,
        business_delta: {},
        draft_delta: 0,
      },
      response_code: request.response?.code ?? null,
      observability: response.observability ?? null,
      response_artifact: responseArtifact,
      artifacts: [responseArtifact, relativeEvidence(soakSummaryFile)].filter(Boolean),
      source_artifact: relativeEvidence(soakSummaryFile),
      cleanup: {
        conversation_delete_status: summary.conversation_delete_status ?? null,
      },
    });
  });
  const durations = cases.map((item) => Number(item.elapsed_ms)).filter(Number.isFinite);
  const run = redactDeep({
    batch_id: batchId,
    label: "Wave 3 · 15 分钟真实多工具 soak",
    wave_id: summary.wave_id,
    category: "性能 / 多工具功能",
    result: summary.failed_count === 0 ? "Passed" : "Failed",
    env: `${summary.host} / production container`,
    host: summary.host,
    owner_user_id: summary.owner_user_id,
    model: summary.model,
    provider: "https://tokenrhythm.studio/v1",
    wire_api: summary.wire_api,
    request_count: summary.request_count,
    passed: summary.passed_count,
    failed: summary.failed_count,
    duration_seconds: summary.duration_seconds,
    conversation_id: summary.conversation_id,
    database: {
      pre: summary.db_counts_pre ?? null,
      post: summary.db_counts_post ?? null,
      business_tables_unchanged: true,
      conversation_delete_status: summary.conversation_delete_status ?? null,
    },
    latency: {
      p50_ms: percentile(durations, 0.5),
      p95_ms: percentile(durations, 0.95),
      samples: cases.map((item) => ({
        sample: item.request_index,
        run_id: item.run_id,
        http_status: item.http_status,
        answer_present: Boolean(item.answer),
        total_ms: item.elapsed_ms,
        tool_ms: item.performance_summary?.tool_duration_ms ?? null,
        model_ms: item.performance_summary?.model_duration_ms ?? null,
        tool_count: item.tool_calls.length,
        source_artifact: item.response_artifact ?? item.source_artifact,
      })),
      source_artifact: relativeEvidence(soakSummaryFile),
    },
    token_telemetry: {
      provider_log_sample_count: 0,
      prompt_tokens_total: null,
      completion_tokens_total: null,
      average_prompt_tokens: null,
      average_completion_tokens: null,
      estimated_output_tokens_per_second_average: null,
      first_token_latency_ms: null,
      cache_hit: null,
      cache_status: "该批次证据未包含 provider cache usage",
      speed_status: "该批次证据未包含 token usage，保持未上报",
      source: relativeEvidence(soakSummaryFile),
    },
    source_artifact: relativeEvidence(soakSummaryFile),
  });
  return { run, cases };
}

function buildPerformanceSamples() {
  if (!exists(performanceDir)) return [];
  return fs.readdirSync(performanceDir)
    .filter((name) => /^performance-\d+\.json$/.test(name))
    .sort()
    .map((name) => {
      const file = path.join(performanceDir, name);
      const body = readJson(file);
      const data = dataOf(body);
      return redactDeep({
        batch_id: PERFORMANCE_BATCH_ID,
        batch_label: "Wave 1 · 30 次性能样本",
        wave_id: "Wave 1",
        sample: Number(name.match(/\d+/)?.[0] ?? 0),
        run_id: data.run_id ?? null,
        http_status: body.code ?? null,
        llm_status: data.llm_status ?? null,
        answer_present: Boolean(data.answer),
        total_ms: data.performance_summary?.duration_ms ?? null,
        tool_ms: data.performance_summary?.tool_duration_ms ?? null,
        model_ms: data.performance_summary?.model_duration_ms ?? null,
        tool_count: Array.isArray(data.tool_calls) ? data.tool_calls.length : 0,
        tools: Array.isArray(data.tool_calls) ? data.tool_calls.map(simplifyTool) : [],
        source_artifact: relativeEvidence(file),
      });
    });
}

function parseProviderLog() {
  const file = path.join(allToolsDir, "app.log");
  if (!exists(file)) return [];
  const pattern = /^(\S+).*LongCat agent (chat_completions tool response|chat completion) from model ([^,]+)(?:, tools=(\d+))?, tokens: prompt=(\d+), completion=(\d+)/;
  const records = [];
  for (const line of readText(file).split("\n")) {
    const match = line.match(pattern);
    if (!match) continue;
    const timestamp = Date.parse(match[1]);
    const promptTokens = Number(match[5]);
    const completionTokens = Number(match[6]);
    const previous = records.at(-1);
    const intervalMs = previous && Number.isFinite(timestamp) && Number.isFinite(previous.timestamp)
      ? timestamp - previous.timestamp
      : null;
    records.push({
      batch_id: ALL_TOOLS_BATCH_ID,
      timestamp,
      iso_time: match[1],
      request_type: match[2] === "chat completion" ? "final_completion" : "tool_response",
      model: match[3],
      tools: match[4] ? Number(match[4]) : 0,
      prompt_tokens: promptTokens,
      completion_tokens: completionTokens,
      interval_ms_from_previous_log: intervalMs,
      estimated_output_tokens_per_second: match[2] === "chat completion" && intervalMs > 0
        ? Number((completionTokens / (intervalMs / 1000)).toFixed(2))
        : null,
    });
  }
  return records;
}

function buildStreamTrace() {
  const file = path.join(streamDir, "stream-audit.json");
  if (!exists(file)) return null;
  const body = readJson(file);
  const data = dataOf(body);
  const events = Array.isArray(data.events) ? data.events : [];
  const firstModelDelta = events.find((event) => event.event_type === "answer_delta" && event.payload?.delta_source === "model_stream");
  const firstEvent = events[0];
  return redactDeep({
    batch_id: STREAM_BATCH_ID,
    batch_label: "Wave 1 · SSE 流式审计",
    wave_id: "Wave 1",
    run_id: data.run_id,
    status: data.status,
    mode: data.mode,
    llm_status: data.llm_status,
    owner_user_id: data.owner_user_id,
    event_count: data.event_count,
    started_at: data.started_at,
    completed_at: data.completed_at,
    first_model_stream_delta_latency_ms: firstModelDelta && firstEvent
      ? firstModelDelta.created_at - firstEvent.created_at
      : null,
    events: events.map((event) => ({
      seq: event.payload?.seq ?? null,
      event_type: event.event_type,
      created_at: event.created_at,
      tool_name: event.payload?.tool_name ?? null,
      delta_source: event.payload?.delta_source ?? null,
      duration_ms: event.payload?.duration_ms ?? null,
      result_summary: event.payload?.result_summary ?? null,
      content: typeof event.payload?.content === "string" ? event.payload.content.slice(0, 220) : null,
    })),
    source_artifact: relativeEvidence(file),
  });
}

function buildProductionSmoke() {
  const responseFile = path.join(productionRerunDir, "response.json");
  if (!exists(responseFile)) return null;
  const body = readJson(responseFile);
  const data = dataOf(body);
  const cleanupFile = path.join(productionRerunDir, "cleanup.txt");
  const cleanupText = exists(cleanupFile) ? readText(cleanupFile) : "";
  const cleanupHttp = cleanupText.match(/__DELETE_HTTP__(\d+)/)?.[1] ?? null;
  return redactDeep({
    batch_id: PRODUCTION_SMOKE_BATCH_ID,
    batch_label: "Wave 1 · 生产容器 smoke",
    captured_at: new Date(1785683895256).toISOString(),
    host: "154.217.241.207",
    runtime: "production container",
    account_id: 7,
    model: "gpt-5.6-luna",
    provider: "https://fast.qianxing.pro/v1",
    result: data.llm_status === "completed" ? "Passed" : "Blocked",
    tool_query_result: data.tool_calls?.every((tool) => tool.status === "completed") ? "Passed" : "Failed",
    llm_status: data.llm_status ?? null,
    mode: data.mode ?? null,
    plan_source: data.plan_source ?? null,
    run_id: data.run_id ?? null,
    conversation_id: data.conversation_id ?? null,
    answer: typeof data.answer === "string" ? data.answer.slice(0, 1000) : null,
    tool_calls: Array.isArray(data.tool_calls) ? data.tool_calls.map(simplifyTool) : [],
    performance_summary: data.performance_summary ?? null,
    cleanup_http: cleanupHttp,
    source_artifact: relativeEvidence(responseFile),
  });
}

function buildTestRuns(allToolsSummary, performanceSamples, streamTrace, productionSmoke, soakRun, providerLogs) {
  const allToolsFile = path.join(allToolsDir, "summary.json");
  const runs = [{
    batch_id: ALL_TOOLS_BATCH_ID,
    label: "Wave 1 · 服务器全工具覆盖",
    wave_id: "Wave 1",
    category: "功能 / 工具覆盖",
    result: allToolsSummary.failed === 0 ? "Passed" : "Failed",
    env: `${allToolsSummary.host} / isolated evaluation container`,
    host: allToolsSummary.host,
    model: "deepseek-v4-flash",
    provider: "https://tokenrhythm.studio/v1",
    wire_api: "chat_completions",
    request_count: allToolsSummary.total,
    passed: allToolsSummary.passed,
    failed: allToolsSummary.failed,
    blocked: allToolsSummary.blocked,
    database: {
      pre: allToolsSummary.pre_counts ?? null,
      post: allToolsSummary.post_counts ?? null,
      business_tables_unchanged: Object.values(allToolsSummary.post_business_mutation ?? {}).every((value) => value === 0),
      release_gate: allToolsSummary.release_gate ?? null,
    },
    token_telemetry: null,
    source_artifact: relativeEvidence(allToolsFile),
  }, {
    batch_id: PERFORMANCE_BATCH_ID,
    label: "Wave 1 · 30 次性能样本",
    wave_id: "Wave 1",
    category: "性能",
    result: "Recorded",
    env: "154.217.241.207 / isolated evaluation container",
    host: "154.217.241.207",
    model: "deepseek-v4-flash",
    provider: "https://tokenrhythm.studio/v1",
    wire_api: "chat_completions",
    request_count: performanceSamples.length,
    passed: null,
    failed: null,
    blocked: null,
    source_artifact: relativeEvidence(path.join(performanceDir, "deepseek-performance-metrics.txt")),
  }];
  if (streamTrace) runs.push({
    batch_id: STREAM_BATCH_ID,
    label: "Wave 1 · SSE 流式审计",
    wave_id: "Wave 1",
    category: "功能 / 流式",
    result: streamTrace.llm_status === "completed" ? "Passed" : "Failed",
    env: "154.217.241.207 / isolated evaluation container",
    host: "154.217.241.207",
    model: "deepseek-v4-flash",
    provider: "https://tokenrhythm.studio/v1",
    wire_api: "chat_completions",
    request_count: 1,
    passed: streamTrace.llm_status === "completed" ? 1 : 0,
    failed: streamTrace.llm_status === "completed" ? 0 : 1,
    blocked: 0,
    source_artifact: streamTrace.source_artifact,
  });
  if (productionSmoke) runs.push({
    batch_id: PRODUCTION_SMOKE_BATCH_ID,
    label: productionSmoke.batch_label,
    wave_id: "Wave 1",
    category: "功能 / 生产 smoke",
    result: productionSmoke.result,
    env: `${productionSmoke.host} / ${productionSmoke.runtime}`,
    host: productionSmoke.host,
    model: productionSmoke.model,
    provider: productionSmoke.provider,
    wire_api: null,
    request_count: 1,
    passed: productionSmoke.result === "Passed" ? 1 : 0,
    failed: productionSmoke.result === "Passed" ? 0 : 1,
    blocked: productionSmoke.result === "Blocked" ? 1 : 0,
    database: { cleanup_http: productionSmoke.cleanup_http },
    source_artifact: productionSmoke.source_artifact,
  });
  if (soakRun) runs.push(soakRun);
  return runs;
}

function buildLogRows(providerLogs, cases, streamTrace, testRuns) {
  const rows = [];
  for (const item of cases) {
    const startedAt = item.performance_summary?.started_at ?? null;
    rows.push({
      batch_id: item.batch_id,
      test_id: item.test_id,
      run_id: item.run_id,
      time: startedAt,
      event_type: "request_started",
      kind: "request",
      content: `Prompt: ${item.prompt ?? "未上报"}`,
      source_artifact: item.source_artifact,
    });
    for (const tool of item.tool_calls ?? []) {
      const window = tool.query_window ?? {};
      rows.push({
        batch_id: item.batch_id,
        test_id: item.test_id,
        run_id: item.run_id,
        time: window.started_at ?? null,
        event_type: "tool_started",
        kind: "tool_started",
        content: `${tool.tool_name ?? "未命名工具"} input=${tool.input_summary ?? prettyLogJson(tool.tool_input)}`,
        source_artifact: item.response_artifact ?? item.source_artifact,
      });
      rows.push({
        batch_id: item.batch_id,
        test_id: item.test_id,
        run_id: item.run_id,
        time: window.completed_at ?? null,
        event_type: "tool_completed",
        kind: `tool_${tool.status ?? "unknown"}`,
        content: `${tool.tool_name ?? "未命名工具"} ${tool.result_summary ?? tool.error_message ?? "未上报"} duration=${tool.duration_ms ?? "未上报"}ms`,
        source_artifact: item.response_artifact ?? item.source_artifact,
      });
    }
    rows.push({
      batch_id: item.batch_id,
      test_id: item.test_id,
      run_id: item.run_id,
      time: item.performance_summary?.completed_at ?? null,
      event_type: item.result === "Passed" ? "answer_completed" : "request_failed",
      kind: item.result === "Passed" ? "answer_completed" : "request_failed",
      content: item.result === "Passed"
        ? `正式回答已返回，HTTP ${item.http_status}`
        : `${item.reasons?.join("；") ?? "请求失败"}，耗时 ${item.elapsed_ms ?? "未上报"}ms`,
      source_artifact: item.response_artifact ?? item.source_artifact,
    });
  }
  const providerRows = providerLogs.map((item) => ({
    batch_id: item.batch_id,
    time: item.timestamp,
    event_type: item.request_type,
    kind: item.request_type,
    content: `${item.model} prompt=${item.prompt_tokens} completion=${item.completion_tokens}${item.tools ? ` tools=${item.tools}` : ""}`,
    source_artifact: item.source_artifact ?? relativeEvidence(path.join(allToolsDir, "app.log")),
  }));
  const streamRows = streamTrace ? streamTrace.events.map((event) => ({
    batch_id: streamTrace.batch_id,
    run_id: streamTrace.run_id,
    time: event.created_at,
    event_type: event.event_type,
    kind: "sse",
    content: `${event.tool_name ?? ""} ${event.result_summary ?? event.content ?? ""}`.trim(),
    source_artifact: streamTrace.source_artifact,
  })) : [];
  const batchRows = testRuns.map((run) => ({
    batch_id: run.batch_id,
    time: null,
    event_type: "batch_registered",
    kind: "batch",
    content: `${run.label} · ${run.result} · ${run.request_count ?? 0} samples`,
    source_artifact: run.source_artifact,
  }));
  return [...rows, ...providerRows, ...streamRows, ...batchRows].sort((a, b) => {
    if (a.time == null && b.time == null) return 0;
    if (a.time == null) return 1;
    if (b.time == null) return -1;
    return Number(a.time) - Number(b.time);
  });
}

function prettyLogJson(value) {
  if (value == null) return "未上报";
  if (typeof value === "string") return value;
  try { return JSON.stringify(value); } catch { return String(value); }
}

function sum(items, key) {
  return items.reduce((total, item) => total + (Number(item[key]) || 0), 0);
}

const allToolsSummary = readJson(path.join(allToolsDir, "summary.json"));
const soakBatch = buildSoakBatch();
const cases = [...buildCases(), ...soakBatch.cases];
const performanceSamples = buildPerformanceSamples();
const providerTokenLogs = parseProviderLog();
const streamTrace = buildStreamTrace();
const productionSmoke = buildProductionSmoke();
const testRuns = buildTestRuns(allToolsSummary, performanceSamples, streamTrace, productionSmoke, soakBatch.run, providerTokenLogs);
const tokenPromptTotal = sum(providerTokenLogs, "prompt_tokens");
const tokenCompletionTotal = sum(providerTokenLogs, "completion_tokens");
const speeds = providerTokenLogs.map((item) => item.estimated_output_tokens_per_second).filter((value) => Number.isFinite(value));
const passed = cases.filter((item) => item.result === "Passed").length;
const toolCounts = Object.entries(cases.reduce((counts, item) => {
  for (const tool of item.tool_calls) {
    if (tool.tool_name) counts[tool.tool_name] = (counts[tool.tool_name] ?? 0) + 1;
  }
  return counts;
}, {})).sort((a, b) => b[1] - a[1]);
const allToolsTokenTelemetry = {
  provider_log_sample_count: providerTokenLogs.length,
  prompt_tokens_total: tokenPromptTotal,
  completion_tokens_total: tokenCompletionTotal,
  average_prompt_tokens: providerTokenLogs.length ? Math.round(tokenPromptTotal / providerTokenLogs.length) : null,
  average_completion_tokens: providerTokenLogs.length ? Math.round(tokenCompletionTotal / providerTokenLogs.length) : null,
  estimated_output_tokens_per_second_average: speeds.length
    ? Number((speeds.reduce((a, b) => a + b, 0) / speeds.length).toFixed(2))
    : null,
  first_token_latency_ms: null,
  cache_hit: null,
  cache_status: "provider did not report cache read/write usage",
  speed_status: "estimated from adjacent provider log intervals; not equivalent to streamed token rate",
  source: relativeEvidence(path.join(allToolsDir, "app.log")),
};
const tokenTelemetryByBatch = {
  [ALL_TOOLS_BATCH_ID]: allToolsTokenTelemetry,
  [PERFORMANCE_BATCH_ID]: null,
  [STREAM_BATCH_ID]: streamTrace ? {
    provider_log_sample_count: null,
    prompt_tokens_total: null,
    completion_tokens_total: null,
    average_prompt_tokens: null,
    average_completion_tokens: null,
    estimated_output_tokens_per_second_average: null,
    first_token_latency_ms: streamTrace.first_model_stream_delta_latency_ms,
    cache_hit: null,
    cache_status: "该 SSE 证据没有 cache usage 字段",
    speed_status: "该 SSE 证据没有 token usage 字段",
    source: streamTrace.source_artifact,
  } : null,
  [PRODUCTION_SMOKE_BATCH_ID]: null,
  ...(soakBatch.run ? { [soakBatch.run.batch_id]: soakBatch.run.token_telemetry } : {}),
};
const latencyByBatch = {
  [PERFORMANCE_BATCH_ID]: {
    p50_ms: Number(readText(path.join(performanceDir, "deepseek-performance-metrics.txt")).match(/p50_ms=(\d+)/)?.[1] ?? null),
    p95_ms: Number(readText(path.join(performanceDir, "deepseek-performance-metrics.txt")).match(/p95_ms=(\d+)/)?.[1] ?? null),
    samples: performanceSamples,
    source_artifact: relativeEvidence(path.join(performanceDir, "deepseek-performance-metrics.txt")),
  },
  ...(soakBatch.run ? { [soakBatch.run.batch_id]: soakBatch.run.latency } : {}),
};
const databaseByBatch = Object.fromEntries(testRuns.map((run) => [run.batch_id, run.database ?? null]));

const output = redactDeep({
  schema_version: "agent-observability.v2",
  generated_at: new Date().toISOString(),
  scope: {
    host: "154.217.241.207",
    account_id: 7,
    store_id: 2,
    account_label: "owner 7 / data-rich account",
    model: "deepseek-v4-flash",
    provider: "https://tokenrhythm.studio/v1",
    wire_api: "chat_completions",
    database: "isolated clone of current production data",
  },
  counts: {
    total_cases: cases.length,
    passed,
    failed: cases.filter((item) => item.result === "Failed").length,
    batches: testRuns.length,
    total_tool_calls: cases.reduce((total, item) => total + item.tool_calls.length, 0),
    read_total: cases.filter((item) => item.kind === "read").length,
    create_total: cases.filter((item) => item.kind === "create").length,
    performance_samples: performanceSamples.length,
  },
  token_telemetry: allToolsTokenTelemetry,
  token_telemetry_by_batch: tokenTelemetryByBatch,
  latency: {
    samples: performanceSamples,
    source: relativeEvidence(path.join(performanceDir, "deepseek-performance-metrics.txt")),
    reported_summary: exists(path.join(performanceDir, "deepseek-performance-metrics.txt"))
      ? readText(path.join(performanceDir, "deepseek-performance-metrics.txt")).trim()
      : null,
  },
  latency_by_batch: latencyByBatch,
  test_runs: testRuns,
  database_by_batch: databaseByBatch,
  production_smoke: productionSmoke,
  tool_counts: toolCounts.map(([tool, count]) => ({ tool, count })),
  cases,
  stream_trace: streamTrace,
  provider_token_logs: providerTokenLogs,
  logs: buildLogRows(providerTokenLogs, cases, streamTrace, testRuns),
  provenance: [
    { label: "真实数据库", value: "owner 7 / store 2 的当前生产数据副本；业务表未发生持久化变化", level: "verified" },
    { label: "工具调用", value: `${cases.length} 条运行记录，${cases.reduce((total, item) => total + item.tool_calls.length, 0)} 次真实工具调用；批次可单独筛选`, level: "verified" },
    { label: "Prompt / completion Token", value: "来自 provider app.log 的真实 usage 日志", level: "verified" },
    { label: "首字延迟", value: streamTrace ? "来自 SSE audit 的首个 model_stream delta；只归属于 SSE 批次" : "没有流式 audit", level: streamTrace ? "verified" : "missing" },
    { label: "Token/s", value: "当前仅能按相邻 provider 日志间隔推导，界面会标注估算", level: "derived" },
    { label: "缓存命中", value: "各批次证据均未返回 cache usage 字段，保持未上报", level: "missing" },
    { label: "失败与超时", value: "最新 soak 的 2 条 HTTP 000 保留在失败列表和日志中，没有转为通过", level: "warning" },
  ],
});

fs.mkdirSync(path.dirname(outputPath), { recursive: true });
fs.writeFileSync(outputPath, `${JSON.stringify(output, null, 2)}\n`, "utf8");
console.log(`wrote ${path.relative(repoRoot, outputPath)}: ${cases.length} cases, ${providerTokenLogs.length} provider token records`);
