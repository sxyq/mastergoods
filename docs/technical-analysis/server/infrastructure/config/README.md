# Server infrastructure/config 模块分析

- 对应源码目录：`src/main/java/com/zhihuiji/backend/infrastructure/config`
- 关键源码：
  - `AgentLlmProperties.java`
  - `AgentTaskConfig.java`
  - `HttpClientConfig.java`
  - `LocalDemoDataInitializer.java`
  - `SecurityConfig.java`

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
| 首版配置类集合 | 新版已做 | 旧版无当前 Spring 配置层 | 支撑现有运行时、安全、AI、演示数据 | 当前 5 个配置类已存在 | 是后端运行基础 |
| `HttpClientConfig` | 新版已做 | 旧版无显式 `RestClient.Builder` Bean | 为 AI 基础设施和后续外部 HTTP 客户端提供统一构造入口 | 已显式提供 `restClientBuilder()` | 已用于修复 AI 客户端装配 |
| `AgentTaskConfig` 执行器契约 | 新版已做 | 首版更偏宽松线程池配置 | 让 agent 任务执行器与 service 注入类型严格对齐 | `agentTaskExecutor` 现返回 `ExecutorService`，并声明 `destroyMethod = shutdown` | 已消除构造注入不匹配 |
| `/v2` 环境与 owner 策略配置 | 新版待做 | 旧版无 `/v2` | 支持新版接口分层与多账号隔离配置 | 当前仍以首版运行配置为主 | 等后端重构时落地 |
| 宽松联调导向配置 | 需重构 | 首版更重开发效率 | 收紧为可上线策略，同时保留 `/v1` 兼容 | 当前部分配置仍偏首版过渡形态 | 文档先行 |
