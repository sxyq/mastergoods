# 2026-07-03 Codex Provider 原生工具规划本地收口证据

## 目标

- 验证当前仓库是否已经把真实 provider 的非流式规划/总结链路接回可用状态
- 验证 `/v2/agent/chat` 不再停留在 `llm_status=failed_or_empty` + `plan_source=keyword_fallback`
- 记录仍未闭口的真实工具失败项，避免把本地联调误报成“完美通过”

## 本轮代码修复

1. `LongCatAnthropicClient`
   - OpenAI 风格非流式请求改为“先读取原始字符串，再自行 JSON 解析”，不再受错误 `Content-Type` 影响。
   - OpenAI 风格 endpoint 改为基于 `normalizedBaseUrl` 的绝对地址拼接，避免 `RestClient baseUrl=/v1` + 相对路径时把 `/v1` 路径段丢掉，误打到网关首页 HTML。
   - `responses + tools` 失败时，会自动回退到已经验证可用的 `chat/completions + tools`。
2. `ToolPlanner`
   - 原生工具规划首次未命中时，会基于关键词候选工具缩小集合后再次尝试原生 tool calling。

## 本地运行环境

- Base URL: `http://127.0.0.1:18081`
- Spring profile: `local`
- LLM config source: 当前 `~/.codex/config.toml`
- Provider model: `gpt-5.4-mini`
- Token source: 本地注册账号 token

## 本次真实联调结果

- 请求接口：`POST /v2/agent/chat`
- 问题：`最近库存情况怎么样`
- 运行结果：
  - `run_id`: `efeaa856-e5a2-4274-aa0a-60e8eefe876e`
  - `mode`: `tool_query_llm_synthesized`
  - `llm_status`: `available`
  - `plan_source`: `react_iterated`
  - `plan_summary`: `模型通过原生 Function Calling 选择工具 + 迭代补充(2)：inventory_snapshot_lookup、inventory_low_stock_lookup、inventory_panorama_lookup、inventory_ledger_lookup`

## 结论

1. “当前 provider 只能做真实流式回答，不能做本地真实原生工具规划”这一条已被当前工作树推翻。
2. `/v2/agent/chat` 已恢复到真实模型参与的本地可用状态，不再是 `failed_or_empty`。
3. 本地非流式链路已经能证明：
   - 真实模型可参与工具规划
   - 真实模型可参与结果综合
   - 规划结果可进入迭代补充阶段

## 仍未闭口项

1. 本次 run 中 `inventory_panorama_lookup` 真实失败 2 次，错误为 `UnsupportedOperationException: 工具执行失败`。
2. 这说明“真实模型规划已可用”不等于“所有工具链已经完美通过”。
3. Android 真机配对截图 / UI tree / logcat 仍缺。
4. 当前证据仍是 local profile，本轮未新增生产或准生产 profile 证据。
