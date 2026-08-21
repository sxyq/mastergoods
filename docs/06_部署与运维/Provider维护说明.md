# Provider 维护说明

## 文档信息

| 字段 | 内容 |
|---|---|
| 文档类型 | 运维 |
| 当前状态 | 已完成 |
| 适用端 | 后端 |
| 依据源码 | `application-prod.yml`（agent.llm.*）、`infrastructure/config/AgentLlmProperties.java` |
| 依据测试 | 8220 Provider 直连探针 |
| 依据证据 | `testing/.artifacts/2026-08-18-8220-current-baseline/current-8220-baseline.md` |
| 最后核对 | 2026-08-20 |

## 一、Provider 配置

| 配置 | 值 | env |
|---|---|---|
| 模型 | gpt-5.6-luna | AGENT_LLM_MODEL |
| Base URL | https://oneapi.sxyq27.online/v1 | AGENT_LLM_BASE_URL |
| Wire API | chat_completions | AGENT_LLM_WIRE_API |
| 认证 | OpenAI auth | AGENT_LLM_REQUIRES_OPENAI_AUTH |
| Key | 仅运行时 Secret | AGENT_LLM_API_KEY |
| max-tokens / temperature | 4096 / 0.2 | AGENT_LLM_MAX_TOKENS / AGENT_LLM_TEMPERATURE |
| thinking | true / 2048 | AGENT_LLM_ENABLE_THINKING / AGENT_LLM_THINKING_BUDGET |

## 二、维护动作

1. 修改模型/URL：通过容器环境变量覆盖，不修改代码。
2. 变更后必须执行直连探针（非流式 / SSE / tool_choice=auto）。
3. Key 变更：更新运行时 Secret，不落仓库。
4. 生图 Provider：当前未配置（Deferred）。

## 三、切换 Wire API 注意事项

- 当前 `chat_completions`；154 末期曾用 `responses`（historical-only）。
- 切换后需重跑 Provider 探针与 `function_call_output` 续轮验证（当前续轮在 8220 待独立验证，#15）。

## 对应实现

- 后端代码：`AgentLlmProperties.java`、`LongCatAnthropicClient.java`
- Android/iOS/Web 代码：不适用
- Agent 代码：`V2AgentAiService`

## 对应接口

- 接口路径：Agent 对话接口
- 请求模型：无
- 响应模型：无
- SSE 事件：无

## 对应测试

- 探针：`testing/.artifacts/2026-07-19-agent-llm-live-recheck/gpt-5.6-luna-provider-probe.md`
- 基线：8220 基线（Provider 直连 200）

## 当前限制

- 未完成内容：原生 function_call_output 续轮验证
- Blocked 内容：无
- Deferred 内容：生图 Provider
- historical-only 内容：154 Provider 配置
