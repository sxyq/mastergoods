# Server infrastructure/ai 模块分析

- 对应源码目录：`src/main/java/com/zhihuiji/backend/infrastructure/ai`
- 关键源码：`LongCatAnthropicClient.java`

## 状态图例

- `新版已做`
- `新版待做`
- `旧版存在新版未做`
- `新版需要去掉`
- `需重构`
- `待验证`

## 状态表

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| AI LLM 客户端基础设施 | 新版已做 | 旧版无 AI 助手域 | 保持现有 AI 基础调用能力 | `LongCatAnthropicClient.java` 已存在 | 支撑 agent 服务 |
| `RestClient.Builder` 依赖装配 | 新版已做 | 首版未显式提供 HTTP 客户端 builder Bean | 保证 AI 客户端装配稳定、后续可复用 | 已由 `HttpClientConfig` 提供 `restClientBuilder()` | 与 AI client 基础设施保持分层 |
| 会话缓存、结构化结果与 `/v2/agent` 运行时 | 新版待做 | 旧版无该域 | 让 AI 域更完整可扩展 | 当前仍是首版客户端实现 | 后续与 agent domain 一起扩展 |
