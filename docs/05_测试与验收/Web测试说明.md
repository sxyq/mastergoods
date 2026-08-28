# Web 测试说明

## 文档信息

| 字段 | 内容 |
|---|---|
| 文档类型 | 测试验收 |
| 当前状态 | 待验证 |
| 适用端 | Web |
| 依据源码 | `Code/frontend/web/src/` |
| 依据测试 | `testing/web/功能测试/TEST_PLAN.md`、`testing/web/单元测试/TEST_PLAN.md`、`testing/web/性能测试/TEST_PLAN.md` |
| 依据证据 | 无 8220 运行证据 |
| 最后核对 | 2026-08-20 |

## 一、测试覆盖

- `testing/web/功能测试/TEST_PLAN.md` + `functional_feature_matrix.csv`。
- `testing/web/单元测试/TEST_PLAN.md` + `unit_function_coverage.csv`。
- `testing/web/性能测试/TEST_PLAN.md` + `performance_scope_matrix.csv`。
- `testing/web/审计/` + `audit_function_ledger.csv`。
- `testing/web/破坏性逆向安全测试/TEST_PLAN.md` + `reverse_attack_matrix.csv`。

## 二、当前状态

- 源码存在（AgentPage、agent-stream、routes）。
- Agent 主流程本轮未展开测试（`testing/Agent/客户端/TEST_PLAN.md`：本轮不展开 Web/iOS 主流程）。
- 无 8220 运行证据——所有 Web Agent 行为标记待验证。

## 三、历史证据

- `docs/05_测试与验收/验收证据/b11/web/20260630-1351-web-id-entityid-build.md`：Web id/entityid 构建历史证据（154 时期，historical-only）。

## 对应实现

- Web 代码：`Code/frontend/web/src/`
- 后端代码：不适用
- Android 代码：不适用
- iOS 代码：不适用
- Agent 代码：`pages/agent/AgentPage.vue`、`shared/api/agent-stream.ts`

## 对应接口

- 接口路径：`/v2/*`
- 请求模型：`shared/api/contracts.ts`
- 响应模型：同上
- SSE 事件：`agent-stream.ts`

## 对应测试

- 单元测试：`testing/web/单元测试/TEST_PLAN.md`
- 功能测试：`testing/web/功能测试/TEST_PLAN.md`
- 性能测试：`testing/web/性能测试/TEST_PLAN.md`

## 当前限制

- 未完成内容：Agent 主流程测试
- Blocked 内容：无
- Deferred 内容：多模态
- historical-only 内容：154 时期 Web 构建证据
