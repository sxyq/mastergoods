# Agent 单元测试全覆盖方案

## Objective

从跨端视角为 Agent 建立专项函数覆盖体系，覆盖后端规划执行、Android 状态管理、Web 解析逻辑和 iOS 交互状态。

## Scope

### Backend

- `V2AgentAiService`
- `V2AgentConversationService`
- `RunAuditService`
- `SseStreamEmitter`
- `ToolPlanner`
- `AnswerSynthesizer`
- `ToolRegistry`
- all `agent/tool/*`
- `LongCatAnthropicClient`
- `AgentImageService`

### Android

- `AgentChatViewModel`
- `AgentV2Repository`
- `AgentSseClient`
- Agent model serializers
- result block rendering helpers

### Web

- agent entities
- agent page helpers
- event and evidence formatting logic

### iOS

- agent view state and history replay logic

## Unit Coverage Rules

Each Agent function family must cover:

1. happy path
2. blocked path
3. fallback path
4. cancellation path
5. parsing failure path
6. empty-result path
7. evidence/audit emission path

## Tool Coverage Matrix

Every registered Agent tool must have:

1. request param decode test
2. owner-scope enforcement test
3. result shape test
4. insufficient-result branch test if supported

## Deliverables

- `agent-function-coverage-ledger.csv`
- tool coverage matrix
- provider response fixture set

## Exit Criteria

1. Every Agent production class is mapped.
2. Every Agent tool is mapped.
3. Every cross-platform Agent parser has at least one positive and one negative test.
