# Agent 测试说明

## 文档信息

| 字段 | 内容 |
|---|---|
| 文档类型 | 测试验收 |
| 当前状态 | 已完成（计划与台账）；运行链路 Blocked |
| 适用端 | Agent |
| 依据源码 | `Code/backend/src/test/java/.../application/service/v2/` |
| 依据测试 | `testing/Agent/功能测试/TEST_PLAN.md`、`testing/Agent/单元测试/TEST_PLAN.md`、`testing/Agent/性能测试/TEST_PLAN.md`、`testing/Agent/审计/` |
| 依据证据 | `testing/.artifacts/2026-08-18-8220-current-baseline/current-8220-baseline.md` |
| 最后核对 | 2026-08-20 |

## 一、测试范围

`testing/Agent/功能测试/TEST_PLAN.md` 明确：本轮主执行覆盖**后端链 + 安卓链**，不展开 Web/iOS 主流程。

## 二、Wave 划分（Agent）

| Wave | 内容 |
|---|---|
| Wave 0 | 确认已有会话/消息/run audit/draft 可读；Agent 页可访问；接口可调 |
| Wave 1 | 复用 live data：conversation list/detail/reload、message history、draft list、run audit readback |
| Wave 2 | 增量写入：new conversation、send text、cancel run、draft create/confirm/cancel、stream retry |
| Wave 3 | multimodal、image upload、text-to-image、long conversation continuity |

## 三、单元测试覆盖（源码测试）

| 组件 | 测试 |
|---|---|
| 主编排 | `V2AgentAiServiceTest.java` |
| 会话 | `V2AgentConversationServiceTest.java` |
| 草稿 | `AgentDraftConfirmServiceTest.java` |
| 提示词 | `AgentPromptCatalogTest.java` |
| 回答 | `AnswerSynthesizerTest.java` |
| 安全 | `SafetyGuardTest.java` |
| 规划 | `ToolPlannerTest.java`、`ToolInvocationIdentityTest.java` |
| 注册 | `ToolRegistryTest.java` |
| 只读工具 | `CustomerProfileLookupToolTest`、`ProductCatalogLookupToolTest`、`PurchaseOrderLookupToolTest`、`PurchaseTrackingLookupToolTest`、`ReportQueryToolTest`、`GeneratePosterPromptToolTest` 等 |

## 四、当前状态（8220 基线）

| 范围 | 状态 |
|---|---|
| Provider 直连 | Passed |
| 生产 Agent 鉴权与数据基线 | Blocked |
| Wave 1/2/3 | Blocked（前置未满足） |

## 五、专项验收

- 流式对话验收：`05_测试与验收/流式对话验收说明.md`
- 历史会话验收：`05_测试与验收/历史会话验收说明.md`
- 图表结果验收：`05_测试与验收/图表结果验收说明.md`

## 对应实现

- 后端代码：`application/service/v2/agent/`
- Android 代码：`feature/agent/`、`core/network/AgentSseClient.kt`
- iOS 代码：待验证
- Web 代码：待验证
- Agent 代码：`application/service/v2/agent/`

## 对应接口

- 接口路径：`/v2/agent/*`
- 请求模型：`V2AgentDtos`
- 响应模型：`V2AgentDtos`
- SSE 事件：`SseStreamEmitter`

## 对应测试

- 单元测试：`Code/backend/src/test/java/.../application/service/v2/`
- 功能测试：`testing/Agent/功能测试/TEST_PLAN.md`
- 性能测试：`testing/Agent/性能测试/TEST_PLAN.md`
- 审计：`testing/Agent/审计/`

## 当前限制

- 未完成内容：Web / iOS Agent 主流程测试
- Blocked 内容：8220 生产 Agent 链路、Wave 1/2/3
- Deferred 内容：多模态 Wave 3
- historical-only 内容：154 环境 Agent 测试证据
