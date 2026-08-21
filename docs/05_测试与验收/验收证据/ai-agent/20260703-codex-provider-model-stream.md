# 2026-07-03 Codex Provider `model_stream` 本地联调证据

## 范围

- 目标：验证当前仓库是否已接入真实 AI provider，并产出真实 `delta_source=model_stream` SSE 证据
- 后端：本地 `SPRING_PROFILES_ACTIVE=local`，端口 `18081`
- 模型配置来源：`~/.codex/config.toml`
- 当前模型：`gpt-5.4-mini`
- 当前 wire API：`chat_completions`
- 配置导出脚本：`tools/export_codex_llm_env.sh`

## 证据入口

- 原始 SSE 包：`docs/acceptance-evidence/ai-agent/20260703-2250-41300-3xvunYP8-manual/`
- 关键文件：
  - `00-env.md`
  - `01-http-headers.txt`
  - `02-raw-sse.log`

## 关键运行事实

- run id：`e834a5d3-7c75-486b-ad7b-14ac00fb24f3`
- audit id：`e834a5d3-7c75-486b-ad7b-14ac00fb24f3:audit`
- trace id：`e834a5d3-7c75-486b-ad7b-14ac00fb24f3:trace`
- 最终模式：`tool_query_llm_streamed`
- 最终 `llm_status`：`streaming`
- 最终 `plan_source`：`keyword_fallback`
- 审计 `event_count`：`34`
- 审计丢失：`audit_lossy=false`

## 已确认通过的点

1. 接口返回真实 SSE：
   - `01-http-headers.txt` 为 `HTTP/1.1 200`
   - `Content-Type: text/event-stream`
2. 存在真实 provider 模型增量：
   - `02-raw-sse.log` 第一个可见回答增量已出现 `delta_source":"model_stream"`
3. 结构化结果没有抢在第一段可见回答前：
   - `seq=11` 为首个 `answer_delta(model_stream)`
   - `seq=12` 才开始发第一个 `result_block`
4. 审计接口与 SSE 一致：
   - `/v2/agent/runs/{runId}/audit` 返回 `mode=tool_query_llm_streamed`
   - `llm_status=streaming`
   - `event_count=34`
   - `emitted_event_count=34`

## 这次关闭了什么阻塞

- “当前仓库尚无真实 provider `model_stream` 本地证据”这一条已关闭
- “只能证明规则摘要降级，不能证明真实模型流式”这一条已关闭

## 仍未关闭的点

1. `plan_source` 仍是 `keyword_fallback`
   - 说明真实模型流式已接通
   - 但 provider 原生工具规划 / JSON 规划兼容性仍未稳定闭环，不能宣称已达到完美 agentic planning
2. 这次只有本地接口证据
   - 还缺与当前安装包同轮次配对的 Android 截图、UI tree、录屏与操作证据
3. 还缺 provider 异常分支证据
   - 包括 empty stream / interrupted stream / tool-calling 失败回退
4. 还缺生产或准生产 profile 证据
   - 当前结论仅覆盖 local profile

## 附加说明

- 同轮手工探测已确认 `chat/completions` 普通调用可返回 `200`
- `responses` 普通调用也可返回 `200`
- 但带 tools 的 provider 兼容性仍不应直接按“已全部完成”记账
