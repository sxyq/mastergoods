# Provider 运行配置

## 文档信息

| 字段 | 内容 |
|---|---|
| 文档类型 | 系统设计 |
| 当前状态 | 已完成 |
| 适用端 | 后端 |
| 依据源码 | `application.yml`、`application-prod.yml`（agent.llm.*）、`infrastructure/config/AgentLlmProperties.java`、`AgentImageProperties.java`、`HttpClientConfig.java` |
| 依据测试 | 8220 Provider 直连探针 |
| 依据证据 | `testing/.artifacts/2026-08-18-8220-current-baseline/current-8220-baseline.md` |
| 最后核对 | 2026-08-20 |

## 一、运行配置

| 配置 | 值（8220） | env |
|---|---|---|
| AGENT_LLM_ENABLED | true（运行时） | `AGENT_LLM_ENABLED`（默认 false） |
| 模型 | gpt-5.6-luna | `AGENT_LLM_MODEL` |
| Base URL | `<LLM_PROVIDER_BASE_URL>` | `AGENT_LLM_BASE_URL` |
| Wire API | chat_completions | `AGENT_LLM_WIRE_API` |
| OpenAI 认证 | 已启用 | `AGENT_LLM_REQUIRES_OPENAI_AUTH=true` |
| API Key | `<LLM_PROVIDER_API_KEY>`，仅运行时 Secret | `AGENT_LLM_API_KEY`（不落文档） |
| max-tokens | 4096 | `AGENT_LLM_MAX_TOKENS` |
| temperature | 0.2 | `AGENT_LLM_TEMPERATURE` |
| enable-thinking | true | `AGENT_LLM_ENABLE_THINKING` |
| thinking-budget | 2048 | `AGENT_LLM_THINKING_BUDGET` |

### 生图配置（占位符）

| 配置键 | 占位值 | 运行约束 |
|---|---|---|
| `agent.image.base-url` | `<IMAGE_PROVIDER_BASE_URL>` | 只从运行时配置读取 |
| `agent.image.api-key` | `<IMAGE_PROVIDER_API_KEY>` | 只从运行时 Secret 注入，禁止日志回显 |
| `agent.image.model` | `<IMAGE_PROVIDER_MODEL>` | 未配置时生图确认失败并保留可重试草稿 |
| HTTP 超时 | 连接 10000ms / 读取 120000ms | 取消和超时必须释放连接与临时资源 |

## 二、直连验证结果（8220 基线）

- 非流式 chat/completions：HTTP 200。
- SSE chat/completions：HTTP 200，3 个数据 chunk + 1 个 `[DONE]`。
- tool_choice=auto：HTTP 200。
- LLM 和生图 key 只从运行时 Secret 读取，本文不保存不回显；URL 仅保留占位符。

## 三、切换与回滚

- 配置修改通过环境变量覆盖（容器启动注入），不修改应用代码。
- 回滚：`sxyq27-zhj-api:rollback-20260818` 镜像保留。

## 对应实现

- 后端代码：`application.yml`、`application-prod.yml`、`AgentLlmProperties.java`
- Android/iOS/Web 代码：不适用
- Agent 代码：`LongCatAnthropicClient`

## 对应接口

- 接口路径：Agent 对话接口（依赖 Provider）
- 独立生图接口：`POST /v2/agent/images/generate`；Agent `image_generate` 工具由对话链先创建草稿，确认后才调用该 Service。
- 请求模型：无
- 响应模型：无
- SSE 事件：无

## 对应测试

- Provider 探针：`testing/.artifacts/2026-07-19-agent-llm-live-recheck/gpt-5.6-luna-provider-probe.md`
- 基线：`testing/.artifacts/2026-08-18-8220-current-baseline/current-8220-baseline.md`

## 当前限制

- 未完成内容：原生 function_call_output 续轮 8220 独立验证
- Blocked 内容：无
- Deferred 内容：生图 Provider、真实资源消耗、结果 URL/b64_json 安全处理和客户端确认展示（未配置/未验证）
- historical-only 内容：154 环境 Provider 配置（wire_api=responses）
