# AI 层技术分析

> 路径: `src/main/java/com/zhihuiji/backend/infrastructure/ai/`

本层封装与 LLM（大语言模型）的交互逻辑。

---

## LongCatAnthropicClient

- **文件**: `LongCatAnthropicClient.java`
- **注解**: `@Component`
- **条件**: `@ConditionalOnProperty(name = "agent.llm.enabled", havingValue = "true")`
- **作用**: ✏️ Anthropic Claude API 客户端封装，使用 Spring RestClient + SimpleClientHttpRequestFactory 发送请求。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `properties` | `AgentLlmProperties` | LLM 配置属性 | 无 |
| ✏️ `restClient` | ✏️ `RestClient` | ✏️ Spring REST 客户端 | 无 |
| `objectMapper` | `ObjectMapper` | JSON 序列化 | 无 |

### 函数

| 函数 | 作用 | 修改建议 |
|------|------|----------|
| ✏️ `createJsonMessage(userMessage, systemPrompt)` | ✏️ 发送聊天请求，返回 JSON 响应 | 无 |
| `chat(userMessage, systemPrompt)` | 发送聊天请求，返回文本响应 | 无 |
| `buildMessages(userMessage)` | 构建消息体 | 无 |
| `parseContent(responseBody)` | 解析响应内容 | 无 |
| `parseJsonContent(responseBody)` | 解析 JSON 响应内容 | 提取第一个 content block 的 text |

### HTTP 请求细节

| 参数 | 值 | 修改建议 |
|------|------|----------|
| ✏️ API 版本 | ✏️ `2023-06-01`，通过 `${AGENT_LLM_ANTHROPIC_VERSION}` 可配置 | ✏️ 已可通过环境变量配置，非硬编码；但仍应检查默认值是否为最新版本 |
| ✏️ 超时设置 | ✏️ SimpleClientHttpRequestFactory 已配置 connectTimeout=10s, readTimeout=25s | ✏️ 已有超时配置，但 LLM 调用可能需更长时间，readTimeout 可考虑增大 |
| 重试 | 无 | 应增加指数退避重试 |
| 请求日志 | 无 | 应增加请求/响应日志（脱敏后） |

### 修改建议

1. ✏️ **超时配置已有**: SimpleClientHttpRequestFactory 已配置 connectTimeout=10s、readTimeout=25s，非 OkHttpClient 默认超时。但 LLM 调用可能需更长时间，readTimeout 可考虑增大至 60s。
2. **无重试机制**: 网络抖动或 API 限流时直接失败，应增加重试（指数退避 + 最大重试次数）。
3. **无速率限制**: 未实现客户端速率限制，可能触发 API 限流。
4. **无 Token 统计**: 未记录 prompt_tokens 和 completion_tokens，无法监控成本。
5. **错误处理不完整**: 仅抛出 RuntimeException，应区分网络错误、API 错误和解析错误。
6. ✏️ **单模型配置**: 模型名通过 `AgentLlmProperties.model` 可配置，非硬编码；但仅支持单一模型，无法按任务类型切换不同模型（如快速任务用 Haiku、复杂任务用 Sonnet）。
7. **建议引入 SDK**: 考虑使用官方 `anthropic-sdk-java` 替代手写 HTTP 客户端，减少维护成本。
