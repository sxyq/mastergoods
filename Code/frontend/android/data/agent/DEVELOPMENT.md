# data/agent 当前边界

旧版 V1 Agent 数据层已删除，当前由 `AgentV2Repository` 承接会话、消息、草稿、工作台和流式聊天。

## 仍需验证

- 非流式 chat、流式 SSE、cancel、audit、draft 必须以真实后端返回验证。
- provider 多轮工具结果回灌和最终正式回答仍未完全闭环。
- 工具返回的结构化数据必须来自当前账户数据库，不能由本地 fake 或默认数据补齐。

## 验收入口

- 源码：`data/agent/src/main/`
- 单元测试：`data/agent/src/test/`
- 测试台账：`testing/Agent/`
