# 2026-06-08 Android Device AI Agent Evidence

Status: partial-pass for Android UI and rule-summary agent flow.

## Scope

- Device: physical Android device `d715a3a4`, screen override `1080x2400`, density `420`.
- App package: `com.zhihuiji.app`.
- Backend path: device used `adb reverse tcp:18080 tcp:18080` and app base URL `http://127.0.0.1:18080/`.
- Prompt used for device QA: `recent sales purchase finance business overview`.
- Backend run id: `aa7c517e-f2b1-4e1d-ace6-be1e2f613bc0`.

## Evidence Map

- `03-ai-home.png` / `03-ai-home-ui.xml` / `03-ai-home-ui-summary.txt`: AI home first screen is clean. It shows greeting and start-entry copy, with no KPI cards, report dashboard, sales chart, or default report data.
- `06-streaming.png` / `06-streaming-ui.xml` / `06-streaming-ui-summary.txt`: chat request enters the streaming state instead of showing only an instant final reply.
- `07-chat-result-top.png` / `07-chat-result-top-ui.xml` / `07-chat-result-top-ui-summary.txt`: Markdown-like answer renders as readable paragraphs and numbered points, followed by `结构化结果 · 14 个结果块`.
- `08-result-blocks-mid.png` / `09-result-blocks-lower.png` / `10-result-blocks-charts.png` / `11-result-blocks-sales-evidence.png`: structured result blocks render KPI grids, bar chart, table, donut/empty state, sales overview, and the `近7天销售趋势` line chart with visible date labels.
- `13-evidence-card-runtrace.png` / `14-runtrace-expanded.png` / `15-runtrace-tools.png`: evidence card and expanded RunTrace expose tool sources, audit id, trace id, log ref, mode, model status, planner, inputs, range, duration, and returned counts.
- `18-logcat-final.txt`: app issued `POST http://127.0.0.1:18080/v2/agent/chat/stream` and received HTTP 200 during the captured flow.
- `22-device-run-audit.json`: backend audit status is `completed`, mode is `tool_query_rule_summary`, `llm_status` is `disabled`, `plan_source` is `keyword`, `tool_count` is `4`, and `event_count` is `28`.

## Confirmed Behavior

- The AI assistant initial screen stays clean; report-like data remains outside the AI home.
- The Android chat surface renders the backend answer and structured result blocks, including chart-like blocks generated from server result data.
- The RunTrace UI is connected to real backend audit metadata, not a static mock.
- English business keywords can drive the same real tool-query path used by device QA: `supplier_payable_lookup`, `purchase_order_lookup`, `finance_record_lookup`, and `sales_overview_lookup`.

## Remaining Risks

- This run used `AGENT_LLM_ENABLED=false`, so it proves rule-summary fallback plus streaming endpoint behavior, not provider-backed `model_stream` deltas.
- `16-gfxinfo-after-flow.txt` is a baseline performance snapshot, not a pass: total frames `2124`, janky frames `511 (24.06%)`, p50 `23ms`, p90 `38ms`, p95 `42ms`, p99 `53ms`, slow UI thread `316`.
- This package focuses on AI Agent Android flow evidence; it does not prove every app screen is one-to-one with the design reference.
