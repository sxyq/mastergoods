# Provider 兼容性设计

## 文档信息

| 字段 | 内容 |
|---|---|
| 文档类型 | 系统设计 |
| 当前状态 | 已完成 |
| 适用端 | 后端 / Agent |
| 依据源码 | `infrastructure/ai/LongCatAnthropicClient.java`、`infrastructure/config/AgentLlmProperties.java`、`application/service/v2/agent/component/AnswerSynthesizer.java` |
| 依据测试 | `V2AgentAiServiceTest.java`、`AnswerSynthesizerTest.java`、`ToolPlannerTest.java` |
| 依据证据 | `testing/.artifacts/2026-08-18-8220-current-baseline/current-8220-baseline.md`、`testing/.artifacts/2026-07-19-agent-llm-live-recheck/` |
| 最后核对 | 2026-08-20 |

## 一、Provider 配置兼容面

| 配置 | 值（8220） | 说明 |
|---|---|---|
| AGENT_LLM_ENABLED | true（运行时） | 默认 false（application.yml），由运行时环境变量覆盖 |
| AGENT_LLM_MODEL | gpt-5.6-luna | 模型 |
| AGENT_LLM_BASE_URL | https://oneapi.sxyq27.online/v1 | Base URL |
| AGENT_LLM_WIRE_API | chat_completions | Wire API（历史 154 曾用 responses） |
| AGENT_LLM_REQUIRES_OPENAI_AUTH | true | OpenAI 认证头 |
| AGENT_LLM_ANTHROPIC_VERSION | 2023-06-01 | Anthropic 兼容版本头 |
| AGENT_LLM_MAX_TOKENS / TEMPERATURE | 4096 / 0.2 | 生成参数 |
| AGENT_LLM_ENABLE_THINKING / THINKING_BUDGET | true / 2048 | 思考预算 |

## 二、调用面（LongCatAnthropicClient）

| 方法 | 用途 |
|---|---|
| `isConfigured()` | Provider 是否启用 |
| `createJsonMessage()` | 非流式 JSON 回答 |
| `streamTextMessage()` | 流式文本回答（answer_delta） |
| 原生工具续轮 | `model_native_continuation` 分支（function_call_output） |
| `cancelStream(runId)` | 取消进行中的流 |

## 三、wire_api 兼容

- 当前：`chat_completions`（8220）。
- 历史：154 曾用 `responses`（`testing/README.md` Historical Snapshot）。
- 兼容策略：`agent.llm.wire-api` 配置化；`LongCatAnthropicClient` 按配置组织请求。
- 已知边界：原生 `function_call_output` 续轮在 154 探针 HTTP 400；当前应用侧工具编排 + 真实模型重请求路径保证回答真实（已知问题 #15）。

## 四、安全与敏感信息

- API Key 只从运行时 Secret 读取（`AGENT_LLM_API_KEY` env），不落仓库、不回显。
- 工具参数 `input_summary` 与审计字段使用脱敏摘要（`SseStreamEmitter.toolInputSummary`）。

## 五、Provider 兼容矩阵

| 能力 | 当前状态 |
|---|---|
| 非流式 chat_completions | 8220 直连 HTTP 200（Passed） |
| SSE chat_completions | 8220 直连 HTTP 200、3 chunk、1 DONE（Passed） |
| tool_choice=auto | 8220 直连 HTTP 200（Passed） |
| 原生 function_call_output 续轮 | 154 探针 HTTP 400；需 8220 独立验证（待验证） |
| 多模态 / 生图 | Deferred（Provider 未配置） |

## 对应实现

- 后端代码：`LongCatAnthropicClient.java`、`AgentLlmProperties.java`、`AnswerSynthesizer.java`
- Android 代码：不适用
- iOS 代码：不适用
- Web 代码：不适用
- Agent 代码：`V2AgentAiService`、`AnswerSynthesizer`

## 对应接口

- 接口路径：`POST /v2/agent/chat`、`POST /v2/agent/chat/stream`
- 请求模型：`AgentChatRequest`
- 响应模型：`AgentChatResponse`
- SSE 事件：`answer_delta`、`answer_completed`、`error`

## 对应测试

- 单元测试：`V2AgentAiServiceTest.java`、`AnswerSynthesizerTest.java`
- Provider 探针：`testing/.artifacts/2026-07-19-agent-llm-live-recheck/gpt-5.6-luna-provider-probe.md`
- 基线：`testing/.artifacts/2026-08-18-8220-current-baseline/current-8220-baseline.md`

## 当前限制

- 未完成内容：原生 function_call_output 续轮在 8220 独立验证
- Blocked 内容：无
- Deferred 内容：多模态、生图 Provider
- historical-only 内容：154 环境 wire_api=responses 配置与探针
